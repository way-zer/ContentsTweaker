package cf.wayzer.contentsTweaker

import arc.util.Log
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import cf.wayzer.contentsTweaker.resolvers.*

object PatchHandler {
    fun interface Resolver {
        fun resolve(node: Node, child: String): Node?
    }

    private val resolvers = ContentsTweaker.resolvers
    private val storeMap = mutableMapOf<String, Node>()

    /**parser处理完毕后,进行收尾操作*/
    private val afterHandler = mutableMapOf<String, () -> Unit>()

    /**
     * Node储存规则
     * * 仅支持[recover]的能够储存到store中
     * * 全部恢复时,优先恢复[parent] (impl: 按[key]排序即可
     */
    abstract class Node(val key: String) {
        abstract val parent: Node

        //self First, super as fallback
        @Throws(Throwable::class)
        open fun resolve(child: String): Node {
            return resolvers.firstNotNullOfOrNull { it.resolve(this, child) }
                ?: error("Can't resolve child node: ${subKey(child)}")
        }

        fun subKey(child: String) = "$key$child." // end with .


        fun stored(depth: Int = 0): Boolean {
            if (this == Root) return false
            return parent.stored(depth + 1)
                    || (this is Storable && key in storeMap && (storeDepth >= depth))
        }


        fun beforeModify(depth: Int = 0) {
            if (!stored(depth)) store(depth)
            onModify()
        }

        private fun store(depth: Int = 0) {
            if (this == Root) error("Can't store")
            if (this is Storable && storeDepth >= depth) {
                doSave()
                storeMap[key] = this
            } else parent.store(depth + 1)
        }

        /** for register afterHandler */
        protected open fun onModify() {
            parent.onModify()
        }

        override fun toString(): String {
            return "Node(key='$key')"
        }

        interface WithObj<T> {
            val obj: T
            val type: Class<*>
            val elementType: Class<*>? get() = null
            val keyType: Class<*>? get() = null
        }

        //Node with obj, not Modifiable, use as simple node
        class ObjNode<T : Any>(override val parent: Node, key: String, override val obj: T, override val type: Class<T> = obj.javaClass) :
            Node(key), WithObj<T>

        interface Storable {
            val storeDepth: Int
            fun doSave()
            fun doRecover()
        }

        interface Modifiable<T> : WithObj<T>, Storable {
            fun setValue(value: T)

            fun setValueAny(value: Any?) {
                @Suppress("UNCHECKED_CAST")
                val v = (if (type.isPrimitive) value else type.cast(value)) as T
                setValue(v)
            }
        }

        fun interface Modifier {
            /**
             * 1. 判断状态(parent), 根据[Modifiable.type],[Modifiable.elementType],[Modifiable.keyType]解析[json]
             * 2. 然后调用[beforeModify]
             * 3. 调用[Modifiable.setValue]
             */
            @Throws(Throwable::class)
            fun setValue(json: JsonValue)
        }

        abstract class ModifierNode(override val parent: Node, key: String) : Node(key), Modifier

        object Root : Node("") {
            override val parent: Node get() = error("Root no parent")
            override fun onModify() = Unit
            override fun toString() = "Node(ROOT)"
        }
    }

    fun <T : Node> T.withModifier(type: String, impl: T.(json: JsonValue) -> Unit): Node.ModifierNode {
        return object : Node.ModifierNode(this, subKey(type)) {
            override fun setValue(json: JsonValue) = this@withModifier.impl(json)
        }
    }

    fun handle(json: JsonValue, node: Node = Node.Root) {
        //部分简化,如果value不是object可省略=运算符
        if (node !is Node.Modifier && !json.isObject)
            return handle(json, node.resolve("="))
        if (node is Node.Modifier) {
            try {
                node.setValue(json)
            } catch (e: Throwable) {
                Log.err("Fail to handle ${node.key} ${json.prettyPrint(JsonWriter.OutputType.minimal, 0)}", e)
            }
            return
        }

        for (child in json) {
            val names = child.name.split(".")
            val childNode = try {
                names.fold(node, Node::resolve)
            } catch (e: Throwable) {
                Log.err(e)
                continue
            }
            handle(child, childNode)
        }
    }

    fun registerAfterHandler(key: String, body: () -> Unit) {
        afterHandler.putIfAbsent(key, body)
    }

    fun doAfterHandle() {
        afterHandler.values.forEach { it.invoke() }
        afterHandler.clear()
    }

    fun recoverAll() {
        storeMap.values.sortedBy { it.key }.forEach {
            it.beforeModify()
            (it as Node.Storable).doRecover()
        }
        doAfterHandle()
        storeMap.clear()
    }


    @Deprecated("for simple", level = DeprecationLevel.ERROR)
    fun simple() {
        //block.copper-wall-large.health = 120
        val node = Node.Root
            .resolve("block")
            .resolve("copper-wall-large")
            .resolve("health")
            .resolve("=")
        (node as Node.Modifier).setValue(JsonValue(120))
    }
}