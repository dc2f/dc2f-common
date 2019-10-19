package com.dc2f.common.theme

import com.dc2f.*
import com.dc2f.common.contentdef.*
import com.dc2f.render.*
import kotlinx.html.*

fun Theme.embeddable() {
    // TODO maybe create a custom variant to register embeddable figures?
    config.pageRenderer<FigureEmbeddable> {
        if (node.inlineImage) {
            appendHtmlPartial().img { renderFigureImage(context) }
        } else {
            appendHtmlPartial().figure {
                img { renderFigureImage(context) }
                node.title?.let { title ->
                    figcaption {
                        h4 { +title }
                    }
                }
            }
        }
    }

}


private fun IMG.renderFigureImage(
    context: RenderContext<FigureEmbeddable>
) {
    renderImg(context, context.node.image, context.node.resize)
    alt = context.node.alt ?: context.node.title ?: ""
//                        width = "200"//child.screenshot.width.toString()
//                        height = "200"//child.screenshot.height.toString()
}

fun IMG.renderImg(context: RenderContext<*>, image: ImageAsset, resize: ResizeConfig?) {
    resize?.let {
        val resized = image.resize(
            context,
            resize.width ?: Int.MAX_VALUE,
            resize.height ?: Int.MAX_VALUE,
            fillType = resize.fillType ?: FillType.Cover
        )
        src = resized.href
        width = resized.width.toString()
        height = resized.height.toString()
    } ?: run {
        src = image.href(context)
    }
}


fun FlowOrInteractiveOrPhrasingContent.img(context: RenderContext<*>, image: ImageAsset, resize: ResizeConfig?, block : IMG.() -> Unit = {}) {
    img {
        renderImg(context, image, resize)
        block()
    }
}

data class Resize(
    override val width: Int? = null,
    override val height: Int? = null,
    override val fillType: FillType? = null
) : ResizeConfig
