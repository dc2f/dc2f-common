@file:Suppress("UnstableApiUsage")

package com.dc2f.common.theme

import com.dc2f.FillType
import com.dc2f.common.contentdef.*
import com.google.common.net.MediaType
import kotlinx.html.*

fun BaseTheme.landingPageTemplates() {

    config.pageRenderer<LandingPage> {
        appendHTML().baseTemplate(
            this,
            node.seo
        ) {
            //        div {
            node.children.map { child ->
                when (child) {
                    is LandingPageElement.Intro -> {
                        div("homepage-hero-module") {
                            div("video-container") {
                                // TODO video stuff
                                div("filterx")
                                video("fillWidth is-hidden-mobile") {
                                    autoPlay = true
                                    loop = true
                                    attributes["muted"] = "muted"
                                    poster = child.backgroundVideo.placeholder.href(context)
                                    source {
                                        src = child.backgroundVideo.videoMp4.href(context)
                                        type = MediaType.MP4_VIDEO.toString()
                                    }
                                    source {
                                        src = child.backgroundVideo.videoWebm.href(context)
                                        type = MediaType.WEBM_VIDEO.toString()
                                    }
                                }
                                div("poster") {
                                    style =
                                        "background-image: url('${child.backgroundVideo.placeholder.href(
                                            context
                                        )}')"
                                }
                            }
                            div("hero-module-content") {
                                div("section") {
                                    div("has-text-centered") {
                                        h1("title") { +child.teaser }
                                        h2("subtitle") {
                                            +"Success per stock. Multiple Currencies. Compare Performance. Monthly Reports."
                                        }
                                        div {
                                            a(
                                                "#start-element",
                                                classes = "button is-primary is-large"
                                            ) {
                                                +child.buttonLabel
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    is LandingPageElement.Hero -> {
                        section("landing-hero-element section hero") {
                            when(child.colorTheme) {
                                ColorTheme.Primary -> classes = classes + "is-primary" + "is-bold"
                                ColorTheme.Default -> {}
                            }
                            div("hero-content") {
                                div("container") {
                                    div("columns is-vcentered") {
                                        if (!child.textInLeftColumn) {
                                            classes = classes + "columns-reversed"
                                        }
                                        div("column content") {
                                            if (child.bodyTextAlign == TextAlign.Center) {
                                                classes = classes + "has-text-centered"
                                            }
                                            h2("title is-size-1") { +child.title }
                                            markdown(context, child.body)
//                                        unsafe {
//                                            +child.body.toString()
//                                        }
                                        }
                                        div("column is-7") {
                                            // TODO add image resizing/optimization stuff
                                            child.screenshot.resize(
                                                context,
                                                1200,
                                                Int.MAX_VALUE,
                                                FillType.Fit
                                            ).let { image ->
                                                figure("image screenshot") {
                                                    attributes["data-aos"] = "fade-up"
                                                    attributes["data-name"] = child.screenshot.name
                                                    img {
                                                        src = image.href
                                                        // DIFF for compatibility. but maybe we should use child.title instead of file name.
//                                                    alt = child.title
                                                        alt = child.screenshot.name
                                                        width = image.width.toString()
                                                        height = image.height.toString()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is LandingPageElement.Start -> {
                        section("section has-background-primary-light") {
                            div("anchor") {
                                div {
                                    id = "start-element"
                                    attributes["data-target"] = "start-element-input"
                                }
                            }

                            div("container") {
                                div("columns") {
                                    div("column has-text-centered") {
                                        div("is-size-3") { +child.title }
                                        h4("subtitle is-size-5 is-bold") { +child.subTitle }
                                    }
                                    div("column has-text-centered") {
                                        div("is-size-3 email-form-spacing") { unsafe { raw("&nbsp;") } }
                                        form(classes = "email-form") {
                                            div("field") {
                                                div("control has-icons-left") {
                                                    span("icon is-small is-left") {
                                                        i("fas fa-user")
                                                    }
                                                    textInput(name = "email", classes = "input") {
                                                        id = "start-element-input"
                                                        placeholder = "Email Address"
                                                    }
                                                }
                                            }
                                            submitInput(classes = "button is-primary") {
                                                value = "Sign Up for free"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is LandingPageElement.NotReady -> unsafe {
                        raw(
                            """
<section class="section">
    <div class="container">
        <div class="content has-text-centered">
            <p>
                Not ready yet to commit <span class="has-text-success has-text-weight-bold"
                                              data-fsc-item-path="anlage-app-premium-sub"
                                              data-fsc-item-total></span>?</p>
            <p>
                <a href="" class="button" data-fsc-action="Reset,Add,Checkout" data-fsc-item-path-value="anlage-app-free-sub">Start free Trial</a>
            </p>
        </div>
    </div>
</section>
                    """
                        )
                    }
                    is LandingPageElement.Content ->
                        section("section") {
                            div("container content") {
                                richText(createSubContext(child, out = out, enclosingNode = node), child.body)
                            }
                        }
                    is LandingPageElement.RawContent ->
//                    section("section") {
                        richText(createSubContext(child, out = out, enclosingNode = node), child.body)
//                    }

                    else ->
                        unsafe { raw(renderNode(child)) }

                }.let { } // https://discuss.kotlinlang.org/t/sealed-classes-and-when-expressions/3980
            }
        }
    }
}

