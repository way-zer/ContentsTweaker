package cf.wayzer.contentsMod

import arc.util.Log
import mindustry.content.flood.Blocks
import mindustry.content.flood.Bullets
import mindustry.content.flood.UnitTypes
import mindustry.ctype.ContentType

@Suppress("MemberVisibilityCanBePrivate")
object Contents {
    fun flood(): String {
        ContentsLoader.overwriteContents(ContentType.block, Blocks())
        ContentsLoader.overwriteContents(ContentType.bullet, Bullets())
        ContentsLoader.overwriteContents(ContentType.unit, UnitTypes())
        return "OK"
    }

    fun origin(): String {
        MyContentLoader.contents.forEach {
            it.content = it.default
        }
        return "OK"
    }

    fun loadType(type: String) = when (type.lowercase()) {
        "flood" -> flood()
        "origin" -> origin()
        else -> {
            Log.infoTag("ContentsLoader", "Unknown contents type $type")
            origin()
            "NOTFOUND"
        }
    }
}