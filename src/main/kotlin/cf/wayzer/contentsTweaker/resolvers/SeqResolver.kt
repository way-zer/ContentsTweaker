package cf.wayzer.contentsTweaker.resolvers

import arc.struct.Seq
import cf.wayzer.contentsTweaker.*

object SeqResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        node.checkObjInfoOrNull<Seq<Any>>()?.extend()
    }

    private fun CTNodeTypeChecked<Seq<Any>>.extend() {
        val list = objInfo.obj
        list.forEachIndexed { index, item ->
            node.getOrCreate(index.toString()) += CTNode.ObjInfo(item)
            //不支持 Modifiable，因为在对象变化后难以获取当前索引。修改某项应该使用`-`和`+`运算配合
        }
        modifier("-") { json ->
            val item = if (json.isNumber && json.asInt() < list.size) {
                list[json.asInt()]//删除原始数组中对应索引的元素
            } else {
                TypeRegistry.resolveType(json, objInfo.elementType)
            }
            copy().apply { remove(item) }
        }
        modifier("+") { json ->
            val value = TypeRegistry.resolveType(json, objInfo.elementType)
            copy().apply { add(value) }
        }
        modifier("+=") { json ->
            val value = TypeRegistry.resolveType(json, objInfo.type, objInfo.elementType)
            copy().apply { addAll(value) }
        }
    }
}