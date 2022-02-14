package cf.wayzer.contentsMod

import arc.Events
import arc.util.Log
import mindustry.Vars
import mindustry.ctype.Content
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
            //fastPath
            if (MyContentLoader.contents.all { it.content == it.lastContent }) {
                MyContentLoader.contents.forEach { it.content = it.default }
                return@on
            }
            MyContentLoader.contents.forEach {
                val time = measureTimeMillis { it.load() }
                Log.infoTag("ContentsLoader", "Loaded ${it.lastContent!!::class.qualifiedName} costs ${time}ms")
            }
            if (!Vars.headless) {
                val timeLoadIcon = measureTimeMillis {
                    MyContentLoader.contents.forEach { it.contentMap.forEach(Content::loadIcon) }
                }
                Log.infoTag("ContentsLoader", "Content.loadIcon costs ${timeLoadIcon}ms")
                val timeLoad = measureTimeMillis {
                    MyContentLoader.contents.forEach { it.contentMap.forEach(Content::load) }
                }
                Log.infoTag("ContentsLoader", "Content.load costs ${timeLoad}ms")
            }
        }
        Vars.content = MyContentLoader
        Vars.netClient.addPacketHandler("ContentsLoader|load") {
            Log.infoTag("ContentsLoader", "ToLoad $it")
            Call.serverPacketReliable("ContentsLoader|load", loadType(it))
        }
        Log.infoTag("ContentsLoader", "Finish Load Mod")
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

        fun loadType(type: String) = Contents.loadType(type)
    }
}