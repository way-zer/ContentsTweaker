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
import cf.wayzer.contentsTweaker.PatchHandler
import cf.wayzer.contentsTweaker.PatchHandler.withModifier
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
open class UIExtNode(override val parent: PatchHandler.Node, key: String, val uiNode: Element) : PatchHandler.Node(key), PatchHandler.Node.WithObj {
    private var tableCell: Cell<Element>? = null
    val children = mutableMapOf<String, UIExtNode>()
    override val obj get() = uiNode
    override val type: Class<*> = uiNode::class.java

    override fun resolve(child: String): PatchHandler.Node {
        resolveSpecialChild(child)?.let { return it }
        if (child.startsWith("#"))
            return children[child] ?: error("child $child not found in $key")
        if (child.startsWith("+")) {
            val idStart = child.indexOf('#')
            if (idStart < 0) error("Must provide element id")
            val id = child.substring(idStart)
            return children.getOrPut(id) {
                val type = child.substring(1, idStart)
                val element = createUIElement(type)
                val node = UIExtNode(this, subKey(id), element)
                when (uiNode) {
                    is Table -> node.tableCell = uiNode.add(element)
                    is Group -> uiNode.addChild(element)
                    else -> error("Only Group can add child element")
                }
                node
            }
        }
        if (child == "-") return withModifier("-") {
            val id = it.asString()
            beforeModify()
            children.remove(id)?.uiNode?.remove()
        }
        return super.resolve(child)
    }

    private fun resolveSpecialChild(child: String): PatchHandler.Node? = when (child) {
        "align" -> withModifier("align") {
            val v = if (it.isNumber) it.asInt() else alignMap[it.asString()] ?: error("invalid align: $it")
            beforeModify()
            when (uiNode) {
                is Table -> uiNode.align(v)
                else -> error("TODO: only support Table align")
            }
        }

        "margin" -> withModifier("margin") { json ->
            val v = if (json.isNumber) json.asFloat().let { v -> FloatArray(4) { v } }
            else json.asFloatArray()?.takeIf { it.size == 4 } ?: error("invalid margin: $json")
            beforeModify()
            when (uiNode) {
                is Table -> uiNode.margin(v[0], v[1], v[2], v[3])
                else -> error("TODO: only support Table margin")
            }
        }

        "pad" -> withModifier("pad") { json ->
            val v = if (json.isNumber) json.asFloat().let { v -> FloatArray(4) { v } }
            else json.asFloatArray()?.takeIf { it.size == 4 } ?: error("invalid pad: $json")
            beforeModify()
            (tableCell ?: error("TODO: only support Cell pad"))
                .pad(v[0], v[1], v[2], v[3])
        }

        "text" -> withModifier("text") { json ->
            val v = json.asString()
            beforeModify()
            when (uiNode) {
                is Label -> uiNode.setText(v)
                is TextButton -> uiNode.setText(v)
                else -> error("TODO: only support Label text")
            }
        }

        "style" -> withModifier("style") {
            beforeModify()
            when (uiNode) {
                is Label -> {
                    val v = stylesMap[it.asString()] as? LabelStyle ?: error("invalid style: $it")
                    uiNode.style = v
                }

                is Button -> {
                    val v = stylesMap[it.asString()] as? Button.ButtonStyle ?: error("invalid style: $it")
                    uiNode.style = v
                }

                is ScrollPane -> {
                    val v = stylesMap[it.asString()] as? ScrollPane.ScrollPaneStyle ?: error("invalid style: $it")
                    uiNode.style = v
                }

                is Table -> {
                    val v = stylesMap[it.asString()] as? Drawable ?: error("invalid style: $it")
                    uiNode.background = v
                }

                else -> error("TODO: style only support Table,Label,Button,ScrollPane")
            }
        }

        "onClick" -> withModifier("onClick") {
            val message = it.asString()
            uiNode.tapped { Call.sendChatMessage(message) }
        }

        else -> null
    }

    object Root : UIExtNode(PatchHandler.Node.Root, "uiExt.", Core.scene.root ?: Element()), Storable {
        override val storeDepth: Int = Int.MAX_VALUE
        override fun doSave() {}
        override fun doRecover() {
            children.values.toList().forEach { it.uiNode.remove() }
            children.clear()
        }
    }

    companion object Resolver : PatchHandler.Resolver {
        val alignMap = Align::class.java.declaredFields.associate { it.name to it.getInt(null) }
        val stylesMap = Styles::class.java.declaredFields.associate { it.name to it.get(null)!! }
        fun createUIElement(type: String): Element = when (type) {
            "Table" -> Table()
            "Label" -> Label("")
            else -> error("TODO: not support Element: $type")
        }

        override fun resolve(node: PatchHandler.Node, child: String): PatchHandler.Node? {
            if (Vars.headless) return null
            if (node == PatchHandler.Node.Root && child == "uiExt") return Root
            return null
        }
    }
}