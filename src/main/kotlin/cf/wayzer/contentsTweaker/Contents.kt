package cf.wayzer.contentsTweaker

object Contents {
    fun exFactoryNotConsume() {
//        Vars.content.blocks().each({ it is GenericCrafter || it is Separator }) { it: Block ->
//            it.canOverdrive = false
//            it.consumes.apply {
//                if (has(ConsumeType.item)) {
//                    add(object : ConsumeItems() {
//                        override fun valid(entity: Building?) = true
//                        override fun trigger(entity: Building?) = Unit
//                    })
//                }
//                if (has(ConsumeType.liquid)) {
//                    val old = get<ConsumeLiquidBase>(ConsumeType.liquid)
//                    val type = when (old) {
//                        is ConsumeLiquidFilter -> Vars.content.liquids().first(old.filter::get)
//                        is ConsumeLiquid -> old.liquid
//                        else -> null
//                    }
//                    val liquidCapacity = it.liquidCapacity
//                    add(object : ConsumeLiquid(type, old.amount) {
//                        override fun applyLiquidFilter(filter: Bits?) = Unit
//                        override fun update(entity: Building?) = Unit
//                        override fun valid(entity: Building?): Boolean {
//                            entity?.liquids?.apply {
//                                add(type, liquidCapacity - get(type))
//                            }
//                            return true
//                        }
//                    })
//                }
//            }
//        }
    }
}