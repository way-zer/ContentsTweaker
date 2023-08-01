package cf.wayzer.contentsTweaker

import arc.util.Log
import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.resolvers.*
import mindustry.io.JsonIO
import kotlin.system.measureTimeMillis

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

    fun loadPatch(name: String, content: String, doAfter: Boolean = true) {
        val time = measureTimeMillis {
            handle(JsonIO.read(null, content))
            if (doAfter) afterHandle()
        }
        Log.infoTag("ContentsTweaker", "Load Content Patch '$name' costs $time ms")
    }

    fun recoverAll() {
        CTNode.PatchHandler.recoverAll()
    }
}