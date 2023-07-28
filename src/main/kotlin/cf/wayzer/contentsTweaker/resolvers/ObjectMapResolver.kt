package cf.wayzer.contentsTweaker.resolvers

import arc.struct.ObjectMap
import cf.wayzer.contentsTweaker.*

object ObjectMapResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        node.checkObjInfoOrNull<ObjectMap<Any, Any>>()?.extend()
    }

    private fun CTNodeTypeChecked<ObjectMap<Any, Any>>.extend() {
        val map = objInfo.obj
        map.forEach {
            node.getOrCreate(TypeRegistry.getKeyString(it.key)).apply {
                +CTNode.ObjInfo(it.value)
                +object : CTNode.Modifiable<Any?>(getObjInfo<Any?>()!!) {
                    override val currentValue: Any get() = map[it.key]
                    override fun setValue0(value: Any?) {
                        if (value == null) map.remove(it.key)
                        else map.put(it.key, value)
                    }
                }
            }
        }

        val keyType = objInfo.keyType ?: (map.keys().firstOrNull()?.javaClass)
        modifier("-") {
            val key = TypeRegistry.resolveType(it, keyType)
            map.copy().apply { remove(key) }
        }
    }
}