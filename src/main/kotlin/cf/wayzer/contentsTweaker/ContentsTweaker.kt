package cf.wayzer.contentsTweaker

import arc.util.Log
import arc.util.serialization.BaseJsonWriter
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

    fun afterHandle() {
        CTNode.PatchHandler.doAfterHandle()
    }

    fun loadPatch(name: String, content: String, doAfter: Boolean = true) {
        val time = measureTimeMillis {
            CTNode.PatchHandler.handle(JsonIO.read(null, content))
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
            this.`object`()
            node.getObjInfo<Any>()?.obj?.let(visited::add)
            for ((k, v) in node.children) {
                name(k)
                v.collectAll()
                when {
                    node.get<CTNode.ToJson>()?.write(this) != null -> {}
                    k == "techNode" -> value("...")
                    node.getObjInfo<Any>()?.obj in visited -> value("RECURSIVE")
                    v.get<CTNode.Modifier>() != null -> {
                        if (k == "=") {
                            value(node.get<CTNode.Modifiable<Any>>()?.currentValue?.let(TypeRegistry::getKeyString))
                        } else value("CT_MODIFIER")
                    }

                    //只有=的简单节点，省略=
                    v.children.keys.singleOrNull() == "=" ->
                        value(v.get<CTNode.Modifiable<Any>>()?.currentValue?.let(TypeRegistry::getKeyString))

                    else -> writeNode(v)
                }
            }
            node.getObjInfo<Any>()?.obj?.let(visited::remove)
            return this.pop()
        }

        val writer = JsonWriter(Vars.dataDirectory.child("CT.json").writer(false))
        writer.setOutputType(JsonWriter.OutputType.json)
        writer.writeNode(CTNode.Root.collectAll()).close()
    }
}