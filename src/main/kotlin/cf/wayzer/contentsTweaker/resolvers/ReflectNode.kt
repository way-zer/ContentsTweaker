package cf.wayzer.contentsTweaker.resolvers

import arc.util.serialization.Json.FieldMetadata
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import java.lang.reflect.Field


class ReflectNode<T>(override val parent: Node, key: String, val f: Field) : Node(key), Node.Modifiable<T> {
    @Suppress("UNCHECKED_CAST")
    override val obj: T get() = f.get((parent as WithObj<*>).obj) as T

    @Suppress("UNCHECKED_CAST")
    override val type: Class<T> get() = this.f.type as Class<T>
    private val typeMeta: FieldMetadata by lazy { this.f.let(::FieldMetadata) }
    override val elementType: Class<*>? get() = typeMeta.elementType
    override val keyType: Class<*>? get() = typeMeta.keyType

    override val storeDepth: Int get() = 0
    private val bak = obj
    override fun doSave() {}//already
    override fun doRecover() {
        setValue(bak)
    }

    override fun setValue(value: T) {
        f.set((parent as WithObj<*>).obj, value)
    }

    companion object Resolver : PatchHandler.Resolver {
        private val fieldCache = mutableMapOf<Pair<Class<*>, String>, Field>()
        private fun getField(obj: Any, name: String): Field {
            var cls = obj.javaClass
            if (cls.isAnonymousClass)
                cls = cls.superclass
            return fieldCache.getOrPut(cls to name) {
                cls.getField(name).apply {
                    if (java.lang.reflect.Modifier.isFinal(modifiers))
                        isAccessible = true
                }
            }
        }

        override fun resolve(node: Node, child: String): Node? {
            if (node !is WithObj<*>) return null
            val obj = node.obj ?: return null
            val field = kotlin.runCatching { getField(obj, child) }
                .getOrNull() ?: return null
            return ReflectNode<Any>(node, node.subKey(child), field)
        }
    }
}