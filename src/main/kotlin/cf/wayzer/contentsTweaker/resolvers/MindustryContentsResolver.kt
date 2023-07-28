package cf.wayzer.contentsTweaker.resolvers

import arc.util.Strings
import cf.wayzer.contentsTweaker.CTNode
import cf.wayzer.contentsTweaker.ContentsTweaker
import cf.wayzer.contentsTweaker.getObjInfo
import mindustry.Vars
import mindustry.content.Bullets
import mindustry.ctype.ContentType
import mindustry.ctype.MappableContent
import mindustry.entities.bullet.BulletType

object MindustryContentsResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        if (node == CTNode.Root)
            return node.rootAddContentTypes()
        val type = node.getObjInfo<ContentType>()?.obj ?: return
        node.listContents(type)
    }

    private fun CTNode.rootAddContentTypes() {
        ContentType.all.forEach {
            getOrCreate(it.name) += CTNode.ObjInfo(it, ContentType::class.java)
        }
    }

    private fun CTNode.listContents(type: ContentType) {
        if (type == ContentType.bullet) {
            bulletMap.forEach { (name, v) ->
                getOrCreate(name) += CTNode.ObjInfo(v)
            }
            return
        }
        Vars.content.getBy<MappableContent>(type).forEach {
            getOrCreate(it.name) += CTNode.ObjInfo(it)
        }
    }

    private val bulletMap by lazy {
        Bullets::class.java.fields
            .filter { it.type == BulletType::class.java }
            .map { Strings.camelToKebab(it.name) to it.get(null) as BulletType }
    }
}