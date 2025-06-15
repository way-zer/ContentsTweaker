package cf.wayzer.contentsTweaker.resolvers

import arc.struct.ObjectMap
import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.*

object ObjectMapResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        node.checkObjInfoOrNull<ObjectMap<Any, Any>>()?.extend()
    }

    private fun CTNodeTypeChecked<ObjectMap<Any, Any>>.extend() {
        val map = objInfo.obj
        map.forEach {
            node.getOrCreate("#" + TypeRegistry.getKeyString(it.key)).apply {
                +CTNode.ObjInfo(it.value)
                +object : CTNode.Modifiable<Any?>(node) {
                    override val currentValue: Any get() = map[it.key]
                    override fun setValue0(value: Any?) {
                        if (value == null) map.remove(it.key)
                        else map.put(it.key, value)
                    }
                }
            }
        }

        val keyType = objInfo.keyType ?: (map.keys().firstOrNull()?.javaClass)
        node += object : CTNode.Indexable {
            override fun resolveIndex(key: String): CTNode? {
                val keyV = TypeRegistry.resolveType(JsonValue(key), keyType)
                val value = map.get(keyV)
                val elementType = objInfo.elementType
                if (value == null && elementType == null) return null
                return node.getOrCreate("#" + TypeRegistry.getKeyString(keyV)).apply {
                    if (value != null) {
                        extendOnce<CTNode.ObjInfo<Any?>>(CTNode.ObjInfo(value))
                    } else {
                        extendOnce<CTNode.ObjInfo<Any?>>(CTNode.ObjInfo(null, elementType!!))
                    }
                }
            }

            override fun resolve(name: String): CTNode? {
                //后向兼容
                return super.resolve(name) ?: runCatching { resolveIndex(name) }.getOrNull()
            }
        }
        modifier("-") {
            val key = TypeRegistry.resolveType(it, keyType)
            map.copy().apply { remove(key) }
        }
    }
}