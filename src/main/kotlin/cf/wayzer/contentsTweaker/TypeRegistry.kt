package cf.wayzer.contentsTweaker

import arc.func.Prov
import arc.struct.EnumSet
import arc.struct.ObjectMap
import arc.util.serialization.Json
import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.util.reflectDelegate
import mindustry.Vars
import mindustry.mod.ContentParser
import mindustry.mod.Mods

object TypeRegistry {
    private val Mods.parser: ContentParser by reflectDelegate()
    private val ContentParser.parser: Json by reflectDelegate()
    private val jsonParser by lazy { Vars.mods.parser.parser }

    interface Resolver {
        fun <T : Any> resolveType(json: JsonValue, type: Class<T>?, elementType: Class<*>? = null, keyType: Class<*>? = null): T?
    }

    private val resolvers = ContentsTweaker.typeResolvers

    fun <T : Any> resolveType(json: JsonValue, type: Class<T>?, elementType: Class<*>? = null, keyType: Class<*>? = null): T {
        @Suppress("UNCHECKED_CAST")
        when (type) {
            EnumSet::class.java -> {
                fun <T : Enum<T>> newEnumSet(arr: Array<Enum<*>>) = EnumSet.of(*arr as Array<T>)
                return newEnumSet(resolve(json, elementType)) as T
            }

            Prov::class.java -> {
                val cls = getTypeByName(json.asString(), null)
                val method = ContentParser::class.java.getDeclaredMethod("supply", Class::class.java)
                method.isAccessible = true
                return method.invoke(Vars.mods.parser, cls) as T
            }
        }
        return resolvers.firstNotNullOfOrNull { it.resolveType(json, type, elementType, keyType) }
            ?: jsonParser.readValue(type, elementType, json, keyType)
    }

    inline fun <reified T : Any> resolve(json: JsonValue, elementType: Class<*>? = null, keyType: Class<*>? = null): T {
        return resolveType(json, T::class.java, elementType, keyType)
    }

    fun getTypeByName(name: String, def: Class<*>?): Class<*> {
        val method = ContentParser::class.java.getDeclaredMethod("resolve", String::class.java, Class::class.java)
        method.isAccessible = true
        return method.invoke(Vars.mods.parser, name, def) as Class<*>
    }

    private val ContentParser.contentTypes: ObjectMap<*, *> by reflectDelegate()
    private fun initContentParser() {
        if (Vars.mods.parser.contentTypes.isEmpty) {
            val method = ContentParser::class.java.getDeclaredMethod("init")
            method.isAccessible = true
            method.invoke(Vars.mods.parser)
        }
    }

    init {
        initContentParser()
    }
}