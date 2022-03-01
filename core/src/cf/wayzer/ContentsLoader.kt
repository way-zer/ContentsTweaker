package cf.wayzer

import arc.Core
import arc.Events
import arc.files.Fi
import arc.func.Cons
import arc.struct.ObjectIntMap
import arc.struct.Seq
import mindustry.Vars
import mindustry.content.*
import mindustry.core.ContentLoader
import mindustry.ctype.*
import mindustry.game.EventType
import mindustry.io.SaveVersion
import mindustry.logic.LExecutor
import mindustry.mod.Mods
import kotlin.system.measureTimeMillis

object ContentsLoader : ContentLoader() {
    class ContentContainer(val type: ContentType?, val default: ContentList) {
        var content: ContentList = default
        val contentMap: Seq<Content> = if (type == null) Seq() else Vars.content.getBy<Content>(type).copy()
        val nameMap = contentMap.filterIsInstance<MappableContent>().associateByTo(mutableMapOf()) { it.name }

        @Synchronized
        fun load(ex: Throwable? = null) {
            contentMap.clear()
            nameMap.clear()
            val result = kotlin.runCatching { content.load() }
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
        ContentContainer(null, Loadouts()),
        ContentContainer(null, TechTree()),
    )
    val contentMap = contents.filter { it.type != null }.associateBy { it.type!! }

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

    override fun getContentMap(): Array<Seq<Content>> {
        val ret = origin.contentMap.clone()
        contentMap.forEach { (k, v) -> ret[k.ordinal] = v.contentMap }
        return ret
    }

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
        ContentsLoader.temporaryMapper = temporaryMapper
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

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    object Api {
        val supportContents by ContentsLoader::contents
        val contentPacks = mutableMapOf(
            "origin" to { contents.forEach { it.content = it.default } }
        )
        val toLoadPacks = mutableListOf<String>()
        var lastLoadedPacks = listOf<String>()
            private set

        //platform impl
        var logTimeCost: (tag: String, ms: Long) -> Unit = { _, _ -> }

        private inline fun doMeasureTimeLog(tag: String, body: () -> Unit) {
            val time = measureTimeMillis(body)
            logTimeCost(tag, time)
        }

        /**
         * Should before world load
         */
        fun loadContent(outNotFound: MutableList<String>) {
            fun checkPackExist(pack: String): Boolean {
                if (pack in contentPacks)
                    return true
                outNotFound.add(pack)
                return false
            }

            val mainPack = toLoadPacks.lastOrNull { !it.startsWith("EX-") }?.takeIf(::checkPackExist) ?: "origin"
            val exPack = toLoadPacks.filter { it.startsWith("EX-") && checkPackExist(it) }.toSet().sorted()
            toLoadPacks.clear()

            //fastPath
            val packs = listOf(mainPack) + exPack
            if (lastLoadedPacks == packs) return
            lastLoadedPacks = packs

            doMeasureTimeLog("Load Main ContentsPack '$mainPack'") {
                contentPacks[mainPack]!!.invoke()
                contents.forEach(ContentContainer::load)
            }
            exPack.forEach { pack ->
                doMeasureTimeLog("Load Extra ContentsPack '$pack'") {
                    contentPacks[pack]!!.invoke()
                }
            }
            doMeasureTimeLog("Content.init") {
                contents.forEach {
                    it.contentMap.forEach(Content::init)
                }
            }
            if (Vars.constants != null) {
                doMeasureTimeLog("Vars.constants.init") {
                    Vars.constants.apply {
                        javaClass.getDeclaredField("namesToIds")
                            .apply { isAccessible = true }
                            .set(this, ObjectIntMap<String>())
                        javaClass.getDeclaredField("vars")
                            .apply { isAccessible = true }
                            .set(this, Seq<LExecutor.Var>(LExecutor.Var::class.java))
                        init()
                    }
                }
            }
            Events.fire(EventType.ContentInitEvent())
            if (!Vars.headless) {
                doMeasureTimeLog("ContentLoader.loadColors") {
                    loadColors()
                }
                doMeasureTimeLog("Content.loadIcon") {
                    contents.forEach { it.contentMap.forEach(Content::loadIcon) }
                }
                doMeasureTimeLog("Content.load") {
                    contents.forEach { it.contentMap.forEach(Content::load) }
                }
                doMeasureTimeLog("Vars.schematics.load") {
                    Vars.schematics.load()
                }
                doMeasureTimeLog("Set UnlockableContent.iconId") {
                    //copy from mindustry.ui.Fonts.loadContentIcons
                    var lastCid = 1
                    each {
                        if (it is UnlockableContent && Core.atlas.find(it.name + "-icon-logic").found()) {
                            it.iconId = lastCid
                            lastCid++
                        }
                    }
                }
            }
        }

        fun overwriteContents(type: ContentType, list: ContentList) {
            val c = contentMap[type] ?: throw IllegalArgumentException("Not Support Overwrite ContentType")
            c.content = list
        }
    }
}