package cf.wayzer

import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import arc.util.Strings
import arc.util.serialization.Json
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import cf.wayzer.util.reflectDelegate
import mindustry.Vars
import mindustry.content.Bullets
import mindustry.ctype.Content
import mindustry.ctype.ContentType
import mindustry.entities.bullet.BulletType
import mindustry.io.JsonIO
import mindustry.mod.ContentParser
import mindustry.mod.Mods
import mindustry.type.Item
import mindustry.world.consumers.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object ContentsPatcher {
    private val Mods.parser: ContentParser by reflectDelegate()
    private val ContentParser.parser: Json by reflectDelegate()
    private val json by lazy { Vars.mods.parser.parser }
    private val bulletMap by lazy {
        Bullets::class.java.fields
            .filter { it.type == BulletType::class.java }
            .associate { it.name to it.get(null) as BulletType }
    }


    private fun findContent(type: ContentType, name: String): Content {
        return when (type) {
            ContentType.bullet -> bulletMap[Strings.kebabToCamel(name)]
            else -> Vars.content.getByName(type, Strings.camelToKebab(name))
        } ?: error("Not found $type : $name")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> readType(cls: Class<T>, jsonValue: JsonValue): T {
        return when (cls) {
            Consumers::class.java -> Consumers().apply {
                jsonValue.forEach { child ->
                    when (child.name) {
                        "item" -> item(findContent(ContentType.item, child.asString()) as Item)
                        "items" -> add(readType(ConsumeItems::class.java, child))
                        "liquid" -> add(readType(ConsumeLiquid::class.java, child))
                        "coolant" -> add(readType(ConsumeCoolant::class.java, child))
                        "power" -> {
                            if (child.isNumber) power(child.asFloat())
                            else add(readType(ConsumePower::class.java, child))
                        }
                        "powerBuffered" -> powerBuffered(child.asFloat())
                        else -> error("Unknown consumption type: ${child.name}")
                    }
                }
                init()
            } as T
            else -> json.readValue(cls, jsonValue)
        }
    }

    open class MyField(val obj: Any) {
        class Mutable(obj: Any, val type: Class<*>, val set: (Any) -> Unit) : MyField(obj)
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveObj(baseObj: Any, keys: String): MyField {
        fun resolveKey(last: MyField, key: String): MyField {
            when (val obj = last.obj) {
                is ObjectMap<*, *> -> {
                    if (!obj.isEmpty) {
                        when (val mapKey = obj.keys().first()) {
                            is String -> return (obj as ObjectMap<String, Any>).run {
                                var type = get(key).javaClass
                                if (Modifier.isFinal(type.modifiers)) type = type.superclass
                                MyField.Mutable(get(key), type) { put(key, it) }
                            }
                            is Content -> return (obj as ObjectMap<Content, Any>).run {
                                val keyN = findContent(mapKey.contentType, key)
                                var type = get(keyN).javaClass
                                if (Modifier.isFinal(type.modifiers)) type = type.superclass
                                MyField.Mutable(get(keyN), type) { put(keyN, it) }
                            }
                        }
                    }
                }
                is Seq<*> -> {
                    if (last is MyField.Mutable && key == "+") {//appear
                        return MyField.Mutable(obj, last.type) {
                            last.set(obj.copy().addAll(it as Seq<out Nothing>))
                        }
                    }
                    return MyField(obj.get(key.toInt()))
                }
                is Consumers -> {
                    if (last is MyField.Mutable && key == "+") {
                        return MyField.Mutable(obj, last.type) {
                            last.set(Consumers().apply {
                                obj.all().forEach(this::add)
                                (it as Consumers).all().forEach(this::add)
                                init()
                            })
                        }
                    }
                }
            }
            return getField(baseObj, key).run {
                MyField.Mutable(get(baseObj), type) { set(baseObj, it) }
            }
        }

        return try {
            val spKeys = keys.split(".")
            spKeys.fold(MyField(baseObj), ::resolveKey)
        } catch (e: Throwable) {
            throw Error("Can't resolve key '$keys' on $baseObj", e)
        }
    }

    private val bakField = mutableMapOf<String, () -> Unit>()
    private fun handleContent(type: String, value: JsonValue) {
        val content: Any = findContent(ContentType.valueOf(type), value.name)
        value.forEach { prop ->
            val id = "$type.${value.name}.${prop.name}"
            try {
                val field = this.resolveObj(content, prop.name)
                if (field !is MyField.Mutable) error("target property is not mutable.")
                bakField.putIfAbsent(id) { field.set(field.obj) }
                field.set(readType(field.type, prop))
                Log.info("Load Content $id = ${prop.toJson(JsonWriter.OutputType.javascript)}")
            } catch (e: Throwable) {
                Log.err("Fail to handle Content \"$id\"", e)
            }
        }
    }

    private val fieldCache = mutableMapOf<Pair<Class<*>, String>, Field>()
    private fun getField(obj: Any, name: String): Field {
        var cls = obj.javaClass
        if (cls.isAnonymousClass)
            cls = cls.superclass
        return fieldCache.getOrPut(cls to name) {
            cls.getField(name).apply {
                if (Modifier.isFinal(modifiers))
                    isAccessible = true
            }
        }
    }

    object Api {
        const val tagName = "ContentsPatch"
        fun load(text: String) {
            val json = JsonIO.read<JsonValue>(null, text)
            json.forEach { type ->
                type.forEach { content ->
                    handleContent(type.name, content)
                }
            }
        }

        fun reset() {
            bakField.values.forEach { it.invoke() }
            bakField.clear()
        }
    }
}