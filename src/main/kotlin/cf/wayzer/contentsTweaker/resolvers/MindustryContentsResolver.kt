package cf.wayzer.contentsTweaker.resolvers

import cf.wayzer.contentsTweaker.*
import mindustry.Vars
import mindustry.ctype.Content
import mindustry.ctype.ContentType
import mindustry.ctype.MappableContent

object MindustryContentsResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        if (node == CTNode.Root)
            return node.rootAddContentTypes()
        node.checkObjInfoOrNull<ContentType>()?.extendContents()
        node.checkObjInfoOrNull<Content>()?.extend()
    }

    private fun CTNode.rootAddContentTypes() {
        ContentType.all.forEach {
            getOrCreate(it.name) += CTNode.ObjInfo(it, ContentType::class.java)
        }
    }

    private fun CTNodeTypeChecked<ContentType>.extendContents() {
        val type = objInfo.obj
        Vars.content.getBy<Content>(type).forEach {
            val name = if (it is MappableContent) it.name else "#${it.id}"
            node.getOrCreate(name) += CTNode.ObjInfo(it)
        }
    }

    private fun CTNodeTypeChecked<Content>.extend() {
        if (node.parent?.getObjInfo<ContentType>() != null) return
        node += CTNode.ToJson {
            val content = objInfo.obj
            val nameOrId = if (content is MappableContent) content.name else content.id.toString()
            it.value("${content.contentType}#${nameOrId}")
        }
    }
}