package cf.wayzer.contentsMod

import arc.func.Cons
import arc.func.Prov
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import mindustry.Vars
import mindustry.gen.Call
import mindustry.gen.ClientPacketReliableCallPacket
import mindustry.mod.Mod
import mindustry.net.Net
import mindustry.net.Packet
import mindustry.net.Packets

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
        MyContentLoader.Api.apply {
            logTimeCost = { tag, time ->
                Log.infoTag("ContentsLoader", "$tag costs ${time}ms")
            }
            Contents.register()
        }

        //hack: hook to beforeWorldLoad
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

        //hack: modify ClientPacketReliableCallPacket priority
        val packetProvs = Net::class.java.getDeclaredField("packetProvs").run {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            get(null) as Seq<Prov<out Packet>>
        }
        packetProvs[Net.getPacketId(ClientPacketReliableCallPacket()).toInt()] = ::MYClientPacketReliableCallPacket

        Vars.netClient.addPacketHandler("ContentsLoader|load", MyContentLoader.Api.toLoadPacks::add)
        Log.infoTag("ContentsLoader", "Finish Load Mod")
    }

    fun beforeWorldLoad() {
        Log.infoTag("ContentsLoader", "ToLoad ${MyContentLoader.Api.toLoadPacks}")
        val notFound = mutableListOf<String>()
        MyContentLoader.Api.loadContent(notFound)
        Call.serverPacketReliable("ContentsLoader|load", "LOADED: ${MyContentLoader.Api.lastLoadedPacks}")
        Call.serverPacketReliable("ContentsLoader|load", "NOTFOUND: $notFound")
    }
}