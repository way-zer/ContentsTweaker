package cf.wayzer.contentsTweaker.resolvers

import arc.util.serialization.Json.FieldMetadata
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.Node
import cf.wayzer.contentsTweaker.PatchHandler.registryResetHandler
import java.lang.reflect.Field


class ReflectNode<T>(override val parent: WithObj<Any>, override val key: String, val f: Field) : Node.Modifiable<T>() {
    @Suppress("UNCHECKED_CAST")
    override val obj: T get() = f.get(parent.obj) as T

    @Suppress("UNCHECKED_CAST")
    override val type: Class<T> get() = this.f.type as Class<T>
    private val typeMeta: FieldMetadata by lazy { this.f.let(::FieldMetadata) }
    override val elementType: Class<*>? get() = typeMeta.elementType
    override val keyType: Class<*>? get() = typeMeta.keyType

    private val originV = obj
    override val externalObject: Boolean get() = obj !== originV
    override fun saveValue0() {
        registryResetHandler(parent.obj, f) { obj ->
            fun() {
                f.set(obj, originV)
            }
        }
    }


    override fun setValue(value: T) {
        f.set(parent.obj, value)
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
            @Suppress("UNCHECKED_CAST")
            node as WithObj<Any>

            val field = kotlin.runCatching { getField(obj, child) }
                .getOrNull() ?: return null
            return ReflectNode<Any>(node, child, field)
        }
    }
}