package com.example.chartjs

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import io.kweb.dom.element.creation.tags.CanvasElement
import io.kweb.random
import io.kweb.toJson
import java.time.Instant

class Chart(canvas: CanvasElement, chartConfig: ChartJSConfig) {
    private val chartVarName = "c${random.nextInt(10000000)}"

    init {
        canvas.creator?.require(ChartJsPlugin::class)
        canvas.execute("""
            var $chartVarName = new Chart(${canvas.jsExpression}.getContext('2d'), ${Klaxon().converter(
            JSFunctionConverter).toJsonString(chartConfig)})
        """.trimIndent())
    }
}

data class ChartJSConfig(val type: ChartType, val data: ChartJSData, val options: JsonObject)

enum class ChartType {
    bar, line
}

data class ChartJSData(val labels: List<String>,
                     val datasets: List<DataSetJS>)

class DataSetJS(val label: String,
                dataList: DataList,
                val type: ChartType? = null,
                val steppedLine: String = "after",
                val fill: String = "-1",
                val backgroundColor: String = "rgba(0,0,0,0.1)"
) {
    val data : Array<out Any> = dataList.list
}
data class Point(val x : Number, val y : Number)
data class DatePoint(val x : Instant, val y : Number)

sealed class DataList(val list : Array<out Any>) {
    class Numbers(vararg numbers : Number) : DataList(numbers)
    class Points(vararg points : Point) : DataList(points)
}

class JSFunction(val func: String?)
val JSFunctionConverter = object: Converter {
    override fun canConvert(cls: Class<*>): Boolean = cls == JSFunction::class.java

    override fun fromJson(jv: JsonValue) = JSFunction(jv.string)

    override fun toJson(value: Any): String {
        (value as JSFunction).func?.let {
            return it
        }
        return ""
    }
}

