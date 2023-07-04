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

    val resetHandler = mutableMapOf<Pair<Any, Any?>, () -> Unit>()// obj,field -> recover
    inline fun <Obj : Any> registryResetHandler(obj: Obj, field: Any?, block: (Obj) -> (() -> Unit)) {
        if ((obj to field) in resetHandler) return
        resetHandler[obj to field] = block(obj)
    }

    val afterHandler = mutableMapOf<Any, () -> Unit>()// key -> callback
    inline fun registerAfterHandler(key: Any, crossinline body: () -> Unit) {
        if (key in afterHandler) return
        afterHandler[key] = {
            body()
        }
    }

    /**
     * Node储存规则
     * * 仅支持[recover]的能够储存到store中
     * * 全部恢复时,优先恢复[parent] (impl: 按[key]排序即可
     */
    abstract class Node {
        protected open val childrenNode = LinkedHashMap<String, Node>()
        abstract val parent: Node

        abstract val key: String
        val id: String get() = idPrefix + key
        open val idPrefix: String get() = parent.idPrefix + key + "."

        //self First, super as fallback
        @Throws(Throwable::class)
        open fun resolve(child: String): Node {
            childrenNode[child]?.let { return it }
            return resolvers.firstNotNullOfOrNull { it.resolve(this, child) }
                ?.also { childrenNode[child] = it }
                ?: error("Can't resolve child node: $id")
        }

        /** for register afterHandler */
        protected open fun afterModify(modifier: Modifier) {
            parent.afterModify(modifier)
        }

        override fun toString(): String {
            return "Node(id='$id')"
        }

        abstract class WithObj<T> : Node() {
            open val externalObject: Boolean = false
            abstract val obj: T
            abstract val type: Class<out T>
            open val elementType: Class<*>? get() = null // List.T or Map.V
            open val keyType: Class<*>? get() = null // Map.K
        }

        class ObjNode<T : Any>(
            override val parent: Node, override val key: String, override val obj: T,
            override val type: Class<out T> = obj.javaClass,
            override val externalObject: Boolean = false
        ) : WithObj<T>()

        abstract class Modifiable<T> : WithObj<T>() {
            fun saveValue() {
                //don't save for external object
                var node: Node = parent
                while (node != Root) {
                    if (node is WithObj<*> && node.externalObject) return
                    node = node.parent
                }
                saveValue0()
            }

            protected abstract fun saveValue0()
            abstract fun setValue(value: T)
            fun setValueAny(value: Any?) {
                @Suppress("UNCHECKED_CAST")
                val v = (if (type.isPrimitive) value else type.cast(value)) as T
                setValue(v)
            }

            override fun resolve(child: String): Node {
                if (child == "=") {
                    return withModifier(child) { json ->
                        val value = TypeRegistry.resolveType(json, type, elementType, keyType)
                        setValueAny(value)
                    }
                }
                return super.resolve(child)
            }
        }

        abstract class Modifier : Node() {
            /**
             * 1. 判断状态(parent), 根据[Modifiable.type],[Modifiable.elementType],[Modifiable.keyType]解析[json]
             * 2. 然后调用[Modifiable.saveValue]
             * 3. 调用[Modifiable.setValue] (depth=0) 或者修改[WithObj.obj]属性(depth=1)
             * 4. 然后调用[afterModify]
             */
            @Throws(Throwable::class)
            abstract fun setValue(json: JsonValue)
        }

        object Root : Node() {
            override val key: String = "ROOT"
            override val parent: Node get() = error("Root no parent")
            override val idPrefix: String = ""
            override fun afterModify(modifier: Modifier) = Unit
            override fun toString() = "Node(ROOT)"
        }
    }

    fun <T : Node> T.withModifier(type: String, block: T.(json: JsonValue) -> Unit): Node.Modifier {
        return object : Node.Modifier() {
            override val parent: Node = this@withModifier
            override val key: String = type
            override fun setValue(json: JsonValue) {
                block(json)
                afterModify(this)
            }
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

    fun doAfterHandle() {
        afterHandler.values.forEach { it.invoke() }
        afterHandler.clear()
    }

    fun recoverAll() {
        resetHandler.values.forEach { it() }
        resetHandler.clear()
        doAfterHandle()
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