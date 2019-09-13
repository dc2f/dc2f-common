package com.dc2f.common.contentdef

import com.dc2f.*
import com.dc2f.richtext.RichText
import com.dc2f.richtext.markdown.Markdown
import com.fasterxml.jackson.annotation.JacksonInject

interface BackgroundVideo : ContentDef {
    val videoWebm: FileAsset
    val videoMp4: FileAsset
    val placeholder: ImageAsset
}

enum class TextAlign {
    Left,
    Right,
    Center
    ;
}

enum class ColorTheme {
    Default,
    Primary,
}

abstract class LandingPageElement : ContentDef {
    abstract val embed: Embeddables?

    @Nestable("intro")
    abstract class Intro : LandingPageElement() {
        abstract val teaser: String
        abstract val buttonLabel: String
        abstract val backgroundVideo: BackgroundVideo
    }
    @Nestable("hero")
    abstract class Hero : LandingPageElement() {
        open var colorTheme = ColorTheme.Default
        abstract val title: String
        @set:JacksonInject("body")
        abstract var body: Markdown
        open var bodyTextAlign : TextAlign = TextAlign.Center
        abstract val screenshot: ImageAsset
        abstract val textInLeftColumn: Boolean
    }
    @Nestable("start")
    abstract class Start : LandingPageElement() {
        abstract val title: String
        abstract val subTitle: String
    }
    @Nestable("notready")
    abstract class NotReady : LandingPageElement()

    @Nestable("content")
    abstract class Content  : LandingPageElement() {
        @set:JacksonInject("body")
        abstract var body: RichText
    }
    @Nestable("raw")
    abstract class RawContent  : LandingPageElement() {
        @set:JacksonInject("body")
        abstract var body: RichText
    }}


interface LandingPage : ContentDef, WebsiteFolderContent, ContentBranchDef<LandingPageElement> {
    /** the pages seo */
    var seo: PageSeo
    val embed: Embeddables?
//    @set:JacksonInject(PROPERTY_CHILDREN)
//    var children: List<LandingPageElement>
}

@Nestable("landingpage")
interface SimpleLandingPage : LandingPage {

}
