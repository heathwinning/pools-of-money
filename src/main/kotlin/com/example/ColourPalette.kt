package com.example

import com.github.ajalt.colormath.HSV

val values = listOf(70, 90)
val saturations = listOf(60)
val colourPalette = sequence {
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
