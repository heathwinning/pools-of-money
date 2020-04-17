package com.github.heathwinning.plugins.googleAnalytics

import io.kweb.plugins.KwebPlugin

class GoogleAnalyticsPlugin(private val trackingId: String) : KwebPlugin() {
    override fun decorate(startHead: StringBuilder, endHead: StringBuilder) {
        startHead.appendln("""
            <!-- Global site tag (gtag.js) - Google Analytics -->
            <script async src="https://www.googletagmanager.com/gtag/js?id=$trackingId"></script>
            <script>
              window.dataLayer = window.dataLayer || [];
              function gtag(){dataLayer.push(arguments);}
              gtag('js', new Date());

              gtag('config', '$trackingId');
            </script>
        """.trimIndent())
    }
}
