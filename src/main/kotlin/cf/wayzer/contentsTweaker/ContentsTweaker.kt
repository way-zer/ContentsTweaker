package cf.wayzer.contentsTweaker

import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.resolvers.*

object ContentsTweaker {
    val resolvers: MutableList<PatchHandler.Resolver> = mutableListOf(
        BlockConsumesResolver,
        ContentTypeNode.Resolver,
        UIExtNode.Resolver,

        SeqResolver,
        ObjectMapItemNode.Resolver,
        ReflectNode.Resolver,
    )
    val typeResolvers = mutableListOf<TypeRegistry.Resolver>(
        BlockConsumesResolver
    )

    fun handle(json: JsonValue) = PatchHandler.handle(json)
    fun afterHandle() {
        PatchHandler.doAfterHandle()
    }

    fun recoverAll() {
        PatchHandler.recoverAll()
    }
}