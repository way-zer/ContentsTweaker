package cf.wayzer.contentsTweaker

import arc.Events
import arc.util.Log
import mindustry.Vars
import mindustry.game.EventType.ResetEvent
import mindustry.game.EventType.WorldLoadBeginEvent
import mindustry.gen.Call
import mindustry.mod.Mod

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ModMain : Mod() {
    override fun init() {
        registerContentsParser()
        Log.infoTag("ContentsTweaker", "Finish Load Mod")
    }

    fun registerContentsParser() {
        ContentsTweaker//ensure all resolver init
        Events.on(ResetEvent::class.java) { ContentsTweaker.recoverAll() }

        Events.on(WorldLoadBeginEvent::class.java) {
            if (ContentsTweaker.worldReloading) return@on
            Call.serverPacketReliable("ContentsLoader|version", Vars.mods.getMod(javaClass).meta.version)
            Vars.state.rules.tags.get("ContentsPatch")?.split(";")?.forEach { name ->
                if (name.isBlank()) return@forEach
                val patch = Vars.state.rules.tags.get("CT@$name")
                    ?: return@forEach Call.serverPacketReliable("ContentsLoader|requestPatch", name)
                ContentsTweaker.loadPatch(name, patch, doAfter = false)
            }
            ContentsTweaker.afterHandle()
        }
        Vars.netClient.addPacketHandler("ContentsLoader|newPatch") {
            val (name, content) = it.split('\n', limit = 2)
            ContentsTweaker.loadPatch(name, content)
        }
        //TODO: Deprecated
        Vars.netClient.addPacketHandler("ContentsLoader|loadPatch") { name ->
            val patch = Vars.state.rules.tags.get("CT@$name")
                ?: return@addPacketHandler Call.serverPacketReliable("ContentsLoader|requestPatch", name)
            ContentsTweaker.loadPatch(name, patch)
        }
        Vars.mods.scripts.scope.apply {
            put("CT", this, ContentsTweaker)
            put("CTRoot", this, CTNode.Root)
        }
    }
}