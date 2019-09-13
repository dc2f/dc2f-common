package com.dc2f.common.theme

import com.dc2f.FillType
import com.dc2f.common.contentdef.*
import com.dc2f.common.theme.img
import com.dc2f.render.*
import com.dc2f.richtext.RichText
import kotlinx.html.*

fun Theme.baseTheme() {
    robotsTxt()
    config.pageRenderer<BaseWebsite> {
        renderChildren(node.children)
        createSubContext(node.index, out, enclosingNode = null).render()
    }
    config.pageRenderer<PartialFolder> {} // no need to render anything.
    config.pageRenderer<LandingPage> { landingPage() }
    config.pageRenderer<ContentPage> { contentPage() }
    config.pageRenderer<ContentPageFolder> {
        renderChildren(node.children)
//        node.index?.let(::renderNode)
        node.index?.let { index ->
            createSubContext(index, out = out, enclosingNode = null).render()
        }
    }
    // TODO maybe create a custom variant to register embeddable figures?
    config.pageRenderer<FigureEmbeddable> {
        if (node.inlineImage) {
            appendHTML().img { renderFigureImage(context) }
        } else {
            appendHTML().figure {
                //                figure {
                img { renderFigureImage(context) }
                node.title?.let { title ->
                    figcaption {
                        h4 { +title }
                    }
                }
//                }

            }
        }
    }
}

private fun RenderContext<ContentPage>.contentPage() {
    appendHTML().baseTemplate(
        this,
        node.seo
    ) {
        div("section") {
            div("container") {
                div("content") {
                    richText(context, node.body)
                }
            }
        }
    }
}

private fun IMG.renderFigureImage(
    renderContext: RenderContext<FigureEmbeddable>
) {
        renderContext.node.resize?.let { resize ->
            val resized = renderContext.node.image.resize(
                renderContext.context,
                resize.width ?: Int.MAX_VALUE,
                resize.height ?: Int.MAX_VALUE,
                fillType = resize.fillType ?: FillType.Cover
            )
            src = resized.href
            width = resized.width.toString()
            height = resized.height.toString()
        } ?: run {
            src = renderContext.node.image.href(renderContext.context)
        }
        alt = renderContext.node.alt ?: renderContext.node.title ?: ""
//                        width = "200"//child.screenshot.width.toString()
//                        height = "200"//child.screenshot.height.toString()
}


fun Theme.contentTemplates() {
}

fun Theme.robotsTxt() {
    config.pageRenderer<BaseWebsite>(OutputType.robotsTxt) {
        out.appendln("User-agent: *")
        if (Dc2fEnv.current == Dc2fEnv.Production) {
            out.appendln("Allow: /")
            out.appendln("")
            out.appendln("Sitemap: https://anlage.app/sitemap.xml")
        } else {
            out.appendln("Disallow: /")
        }
    }
}
