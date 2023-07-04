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
import cf.wayzer.contentsTweaker.PatchHandler.registryResetHandler
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
open class UIExtNode<T : Element>(override val parent: PatchHandler.Node, override val key: String, final override val obj: T) : PatchHandler.Node.WithObj<T>() {
    override val externalObject: Boolean get() = true
    override val type: Class<out T> = obj::class.java

    private var tableCell: Cell<Element>? = null

    override fun resolve(child: String): PatchHandler.Node {
        resolveSpecialChild(child)?.let { return it }
        if (child.startsWith("#"))
            return childrenNode[child] as UIExtNode<*>? ?: error("child $child not found in $key")
        if (child.startsWith("+")) {
            val idStart = child.indexOf('#')
            if (idStart < 0) error("Must provide element id")
            val id = child.substring(idStart)
            return childrenNode.getOrPut(id) {
                val type = child.substring(1, idStart)
                val element = createUIElement(type)
                val node = UIExtNode(this, id, element)
                when (obj) {
                    is Table -> node.tableCell = obj.add(element)
                    is Group -> obj.addChild(element)
                    else -> error("Only Group can add child element")
                }
                node
            }
        }
        if (child == "-") return withModifier("-") {
            val id = it.asString()
            check(id.startsWith('#')) { "Must provide element id" }
            (childrenNode.remove(id) as UIExtNode<*>?)?.obj?.remove()
        }
        return super.resolve(child)
    }

    private fun resolveSpecialChild(child: String): PatchHandler.Node? = when (child) {
        "align" -> withModifier("align") {
            val v = if (it.isNumber) it.asInt() else alignMap[it.asString()] ?: error("invalid align: $it")
            when (obj) {
                is Table -> obj.align(v)
                else -> error("TODO: only support Table align")
            }
        }

        "margin" -> withModifier("margin") { json ->
            val v = if (json.isNumber) json.asFloat().let { v -> FloatArray(4) { v } }
            else json.asFloatArray()?.takeIf { it.size == 4 } ?: error("invalid margin: $json")
            when (obj) {
                is Table -> obj.margin(v[0], v[1], v[2], v[3])
                else -> error("TODO: only support Table margin")
            }
        }

        "pad" -> withModifier("pad") { json ->
            val v = if (json.isNumber) json.asFloat().let { v -> FloatArray(4) { v } }
            else json.asFloatArray()?.takeIf { it.size == 4 } ?: error("invalid pad: $json")
            (tableCell ?: error("TODO: only support Cell pad"))
                .pad(v[0], v[1], v[2], v[3])
        }

        "text" -> withModifier("text") { json ->
            val v = json.asString()
            when (obj) {
                is Label -> obj.setText(v)
                is TextButton -> obj.setText(v)
                else -> error("TODO: only support Label text")
            }
        }

        "style" -> withModifier("style") {
            when (obj) {
                is Label -> {
                    val v = stylesMap[it.asString()] as? LabelStyle ?: error("invalid style: $it")
                    obj.style = v
                }

                is Button -> {
                    val v = stylesMap[it.asString()] as? Button.ButtonStyle ?: error("invalid style: $it")
                    obj.style = v
                }

                is ScrollPane -> {
                    val v = stylesMap[it.asString()] as? ScrollPane.ScrollPaneStyle ?: error("invalid style: $it")
                    obj.style = v
                }

                is Table -> {
                    val v = stylesMap[it.asString()] as? Drawable ?: error("invalid style: $it")
                    obj.background = v
                }

                else -> error("TODO: style only support Table,Label,Button,ScrollPane")
            }
        }

        "onClick" -> withModifier("onClick") {
            val message = it.asString()
            obj.tapped { Call.sendChatMessage(message) }
        }

        else -> null
    }

    object Root : UIExtNode<Element>(PatchHandler.Node.Root, "uiExt", Core.scene.root ?: Element()) {
        override fun afterModify(modifier: Modifier) {
            registryResetHandler(this, "reset") {
                fun() {
                    childrenNode.values.forEach {
                        if (it is UIExtNode<*>) it.obj.remove()
                    }
                    childrenNode.clear()
                }
            }
        }
    }

    companion object Resolver : PatchHandler.Resolver {
        val alignMap by lazy { Align::class.java.declaredFields.associate { it.name to it.getInt(null) } }
        val stylesMap by lazy { Styles::class.java.declaredFields.associate { it.name to it.get(null)!! } }
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