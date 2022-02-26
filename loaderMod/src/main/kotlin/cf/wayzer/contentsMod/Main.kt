package cf.wayzer.contentsMod

import Contents
import arc.func.Cons
import arc.func.Prov
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import cf.wayzer.ContentsLoader
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

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Main : Mod() {
    override fun init() {
        Vars.content = ContentsLoader
        ContentsLoader.Api.apply {
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

        Vars.netClient.addPacketHandler("ContentsLoader|load", ContentsLoader.Api.toLoadPacks::add)
        Log.infoTag("ContentsLoader", "Finish Load Mod")
    }

    fun beforeWorldLoad() {
        Log.infoTag("ContentsLoader", "ToLoad ${ContentsLoader.Api.toLoadPacks}")
        val notFound = mutableListOf<String>()
        ContentsLoader.Api.loadContent(notFound)
        Call.serverPacketReliable("ContentsLoader|load", "LOADED: ${ContentsLoader.Api.lastLoadedPacks}")
        Call.serverPacketReliable("ContentsLoader|load", "NOTFOUND: $notFound")
    }
}