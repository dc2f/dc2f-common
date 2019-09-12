package com.dc2f.common.theme

import com.dc2f.common.contentdef.*
import com.dc2f.render.*

fun Theme.baseTheme() {
    robotsTxt()
    config.pageRenderer<BaseWebsite> {
        renderChildren(node.children)
        createSubContext(node.index, out, enclosingNode = null).render()
    }
    config.pageRenderer<LandingPage> { landingPage() }
}


fun Theme.contentTemplates() {
}

fun Theme.robotsTxt() {
    config.pageRenderer<BaseWebsite>(OutputType.robotsTxt) {
        out.appendln("User-agent: *")
        if (Dc2fEnv.current == Dc2fEnv.Production) {
            out.appendln("Allow: /")
            out.appendln("")
            out.appendln("Sitemap: https://anlage.app/sitemap.xml")
        } else {
            out.appendln("Disallow: /")
        }
    }
}
