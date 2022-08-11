package cf.wayzer.contentsTweaker.resolvers

import arc.struct.Seq
import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
import cf.wayzer.contentsTweaker.TypeRegistry
import cf.wayzer.contentsTweaker.util.reflectDelegate
import mindustry.io.JsonIO
import mindustry.world.Block
import mindustry.world.consumers.*

object MindustryExt : PatchHandler.Resolver, TypeRegistry.Resolver {
    private var Block.consumeBuilder: Seq<Consume> by reflectDelegate()

    class BlockConsumersNode(override val parent: Node, key: String) : Node(key), Node.Modifiable {
        val block = (parent as WithObj).obj as Block
        override val obj = block.consumeBuilder
        override val type: Class<*> = Seq::class.java
        override val elementType: Class<*> = Consume::class.java

        override val mutableObj: Boolean get() = true
        private lateinit var backup: Seq<Consume>
        override fun doStore() {
            backup = obj.map { JsonIO.copy(it) }
        }

        override fun recover() = setValue(backup)

        override fun setValue(value: Any?) {
            @Suppress("UNCHECKED_CAST")
            block.consumeBuilder = value as Seq<Consume>
        }

        override fun resolve(child: String): Node {
            fun modifier(body: (JsonValue) -> Unit) = withModifier(child) { jsonValue ->
                if (hasStore()) doStore()
                body(jsonValue)
                PatchHandler.registerAfterHandler(key) {
                    block.apply {
                        consumers = obj.toArray()
                        optionalConsumers = obj.select { it.optional && !it.ignore() }.toArray()
                        nonOptionalConsumers = obj.select { !it.optional && !it.ignore() }.toArray()
                        hasConsumers = consumers.isNotEmpty()
                        itemFilter.fill(false)
                        liquidFilter.fill(false)
                        consumers.forEach { it.apply(this) }
                        setBars()
                    }
                }
            }
            when (child) {
                "clearItems" -> modifier { obj.removeAll { it is ConsumeItems || it is ConsumeItemFilter } }
                "item" -> modifier { block.consumeItem(TypeRegistry.resolve(it)) }
                "items" -> modifier { block.consume(TypeRegistry.resolve<ConsumeItems>(it)) }
                "itemCharged" -> modifier { block.consume(TypeRegistry.resolve<ConsumeItemCharged>(it)) }
                "itemFlammable" -> modifier { block.consume(TypeRegistry.resolve<ConsumeItemFlammable>(it)) }
                "itemRadioactive" -> modifier { block.consume(TypeRegistry.resolve<ConsumeItemRadioactive>(it)) }
                "itemExplosive" -> modifier { block.consume(TypeRegistry.resolve<ConsumeItemExplosive>(it)) }
                "itemExplode" -> modifier { block.consume(TypeRegistry.resolve<ConsumeItemExplode>(it)) }

                "clearLiquids" -> modifier { obj.removeAll { it is ConsumeLiquidBase || it is ConsumeLiquids } }
                "liquid" -> modifier { block.consume(TypeRegistry.resolve<ConsumeLiquid>(it)) }
                "liquids" -> modifier { block.consume(TypeRegistry.resolve<ConsumeLiquids>(it)) }
                "liquidFlammable" -> modifier { block.consume(TypeRegistry.resolve<ConsumeLiquidFlammable>(it)) }
                "coolant" -> modifier { block.consume(TypeRegistry.resolve<ConsumeCoolant>(it)) }

                "clearPower" -> modifier { obj.removeAll { it is ConsumePower } }
                "power" -> modifier { block.consume(TypeRegistry.resolve<ConsumePower>(it)) }
                "powerBuffered" -> modifier { block.consumePowerBuffered(it.asFloat()) }
            }
            return super.resolve(child)
        }
    }

    override fun resolve(node: Node, child: String): Node? {
        if (node !is Node.WithObj) return null
        val obj = node.obj
        when {
            obj is Block && child == "consumes" -> return BlockConsumersNode(node, node.subKey(child))
        }
        return null
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