package cf.wayzer.contentsTweaker.resolvers

import arc.util.Strings
import cf.wayzer.contentsTweaker.CTNode
import cf.wayzer.contentsTweaker.CTNodeTypeChecked
import cf.wayzer.contentsTweaker.ContentsTweaker
import cf.wayzer.contentsTweaker.checkObjInfoOrNull
import mindustry.Vars
import mindustry.ctype.Content
import mindustry.ctype.ContentType
import mindustry.ctype.MappableContent

object MindustryContentsResolver : ContentsTweaker.NodeCollector {
    private val contentNodes = mutableMapOf<Content, CTNode>()
    override fun collectChild(node: CTNode) {
        if (node == CTNode.Root)
            return node.rootAddContentTypes()
        node.checkObjInfoOrNull<ContentType>()?.extendContents()
        node.checkObjInfoOrNull<Content>()?.extend()
    }

    private fun CTNode.rootAddContentTypes() {
        contentNodes.clear()
        ContentType.all.forEach {
            getOrCreate(it.name) += CTNode.ObjInfo(it, ContentType::class.java)
        }
    }

    private fun CTNodeTypeChecked<ContentType>.extendContents() {
        val type = objInfo.obj
        Vars.content.getBy<Content>(type).forEach {
            val name = if (it is MappableContent) it.name else "#${it.id}"
            node.getOrCreate(name).apply {
                +CTNode.ObjInfo(it)
                contentNodes[it] = this
            }
        }
        node += object : CTNode.Indexable {
            override fun resolveIndex(key: String): CTNode? = null
            override fun resolve(name: String): CTNode? {
                val normalize = Strings.camelToKebab(name)
                return node.children[normalize]
            }
        }
    }

    private fun CTNodeTypeChecked<Content>.extend() {
        if (contentNodes[objInfo.obj] === node) return
        node += CTNode.ToJson {
            val content = objInfo.obj
            val nameOrId = if (content is MappableContent) content.name else content.id.toString()
            it.value("${content.contentType}#${nameOrId}")
        }
    }
}