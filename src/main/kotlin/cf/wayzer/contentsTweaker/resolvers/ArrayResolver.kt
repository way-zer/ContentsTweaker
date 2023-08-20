package cf.wayzer.contentsTweaker.resolvers

import cf.wayzer.contentsTweaker.*

object ArrayResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        node.checkObjInfoOrNull<Array<Any?>>()?.extend()
    }

    //    private val types = mutableSetOf<Any>()
    private fun CTNodeTypeChecked<Array<Any?>>.extend() {
        objInfo.type.componentType.let {
            if (it.isArray || it.`package`.name.startsWith("arc")) {
//                if (types.add(it)) println(it)
                return
            }
        }
        val list = objInfo.obj
        list.forEachIndexed { index, item ->
            if (item != null)
                node.getOrCreate("#$index") += CTNode.ObjInfo(item)
        }
        modifier("-") { json ->
            val item = if (json.isNumber && json.asInt() < list.size) {
                list[json.asInt()]//删除原始数组中对应索引的元素
            } else {
                TypeRegistry.resolveType(json, objInfo.elementType)
            }
            filter { it != item }.toTypedArray()
        }
        modifier("+") { json ->
            val value = TypeRegistry.resolveType(json, objInfo.elementType)
            this.plus(element = value)
        }
        modifier("+=") { json ->
            val value = TypeRegistry.resolveType(json, objInfo.type, objInfo.elementType)
            this.plus(elements = value)
        }
    }
}