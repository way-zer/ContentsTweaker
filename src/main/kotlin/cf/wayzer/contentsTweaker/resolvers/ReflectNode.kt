package cf.wayzer.contentsTweaker.resolvers

import arc.util.serialization.Json.FieldMetadata
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import java.lang.reflect.Field


class ReflectNode(override val parent: Node, key: String, val field: Field) : Node(key), Node.Modifiable {
    override val obj: Any = field.get((parent as WithObj).obj)
    override val type: Class<out Any> get() = this.field.type
    private val typeMeta: FieldMetadata by lazy { this.field.let(::FieldMetadata) }
    override val elementType: Class<*>? get() = typeMeta.elementType
    override val keyType: Class<*>? get() = typeMeta.keyType

    override val storeDepth: Int get() = 0
    override fun doSave() {}//already
    override fun doRecover() {
        setValue(obj)
    }

    override fun setValue(value: Any?) {
        field.set((parent as WithObj).obj, value)
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
            if (node !is WithObj) return null
            val obj = node.obj ?: return null
            val field = kotlin.runCatching { getField(obj, child) }
                .getOrNull() ?: return null
            return ReflectNode(node, node.subKey(child), field)
        }
    }
}