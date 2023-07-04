package cf.wayzer.contentsTweaker.resolvers

import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
import cf.wayzer.contentsTweaker.TypeRegistry
import mindustry.type.ItemStack
import mindustry.world.Block
import mindustry.world.consumers.*

object BlockConsumesResolver : PatchHandler.Resolver, TypeRegistry.Resolver {
    override fun resolve(node: Node, child: String): Node? {
        if (node !is Node.Modifiable<*> || !Array<Consume>::class.java.isAssignableFrom(node.type))
            return null
        @Suppress("UNCHECKED_CAST")
        node as Node.Modifiable<Array<Consume>>

        val block = ((node.parent as Node.WithObj<*>).obj as? Block) ?: return null
        fun modifier(body: Array<Consume>.(JsonValue) -> Array<Consume>) = node.withModifier(child) { v ->
            val new = obj.body(v)
            saveValue()
            PatchHandler.registerAfterHandler(id) {
                block.apply {
                    consPower = consumers.filterIsInstance<ConsumePower>().firstOrNull()
                    optionalConsumers = consumers.filter { it.optional && !it.ignore() }.toTypedArray()
                    nonOptionalConsumers = consumers.filter { !it.optional && !it.ignore() }.toTypedArray()
                    updateConsumers = consumers.filter { it.update && !it.ignore() }.toTypedArray()
                    hasConsumers = consumers.isNotEmpty()
                    itemFilter.fill(false)
                    liquidFilter.fill(false)
                    consumers.forEach { it.apply(this) }
                    setBars()
                }
            }
            setValue(new)
        }
        return when (child) {
            "clearItems" -> modifier { filterNot { it is ConsumeItems || it is ConsumeItemFilter }.toTypedArray() }
            "item" -> modifier { this + ConsumeItems(arrayOf(ItemStack(TypeRegistry.resolve(it), 1))) }
            "items" -> modifier { this + TypeRegistry.resolve<ConsumeItems>(it) }
            "itemCharged" -> modifier { this + TypeRegistry.resolve<ConsumeItemCharged>(it) }
            "itemFlammable" -> modifier { this + TypeRegistry.resolve<ConsumeItemFlammable>(it) }
            "itemRadioactive" -> modifier { this + TypeRegistry.resolve<ConsumeItemRadioactive>(it) }
            "itemExplosive" -> modifier { this + TypeRegistry.resolve<ConsumeItemExplosive>(it) }
            "itemExplode" -> modifier { this + TypeRegistry.resolve<ConsumeItemExplode>(it) }

            "clearLiquids" -> modifier { filterNot { it is ConsumeLiquidBase || it is ConsumeLiquids }.toTypedArray() }
            "liquid" -> modifier { this + TypeRegistry.resolve<ConsumeLiquid>(it) }
            "liquids" -> modifier { this + TypeRegistry.resolve<ConsumeLiquids>(it) }
            "liquidFlammable" -> modifier { this + TypeRegistry.resolve<ConsumeLiquidFlammable>(it) }
            "coolant" -> modifier { this + TypeRegistry.resolve<ConsumeCoolant>(it) }

            "clearPower" -> modifier { filterNot { it is ConsumePower }.toTypedArray() }
            "power" -> modifier {
                this.filterNot { c -> c is ConsumePower }.toTypedArray() + TypeRegistry.resolve<ConsumePower>(it)
            }

            "powerBuffered" -> modifier {
                this.filterNot { c -> c is ConsumePower }.toTypedArray() + ConsumePower(0f, it.asFloat(), true)
            }

            else -> null
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