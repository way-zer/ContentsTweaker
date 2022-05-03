package cf.wayzer

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

    private val bakField = mutableMapOf<String, () -> Unit>()
    private fun handleContent(type: String, value: JsonValue) {
        val content: Any = findContent(ContentType.valueOf(type), value.name)
        value.forEach { prop ->
            val id = "$type.${value.name}.${prop.name}"
            try {
                val (obj, field) = resolveObj(content, prop.name)
                val bakV = field.get(obj)
                bakField.putIfAbsent(id) { field.set(obj, bakV) }
                field.set(obj, readType(field.type, prop))
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
                if (name == "consumes")
                    isAccessible = true
            }
        }
    }

    private fun resolveObj(obj: Any, key: String): Pair<Any, Field> {
        var o = obj
        var field: Field? = null
        for (it in key.split(".")) {
            if (field != null) o = field.get(o)
            field = getField(o, it)
        }
        return o to field!!
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