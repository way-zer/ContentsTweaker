package cf.wayzer.contentsTweaker.resolvers

import arc.util.serialization.Json.FieldMetadata
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object ReflectResolver : PatchHandler.Resolver {
    class ReflectNode(override val parent: Node, key: String, val field: Field) : Node(key), Node.Modifiable {
        override val obj: Any = field.get((parent as WithObj).obj)

        override val type: Class<out Any> get() = this.field.type
        private val typeMeta: FieldMetadata by lazy { this.field.let(::FieldMetadata) }
        override val elementType: Class<*>? get() = typeMeta.elementType
        override val keyType: Class<*>? get() = typeMeta.keyType

        override fun setValue(value: Any?) {
            field.set((parent as WithObj).obj, value)
        }
    }

    override fun resolve(node: Node, child: String): Node? {
        if (node !is Node.WithObj) return null
        val obj = node.obj ?: return null
        val field = kotlin.runCatching { getField(obj, child) }
            .getOrNull() ?: return null
        return ReflectNode(node, node.subKey(child), field)
    }

    private val fieldCache = mutableMapOf<Pair<Class<*>, String>, Field>()
    private fun getField(obj: Any, name: String): Field {
        var cls = obj.javaClass
        if (cls.isAnonymousClass)
            cls = cls.superclass
        return fieldCache.getOrPut(cls to name) {
            cls.getField(name).apply {
                if (Modifier.isFinal(modifiers))
                    isAccessible = true
            }
        }
    }
}