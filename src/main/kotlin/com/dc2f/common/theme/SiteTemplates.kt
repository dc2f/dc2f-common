package com.dc2f.common.theme

import com.dc2f.*
import com.dc2f.assets.*
import com.dc2f.common.contentdef.*
import com.dc2f.render.*
import com.dc2f.util.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.MediaType
import kotlinx.html.*
import kotlinx.html.dom.document
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Longest edge of the image served as og:image/twitter:image. X renders the
 * large card at 1200px wide, so anything above this is downloaded by every
 * crawler and then thrown away.
 */
private const val SOCIAL_IMAGE_MAX_EDGE = 1200

/**
 * The card format itself, 1.91:1, used when [BaseConfig.socialImageCrop] is on.
 * These are the dimensions Open Graph documents for og:image, and what X
 * renders `summary_large_image` at.
 */
private const val SOCIAL_CARD_WIDTH = 1200
private const val SOCIAL_CARD_HEIGHT = 630

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

/**
 * OpenGraph is defined in terms of the `property` attribute, but the Twitter
 * card spec documents `name`. Emit each in the form its own spec asks for.
 */
fun HEAD.twitterProperty(propertyName: String, content: String) {
    meta(name = propertyName, content = content)
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

    link(rel = "canonical") {
        href = context.href(context.node, true)
    }

    // toLowerCase() is an error-level deprecation from Kotlin 2.x; lowercase()
    // is locale-independent, which is what we want for an HTML attribute.
    link(rel = LinkRel.stylesheet.lowercase()) {
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
        type = ScriptType.textJavaScript
    ) {
        val digest = DigestTransformer()
        src = context.getAsset("theme/script/main.js")
          .transform(digest)
          .href(
            RenderPath.parse("/script/"),
            context.renderer.urlConfig
          )
        async = true
        integrity = requireNotNull(digest.value?.integrityAttrValue)
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
        twitterProperty("twitter:description", seo.description)
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
            put("@context", "https://schema.org")
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
                // Only when there is a logo: an ImageObject carrying a null
                // url is invalid structured data, which is what sites without
                // config.logo were emitting on every inner page.
                website.config.logo?.href(context, absoluteUri = true)?.let { logoUrl ->
                    put(
                        "logo", mapOf(
                            "@type" to "ImageObject",
                            "url" to logoUrl
                        )
                    )
                }
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

    val article = context.node as? Article
    property("og:type", if (article != null) "article" else "website")
    property("og:site_name", website.name)

    // Articles use their own image; everything else falls back to the site's
    // social image, so the landing page is no longer shared as a bare link.
    // The logo is a last resort -- a branded card beats no card at all, and it
    // means sites that never set socialImage still get one.
    val socialImage = article?.let { it.mainImage() ?: it.teaser }
        ?: website.config.socialImage
        ?: website.config.logo

    // Serve the card a resized copy rather than the original. A crawler fetches
    // og:image on every share, and an original straight off a camera or an image
    // generator is easily several megabytes -- X caps it at 5MB and silently
    // drops anything it cannot fetch in time. Fit never upscales, so an image
    // already below the limit passes through untouched. Animated gifs keep the
    // original, same as in [ImageAsset.transform], and so does SVG -- neither
    // survives ImageIO, and the logo fallback below is frequently an SVG.
    //
    // With [BaseConfig.socialImageCrop] the copy is also cropped to the card's
    // own 1.91:1, which takes the aspect-ratio question below out of the hands
    // of whatever the author happened to upload. Only when the source is big
    // enough to crop from: Cover would otherwise upscale a small image to fill
    // the frame.
    val card = socialImage?.let { image ->
        val cropToCard = website.config.socialImageCrop == true &&
            image.width >= SOCIAL_CARD_WIDTH && image.height >= SOCIAL_CARD_HEIGHT
        when {
            image.imageInfo.isAnimatedGif || image.imageInfo.isSvg ->
                ResizedImage(image.href(context, absoluteUri = true), image.width, image.height)
            cropToCard -> image.resize(
                context,
                SOCIAL_CARD_WIDTH,
                SOCIAL_CARD_HEIGHT,
                FillType.Cover,
                absoluteUri = true
            )
            else -> image.resize(
                context,
                SOCIAL_IMAGE_MAX_EDGE,
                SOCIAL_IMAGE_MAX_EDGE,
                FillType.Fit,
                absoluteUri = true
            )
        }
    }

    if (card != null) {
        property("og:image", card.href)
        property("og:image:width", card.width.toString())
        property("og:image:height", card.height.toString())
        twitterProperty("twitter:image", card.href)
    }

    // twitter:card was never emitted, so X rendered no card at all -- not even
    // on articles that had an og:image. The big card needs an image that is
    // both wide and at least 300x157, which is what X requires for it; a
    // squarer one would be centre-cropped and a smaller one is rejected
    // outright, so both fall back to the thumbnail card. Measured on the
    // resized copy, since that is what actually gets served.
    val fitsLargeCard = card != null &&
        card.width >= 300 && card.height >= 157 &&
        card.width.toDouble() / card.height >= 1.5
    twitterProperty("twitter:card", if (fitsLargeCard) "summary_large_image" else "summary")

//    meta(content = "text/html;charset=utf-8") {
//        httpEquiv = MetaHttpEquiv.contentType
//    }

//    meta(content = "utf-8") {
//        httpEquiv = "encoding"
//    }

    website.headInject?.let { headInject -> unsafe { raw(headInject) } }

}

public fun MenuEntry.href(context: RenderContext<*>): String? =
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