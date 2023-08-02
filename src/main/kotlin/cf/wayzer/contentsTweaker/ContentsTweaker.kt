package cf.wayzer.contentsTweaker

import arc.util.Log
import arc.util.serialization.BaseJsonWriter
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import cf.wayzer.contentsTweaker.resolvers.*
import mindustry.Vars
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

    //Dev test, call from js
    @Suppress("unused")
    fun exportAll() {
        val visited = mutableSetOf<Any>()
        fun JsonWriter.writeNode(node: CTNode): BaseJsonWriter {
            node.collectAll()
            node.get<CTNode.ToJson>()?.write(this)?.let { return this }

            val obj = node.getObjInfo<Any>()?.obj
            if (obj in visited) return value("RECURSIVE")

            this.`object`()
            obj?.let(visited::add)
            for ((k, v) in node.children) {
                name(k)
                when {
                    k == "techNode" -> value("...")
                    v.get<CTNode.Modifier>() != null -> {
                        if (k == "=") {
                            value(node.get<CTNode.Modifiable<Any>>()?.currentValue?.let(TypeRegistry::getKeyString))
                        } else value("CT_MODIFIER")
                    }

                    else -> writeNode(v)
                }
            }
            obj?.let(visited::remove)
            return this.pop()
        }

        val writer = JsonWriter(Vars.dataDirectory.child("CT.json").writer(false))
        writer.setOutputType(JsonWriter.OutputType.json)
        writer.writeNode(CTNode.Root).close()
    }
}