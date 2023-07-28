package cf.wayzer.contentsTweaker

import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.resolvers.*

object ContentsTweaker {
    fun interface NodeCollector {
        fun collectChild(node: CTNode)
    }

    val resolvers: MutableList<NodeCollector> = mutableListOf(
        ReflectResolver,
        SeqResolver,
        ObjectMapResolver,

        MindustryContentsResolver,
        BlockConsumesResolver,
        UIExtResolver,
    )
    val typeResolvers = mutableListOf<TypeRegistry.Resolver>(
        BlockConsumesResolver
    )

    fun handle(json: JsonValue) = CTNode.PatchHandler.handle(json)
    fun afterHandle() {
        CTNode.PatchHandler.doAfterHandle()
    }

    fun recoverAll() {
        CTNode.PatchHandler.recoverAll()
    }
}