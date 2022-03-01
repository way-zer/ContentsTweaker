import arc.struct.Bits
import cf.wayzer.ContentsLoader.Api.contentPacks
import cf.wayzer.ContentsLoader.Api.overwriteContents
import mindustry.Vars
import mindustry.content.flood.Blocks
import mindustry.content.flood.Bullets
import mindustry.content.flood.UnitTypes
import mindustry.ctype.ContentType
import mindustry.gen.Building
import mindustry.world.blocks.production.GenericCrafter
import mindustry.world.consumers.*

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
            it.consumes.apply {
                if (has(ConsumeType.item)) {
                    add(object : ConsumeItems() {
                        override fun valid(entity: Building?) = true
                        override fun trigger(entity: Building?) = Unit
                    })
                }
                if (has(ConsumeType.liquid)) {
                    val type = when (val old = get<ConsumeLiquidBase>(ConsumeType.liquid)) {
                        is ConsumeLiquidFilter -> Vars.content.liquids().first(old.filter::get)
                        is ConsumeLiquid -> old.liquid
                        else -> null
                    }
                    add(object : ConsumeLiquid(type, 0f) {
                        override fun applyLiquidFilter(filter: Bits?) = Unit
                        override fun valid(entity: Building?) = true
                        override fun trigger(entity: Building?) = Unit
                    })
                }
            }
        }
    }

    fun register() {
        contentPacks["flood"] = ::flood
        contentPacks["EX-factoryNotConsume"] = ::exFactoryNotConsume
    }
}