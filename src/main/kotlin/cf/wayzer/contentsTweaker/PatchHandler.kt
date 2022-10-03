package cf.wayzer.contentsTweaker

import arc.util.Log
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import cf.wayzer.contentsTweaker.resolvers.*

object PatchHandler {
    fun interface Resolver {
        fun resolve(node: Node, child: String): Node?
    }

    val resolvers = mutableListOf(
        MindustryExt,
        ContentResolver,
        BaseModifier,

        SeqResolver,
        MapResolver,
        ReflectResolver,
    )
    val store = mutableMapOf<String, Node>()

    /**parser处理完毕后,进行收尾操作*/
    private val afterHandler = mutableMapOf<String, () -> Unit>()

    /**
     * Node储存规则
     * * 仅支持[recover]的能够储存到store中
     * * [parent]已经[hasStore],子[Node]无需[doStore]
     * * 全部恢复时,优先恢复[parent] (impl: 按[key]排序即可
     */
    abstract class Node(val key: String) {
        abstract val parent: Node

        open fun hasStore(): Boolean = parent.hasStore() || (key in store)

        open fun doStore() {
            if (this is Modifiable) {
                doStore0()
                store.putIfAbsent(key, this)
            } else parent.doStore()
        }

        //self First, super as fallback
        @Throws(Throwable::class)
        open fun resolve(child: String): Node {
            return resolvers.firstNotNullOfOrNull { it.resolve(this, child) }
                ?: error("Can't resolve child node: ${subKey(child)}")
        }

        fun subKey(child: String) = "$key$child." // end with .


        override fun toString(): String {
            return "Node(key='$key')"
        }

        interface WithObj {
            val obj: Any?
            val type: Class<*>?
            val elementType: Class<*>? get() = null
            val keyType: Class<*>? get() = null
        }

        //Node with obj, not Modifiable, use as simple node
        class ObjNode(override val parent: Node, key: String, override val obj: Any, override val type: Class<out Any> = obj.javaClass) :
            Node(key), WithObj

        interface Modifiable : WithObj {
            /**
             * 可直接修改[obj]对象.通过其他机制可保证还原
             * true时: [doStore0]负责拷贝对象,并保存,由[recover]负责恢复
             * false时: 由[Modifier]负责产生新对象并[setValue]
             * */
            val mutableObj: Boolean get() = false
            fun setValue(value: Any?)

            fun doStore0() {
                if (mutableObj) error("mutable Modifiable need impl doStore0 to backup obj")
            }

            fun recover() {
                if (mutableObj) error("mutable Modifiable need impl recover to recover obj")
                setValue(obj)
            }
        }

        fun interface Modifier {
            /**
             * 1. 判断状态(parent), 根据[Modifiable.type],[Modifiable.elementType],[Modifiable.keyType]解析[json]
             * 2. 然后判断[hasStore],并调用[doStore]
             * 3. 调用[Modifiable.setValue]
             */
            @Throws(Throwable::class)
            fun setValue(json: JsonValue)
        }

        abstract class ModifierNode(override val parent: Node, key: String) : Node(key), Modifier

        object Root : Node("") {
            override val parent: Node get() = error("Root no parent")
            override fun hasStore() = false
            override fun doStore() = error("No Node support store")
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
        if (node is Node.Modifier || !json.isObject) {
            if (node !is Node.Modifier) return handle(json, node.resolve("="))
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
        store.values.sortedBy { it.key }.forEach {
            (it as Node.Modifiable).recover()
        }
        store.clear()
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