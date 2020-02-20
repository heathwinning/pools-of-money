package com.example.chartjs

import io.kweb.plugins.KwebPlugin

class ChartJsPlugin(val version: String = "2.9.3") : KwebPlugin() {
    override fun decorate(startHead: StringBuilder, endHead: StringBuilder) {
        startHead.appendln("<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/$version/Chart.bundle.min.js\"></script>")
    }
}

