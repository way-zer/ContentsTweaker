package cf.wayzer.contentsTweaker.resolvers

import arc.struct.Seq
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
import cf.wayzer.contentsTweaker.TypeRegistry
import mindustry.io.JsonIO

object SeqResolver : PatchHandler.Resolver {
    class SeqItemNode(override val parent: Node, val index: Int, key: String) : Node(key), Node.Modifiable {
        @Suppress("UNCHECKED_CAST")
        private val seq = (parent as WithObj).obj as Seq<Any>

        override val obj: Any? = seq.get(index)
        override val type: Class<*>?
            get() = (parent as WithObj).elementType ?: obj?.javaClass
        override val storeDepth: Int get() = 0
        override fun doSave() {}
        override fun doRecover() = setValue(obj)

        override fun setValue(value: Any?) {
            seq.set(index, value)
        }
    }

    class AsModifiable(override val parent: Node, override val obj: Seq<*>, val deepCopy: Boolean) : Node(parent.key), Node.Modifiable {
        private lateinit var backup: Seq<*>
        override val type: Class<*> = Seq::class.java
        override val elementType: Class<*>? = (parent as WithObj).elementType ?: obj.firstOrNull()?.javaClass

        override val storeDepth: Int get() = if (deepCopy) Int.MAX_VALUE else 0
        override fun doSave() {
            backup = if (deepCopy)
                obj.map { JsonIO.copy(it) }
            else
                obj.copy()
        }

        override fun doRecover() {
            setValue(backup)
        }

        override fun setValue(value: Any?) {
            @Suppress("UNCHECKED_CAST")
            val value2 = value as Seq<out Nothing>?
            obj.clear()
            if (value2 != null)
                obj.addAll(value2)
        }
    }

    override fun resolve(node: Node, child: String): Node? {
        if (node !is Node.WithObj) return null
        val obj = node.obj
        if (obj !is Seq<*>) return null
        //通过数字索引
        child.toIntOrNull()?.let { return SeqItemNode(node, it, node.subKey(child)) }
        when (child) {
            "+=" -> {
                if (node !is Node.Modifiable) error("${node.key} is Seq<*>, but not Modifiable, try use `asModifiable`")
                return node.withModifier(child) { json ->
                    val value = TypeRegistry.resolve<Seq<Any>>(json, elementType)
                    beforeModify()
                    @Suppress("UNCHECKED_CAST")
                    setValue(obj.copy().addAll(value as Seq<out Nothing>))
                }
            }
            //不支持-运算符,容易导致索引型的解析错误或者失败

            "+" -> {
                if (node !is Node.Modifiable) error("${node.key} is Seq<*>, but not Modifiable, try use `asModifiable`")
                return node.withModifier(child) { json ->
                    val value = TypeRegistry.resolveType(json, elementType)
                    beforeModify()
                    @Suppress("UNCHECKED_CAST")
                    val parentSeq = obj as Seq<Any>
                    setValue(parentSeq.copy().add(value))
                }
            }

            "asModifiable" -> return AsModifiable(node, obj, deepCopy = false)
            "asModifiableDeep" -> return AsModifiable(node, obj, deepCopy = true)
        }
        return null
    }
}