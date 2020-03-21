package com.github.heathwinning

import com.github.ajalt.colormath.HSV

fun colourPalette(saturations: List<Int> = listOf(60), values: List<Int> = listOf(90)) = sequence {
    for (s in saturations) {
        for (v in values) {
            yield(HSV(0,s,v,0.6F))
        }
    }
    var denominator = 2
    while (true) {
        for (numerator in 1 until denominator step 2) {
            val h = 255 * numerator / denominator
            for (s in saturations) {
                for (v in values) {
                    yield(HSV(h,s,v,0.6F))
                }
            }
        }
        denominator *= 2
    }
}
