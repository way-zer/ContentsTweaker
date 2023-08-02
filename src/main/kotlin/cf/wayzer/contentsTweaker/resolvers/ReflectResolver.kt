package cf.wayzer.contentsTweaker.resolvers

import cf.wayzer.contentsTweaker.CTNode
import cf.wayzer.contentsTweaker.ContentsTweaker
import cf.wayzer.contentsTweaker.getObjInfo
import mindustry.io.JsonIO
import java.lang.reflect.Modifier

object ReflectResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        val objInfo = node.getObjInfo<Any?>() ?: return
        val obj = objInfo.obj ?: return
        runCatching { JsonIO.json.getFields(objInfo.type) }.getOrNull()?.forEach { entry ->
            val meta = entry.value
            if (!Modifier.isPublic(meta.field.modifiers)) return@forEach
            node.getOrCreate(entry.key).apply {
                var cls = meta.field.type
                if (cls.isAnonymousClass) cls = cls.superclass
                +CTNode.ObjInfo<Any?>(meta.field.get(obj), cls, meta.elementType, meta.keyType)
                +object : CTNode.Modifiable<Any?>(node) {
                    override val currentValue: Any? get() = meta.field.get(obj)
                    override fun setValue0(value: Any?) {
                        meta.field.set(obj, value)
                    }
                }
            }
        }
    }
}