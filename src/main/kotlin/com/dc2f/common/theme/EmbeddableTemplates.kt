package com.dc2f.common.theme

import com.dc2f.*
import com.dc2f.common.contentdef.*
import com.dc2f.render.*
import kotlinx.html.*

fun Theme.embeddable() {
    // TODO maybe create a custom variant to register embeddable figures?
    config.pageRenderer<FigureEmbeddable> {
        if (node.inlineImage) {
            appendHtmlPartial { renderFigureImage(context) }
        } else {
            appendHtmlPartial {
                figure {
                    renderFigureImage(context)
                    node.title?.let { title ->
                        figcaption {
                            h4 { +title }
                        }
                    }
                }
            }
        }
    }

}


private fun HTMLTag.renderFigureImage(
    context: RenderContext<FigureEmbeddable>
) {
    renderImg(context, context.node.image, context.node.resize, alt = context.node.alt ?: context.node.title ?: "")
}

fun HTMLTag.renderImg(context: RenderContext<*>, image: ImageAsset, resize: ResizeConfig?, alt: String? = null, block: IMG.() -> Unit = {}) {
//    resize?.let {
        imageAsPicture(
            context,
            image,
            alt,
            resize,
            block
        )
//        val resized = image.resize(
//            context,
//            resize.width ?: Int.MAX_VALUE,
//            resize.height ?: Int.MAX_VALUE,
//            fillType = resize.fillType ?: FillType.Cover
//        )
//        src = resized.href
//        width = resized.width.toString()
//        height = resized.height.toString()
//    } ?: run {
//        src = image.href(context)
//    }
}


fun HTMLTag.img(context: RenderContext<*>, image: ImageAsset, resize: ResizeConfig?, block : IMG.() -> Unit = {}) {
    renderImg(context, image, resize, block = block)
}

data class Resize(
    override val width: Int? = null,
    override val height: Int? = null,
    override val fillType: FillType = FillType.Cover
) : ResizeConfig
