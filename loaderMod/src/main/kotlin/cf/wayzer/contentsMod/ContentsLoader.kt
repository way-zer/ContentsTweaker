package cf.wayzer.contentsMod

import arc.func.Cons
import arc.func.Prov
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import mindustry.Vars
import mindustry.ctype.Content
import mindustry.ctype.ContentList
import mindustry.ctype.ContentType
import mindustry.gen.Call
import mindustry.gen.ClientPacketReliableCallPacket
import mindustry.mod.Mod
import mindustry.net.Net
import mindustry.net.Packet
import mindustry.net.Packets
import kotlin.system.measureTimeMillis

class MYClientPacketReliableCallPacket : ClientPacketReliableCallPacket() {
    override fun getPriority(): Int {
        if (type == "ContentsLoader|load") return Packet.priorityHigh
        return super.getPriority()
    }
}

@Suppress("unused")
class ContentsLoader : Mod() {
    override fun init() {
        Vars.content = MyContentLoader

        val clientListeners = Net::class.java.getDeclaredField("clientListeners").run {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            get(Vars.net) as ObjectMap<Class<*>, Cons<Packet>>
        }
        val bak = clientListeners[Packets.WorldStream::class.java]!!
        Vars.net.handleClient(Packets.WorldStream::class.java) {
            beforeWorldLoad()
            bak.get(it)
        }

        val packetProvs = Net::class.java.getDeclaredField("packetProvs").run {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            get(null) as Seq<Prov<out Packet>>
        }
        packetProvs[Net.getPacketId(ClientPacketReliableCallPacket()).toInt()] = ::MYClientPacketReliableCallPacket
        Vars.netClient.addPacketHandler("ContentsLoader|load") {
            Log.infoTag("ContentsLoader", "ToLoad $it")
            Call.serverPacketReliable("ContentsLoader|load", loadType(it))
        }
        Log.infoTag("ContentsLoader", "Finish Load Mod")
    }

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    companion object API {
        infix fun ContentList.eq(contentList: ContentList?): Boolean {
            return javaClass == contentList?.javaClass
        }

        fun beforeWorldLoad() {
            //fastPath
            if (MyContentLoader.contents.all { it.content eq it.lastContent }) {
                MyContentLoader.contents.forEach { it.content = it.default }
                return
            }
            MyContentLoader.contents.forEach {
                val time = measureTimeMillis { it.load() }
                Log.infoTag("ContentsLoader", "Loaded ${it.lastContent!!::class.qualifiedName} costs ${time}ms")
            }
            if (!Vars.headless) {
                val timeLoadColors = measureTimeMillis {
                    MyContentLoader.loadColors()
                }
                Log.infoTag("ContentsLoader", "ContentLoader.loadColors costs ${timeLoadColors}ms")
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