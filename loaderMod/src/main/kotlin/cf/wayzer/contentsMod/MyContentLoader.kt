package cf.wayzer.contentsMod

import arc.files.Fi
import arc.func.Cons
import arc.struct.Seq
import mindustry.Vars
import mindustry.content.*
import mindustry.core.ContentLoader
import mindustry.ctype.Content
import mindustry.ctype.ContentList
import mindustry.ctype.ContentType
import mindustry.ctype.MappableContent
import mindustry.io.SaveVersion
import mindustry.mod.Mods

object MyContentLoader : ContentLoader() {
    class ContentContainer(val type: ContentType, val default: ContentList) {
        var content: ContentList = default
        var lastContent: ContentList? = null
            private set
        val contentMap: Seq<Content> = Vars.content.getBy<Content>(type).copy()
        val nameMap = contentMap.filterIsInstance<MappableContent>().associateByTo(mutableMapOf()) { it.name }

        fun maskChanged() {
            lastContent = null
        }

        @Synchronized
        fun load(ex: Throwable? = null) {
            contentMap.clear()
            nameMap.clear()
            val result = kotlin.runCatching {
                content.load()
                contentMap.forEach(Content::init)
            }
            lastContent = content
            content = default
            if (result.isFailure) {
                if (ex != null) {
                    ex.addSuppressed(result.exceptionOrNull())
                    throw ex
                }
                load(result.exceptionOrNull())
            }
        }
    }

    val origin: ContentLoader = Vars.content

    val contents = arrayOf(
        ContentContainer(ContentType.item, Items()),
        ContentContainer(ContentType.status, StatusEffects()),
        ContentContainer(ContentType.liquid, Liquids()),
        ContentContainer(ContentType.bullet, Bullets()),
        ContentContainer(ContentType.unit, UnitTypes()),
        ContentContainer(ContentType.block, Blocks()),
    )
    val contentMap = contents.associateBy { it.type }

    override fun clear() = Unit/*throw NotImplementedError()*/
    override fun createBaseContent() = throw NotImplementedError()
    override fun createModContent() = throw NotImplementedError()
    override fun logContent() = throw NotImplementedError()
    override fun init() = throw NotImplementedError()
    override fun load() = throw NotImplementedError()
    override fun getLastAdded() = throw NotImplementedError()
    override fun removeLast() = throw NotImplementedError()
    override fun handleContent(content: Content) {
        val c = contentMap[content.contentType] ?: return origin.handleContent(content)
        c.contentMap.add(content)
    }

    private var currentMod: Mods.LoadedMod? = null
    override fun setCurrentMod(mod: Mods.LoadedMod?) {
        origin.setCurrentMod(mod)
        currentMod = mod
    }

    override fun transformName(name: String?): String = origin.transformName(name)
    override fun handleMappableContent(content: MappableContent) {
        val c = contentMap[content.contentType] ?: return origin.handleMappableContent(content)
        if (content.name in c.nameMap)
            error("""Two content objects cannot have the same name! (issue: '${content.name}')""")
        if (currentMod != null) {
            content.minfo.mod = currentMod
            if (content.minfo.sourceFile == null) {
                content.minfo.sourceFile = Fi(content.name)
            }
        }
        c.nameMap[content.name] = content
    }

    override fun getContentMap(): Array<Seq<Content>> = origin.contentMap
    override fun each(cons: Cons<Content>) {
        ContentType.all.forEach {
            getBy<Content>(it).each(cons)
        }
    }

    override fun <T : MappableContent> getByName(type: ContentType, name: String): T? {
        val c = contentMap[type] ?: return origin.getByName(type, name)
        //load fallbacks
        val name0 = if (type != ContentType.block) name
        else SaveVersion.modContentNameMap[name, name]
        @Suppress("UNCHECKED_CAST")
        return c.nameMap[name0] as T?
    }

    private var temporaryMapper: Array<out Array<MappableContent>>? = null
    override fun setTemporaryMapper(temporaryMapper: Array<out Array<MappableContent>>?) {
        origin.setTemporaryMapper(temporaryMapper)
        MyContentLoader.temporaryMapper = temporaryMapper
    }

    override fun <T : Content> getByID(type: ContentType, id: Int): T? {
        if (id < 0) return null
        temporaryMapper?.getOrNull(type.ordinal)?.takeIf { it.isNotEmpty() }?.let { tempMap ->
            @Suppress("UNCHECKED_CAST")
            return (tempMap.getOrNull(id) ?: tempMap[0]) as T?
        }
        return getBy<T>(type).get(id)
    }

    override fun <T : Content> getBy(type: ContentType): Seq<T> {
        @Suppress("UNCHECKED_CAST")
        return contentMap[type]?.contentMap as Seq<T>? ?: origin.getBy(type)
    }
}