package cf.wayzer.contentsTweaker.resolvers

import arc.Core
import arc.scene.Element
import arc.scene.Group
import arc.scene.style.Drawable
import arc.scene.ui.Button
import arc.scene.ui.Label
import arc.scene.ui.Label.LabelStyle
import arc.scene.ui.ScrollPane
import arc.scene.ui.TextButton
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import arc.util.Align
import cf.wayzer.contentsTweaker.*
import mindustry.Vars
import mindustry.gen.Call
import mindustry.ui.Styles

/**
 * UIExt功能
 * 主体类似一个Map<id,Component>
 * 往节点下增加元素，即在scene新增UI组件
 *
 * 子节点：
 * * #id -> 获取UINode
 * * +type#id 获取或创建UINode
 * * "-" #id 删除ui节点
 *
 * 以实现infoPopup为例
 * ```json5
 * {
 * "uiExt.+Table#scoreboard": {
 *   fillParent: true
 *   touchable: disabled
 *   align: 1
 *   //"actions.+": [{delay:5},{remove:true}] //暂不支持
 *   "+Table#bg": {
 *      touchable: enabled
 *      style: black3,
 *      pad: [top,left,bottom,right],
 *      onClick: "/help" //action为往服务器发送信息
 *      margin: 4
 *      "+Label#label": {
 *        text: "Hello world"
 *        style: outlineLabel
 *      }
 *   }
 * }
 * }
 * ```
 */
object UIExtResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        if (Vars.headless) {
            if (node == CTNode.Root)
                node.children["uiExt"] = CTNode.Nope
            return
        }
        if (node == CTNode.Root) {
            node.getOrCreate("uiExt").apply {
                +CTNode.ObjInfo(Core.scene.root)
                CTNode.PatchHandler.resetHandlers += CTNode.Resettable {
                    children.values.forEach { it.getObjInfo<Element>()?.obj?.remove() }
                }
            }
            return
        }
        node.checkObjInfoOrNull<Element>()?.extend()
        node.checkObjInfoOrNull<Table>()?.extendTable()
        node.checkObjInfoOrNull<Cell<*>>()?.extendCell()
    }

    private fun CTNodeTypeChecked<Element>.extend() {
        val obj = objInfo.obj
        node += CTNode.IndexableRaw { name ->
            if (name.length < 2 || name[0] != '+') return@IndexableRaw null
            val idStart = name.indexOf('#')
            check(idStart > 0) { "Must provide element id" }
            val type = name.substring(1, idStart)
            val id = name.substring(idStart)
            node.children[id] ?: createUIElement(type).let { element ->
                node.getOrCreate(id).apply {
                    +CTNode.ObjInfo(element)
                    when (obj) {
                        is Table -> {
                            val cell = obj.add(element)
                            getOrCreate("cell") += CTNode.ObjInfo(cell)
                        }

                        is Group -> obj.addChild(element)
                        else -> error("Only Group can add child element")
                    }
                }
            }
        }
        node.getOrCreate("+") += CTNode.Modifier {
            val type = it.remove("type")?.asString() ?: error("Must provide Element type")
            val child = node.resolve("+$type#${it.getString("name")}")
            CTNode.PatchHandler.handle(it, child)
        }
        node.getOrCreate("-") += CTNode.Modifier {
            val id = it.asString()
            check(id.startsWith('#')) { "Must provide element #id" }
            node.children[id]?.getObjInfo<Element>()?.obj?.remove()
            node.children.remove(id)
        }
        extendModifiers()
    }

    private fun CTNodeTypeChecked<Table>.extendTable() {
        val obj = objInfo.obj
        node.getOrCreate("row") += CTNode.Modifier { obj.row() }
        node.getOrCreate("align") += CTNode.Modifier {
            val v = if (it.isNumber) it.asInt() else alignMap[it.asString()] ?: error("invalid align: $it")
            obj.align(v)
        }
        node.getOrCreate("margin") += CTNode.Modifier { json ->
            val v = if (json.isNumber) json.asFloat().let { v -> FloatArray(4) { v } }
            else json.asFloatArray()?.takeIf { it.size == 4 } ?: error("invalid margin: $json")
            obj.margin(v[0], v[1], v[2], v[3])
        }
    }

    private fun CTNodeTypeChecked<Element>.extendModifiers() {
        val obj = objInfo.obj
        node.getOrCreate("onClick") += CTNode.Modifier {
            val message = it.asString()
            obj.tapped { Call.sendChatMessage(message) }
        }

        node.getOrCreate("style") += CTNode.Modifier {
            val style = stylesMap[it.asString()] ?: error("style not found: $it")
            when (obj) {
                is Label -> obj.style = style as? LabelStyle ?: error("invalid style: $it")
                is Button -> obj.style = style as? Button.ButtonStyle ?: error("invalid style: $it")
                is ScrollPane -> obj.style = style as? ScrollPane.ScrollPaneStyle ?: error("invalid style: $it")
                is Table -> obj.background = style as? Drawable ?: error("invalid style: $it")
                else -> error("TODO: style only support Table,Label,Button,ScrollPane")
            }
        }

        if (obj is Label || obj is TextButton)
            node.getOrCreate("text") += CTNode.Modifier {
                val v = it.asString()
                when (obj) {
                    is Label -> obj.setText(v)
                    is TextButton -> obj.setText(v)
                }
            }
    }

    private fun CTNodeTypeChecked<Cell<*>>.extendCell() {
        node.getOrCreate("pad") += CTNode.Modifier { json ->
            val v = if (json.isNumber) json.asFloat().let { v -> FloatArray(4) { v } }
            else json.asFloatArray()?.takeIf { it.size == 4 } ?: error("invalid pad: $json")
            objInfo.obj.pad(v[0], v[1], v[2], v[3])
        }
    }

    private val alignMap by lazy { Align::class.java.declaredFields.associate { it.name to it.getInt(null) } }
    private val stylesMap by lazy { Styles::class.java.declaredFields.associate { it.name to it.get(null)!! } }
    fun createUIElement(type: String): Element = when (type) {
        "Table" -> Table()
        "Label" -> Label("")
        else -> error("TODO: not support Element: $type")
    }
}