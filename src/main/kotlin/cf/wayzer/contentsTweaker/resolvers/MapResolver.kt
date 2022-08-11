package cf.wayzer.contentsTweaker.resolvers

import arc.struct.ObjectMap
import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.TypeRegistry

object MapResolver : PatchHandler.Resolver {
    class MapItemNode(override val parent: Node, val mapKey: Any?, key: String) : Node(key), Node.Modifiable {
        @Suppress("UNCHECKED_CAST")
        private val map = ((parent as WithObj).obj as ObjectMap<Any, Any>)
        override val obj: Any? = map.get(mapKey)
        override val type: Class<*>?
            get() = (parent as WithObj).elementType ?: obj?.javaClass

        override fun setValue(value: Any?) {
            map.put(mapKey, value)
        }

        private val oldExists: Boolean = map.containsKey(mapKey)
        override fun recover() {
            if (oldExists) map.put(mapKey, obj)
            else map.remove(mapKey)
        }
    }

    override fun resolve(node: Node, child: String): Node? {
        if (node !is Node.WithObj) return null
        val obj = node.obj
        if (obj !is ObjectMap<*, *>) return null
        val keyType = node.keyType ?: (obj.keys().firstOrNull()?.javaClass)
        val key = TypeRegistry.resolveType(JsonValue(child), keyType)
        return MapItemNode(node, key, node.subKey(child))
    }
}