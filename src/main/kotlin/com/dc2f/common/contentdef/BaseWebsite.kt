package com.dc2f.common.contentdef

import com.dc2f.*
import com.dc2f.render.*
import com.dc2f.richtext.*
import com.fasterxml.jackson.annotation.JacksonInject

interface WithPageSeo : ContentDef, WithSitemapInfo {
    val seo: PageSeo
}

interface PageSeo : ContentDef {
    var title: String
    val description: String
    val noIndex: Boolean?
}

/** Marker interface for content inside folders. */
interface WebsiteFolderContent : ContentDef, SlugCustomization, WithRedirect, WithContentSymlink,
    WithSitemapInfo, WithRenderPathAliases {
    //    val menu: MenuDef?
//    override val redirect: ContentReference?
    @set:JacksonInject("index")
    var index: WebsiteFolderContent?

    var includeInSitemap: Boolean?

    val renderPathAliases: ArrayList<String>?

    override fun contentSymlink(): ContentDef? = index

    override fun includeInSitemap(): Boolean =
        includeInSitemap ?: super.includeInSitemap()

    override fun renderPathAliases(renderer: Renderer): List<RenderPath>? =
        renderPathAliases?.map { RenderPath.parseLeafPath(it) }
}

interface ResizeConfig : ContentDef {
    val width: Int?
    val height: Int?
    val fillType: FillType?
}

abstract class FigureEmbeddable : ContentDef {
    abstract val alt: String?
    abstract val title: String?
    abstract val image: ImageAsset
    abstract val resize: ResizeConfig?
    /** Renders this as an inline <img> tag, instead of a <figure> **/
    open var inlineImage: Boolean = false
}

@Suppress("unused")
interface Embeddables : ContentDef {
    val references: Map<String, ContentReference>?
    val figures: Map<String, FigureEmbeddable>?
    val files: Map<String, FileAsset>?
    val pebble: Map<String, Pebble>?
}

interface Favicon : ContentDef {
    val image: ImageAsset
}

interface BaseConfig : ContentDef {
    val favicons: List<Favicon>
    val url: UrlConfig
    val logo: ImageAsset?

    /**
     * Fallback image for og:image/twitter:image on pages that have no image of
     * their own -- the landing page, index pages, etc. Kept separate from
     * [logo] because a social card is usually not the logo, and because
     * og:image needs a raster format where a logo is often SVG.
     *
     * When unset, [logo] is used instead. Prefer setting this explicitly: an
     * SVG logo cannot serve as an og:image, since the crawlers do not render
     * SVG.
     */
    val socialImage: ImageAsset?

    /**
     * Crop the social card to 1.91:1 instead of preserving the source aspect
     * ratio.
     *
     * The large card (`summary_large_image`) needs an image at least half again
     * as wide as it is tall, so a source that is merely close to it -- 3:2
     * photography sits at 1.5, and a generated image can land just under --
     * silently degrades to the small thumbnail card. Cropping decides that
     * question at build time rather than leaving it to the source material.
     *
     * The cost is that the card is no longer the whole image: roughly a fifth
     * of the height is taken off centre. Worth it for photographic teasers,
     * wrong for anything where the edges carry meaning, which is why this is
     * opt-in per site rather than the default.
     *
     * Ignored for sources smaller than the card, which are never upscaled.
     */
    val socialImageCrop: Boolean?
}

interface NavBar : ContentDef {
    abstract val logo: ImageAsset?
    abstract val title: String?
}

@Suppress("RedundantModalityModifier", "unused", "unused")
interface BaseWebsite : Website<WebsiteFolderContent>, WithSitemapInfo, WithContentSymlink {
    @set:JacksonInject("index")
    abstract var index: LandingPage
    abstract val config: BaseConfig
    abstract val navBar: NavBar?

    abstract val embed: Embeddables?

    abstract val mainMenu: List<MenuEntry>
    abstract val footerMenu: List<Menu>
    abstract val footerContent: ContentReference?

    /** Allows adding additional tags inside <head></head> (e.g. for analytics) */
    abstract val headInject: String?

    override fun contentSymlink(): ContentDef? = index

    open fun createLinkedData(context: RenderContext<*>): Map<String, Any?>? = mapOf(
        "@context" to "http://schema.org",
        "@type" to "Product",
        "url" to context.href(this.index, true),
        "name" to name,
        "logo" to config.logo?.href(context, absoluteUri = true)
    )

}

@Nestable("folder")
interface ContentPageFolder : WebsiteFolderContent, ContentBranchDef<WebsiteFolderContent> {
    override fun includeInSitemap(): Boolean =
      index != null && super.includeInSitemap()
}

@Nestable("content")
interface ContentPage : ContentDef, WebsiteFolderContent, WithPageSeo {
    override var seo: PageSeo
    val embed: Embeddables?
    @set:JacksonInject("body")
    var body: RichText
}

/**
 * Simple page with html content.
 */
@Nestable("htmlpage")
interface HtmlPage : ContentPage, WithRenderPathOverride {
    /** additional code which will end up in the <head> section of the page. */
    @set:JacksonInject("head")
    var head: RichText?
    @set:JacksonInject("html")
    var html: RichText
    var params: Map<String, Any>?
    var renderOnlyHtml: Boolean?
    var renderPath: String?

    override fun renderPath(renderer: Renderer): RenderPath? =
        renderPath?.let { RenderPath.parseLeafPath(it) }

}

@Nestable("partial")
abstract class Partial : ContentDef, Renderable {
    @set:JacksonInject("html")
    abstract var html: RichText
    abstract val embed: Embeddables?

    override fun renderContent(renderContext: RenderContext<*>, arguments: Any?): String =
        html.renderContent(
            renderContext.createSubContext(
                this,
                AppendableOutput(StringBuilder()),
                renderContext.node
            ), arguments
        )
}


@Nestable("partials")
interface PartialFolder : ContentBranchDef<Partial>,
    WebsiteFolderContent
