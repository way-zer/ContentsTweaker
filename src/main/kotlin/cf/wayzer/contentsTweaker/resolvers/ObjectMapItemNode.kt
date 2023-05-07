package cf.wayzer.contentsTweaker.resolvers

import arc.struct.ObjectMap
import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
import cf.wayzer.contentsTweaker.TypeRegistry

class ObjectMapItemNode(override val parent: Node, val mapKey: Any?, key: String) : Node(key), Node.Modifiable<Any?> {
    @Suppress("UNCHECKED_CAST")
    private val map = ((parent as WithObj<*>).obj as ObjectMap<Any, Any>)
    override val type: Class<*>
        get() = (parent as WithObj<*>).elementType ?: obj?.javaClass ?: Any::class.java

    override fun setValue(value: Any?) {
        map.put(mapKey, value)
    }

    override val obj: Any? = map.get(mapKey)
    private val oldExists: Boolean = map.containsKey(mapKey)
    override val storeDepth: Int = 0
    override fun doSave() {}
    override fun doRecover() {
        if (oldExists) map.put(mapKey, obj)
        else map.remove(mapKey)
    }

    override fun resolve(child: String): Node {
        if (child == "-") return withModifier("-") {
            beforeModify()
            map.remove(mapKey)
        }
        return super.resolve(child)
    }

    companion object Resolver : PatchHandler.Resolver {
        override fun resolve(node: Node, child: String): Node? {
            if (node !is WithObj<*>) return null
            val obj = node.obj
            if (obj !is ObjectMap<*, *>) return null
            val keyType = node.keyType ?: (obj.keys().firstOrNull()?.javaClass)
            val key = TypeRegistry.resolveType(JsonValue(child), keyType)
            return ObjectMapItemNode(node, key, node.subKey(child))
        }
    }
}