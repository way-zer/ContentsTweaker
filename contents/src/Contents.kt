import arc.struct.Bits
import cf.wayzer.ContentsLoader.Api.contentPacks
import cf.wayzer.ContentsLoader.Api.overwriteContents
import mindustry.Vars
import mindustry.content.flood.Blocks
import mindustry.content.flood.Bullets
import mindustry.content.flood.UnitTypes
import mindustry.ctype.ContentType
import mindustry.gen.Building
import mindustry.world.Block
import mindustry.world.blocks.production.GenericCrafter
import mindustry.world.blocks.production.Separator
import mindustry.world.consumers.*

@Suppress("MemberVisibilityCanBePrivate")
object Contents {
    fun flood() {
        overwriteContents(ContentType.block, Blocks())
        overwriteContents(ContentType.bullet, Bullets())
        overwriteContents(ContentType.unit, UnitTypes())
    }

    fun exFactoryNotConsume() {
        Vars.content.blocks().each({ it is GenericCrafter || it is Separator }) { it: Block ->
            it.canOverdrive = false
            it.consumes.apply {
                if (has(ConsumeType.item)) {
                    add(object : ConsumeItems() {
                        override fun valid(entity: Building?) = true
                        override fun trigger(entity: Building?) = Unit
                    })
                }
                if (has(ConsumeType.liquid)) {
                    val old = get<ConsumeLiquidBase>(ConsumeType.liquid)
                    val type = when (old) {
                        is ConsumeLiquidFilter -> Vars.content.liquids().first(old.filter::get)
                        is ConsumeLiquid -> old.liquid
                        else -> null
                    }
                    val liquidCapacity = it.liquidCapacity
                    add(object : ConsumeLiquid(type, old.amount) {
                        override fun applyLiquidFilter(filter: Bits?) = Unit
                        override fun update(entity: Building?) = Unit
                        override fun valid(entity: Building?): Boolean {
                            entity?.liquids?.apply {
                                add(type, liquidCapacity - get(type))
                            }
                            return true
                        }
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