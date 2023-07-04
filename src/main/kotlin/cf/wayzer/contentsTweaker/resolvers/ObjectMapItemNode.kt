package cf.wayzer.contentsTweaker.resolvers

import arc.struct.ObjectMap
import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.registryResetHandler
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
import cf.wayzer.contentsTweaker.TypeRegistry

class ObjectMapItemNode<K, V>(override val parent: WithObj<ObjectMap<K, V>>, val mapKey: K, override val key: String) : Node.Modifiable<V>() {
    private val map get() = parent.obj
    override val obj: V get() = map.get(mapKey)
    private val originV = obj
    override val externalObject: Boolean get() = obj !== originV

    @Suppress("UNCHECKED_CAST")
    override val type: Class<V> = ((parent as WithObj<*>).elementType ?: obj?.let { it::class.java } ?: Any::class.java) as Class<V>

    override fun setValue(value: V) {
        map.put(mapKey, value)
    }

    override fun saveValue0() {
        registryResetHandler(map, mapKey) { map ->
            val oldExists = map.containsKey(mapKey)
            fun() {
                if (oldExists) map.put(mapKey, originV)
                else map.remove(mapKey)
            }
        }
    }

    companion object Resolver : PatchHandler.Resolver {
        override fun resolve(node: Node, child: String): Node? {
            if (node !is WithObj<*> || !ObjectMap::class.java.isAssignableFrom(node.type))
                return null
            @Suppress("UNCHECKED_CAST")
            node as WithObj<ObjectMap<*, *>>

            val keyType = node.keyType ?: (node.obj.keys().firstOrNull()?.javaClass)
            return if (child == "-") {
                node.withModifier("-") {
                    val key = TypeRegistry.resolveType(it, keyType)
                    @Suppress("UNCHECKED_CAST")
                    (node.obj as ObjectMap<Any, *>).remove(key)
                }
            } else {
                val key = TypeRegistry.resolveType(JsonValue(child), keyType)
                @Suppress("UNCHECKED_CAST")
                ObjectMapItemNode(node as WithObj<ObjectMap<Any, Any>>, key, child)
            }
        }
    }
}