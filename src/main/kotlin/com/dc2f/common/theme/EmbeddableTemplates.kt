package com.dc2f.common.theme

import com.dc2f.*
import com.dc2f.common.contentdef.*
import com.dc2f.render.*
import kotlinx.html.*

fun Theme.embeddable() {
    config.pageRenderer<FigureEmbeddable> {
        appendHTML().figure {
            //                figure {
            img(context, node.image, node.resize) {
                alt = node.alt ?: node.title ?: ""
            }
            node.title?.let { title ->
                figcaption {
                    h4 { +title }
                }
            }
//                }

        }
    }


}

fun FlowOrInteractiveOrPhrasingContent.img(context: RenderContext<*>, image: ImageAsset, resize: ResizeConfig?, block : IMG.() -> Unit = {}) {
    img {
        resize?.let { resize ->
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
//                        width = "200"//child.screenshot.width.toString()
//                        height = "200"//child.screenshot.height.toString()
    }
}

