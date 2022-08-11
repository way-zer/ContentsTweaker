package cf.wayzer.contentsTweaker

import arc.Events
import arc.util.Log
import mindustry.Vars
import mindustry.game.EventType
import mindustry.game.EventType.ResetEvent
import mindustry.gen.Call
import mindustry.io.JsonIO
import mindustry.mod.Mod
import kotlin.system.measureTimeMillis

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ModMain : Mod() {
    override fun init() {
        registerContentsParser()
        Log.infoTag("ContentsLoader", "Finish Load Mod")
    }

    val patchCache = mutableMapOf<String, String>()
    fun registerContentsParser() {
        PatchHandler//ensure init
        Events.on(ResetEvent::class.java) { PatchHandler.recoverAll() }
        fun loadPatch(name: String) {
            val time = measureTimeMillis {
                if (name !in patchCache) {
                    val localFile = Vars.dataDirectory.child("contents-patch").run {
                        child("$name.hjson").takeIf { it.exists() }
                            ?: child("$name.json").takeIf { it.exists() }
                    } ?: return Call.serverPacketReliable("ContentsLoader|requestPatch", name)
                    patchCache[name] = localFile.readString()
                }
                PatchHandler.handle(JsonIO.read(null, patchCache[name]!!))
            }
            Log.info("Load Content Patch '$name' costs $time ms")
        }
        Events.on(EventType.WorldLoadEvent::class.java) {
            loadPatch("default")
            Vars.state.rules.tags.get("ContentsPatch")?.split(";")
                ?.forEach { loadPatch(it) }
            PatchHandler.doAfterHandle()
        }
        Vars.netClient.addPacketHandler("ContentsLoader|loadPatch") {
            loadPatch(it)
            PatchHandler.doAfterHandle()
        }
        Vars.netClient.addPacketHandler("ContentsLoader|newPatch") {
            val (name, content) = it.split('\n', limit = 2)
            if (!name.startsWith("$"))//$patch see as variable, only cache in server
                patchCache[name] = content
            PatchHandler.handle(JsonIO.read(null, content))
            PatchHandler.doAfterHandle()
        }
    }
}