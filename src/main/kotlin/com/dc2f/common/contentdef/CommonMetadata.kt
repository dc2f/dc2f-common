package com.dc2f.common.contentdef

import com.dc2f.*

interface WithMainImage: ContentDef {
    @JvmDefault
    fun mainImage(): ImageAsset? = null
}

interface WithWordCount: ContentDef {
    companion object {
        private val wordCountPattern = Regex("""\b\w[\w\S]*""").toPattern()
    }

    @JvmDefault
    fun wordCount(): Int? = null

    @JvmDefault
    fun countWords(text: String): Int {
        val matcher = wordCountPattern.matcher(text)
        var count = 0
        while (matcher.find()) {
            count++
        }
        return count
    }
}

interface WithAuthor: ContentDef { val author: String }
