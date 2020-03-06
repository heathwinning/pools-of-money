package com.example.plugins.adsense

import io.kweb.plugins.KwebPlugin

class AdsensePlugin(private val adClient: String) : KwebPlugin() {
    override fun decorate(startHead: StringBuilder, endHead: StringBuilder) {
        startHead.appendln("<script data-ad-client=\"$adClient\" async src=\"https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js\"></script>")
    }
}