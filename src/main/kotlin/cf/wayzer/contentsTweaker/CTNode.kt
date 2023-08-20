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
class CTNode private constructor(val name: String, val parent: CTNode?) : ExtendableClass<CTExtInfo>() {
    val id: String get() = if (parent == null) name else "${parent.id}.$name"
    val children = mutableMapOf<String, CTNode>()
    private var collected = false
    fun collectAll(): CTNode {
        if (collected) return this
        collected = true
        resolvers.forEach { it.collectChild(this) }
        get<Modifiable<Any>>()?.let { modifiable ->
            getOrCreate("=") += Modifier { json ->
                modifiable.setJson(json)
            }
        }
        return this
    }

    fun resolve(name: String): CTNode {
        collectAll()
        return children[name]?.collectAll() ?: error("Not found child $name")
    }

    /** 供[ContentsTweaker.NodeCollector]使用，解析清使用[resolve]*/
    @ExtendableClassDSL
    fun getOrCreate(child: String): CTNode {
        return children.getOrPut(child) { CTNode(child, this) }
    }

    override fun toString(): String {
        return "CTNode(id='$id')"
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
    abstract class Modifiable<T>(val node: CTNode) : CTExtInfo, Resettable {
        val info = node.get<ObjInfo<T>>()!!
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
        val Root = CTNode("ROOT", null)
    }

    object PatchHandler {
        val resetHandlers = mutableSetOf<Resettable>()
        private val afterHandlers = mutableSetOf<AfterHandler>()

        fun modified(modifiable: Modifiable<*>) {
            resetHandlers.add(modifiable)
            var node = modifiable.node
            while (true) {
                afterHandlers += node.getAll<AfterHandler>()
                node = node.parent ?: break
            }
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

        fun handle(json: JsonValue, node: CTNode = Root) {
            //部分简化,如果value不是object可省略=运算符
            if (!json.isObject && node.get<Modifier>() == null)
                return handle(json, node.resolve("="))
            node.get<Modifier>()?.let {
                try {
                    it.setValue(json)
                } catch (e: Throwable) {
                    Log.err("Fail to apply Modifier ${node.id}: ${json.prettyPrint(JsonWriter.OutputType.minimal, 0)}:\n $e")
                }
                return
            }

            for (child in json) {
                val names = child.name.split(".")
                val childNode = try {
                    names.fold(node, CTNode::resolve)
                } catch (e: Throwable) {
                    Log.err("Fail to resolve child ${node.id}->${child.name}:\n $e")
                    continue
                }
                handle(child, childNode)
            }
        }

        @Deprecated("for simple", level = DeprecationLevel.ERROR)
        fun simple() {
            //block.copper-wall-large.health = 120
            val node = Root
                .resolve("block")
                .resolve("copper-wall-large")
                .resolve("health")
                .resolve("=")
            node.get<Modifier>()!!.setValue(JsonValue(120))
        }
    }
}
