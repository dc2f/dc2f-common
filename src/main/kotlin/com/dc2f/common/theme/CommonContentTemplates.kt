package com.dc2f.common.theme

import com.dc2f.render.RenderContext
import com.dc2f.richtext.*
import com.dc2f.richtext.markdown.Markdown
import com.dc2f.util.toStringReflective
import kotlinx.html.*

fun HTMLTag.markdown(context: RenderContext<*>, content: Markdown, asInlineContent: Boolean = false) {
    unsafe { +content.renderedContent(context, asInlineContent = asInlineContent) }
}

fun HTMLTag.markdownSummary(context: RenderContext<*>, content: Markdown) {
    unsafe { +content.summary(context) }
}

fun HTMLTag.richText(context: RenderContext<*>, richText: RichText?, arguments: Any? = null) {
    when (richText) {
        null -> return
        is Markdown -> markdown(context, richText)
        is Mustache -> unsafe { +richText.renderContent(context, arguments) }
        is Pebble -> unsafe { +richText.renderContent(context, arguments) }
        else -> throw Exception("Invalid body ${richText.toStringReflective()}")
    }
}
