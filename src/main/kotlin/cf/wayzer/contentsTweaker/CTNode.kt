package cf.wayzer.contentsTweaker

import arc.util.Log
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import cf.wayzer.contentsTweaker.CTNode.*
import cf.wayzer.contentsTweaker.util.ExtendableClass
import cf.wayzer.contentsTweaker.util.ExtendableClassDSL

/**
 * 所有节点都是[CTNode]
 * 如果一个节点与某个具体对象有关，应当实现[ObjInfo]
 *   [ObjInfo.obj]应当始终返回原始值(不管后来有没有修改过)
 * 如果一个节点绑定属性可被重新赋值，实现[Modifiable]
 * 如果一个节点是运算符节点(末端节点)，可接收JSON对象，实现[Modifier]
 * [AfterHandler]将在节点或子节点[Modifiable.setValue]后注册，批处理结束后统一调用
 *
 * 所有节点，在获得实例，使用前必须调用[collectAll] ([ContentsTweaker.NodeCollector]内不需要)
 * */
class CTNode private constructor() : ExtendableClass<CTExtInfo>() {
    val children = mutableMapOf<String, CTNode>()
    private var collected = false
    fun collectAll(): CTNode {
        if (collected) return this
        collected = true
        resolvers.forEach { it.collectChild(this) }
        get<Modifiable<Any?>>()?.let { modifiable ->
            getOrCreate("=").apply {
                +Modifier { json ->
                    modifiable.setJson(json)
                }
                +ToJson {
                    it.value(modifiable.currentValue?.let(TypeRegistry::getKeyString))
                }
            }
        }
        return this
    }

    fun resolve(name: String): CTNode {
        val node = children[name]
            ?: getAll<Indexable>().firstNotNullOfOrNull { it.resolve(name) }
            ?: error("Not found child $name")
        node.collectAll()
        return node
    }

    /** 供[ContentsTweaker.NodeCollector]使用，解析清使用[resolve]*/
    @ExtendableClassDSL
    fun getOrCreate(child: String): CTNode {
        return children.getOrPut(child) { CTNode() }
    }

    interface CTExtInfo
    data class ObjInfo<T>(
        val obj: T, val type: Class<out T & Any>,
        val elementType: Class<*>? = null, // List.T or Map.V
        val keyType: Class<*>? = null // Map.K
    ) : CTExtInfo {
        constructor(obj: T & Any) : this(obj, obj::class.java)
    }

    fun interface Resettable : CTExtInfo {
        fun reset()
    }

    /** [T] must equal [ObjInfo.type]*/
    @Suppress("MemberVisibilityCanBePrivate")
    abstract class Modifiable<T>(node: CTNode) : CTExtInfo, Resettable {
        val info = node.get<ObjInfo<T>>()!!
        internal var nodeStack: List<CTNode>? = null
        abstract val currentValue: T
        protected abstract fun setValue0(value: T)
        fun setValue(value: T) {
            PatchHandler.modified(this)
            setValue0(value)
        }

        fun setValueAny(value: Any?) {
            val type = info.type

            @Suppress("UNCHECKED_CAST")
            val v = (if (type.isPrimitive) value else type.cast(value)) as T
            setValue(v)
        }

        fun setJson(v: JsonValue) {
            val value = info.run { TypeRegistry.resolveType(v, type, elementType, keyType) }
            setValue(value)
        }

        override fun reset() = setValue(info.obj)
    }

    fun interface Indexable : CTExtInfo {
        /** 解析索引,[key]已去除# */
        fun resolveIndex(key: String): CTNode?

        /** 通用的[name]索引(不一定以#开头) */
        fun resolve(name: String): CTNode? {
            if (name.isEmpty() || name[0] != '#') return null
            return resolveIndex(name.substring(1))
        }
    }

    fun interface Modifier : CTExtInfo {
        /**
         * 1. 判断状态(parent), 根据[ObjInfo.type],[ObjInfo.elementType],[ObjInfo.keyType]解析[json]
         * 2. 赋值直接调用[Modifiable.setValue]。如果是增量修改，使用[Modifiable.currentValue]获取读取值
         */
        @Throws(Throwable::class)
        fun setValue(json: JsonValue)
    }

    fun interface AfterHandler : CTExtInfo {
        fun handle()
    }

    /** 为[ContentsTweaker.exportAll]自定义输出 */
    fun interface ToJson : CTExtInfo {
        fun write(jsonWriter: JsonWriter)
    }

    companion object {
        private val resolvers = ContentsTweaker.resolvers
        val Root = CTNode()
        val Nope = CTNode()

        init {
            Nope.apply {
                +Modifier { }
                +Indexable { Nope }
            }
        }
    }

    object PatchHandler {
        private object NodeStack {
            private val stack = mutableListOf<Pair<CTNode, String>>()
            val last get() = stack.last()
            fun getParents() = stack.map { it.first }
            fun getId(node: CTNode? = null) =
                stack.takeWhile { it.first !== node }.joinToString(".") { it.second }

            fun resolve(node: CTNode, child: String): CTNode {
                stack.add(node to child)
                return node.resolve(child)
            }

            fun pop(node: CTNode) {
                @Suppress("ControlFlowWithEmptyBody")
                while (stack.removeLast().first !== node);
            }
        }

        val resetHandlers = mutableSetOf<Resettable>()
        private val afterHandlers = mutableSetOf<AfterHandler>()

        fun modified(modifiable: Modifiable<*>) {
            resetHandlers.add(modifiable)
            val parents = modifiable.nodeStack ?: NodeStack.getParents().also {
                modifiable.nodeStack = it
            }
            parents.flatMapTo(afterHandlers) { it.getAll<AfterHandler>() }
        }

        fun doAfterHandle() {
            afterHandlers.forEach { it.handle() }
            afterHandlers.clear()
        }

        fun recoverAll() {
            resetHandlers.forEach { it.reset() }
            resetHandlers.clear()
            doAfterHandle()

            Root.children.clear()
            Root.collected = false
        }

        fun handle(json: JsonValue, node: CTNode = Root.collectAll()) {
            //部分简化,如果value不是object可省略=运算符
            if (!json.isObject && node.get<Modifier>() == null)
                return handle(json, NodeStack.resolve(node, "="))
            node.get<Modifier>()?.let {
                try {
                    it.setValue(json)
                } catch (e: Throwable) {
                    Log.err("Fail to apply Modifier ${NodeStack.getId()}: ${json.prettyPrint(JsonWriter.OutputType.minimal, 0)}:\n $e")
                }
                return
            }

            for (child in json) {
                val names = child.name.split(".")
                val childNode = try {
                    names.fold(node, NodeStack::resolve)
                } catch (e: Throwable) {
                    val (errNode, errChild) = NodeStack.last
                    Log.err("Fail to resolve child ${NodeStack.getId(errNode)}->${errChild}:\n $e")
                    NodeStack.pop(node)
                    continue
                }
                handle(child, childNode)
                NodeStack.pop(node)
            }
        }
    }
}
