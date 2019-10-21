package com.dc2f.common.theme

import com.dc2f.*
import com.dc2f.common.contentdef.*
import com.dc2f.render.RenderContext
import kotlinx.html.*
import java.time.format.*

fun BaseTheme.commonBlogTemplates() {
    config.pageRenderer<Blog> {
        renderChildren(node.children)
        baseTemplate(appendHtmlDocument(), this, node.seo) {
            div("container") {
                div("section has-text-centered") {
                    h1("title") { +node.seo.title }
                }
                node.children.sortedByDescending { it.date }.map { child ->
                    div("section") {
                        div("container") {
                            div("columns") {
                                div("column") {
                                    a(context.href(child)) {
                                        figure("image is-3by2") {
                                            imageAsPicture(context, child.teaser, "Teaser image", Resize(480, 320, FillType.Cover)) {
                                                style = "max-width: 100%; height: auto;"
                                            }
                                        }
                                    }
                                }
                                div("column") {
                                    a(context.href(child)) {
                                        h3("title is-size-3") { +child.title }
                                    }
                                    h4("subtitle is-size-6 is-bold") {
                                        // TODO format date?! make it generic, and cache instance?
                                        +child.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
//                                    +child.date.toString()
                                    }
                                    div("content") {
                                        // TODO generate summary?
                                        markdownSummary(context, child.body)
                                    }
                                    a(context.href(child)) {
                                        i("fas fa-chevron-right") { }
                                        +" Read more"
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }


    config.pageRenderer<Article> {
        baseTemplate(appendHtmlDocument(), this, node.seo) {
            div("hero is-medium has-bg-img") {
                div("bg-image") {
                    // TODO image resize and blur
                    style = "background-image: url('${node.teaser.href(context)}')"
                    +"x"
                }
                div("hero-body has-text-centered") {
                    h1("title") { +node.title }
                    h2("subtitle is-size-6 has-text-weight-bold") {
                        // TODO format date
                        +(node.subTitle ?: node.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
                    }
                }
            }

            div("container") {
                div("section") {
                    div("columns") {
                        div("column is-offset-2 is-8") {
                            div("content has-drop-caps") {
                                node.html?.let { richText(context, node.html) }
                                    ?: markdown(context, node.body)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun HTMLTag.imageAsPicture(context: RenderContext<*>, asset: ImageAsset, alt: String?, resize: ResizeConfig?, block: IMG.() -> Unit = { }) {
    dc2fpicture {
        asset.transform(
            context,
            resize?.width ?: Integer.MAX_VALUE,
            resize?.height ?: Integer.MAX_VALUE,
            resize?.fillType ?: FillType.NoResize
        ).also { teaser ->
            teaser.sources.map { imageSource ->
                dc2fsource {
                    attributes["srcset"] = imageSource.href
                    attributes["type"] = imageSource.type
                }
            }
            img(alt) {
                block()
                src = teaser.image.href
                width = teaser.image.width.toString()
                height = teaser.image.height.toString()
            }
        }

    }
}



class DC2FPICTURE(consumer: TagConsumer<*>) :
    HTMLTag("picture", consumer, emptyMap(),
        inlineTag = true,
        emptyTag = false), HtmlInlineTag {
}

fun HTMLTag.dc2fpicture(block: DC2FPICTURE.() -> Unit = {}) {
    DC2FPICTURE(consumer).visit(block)
}

class DC2FSOURCE(consumer: TagConsumer<*>) :
    HTMLTag("source", consumer, emptyMap(),
        inlineTag = true,
        emptyTag = false), HtmlInlineTag {
}
fun HTMLTag.dc2fsource(block: DC2FSOURCE.() -> Unit = {}) {
    DC2FSOURCE(consumer).visit(block)
}