package cf.wayzer.util

import java.lang.reflect.Field
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ReflectDelegate<T, R>(
    private val field: Field, private val cls: Class<R>
) : ReadWriteProperty<T?, R> {
    override fun getValue(thisRef: T?, property: KProperty<*>): R = cls.cast(field.get(thisRef))
    override fun setValue(thisRef: T?, property: KProperty<*>, value: R) = field.set(thisRef, value)
}

inline fun <reified T, reified R> reflectDelegate() = PropertyDelegateProvider<Any?, ReflectDelegate<T, R>> { _, property ->
    val field = T::class.java.getDeclaredField(property.name)
    field.isAccessible = true
    ReflectDelegate(field, R::class.java)
}