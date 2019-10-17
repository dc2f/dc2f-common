package com.dc2f.common.theme

import com.dc2f.common.contentdef.*
import com.dc2f.render.*
import kotlinx.html.*

open class BaseTheme : Theme(), BaseTemplateForTheme {
    override fun configure(config: ThemeConfig) {
        baseTheme()
    }

    fun baseTheme() {
        robotsTxt()
        config.pageRenderer<BaseWebsite> {
            renderChildren(node.children)
            createSubContext(node.index, out, enclosingNode = null).render()
        }
        embeddable()
        commonBlogTemplates()
        config.pageRenderer<PartialFolder> {} // no need to render anything.
        landingPageTemplates()
        config.pageRenderer<HtmlPage> { htmlPage() }
        config.pageRenderer<ContentPage> { contentPage() }
        config.pageRenderer<ContentPageFolder> {
            renderChildren(node.children)
//        node.index?.let(::renderNode)
            node.index?.let { index ->
                createSubContext(index, out = out, enclosingNode = null).render()
            }
        }

    }

    @SuppressWarnings("unused")
    fun <TAG, T : WithPageSeo> TagConsumer<TAG>.baseTemplate(
        context: RenderContext<T>,
        headInject: HEAD.() -> Unit = {},
        mainContent: DIV.() -> Unit
    ) = baseTemplate(this, context, context.node.seo, headInject, mainContent = mainContent)

    private fun RenderContext<ContentPage>.contentPage() {
        baseTemplate(
            appendHTML(),
            this,
            node.seo
        ) {
            section("section") {
                div("container") {
                    div("content") {
                        richText(context, node.body)
                    }
                }
            }
        }
    }

    private fun RenderContext<HtmlPage>.htmlPage() {
        appendHTML().baseTemplate(context, headInject = { richText(context, node.head) }) {
            if (node.renderOnlyHtml == true) {
                requireNotNull(node.html) { "renderOnlyHtml was defined true, but no html attribute was found."}
                richText(context, node.html)
            } else {
                // DIFF because of some reason i have used `div` instead of `section` on old page.
                div("section") {
                    div("container") {
                        div("columns is-centered") {
                            div("column has-text-centered is-half is-narrow") {
                                h1("title") { +node.seo.title }
                                div("content") {
                                    richText(context, node.body)
                                }
                            }
                        }

                        richText(context, node.html)

                    }
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
