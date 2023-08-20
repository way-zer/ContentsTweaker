package cf.wayzer.contentsTweaker

import arc.util.serialization.JsonValue
import cf.wayzer.contentsTweaker.util.ExtendableClassDSL


@ExtendableClassDSL
inline fun <reified T> CTNode.getObjInfo() = get<CTNode.ObjInfo<T>>()
    ?.takeIf { T::class.java.isAssignableFrom(it.type) || (T::class.java == Any::class.java && it.type.isPrimitive) }

@JvmInline
value class CTNodeTypeChecked<T>(val node: CTNode) {
    val objInfo get() = node.get<CTNode.ObjInfo<T>>()!!
    val modifiable get() = node.get<CTNode.Modifiable<T>>() ?: error("Not Modifiable")
    val modifiableNullable get() = node.get<CTNode.Modifiable<T>>()
}

inline fun <reified T> CTNode.checkObjInfoOrNull(): CTNodeTypeChecked<T>? {
    getObjInfo<T>()?.obj ?: return null
    return CTNodeTypeChecked(this)
}

inline fun <reified T> CTNode.checkObjInfo(): CTNodeTypeChecked<T> {
    return checkObjInfoOrNull<T>() ?: error("require type ${T::class.java}")
}

/** @param block *拷贝*当前对象，并输入进行修改*/
@ExtendableClassDSL
inline fun <reified T> CTNodeTypeChecked<T>.modifier(name: String, crossinline block: T.(JsonValue) -> T) {
    val modifiable = modifiableNullable ?: return
    node.getOrCreate(name) += CTNode.Modifier {
        modifiable.setValue(modifiable.currentValue.block(it))
    }
}