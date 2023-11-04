package cf.wayzer.contentsTweaker.util

@DslMarker
annotation class ExtendableClassDSL

@ExtendableClassDSL
abstract class ExtendableClass<Ext : Any> {
    val mixins = mutableListOf<Ext>()

    @ExtendableClassDSL
    inline fun <reified T : Ext> get(): T? {
        if (this is T) return this
        return mixins.filterIsInstance<T>().let {
            require(it.size <= 1) { "More than one ${T::class.java} mixin" }
            it.firstOrNull()
        }
    }

    @ExtendableClassDSL
    inline fun <reified T : Ext> getAll(): List<T> {
        return mixins.filterIsInstance<T>().let {
            if (this is T) return it + this else it
        }
    }

    @ExtendableClassDSL
    operator fun Ext.unaryPlus() {
        mixins.add(this)
    }

    @ExtendableClassDSL
    operator fun plusAssign(ext: Ext) {
        mixins.add(ext)
    }

    @ExtendableClassDSL
    inline fun <reified T : Ext> extendOnce(ext: Ext) {
        if (get<T>() != null) return
        mixins.add(ext)
    }
}