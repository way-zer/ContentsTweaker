package cf.wayzer.contentsTweaker

import arc.util.Log
import arc.util.serialization.BaseJsonWriter
import arc.util.serialization.JsonWriter
import cf.wayzer.contentsTweaker.resolvers.*
import mindustry.Vars
import mindustry.io.JsonIO
import mindustry.io.SaveIO
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.system.measureTimeMillis

object ContentsTweaker {
    fun interface NodeCollector {
        fun collectChild(node: CTNode)
    }

    val resolvers: MutableList<NodeCollector> = mutableListOf(
        ReflectResolver,
        ArrayResolver,
        SeqResolver,
        ObjectMapResolver,

        BetterJsonResolver,
        MindustryContentsResolver,
        BlockConsumesResolver,
        UIExtResolver,
    )
    val typeResolvers = mutableListOf<TypeRegistry.Resolver>(
        BlockConsumesResolver
    )

    fun afterHandle() {
        if (CTNode.PatchHandler.afterHandlers.isEmpty()) return
        val time = measureTimeMillis {
            CTNode.PatchHandler.doAfterHandle()
        }
        Log.infoTag("ContentsTweaker", "Do afterHandle costs $time ms")
    }

    fun loadPatch(name: String, content: String, doAfter: Boolean = true) {
        val time = measureTimeMillis {
            CTNode.PatchHandler.handle(JsonIO.read(null, content))
            if (doAfter) afterHandle()
        }
        Log.infoTag("ContentsTweaker", "Load Content Patch '$name' costs $time ms")
    }

    fun recoverAll() {
        if (worldInReset) return
        worldInReset = true
        CTNode.PatchHandler.recoverAll()
        worldInReset = false
    }

    var worldInReset = false

    fun reloadWorld() {
        if (worldInReset) return
        val time = measureTimeMillis {
            worldInReset = true
            val stream = ByteArrayOutputStream()

            val output = DataOutputStream(stream)
            val writer = SaveIO.getSaveWriter()
            writer.writeMap(output)

            val input = DataInputStream(ByteArrayInputStream(stream.toByteArray()))
            @Suppress("INACCESSIBLE_TYPE")
            writer.readMap(input, Vars.world.context)
            worldInReset = false
        }
        Log.infoTag("ContentsTweaker", "Reload world costs $time ms")
    }

    //Dev test, call from js
    @Suppress("unused")
    fun eval(content: String) {
        loadPatch("console", "{$content}")
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
                    v.get<CTNode.ToJson>()?.write(this) != null -> {}
                    v.getObjInfo<Any>()?.obj in visited -> value("RECURSIVE")
                    v.get<CTNode.Modifier>() != null -> value("CT_MODIFIER")
                    //只有=的简单节点，省略=
                    v.children.keys.singleOrNull() == "=" -> v.resolve("=").get<CTNode.ToJson>()!!.write(this)
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