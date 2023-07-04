package cf.wayzer.contentsTweaker.resolvers

import arc.struct.Seq
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.registryResetHandler
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
import cf.wayzer.contentsTweaker.TypeRegistry

object SeqResolver : PatchHandler.Resolver {
    fun <T> saveSeq(list: Seq<T>) {
        registryResetHandler(list, "items") {
            val backup = it.copy()
            fun() {
                it.clear();it.addAll(backup)
            }
        }
    }

    class SeqItemNode<T : Any>(override val parent: WithObj<Seq<T>>, val index: Int, override val key: String) : Node.Modifiable<T>() {
        override val obj: T get() = parent.obj.get(index)

        @Suppress("UNCHECKED_CAST")
        override val type: Class<out T> get() = ((parent as WithObj<*>).elementType ?: obj.javaClass) as Class<out T>
        override fun saveValue0() {
            saveSeq(parent.obj)
        }

        override fun setValue(value: T) {
            parent.obj.set(index, value)
        }
    }

    class AsModifiable<T : Any>(override val parent: WithObj<Seq<T>>) : Node.Modifiable<Seq<T>>() {
        override val key: String = "AsModifiable"
        override val obj by parent::obj
        override val type by parent::type
        override val elementType by parent::elementType

        override fun saveValue0() {
            saveSeq(parent.obj)
        }

        override fun setValue(value: Seq<T>) {
            obj.clear()
            obj.addAll(value)
        }
    }

    override fun resolve(node: Node, child: String): Node? {
        if (node !is Node.WithObj<*> || !Seq::class.java.isAssignableFrom(node.type))
            return null
        @Suppress("UNCHECKED_CAST")
        node as Node.WithObj<Seq<Any>>

        //通过数字索引
        child.toIntOrNull()?.let { return SeqItemNode(node, it, child) }
        when (child) {
            "+=" -> {
                if (node !is Node.Modifiable<*>) error("${node.id} is Seq<*>, but not Modifiable, try use `asModifiable`")
                return node.withModifier(child) { json ->
                    val value = TypeRegistry.resolve<Seq<Any>>(json, elementType)
                    saveValue()
                    setValueAny(obj.copy().addAll(value))
                }
            }
            //不支持-运算符,容易导致索引型的解析错误或者失败

            "+" -> {
                if (node !is Node.Modifiable<*>) error("${node.id} is Seq<*>, but not Modifiable, try use `asModifiable`")
                return node.withModifier(child) { json ->
                    val value = TypeRegistry.resolveType(json, elementType)
                    saveValue()
                    setValueAny(obj.copy().add(value))
                }
            }

            "asModifiable" -> return AsModifiable(node)
        }
        return null
    }
}