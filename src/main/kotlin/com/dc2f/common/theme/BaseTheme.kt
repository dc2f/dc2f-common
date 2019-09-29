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
    embeddable()
    commonBlogTemplates()
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


fun Theme.contentTemplates() {
}

fun Theme.robotsTxt() {
    config.pageRenderer<BaseWebsite>(OutputType.robotsTxt) {
        out.appendln("User-agent: *")

        if (Dc2fEnv.current == Dc2fEnv.Production) {
            out.appendln("Allow: /")
            out.appendln("")
            out.appendln("Sitemap: ${renderer.href(RenderPath.root.childLeaf("sitemap.xml"), true)}")
        } else {
            out.appendln("Disallow: /")
        }
    }
}
