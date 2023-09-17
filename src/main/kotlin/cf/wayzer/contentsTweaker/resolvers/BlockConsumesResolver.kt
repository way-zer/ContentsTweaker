package cf.wayzer.contentsTweaker.resolvers

import arc.struct.Seq
import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.*
import cf.wayzer.contentsTweaker.util.reflectDelegate
import mindustry.type.ItemStack
import mindustry.world.Block
import mindustry.world.consumers.*

object BlockConsumesResolver : ContentsTweaker.NodeCollector, TypeRegistry.Resolver {
    private val Block.consumeBuilder: Seq<Consume> by reflectDelegate()
    override fun collectChild(node: CTNode) {
        val block = node.getObjInfo<Block>()?.obj ?: return
        node.getOrCreate("consumers").checkObjInfo<Array<Consume>>()
            .extendConsumers(block)
    }

    private fun CTNodeTypeChecked<Array<Consume>>.extendConsumers(block: Block) {
        node += CTNode.AfterHandler {
            block.consumeBuilder.apply {
                clear()
                addAll(*block.consumers)
            }
        }
        modifier("clearItems") { filterNot { it is ConsumeItems || it is ConsumeItemFilter }.toTypedArray() }
        modifier("item") { this + ConsumeItems(arrayOf(ItemStack(TypeRegistry.resolve(it), 1))) }
        modifier("items") { this + TypeRegistry.resolve<ConsumeItems>(it) }
        modifier("itemCharged") { this + TypeRegistry.resolve<ConsumeItemCharged>(it) }
        modifier("itemFlammable") { this + TypeRegistry.resolve<ConsumeItemFlammable>(it) }
        modifier("itemRadioactive") { this + TypeRegistry.resolve<ConsumeItemRadioactive>(it) }
        modifier("itemExplosive") { this + TypeRegistry.resolve<ConsumeItemExplosive>(it) }
        modifier("itemExplode") { this + TypeRegistry.resolve<ConsumeItemExplode>(it) }

        modifier("clearLiquids") { filterNot { it is ConsumeLiquidBase || it is ConsumeLiquids }.toTypedArray() }
        modifier("liquid") { this + TypeRegistry.resolve<ConsumeLiquid>(it) }
        modifier("liquids") { this + TypeRegistry.resolve<ConsumeLiquids>(it) }
        modifier("liquidFlammable") { this + TypeRegistry.resolve<ConsumeLiquidFlammable>(it) }
        modifier("coolant") { this + TypeRegistry.resolve<ConsumeCoolant>(it) }

        modifier("clearPower") { filterNot { it is ConsumePower }.toTypedArray() }
        modifier("power") {
            this.filterNot { c -> c is ConsumePower }.toTypedArray() + TypeRegistry.resolve<ConsumePower>(it)
        }
        modifier("powerBuffered") {
            this.filterNot { c -> c is ConsumePower }.toTypedArray() + ConsumePower(0f, it.asFloat(), true)
        }
    }

    override fun <T : Any> resolveType(json: JsonValue, type: Class<T>?, elementType: Class<*>?, keyType: Class<*>?): T? {
        @Suppress("UNCHECKED_CAST")
        when {
            type == ConsumeItems::class.java && json.isArray ->
                return ConsumeItems(TypeRegistry.resolve(json)) as T

            type == ConsumeLiquids::class.java && json.isArray ->
                return ConsumeLiquids(TypeRegistry.resolve(json)) as T

            type == ConsumePower::class.java && json.isNumber ->
                return ConsumePower(json.asFloat(), 0.0f, false) as T

        }
        return null
    }
}