package cf.wayzer.contentsTweaker.resolvers

import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import cf.wayzer.contentsTweaker.CTNode
import cf.wayzer.contentsTweaker.ContentsTweaker
import cf.wayzer.contentsTweaker.checkObjInfoOrNull
import cf.wayzer.contentsTweaker.getObjInfo
import mindustry.content.TechTree.TechNode
import mindustry.graphics.g3d.PlanetGrid

object BetterJsonResolver : ContentsTweaker.NodeCollector {
    override fun collectChild(node: CTNode) {
        node.checkObjInfoOrNull<Color>()?.apply {
            node += CTNode.ToJson { it.value(objInfo.obj) }
        }
        node.checkObjInfoOrNull<TextureRegion>()?.apply {
            node += CTNode.ToJson { it.value(objInfo.obj.toString()) }
        }
        node.checkObjInfoOrNull<TechNode>()?.apply {
            node.getOrCreate("parent").apply parent@{
                extendOnce<CTNode.ToJson>(CTNode.ToJson {
                    it.value(this@parent.getObjInfo<TechNode>()?.obj?.content?.name)
                })
            }
            node.getOrCreate("children").extendOnce<CTNode.ToJson>(CTNode.ToJson {
                it.value("...")
            })
        }
        node.checkObjInfoOrNull<PlanetGrid>()?.apply {
            node += CTNode.ToJson { it.value("...") }
        }
        node.checkObjInfoOrNull<PlanetGrid.Ptile>()?.apply {
            node += CTNode.ToJson { it.value("...") }
        }
    }
}