import cf.wayzer.ContentsLoader.Api.contentPacks
import cf.wayzer.ContentsLoader.Api.overwriteContents
import mindustry.Vars
import mindustry.content.flood.Blocks
import mindustry.content.flood.Bullets
import mindustry.content.flood.UnitTypes
import mindustry.ctype.ContentType
import mindustry.world.blocks.production.GenericCrafter
import mindustry.world.consumers.ConsumeType

@Suppress("MemberVisibilityCanBePrivate")
object Contents {
    fun flood() {
        overwriteContents(ContentType.block, Blocks())
        overwriteContents(ContentType.bullet, Bullets())
        overwriteContents(ContentType.unit, UnitTypes())
    }

    fun exFactoryNotConsume() {
        Vars.content.blocks().filterIsInstance<GenericCrafter>().forEach {
            it.canOverdrive = false
            it.consumes.remove(ConsumeType.item)
            it.consumes.remove(ConsumeType.liquid)
        }
    }

    fun register() {
        contentPacks["flood"] = ::flood
        contentPacks["EX-factoryNotConsume"] = ::exFactoryNotConsume
    }
}