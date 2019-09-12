package com.dc2f.common.contentdef

import com.dc2f.*
import com.dc2f.util.toStringReflective
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface MenuEntry : ContentDef {
    val name: String?
    val ref: ContentReference?
    val url: String?
}

val MenuEntry.linkLabel: String
    get() = this.name ?: (this.ref?.referencedContent as? WithMenuDef)?.menu?.name ?: (this.ref?.referencedContent as? WithPageSeo)?.seo?.title ?: throw Exception("No name for menu entry. ${this.toStringReflective()}")

interface WithMenuDef : ContentDef {
    val menu: MenuDef?
}

interface MenuDef: ContentDef {
    val name: String
}

fun <T> debugger(id: Any, block: () -> T): T {
    val ret = block()
    logger.debug("got ret: $ret")
    return ret
}

fun List<MenuEntry>.findActiveEntry(loaderContext: LoaderContext, page: ContentDef) =
    debugger(page) {
        map {
            it to it.ref?.referencedContent?.let { ref ->
                loaderContext.subPageDistance(
                    parent = ref,
                    child = page
                )
            }
        }
    }
        .filter { it.second != null }
        .sortedWith(compareBy { it.second })
        .also {
            logger.debug { "got result $it" }
        }
        .firstOrNull()
        ?.first

interface Menu : ContentDef {
    val name: String
    val children: List<MenuEntry>
}
