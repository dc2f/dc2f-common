package com.dc2f.common.theme

import com.dc2f.assets.*
import com.dc2f.common.contentdef.*
import com.dc2f.render.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.MediaType
import kotlinx.html.*
import kotlinx.html.dom.document
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

interface BaseTemplateForTheme : ThemeMarker, ScaffoldTheme {

    open fun <T> baseTemplateNavBar(
        tc: TagConsumer<T>,
        context: RenderContext<*>,
        website: BaseWebsite,
        navbarMenuOverride: (DIV.() -> Unit)? = null
    ) {
        tc.div("navbar-brand") {
            website.navBar?.let { navBar ->
                a("/", classes = "navbar-item") {
                    navBar.logo?.let { logo ->
                        img(context, logo, null) {
                            alt = website.name
                        }
                    }
                    navBar.title?.let {
                        span { +it }
                    }
                }
            }

            a(classes = "navbar-burger") {
                role = "button"
                attributes["data-target"] = "main-menu"
                attributes["aria-label"] = "menu"
                attributes["aria-expanded"] = "false"
                span { attributes["aria-hidden"] = "true" }; +" "
                span { attributes["aria-hidden"] = "true" }; +" "
                span { attributes["aria-hidden"] = "true" }
            }
        }
        tc.div("navbar-menu") {
            id = "main-menu"
            div("navbar-end") {
                if (navbarMenuOverride == null) {
                    val active = website.mainMenu.findActiveEntry(
                        context.renderer.loaderContext,
                        context.node
                    )?.let { activeEntry ->
                        if (activeEntry.ref?.referencedContentPath(
                                context.renderer.loaderContext
                            )?.isRoot == true && activeEntry.ref?.referencedContent != context.node
                        ) {
                            null
                        } else {
                            activeEntry
                        }
                    }
                    website.mainMenu.map { entry ->
                        a(
                            entry.href(context),
                            classes = "navbar-item"
                        ) {
                            entry.ref?.referencedContent?.let { _ ->
                                if (active == entry) {
                                    classes = classes + "is-active"
                                }
                            }
                            title = entry.linkLabel
                            +entry.linkLabel
                        }
                    }
                } else {
                    navbarMenuOverride()
                }
            }
        }
    }

    open fun <T> baseTemplate(
        tc: TagConsumer<T>,
        context: RenderContext<*>,
        seo: PageSeo,
        headInject: HEAD.() -> Unit = {},
        navbarMenuOverride: (DIV.() -> Unit)? = null,
        mainContent: DIV.() -> Unit
    ) =
        scaffold(tc, context, seo, headInject) {
            val website = context.rootNode as BaseWebsite
            nav("main-navbar navbar has-shadow is-spaced is-fixed-top") {
                role = "navigation"
                attributes["aria-label"] = "main navigation"
                div("container") {
                    baseTemplateNavBar(tc, context, website, navbarMenuOverride)
                }
            }

            main {
                div { mainContent() }
            }

            siteFooter(context)
        }
}

fun HEAD.property(propertyName: String, content: String) {
    meta(content = content) {
        attributes["property"] = propertyName
    }
}

enum class Dc2fEnv(val id: String) {
    Production("production"),
    @Suppress("unused")
    ProductionDrafts("production-drafts"),
    Dev("dev"),
    ;

    companion object {
        private val currentFromEnvironment by lazy {
            findById(System.getenv("DC2F_ENV"))
                .also { logger.info { "DC2F_ENV is $it" } }
        }
        private var currentOverride: Dc2fEnv? = null

        var current
            set(value) {
                currentOverride = value
            }
            get() = currentOverride ?: currentFromEnvironment


        private fun findById(id: String?, default: Dc2fEnv = Dev) =
            id?.let { idString -> values().firstOrNull { it.id == idString } }
                ?: default
    }
}

fun HEAD.siteHead(context: RenderContext<*>, seo: PageSeo) {
    val website = context.rootNode as BaseWebsite
    val title = "${seo.title} | ${website.name}"
    title {
        +title
    }
    property("og:title", title)

    @Suppress("UNUSED_VARIABLE")
    val env = Dc2fEnv.current

    meta(charset = "UTF-8")

    link(rel = LinkRel.stylesheet.toLowerCase()) {
        val digest = DigestTransformer()
        href = context.getAsset("theme/scss/main.scss")
            .transform(
                ScssTransformer(
                    includePaths = listOf(
                        File("."),
                        File(context.getAssetFromFileSystem("theme/scss/"))
                    )
                )
            ).transform(digest)
            .href(RenderPath.parse("/styles/css/"), context.renderer.urlConfig)
        integrity = requireNotNull(digest.value?.integrityAttrValue)
    }
    script(
        // TODO add support for typescript transform?
        type = ScriptType.textJavaScript,
        src = context.getAsset("theme/script/main.js").href(
            RenderPath.parse("/script/"),
            context.renderer.urlConfig
        )
    ) {
        async = true
    }

    meta("viewport", "width=device-width, initial-scale=1")

    website.config.favicons.map { favicon ->
        @Suppress("UnstableApiUsage")
        when (val mediaType = MediaType.parse(favicon.image.imageInfo.mimeType)
            .withoutParameters()) {
            // don't ask why, but i'll prefer image/x-icon over image/vnd.microsoft.icon for now.
            MediaType.ICO -> link(
                rel = "icon",
                type = "image/x-icon",
                href = favicon.image.href(context)
            )
            else -> link(
                rel = "icon",
                type = mediaType.toString(),
                href = favicon.image.href(context)
            ) {
                sizes =
                    "${favicon.image.imageInfo.width}x${favicon.image.imageInfo.height}"
            }
        }
    }

    if (seo.description.isNotBlank()) {
        // DIFF we should probably not support empty descriptions.
        meta(name = "description", content = seo.description)
        property("og:description", seo.description)
        property("twitter:description", seo.description)
    }

    property("og:url", context.href(context.node, true))

    if (seo.noIndex == true) {
        meta("robots", "noindex")
    }

    @Suppress("SimplifiableCallChain")
    val linkedData = if (website.index == context.node) {
        website.createLinkedData(context)
    } else {
        LinkedHashMap<String, Any>().apply {
            put("@context", "http://schema.org")
            put("@type", "WebPage")
            put("headline", seo.title)
            put(
                "mainEntityOfPage", mapOf(
                    "@type" to "WebPage",
                    "@id" to context.href(context.node, absoluteUrl = true)
                )
            )
            (context.node as? WithMainImage)?.mainImage()?.let { mainImage ->
                put("image", mainImage.href(context, true))
            }
            (context.node as? WithWordCount)?.wordCount()?.let {
                put("wordcount", it)
            }
            put("url", context.href(context.node, true))
            // TODO: datePublished
            // TODO: dateModified
            put("publisher", LinkedHashMap<String, Any>().apply {
                put("@type", "Organization")
                put("name", website.name)
                put(
                    "logo", mapOf(
                        "@type" to "ImageObject",
                        "url" to website.config.logo?.href(
                            context,
                            absoluteUri = true
                        )
                    )
                )
            })
            (context.node as? WithAuthor)?.author?.let { author ->
                put(
                    "author", mapOf(
                        "@type" to "Person",
                        "name" to author
                    )
                )
            }
            put("description", seo.description)
        }
    }
    if (linkedData != null) {
        script("application/ld+json") {
            unsafe { raw(ObjectMapper().writeValueAsString(linkedData)) }
        }
    }

    (context.node as? Article)?.let { article ->
        val image = article.mainImage() ?: article.teaser
        property("og:image", image.href(context, absoluteUri = true))
        property("og:image:width", image.width.toString())
        property("og:image:height", image.height.toString())
        property("twitter:image", image.href(context, absoluteUri = true))
        property("og:type", "article")
    }

//    meta(content = "text/html;charset=utf-8") {
//        httpEquiv = MetaHttpEquiv.contentType
//    }

//    meta(content = "utf-8") {
//        httpEquiv = "encoding"
//    }

    website.headInject?.let { headInject -> unsafe { raw(headInject) } }

}

private fun MenuEntry.href(context: RenderContext<*>): String? =
    this.ref?.href(context) ?: this.url

fun BODY.siteFooter(context: RenderContext<*>) {
    val website = context.rootNode as BaseWebsite
    footer("footer") {
        div("container") {
            div("columns") {
                website.footerMenu.map { menu ->
                    div("column") {
                        span("footer-title title is-4") { +menu.name }

                        ul {
                            menu.children.map { entry ->
                                li {
                                    a(entry.href(context)) {
                                        +entry.linkLabel
                                    }
                                }
                            }
                        }
                    }
                }


//                div("column content has-text-right") {
//                    p {
//                        unsafe { +"""<strong>ANLAGE.APP</strong> by <a href="https://codeux.design/" target="_blank">codeux.design</a> and Herbert Poul""" }
//                    }
//                    p {
//                        unsafe { +"""Questions? Suggestions? <a href="mailto:hello@anlage.app">hello@anlage.app</a>""" }
//                    }
//                }

            }

        }

        richText(
            context,
            (website.footerContent?.referencedContent as? Partial)?.html,
            mapOf("type" to "footer")
        )
    }
}

interface ScaffoldTheme {

    fun <T> scaffold(
        tc: TagConsumer<T>,
        context: RenderContext<*>,
        seo: PageSeo,
        headInject: HEAD.() -> Unit = {},
        body: BODY.() -> Unit
    ) =
        // TODO: is this document actually ever used?!
        document {

            tc.html {
                lang = "en-us"
                head {
                    siteHead(context, seo)
                    headInject()
                }
                body("has-navbar-fixed-top has-spaced-navbar-fixed-top") {
                    body()
                }
            }
        }
}