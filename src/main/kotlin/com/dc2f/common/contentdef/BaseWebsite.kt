package com.dc2f.common.contentdef

import com.dc2f.*
import com.dc2f.render.*
import com.dc2f.richtext.*
import com.fasterxml.jackson.annotation.JacksonInject
import java.lang.Appendable
import java.lang.StringBuilder

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

interface ResizeConfig {
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
