package cf.wayzer.contentsTweaker.resolvers

import arc.struct.Seq
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
import cf.wayzer.contentsTweaker.TypeRegistry
import mindustry.io.JsonIO

object SeqResolver : PatchHandler.Resolver {
    class SeqItemNode<T : Any>(override val parent: Node, val index: Int, key: String) : Node(key), Node.Modifiable<T> {
        @Suppress("UNCHECKED_CAST")
        private val seq = (parent as WithObj<*>).obj as Seq<T>
        override val obj: T get() = seq.get(index)
        override val type: Class<*> get() = ((parent as WithObj<*>).elementType ?: obj.javaClass)
        override val storeDepth: Int get() = 0
        private val bak = obj
        override fun doSave() {}
        override fun doRecover() = setValue(bak)

        override fun setValue(value: T) {
            seq.set(index, value)
        }
    }

    class AsModifiable<T : Any>(override val parent: Node, override val obj: Seq<T>, val deepCopy: Boolean) : Node(parent.key), Node.Modifiable<Seq<T>> {
        private lateinit var backup: Seq<T>
        override val type: Class<Seq<*>> = Seq::class.java
        override val elementType: Class<*>? = (parent as WithObj<*>).elementType ?: obj.firstOrNull()?.javaClass

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

        override fun setValue(value: Seq<T>) {
            obj.clear()
            obj.addAll(value)
        }
    }

    override fun resolve(node: Node, child: String): Node? {
        if (node !is Node.WithObj<*>) return null
        val obj = node.obj
        if (obj !is Seq<*>) return null
        //通过数字索引
        child.toIntOrNull()?.let { return SeqItemNode<Any>(node, it, node.subKey(child)) }
        when (child) {
            "+=" -> {
                if (node !is Node.Modifiable<*>) error("${node.key} is Seq<*>, but not Modifiable, try use `asModifiable`")
                return node.withModifier(child) { json ->
                    val value = TypeRegistry.resolve<Seq<Any>>(json, elementType)
                    beforeModify()
                    @Suppress("UNCHECKED_CAST")
                    setValueAny(obj.copy().addAll(value as Seq<out Nothing>))
                }
            }
            //不支持-运算符,容易导致索引型的解析错误或者失败

            "+" -> {
                if (node !is Node.Modifiable<*>) error("${node.key} is Seq<*>, but not Modifiable, try use `asModifiable`")
                return node.withModifier(child) { json ->
                    val value = TypeRegistry.resolveType(json, elementType)
                    beforeModify()
                    @Suppress("UNCHECKED_CAST")
                    val parentSeq = obj as Seq<Any>
                    setValueAny(parentSeq.copy().add(value))
                }
            }

            "asModifiable" -> return AsModifiable(node, obj, deepCopy = false)
            "asModifiableDeep" -> return AsModifiable(node, obj, deepCopy = true)
        }
        return null
    }
}