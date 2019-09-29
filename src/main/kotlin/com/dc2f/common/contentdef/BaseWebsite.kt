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

    @JvmDefault
    override fun contentSymlink(): ContentDef? = index

    @JvmDefault
    override fun includeInSitemap(): Boolean =
        includeInSitemap ?: super.includeInSitemap()

    @JvmDefault
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
}

@Suppress("RedundantModalityModifier", "unused", "unused")
interface BaseWebsite : Website<WebsiteFolderContent>, WithSitemapInfo, WithContentSymlink {
    @set:JacksonInject("index")
    abstract var index: LandingPage
    abstract val config: BaseConfig
    abstract val navBarLogo: ImageAsset?

    abstract val embed: Embeddables?

    abstract val mainMenu: List<MenuEntry>
    abstract val footerMenu: List<Menu>
    abstract val footerContent: ContentReference?

    /** Allows adding additional tags inside <head></head> (e.g. for analytics) */
    abstract val headInject: String?

    @JvmDefault
    override fun contentSymlink(): ContentDef? = index

    @JvmDefault
    open fun createLinkedData(context: RenderContext<*>): Map<String, Any?>? = mapOf(
        "@context" to "http://schema.org",
        "@type" to "Product",
        "url" to context.href(this.index, true),
        "name" to name,
        "logo" to config.logo?.href(context, absoluteUri = true)
    )

}

@Nestable("folder")
interface ContentPageFolder : WebsiteFolderContent, ContentBranchDef<WebsiteFolderContent>

@Nestable("content")
interface ContentPage : ContentDef, WebsiteFolderContent {
    var seo: PageSeo
    val embed: Embeddables?
    @set:JacksonInject("body")
    var body: RichText
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
