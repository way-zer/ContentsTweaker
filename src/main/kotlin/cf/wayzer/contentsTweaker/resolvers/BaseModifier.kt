package cf.wayzer.contentsTweaker.resolvers

import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
import cf.wayzer.contentsTweaker.TypeRegistry

object BaseModifier : PatchHandler.Resolver {
    override fun resolve(node: Node, child: String): Node? {
        if (child != "=") return null
        if (node !is Node.Modifiable) error("${node.key} is not Modifiable, can't assign")
        return node.withModifier(child) { json ->
            val value = TypeRegistry.resolveType(json, type, elementType, keyType)
            beforeModify()
            setValue(value)
        }
    }
}