package com.dc2f.common.theme

import com.dc2f.*
import com.dc2f.common.contentdef.*
import com.dc2f.render.*
import com.rometools.rome.feed.synd.*
import com.rometools.rome.io.SyndFeedOutput
import kotlinx.html.*
import java.util.*

open class BaseTheme : Theme(), BaseTemplateForTheme {
    override fun configure(config: ThemeConfig) {
        baseTheme()
    }

    fun baseTheme() {
        robotsTxt()
        rssFeed()
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
        baseTemplate(appendHTML(), context, headInject = { richText(context, node.head) }, seo = node.seo) {
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

fun Theme.rssFeed() {
    config.pageRenderer<Blog>(OutputType.rssFeed) {
//        node.children
        val feed = SyndFeedImpl()
        feed.feedType = "rss_1.0"
        feed.title = node.seo.title
        feed.description = node.seo.description
        feed.link = href(node, true)
        feed.entries = node.children.map { article ->
            SyndEntryImpl().apply {
                title = article.title
                publishedDate = Date.from(article.date.toInstant())
                link = href(article, true)
            }
        }
        out.appendln(SyndFeedOutput().outputString(feed))
    }
    config.pageRenderer<BaseWebsite>(OutputType.rssFeed) { renderChildren(node.children) }
    config.pageRenderer<ContentPageFolder>(OutputType.rssFeed) { renderChildren(node.children) }
    config.pageRenderer<ContentDef>(OutputType.rssFeed) { }
}
