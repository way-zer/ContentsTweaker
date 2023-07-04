package cf.wayzer.contentsTweaker.resolvers

import arc.util.Strings
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import mindustry.Vars
import mindustry.content.Bullets
import mindustry.ctype.Content
import mindustry.ctype.ContentType
import mindustry.entities.bullet.BulletType

class ContentTypeNode(val type: ContentType, override val key: String) : Node() {
    override val parent = Root
    override fun resolve(child: String): Node {
        val normalizedId = Strings.camelToKebab(child)

        @Suppress("USELESS_CAST")
        val content = when (type) {
            ContentType.bullet -> bulletMap[Strings.kebabToCamel(normalizedId)] as Content?
            else -> Vars.content.getByName(type, normalizedId)
        } ?: error("Fail to find $type: $normalizedId($child)")

        return ObjNode(this, normalizedId, content)
    }

    companion object Resolver : PatchHandler.Resolver {
        private val bulletMap by lazy {
            Bullets::class.java.fields
                .filter { it.type == BulletType::class.java }
                .associate { it.name to it.get(null) as BulletType }
        }

        override fun resolve(node: Node, child: String): Node? {
            if (node != Root) return null
            val type = ContentType.all.find { it.name == child } ?: return null
            return ContentTypeNode(type, child)
        }
    }
}