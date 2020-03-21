package com.github.heathwinning

import com.github.ajalt.colormath.toCssRgb
import com.github.heathwinning.plugins.chartjs.*
import com.github.heathwinning.plugins.googleAnalytics.GoogleAnalyticsPlugin
import io.kweb.Kweb
import io.kweb.dom.element.Element
import io.kweb.dom.element.creation.ElementCreator
import io.kweb.dom.element.creation.tags.*
import io.kweb.dom.element.new
import io.kweb.plugins.fomanticUI.fomantic
import io.kweb.plugins.fomanticUI.fomanticUIPlugin
import io.kweb.state.KVar
import io.kweb.state.render.render
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun main() {
    Kweb(
        port = 8080, plugins = listOf(
            fomanticUIPlugin,
            ChartJsPlugin(),
            GoogleAnalyticsPlugin("UA-161523896-1")
        )
    ) {
        doc.body.new {
            val reportingForm = ReportingForm()
            val poolForms = KVar(listOf<PoolForm>(PoolForm()))
            val externalSourcePool = Pool("source")
            val externalSinkPool = Pool("sink")
            val streamForms = KVar(listOf<StreamForm>())
            div(fomantic.ui.main.container).new {
                div(fomantic.ui.form.vertical.segment).new {
                    reportingForm.form(this)
                    h2().text("Pools")
                    render(poolForms) { pools ->
                        for (pool in pools) {
                            div(fomantic.ui.raised.segment).new {
                                if (pools.size > 1) {
                                    div(fomantic.ui.top.attached.label.button).apply {
                                        on.click {
                                            val mutableList = poolForms.value.toMutableList()
                                            mutableList.remove(pool)
                                            poolForms.value = mutableList
                                        }
                                    }.new {
                                        i(fomantic.icon.trash)
                                        span().text("Remove pool")
                                    }
                                }
                                pool.form(this)
                            }
                        }
                        button(fomantic.ui.button).text("Add a pool").apply {
                            on.click {
                                val mutableList = poolForms.value.toMutableList()
                                mutableList.add(PoolForm())
                                poolForms.value = mutableList
                            }
                        }
                    }
                    h2().text("Streams")
                    render(streamForms) { streams ->
                        for (stream in streams) {
                            div(fomantic.ui.raised.segment).new {
                                div(fomantic.ui.top.attached.label.button).apply {
                                    on.click {
                                        val mutableList = streamForms.value.toMutableList()
                                        mutableList.remove(stream)
                                        streamForms.value = mutableList
                                    }
                                }.new {
                                    i(fomantic.icon.trash)
                                    span().text("Remove stream")
                                }
                                stream.form(this, poolForms)
                            }
                        }
                        button(fomantic.ui.button).text("Add a stream").apply {
                            on.click {
                                val mutableList = streamForms.value.toMutableList()
                                mutableList.add(StreamForm())
                                streamForms.value = mutableList
                            }
                        }
                    }
                }
                div(fomantic.ui.vertical.segment).new {
                    val calculateButton = button(fomantic.ui.button).text("Calculate")

                    val networkChartHolder = div(fomantic.ui.grid)
                    val streamChartHolder = div(fomantic.ui.grid)

                    calculateButton.on.click {
                        val reportingStartDate = LocalDate.parse(
                            reportingForm.kStartDate.value,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        )
                        val reportingEndDate = reportingStartDate.plusYears(reportingForm.kDuration.value.toLong())
                        val reportingDateRange = reportingStartDate..reportingEndDate
                        val reportingFrequency =
                            DateStep.fromString(reportingForm.kFrequency.value)

                        val poolsInNetwork = poolForms.value.map { poolForm -> poolForm.toPool() }
                        val allPools = listOf(externalSinkPool, externalSourcePool) + poolsInNetwork

                        val streams = streamForms.value.map { streamForm ->
                            val sourcePool = allPools.find { pool -> pool.name == streamForm.kSourcePool.value }
                            val targetPool = allPools.find { pool -> pool.name == streamForm.kTargetPool.value }
                            return@map streamForm.toStream(sourcePool!!, targetPool!!, reportingEndDate)
                        } + poolForms.value.mapIndexed { index, poolForm ->
                            val pool = poolsInNetwork[index]
                            poolForm.toPoolInterestStream(pool, reportingDateRange)
                        }
                        val network = PoolNetwork(poolsInNetwork, streams)
                        network.simulate(reportingDateRange)

                        val reportingDates = reportingDateRange step reportingFrequency
                        val reportingIndices = reportingDates.map { date -> reportingDateRange.indexOf(date) }

                        val poolColours = colourPalette().take(network.pools.size).toList()
                        val poolDatasets = network.pools.mapIndexed { index, pool ->
                            DataSetJS(
                                label = pool.name,
                                dataList = DataList.Numbers(*reportingIndices.map { i -> pool.history[i] } //downsample
                                    .toTypedArray()),
                                backgroundColor = poolColours[index].toCssRgb()
                            )
                        }
                        val pool = network.pools[0]
                        val streamsIn = network.streamsIn(pool)
                        val streamInColours = colourPalette().take(streamsIn.size).toList()
                        val streamInDatasets = streamsIn.mapIndexed { index, stream ->
                            DataSetJS(
                                label = stream.name,
                                dataList = DataList.Numbers(*reportingIndices.map { i -> stream.targetHistory[i] } //downsample
                                    .toTypedArray()),
                                backgroundColor = streamInColours[index].toCssRgb()
                            )
                        }
                        val streamsOut = network.streamsOut(pool)
                        val streamsOutColours = colourPalette().take(streamsOut.size).toList()
                        val streamsOutDatasets = streamsOut.mapIndexed { index, stream ->
                            DataSetJS(
                                label = stream.name,
                                dataList = DataList.Numbers(*reportingIndices.map { i -> stream.sourceHistory[i] } //downsample
                                    .toTypedArray()),
                                backgroundColor = streamsOutColours[index].toCssRgb()
                            )
                        }
                        networkChartHolder.removeChildren()
                        networkChartHolder.new {
                            div(fomantic.sixteen.wide.column).setAttributeRaw("style", "height: 70vh;").new {
                                chartJSTooltip("network-tooltip")
                                Chart(
                                    canvas(2, 1),
                                    ChartJSConfig(
                                        type = ChartType.bar,
                                        data = ChartJSData(
                                            labels = reportingDates.map { date ->
                                                date.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "MM-yyyy"
                                                    )
                                                )
                                            },
                                            datasets = poolDatasets
                                        ),
                                        options = myChartJSOptions("network-tooltip")
                                    )
                                )
                            }
                            streamChartHolder.removeChildren()
                            streamChartHolder.new {
                                div(fomantic.eight.wide.column).setAttributeRaw("style", "height: 70vh;").new {
                                    chartJSTooltip("streams-in-tooltip")
                                    Chart(
                                        canvas(2, 1),
                                        ChartJSConfig(
                                            type = ChartType.bar,
                                            data = ChartJSData(
                                                labels = reportingDates.map { date ->
                                                    date.format(
                                                        DateTimeFormatter.ofPattern(
                                                            "MM-yyyy"
                                                        )
                                                    )
                                                },
                                                datasets = streamInDatasets
                                            ),
                                            options = myChartJSOptions("streams-in-tooltip")
                                        )
                                    )
                                }
                                div(fomantic.eight.wide.column).setAttributeRaw("style", "height: 70vh;").new {
                                    chartJSTooltip("streams-out-tooltip")
                                    Chart(
                                        canvas(2, 1),
                                        ChartJSConfig(
                                            type = ChartType.bar,
                                            data = ChartJSData(
                                                labels = reportingDates.map { date ->
                                                    date.format(
                                                        DateTimeFormatter.ofPattern(
                                                            "MM-yyyy"
                                                        )
                                                    )
                                                },
                                                datasets = streamsOutDatasets
                                            ),
                                            options = myChartJSOptions("streams-out-tooltip", reverseY = true)
                                        )
                                    )
                                }
                            }
                            execute("""window.scrollBy({left:0,top:window.innerHeight*0.8,behaviour:"smooth"})""")
                        }
                    }
                }
            }
        }
    }
}

private fun ElementCreator<Element>.chartJSTooltip(className: String) {
    div(
        mapOf(
            "style" to """
                                        opacity: 1;
                                        position: absolute;
                                        background: rgba(0, 0, 0, .7);
                                        color: white;
                                        border-radius: 3px;
                                        -webkit-transition: all .1s ease;
                                        transition: all .1s ease;
                                        pointer-events: none;
                                        -webkit-transform: translate(-50%, 0);
                                        transform: translate(-50%, 0);""".trimIndent()
        )
    ).addClasses(className)
}
