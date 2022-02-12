package cf.wayzer.contentsMod

import arc.Events
import arc.util.Log
import mindustry.Vars
import mindustry.content.flood.Blocks
import mindustry.content.flood.Bullets
import mindustry.content.flood.UnitTypes
import mindustry.ctype.ContentList
import mindustry.ctype.ContentType
import mindustry.game.EventType.ResetEvent
import mindustry.gen.Call
import mindustry.mod.Mod
import kotlin.system.measureTimeMillis

@Suppress("unused")
class ContentsLoader : Mod() {
    override fun init() {
        Events.on(ResetEvent::class.java) {
            if (MyContentLoader.contents.all { it.content == it.lastContent }) return@on
            MyContentLoader.contents.forEach {
                val time = measureTimeMillis { it.load() }
                Log.infoTag("ContentsLoader", "Loaded ${it.lastContent!!::class.qualifiedName} costs ${time}ms")
            }
        }
        Vars.content = MyContentLoader
        Vars.netClient.addPacketHandler("ContentsLoader|load") {
            Call.serverPacketReliable("ContentsLoader|load", loadType(it))
        }
    }

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    companion object API {
        fun maskChanged(type: ContentType) {
            val c = MyContentLoader.contentMap[type] ?: throw IllegalArgumentException("Not Support Overwrite ContentType")
            c.maskChanged()
        }

        fun overwriteContents(type: ContentType, list: ContentList) {
            val c = MyContentLoader.contentMap[type] ?: throw IllegalArgumentException("Not Support Overwrite ContentType")
            c.content = list
        }

        fun loadType(type: String) = when (type.lowercase()) {
            "flood" -> {
                overwriteContents(ContentType.block, Blocks())
                overwriteContents(ContentType.bullet, Bullets())
                overwriteContents(ContentType.unit, UnitTypes())
                "OK"
            }
            else -> {
                Log.infoTag("ContentsLoader", "Unknown contents type $type")
                "NOTFOUND"
            }
        }
    }
}