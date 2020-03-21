package com.github.heathwinning

import com.ibm.icu.text.RuleBasedNumberFormat
import io.kweb.Kweb
import io.kweb.dom.element.creation.ElementCreator
import io.kweb.dom.element.creation.tags.*
import io.kweb.dom.element.new
import io.kweb.plugins.fomanticUI.fomantic
import io.kweb.plugins.fomanticUI.fomanticUIPlugin
import io.kweb.state.KVar
import io.kweb.state.render.render
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

fun main() {
    Kweb(port = 8080, plugins = listOf(fomanticUIPlugin)) {
        val streamForm = StreamForm()
        doc.body.new {
            div(fomantic.ui.main.container).new {
                div(fomantic.ui.form.vertical.segment).new {
                    div(fomantic.ui.raised.segment).new {
                        streamForm.form(this, KVar(listOf(
                            PoolForm(),
                            PoolForm()
                        )))
                    }
                }
            }
        }
    }
}

class StreamForm {
    val kSourcePool: KVar<String> = KVar("")
    val kTargetPool: KVar<String> = KVar("")
    val kName: KVar<String> = KVar("")
    val kFlowFrequency: KVar<String> = KVar("1M")
    val kBaseFlow: KVar<String> = KVar("")
    val kFlowType: KVar<String> = KVar("constant")
    val kGrowth: KVar<String> = KVar("")
    val kGrowthFrequency: KVar<String> = KVar("2")
    val kStartDate: KVar<String> = KVar(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    val kLifetime: KVar<String> = KVar("indefinite")
    val kEndAfter: KVar<String> = KVar("")

    val grows = kFlowType.map { type ->
        type == "linear" || type == "exponential"
    }
    val flowTypeUnit = kFlowType.map { type ->
        when (type) {
            "percentage" -> "%"
            else -> "$"
        }
    }
    val growthTypeUnit = kFlowType.map { type ->
        when (type) {
            "linear" -> "$"
            "exponential" -> "%"
            else -> ""
        }
    }
    val flowFrequencyString = kFlowFrequency.map { freq ->
        when (freq) {
            "1D" -> "day"
            "7D" -> "week"
            "14D" -> "fortnight"
            "1M" -> "month"
            "1Q" -> "quarter"
            "2Q" -> "half-year"
            "1A" -> "year"
            else -> ""
        }
    }
    val flowFrequencyPlural = flowFrequencyString.map { freq -> "${freq}s" }

    fun toStream(sourcePool: Pool, targetPool: Pool, reportEndDate: LocalDate): Stream {
        val name = kName.value
        val startDate = LocalDate.parse(kStartDate.value, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endDate =
            if (kLifetime.value == "indefinite") reportEndDate else startDate.plusYears(kEndAfter.value.toLong())
        return when (kFlowType.value) {
            "constant" -> ConstantStream(
                name,
                sourcePool,
                targetPool,
                startDate..endDate step DateStep.fromString(
                    kFlowFrequency.value
                ),
                kBaseFlow.value.toDouble()
            )
            "linear" -> ConstantIncreaseStream(
                name,
                sourcePool,
                targetPool,
                startDate..endDate step DateStep.fromString(
                    kFlowFrequency.value
                ),
                kBaseFlow.value.toDouble(),
                kGrowth.value.toDouble(),
                kGrowthFrequency.value.toInt()
            )
            "exponential" -> PercentageIncreaseStream(
                name,
                sourcePool,
                targetPool,
                startDate..endDate step DateStep.fromString(
                    kFlowFrequency.value
                ),
                kBaseFlow.value.toDouble(),
                kGrowth.value.toDouble(),
                kGrowthFrequency.value.toInt()
            )
            "percentage" -> PercentageStream(
                name,
                sourcePool,
                targetPool,
                startDate..endDate step DateStep.fromString(
                    kFlowFrequency.value
                ),
                kBaseFlow.value.toDouble()
            )
            else -> throw IllegalArgumentException("Unknown growth strategy \"$this\"")
        }
    }

    fun form(elementCreator: ElementCreator<*>, poolForms: KVar<List<PoolForm>>) = elementCreator.div().new {
        div(fomantic.three.fields).new {
            div(fomantic.field).new {
                label().text("Stream name")
                input(
                    type = InputType.text,
                    placeholder = "Stream name",
                    kvarUpdateEvent = "change"
                ).value =
                    kName
            }
            div(fomantic.field).new {
                label().text("Origin")
                render(poolForms) { pools ->
                    select(
                        listOf(Pair("source", "Incoming (external)")) + pools.map {
                            Pair(
                                it.kName.value,
                                it.kName.value
                            )
                        },
                        attributes = fomantic.ui.dropdown
                    ).value = kSourcePool
                }
            }
            div(fomantic.field).new {
                label().text("Destination")
                render(poolForms) { pools ->
                    select(
                        listOf(Pair("sink", "Outgoing (external)")) + pools.map {
                            Pair(
                                it.kName.value,
                                it.kName.value
                            )
                        },
                        attributes = fomantic.ui.dropdown
                    ).value = kTargetPool
                }
            }
        }
        render(kLifetime) { lifetime ->
            div(fomantic.four.fields).new {
                div(fomantic.field).new {
                    label().text("Flow frequency")
                    select(
                        listOf(
                            Pair("1D", "Daily"),
                            Pair("7D", "Weekly"),
                            Pair("14D", "Fortnightly"),
                            Pair("1M", "Monthly"),
                            Pair("1Q", "Quarterly"),
                            Pair("2Q", "Biannually"),
                            Pair("1A", "Annually")
                        ),
                        attributes = fomantic.ui.dropdown
                    ).value = kFlowFrequency
                }
                div(fomantic.field).new {
                    label().text("Flow forever?")
                    select(
                        listOf(
                            Pair("indefinite", "Stream runs forever"),
                            Pair("definite", "Stream has fixed lifetime")
                        ),
                        attributes = fomantic.ui.dropdown
                    ).value = kLifetime
                }
                if (lifetime == "definite") {
                    div(fomantic.field).new {
                        label().text("Run for")
                        div(fomantic.ui.right.labeled.input).new {
                            input(type = InputType.number, kvarUpdateEvent = "change").value = kEndAfter
                            div(fomantic.ui.label).text(flowFrequencyPlural)
                        }
                    }
                    div(fomantic.field).new {
                        label().text("Start date")
                        input(
                            type = InputType.date,
                            placeholder = "Start date",
                            kvarUpdateEvent = "change"
                        ).value =
                            kStartDate
                    }
                }
            }
        }
        div(fomantic.four.fields).new {
            div(fomantic.field).new {
                label().text("Flow type")
                select(
                    listOf(
                        Pair("constant", "Constant"),
                        Pair("percentage", "Percentage of source pool"),
                        Pair("linear", "Grows over time, by fixed amount"),
                        Pair("exponential", "Grows over time, by fixed percentage")
                    ),
                    attributes = fomantic.ui.dropdown
                ).value = kFlowType
            }
            div(fomantic.field).new {
                label().text("Flow amount")
                render(flowTypeUnit) { type ->
                    if (type == "$") {
                        div(fomantic.ui.left.labeled.input).new {
                            div(fomantic.ui.label).text(type)
                            input(
                                type = InputType.number,
                                placeholder = "Flow amount",
                                kvarUpdateEvent = "change"
                            ).value =
                                kBaseFlow
                        }
                    } else {
                        div(fomantic.ui.right.labeled.input).new {
                            input(
                                type = InputType.number,
                                placeholder = "Flow amount",
                                kvarUpdateEvent = "change"
                            ).value =
                                kBaseFlow
                            div(fomantic.ui.label).text("$type of source pool")
                        }
                    }
                }
            }
            div(fomantic.field).new {
                render(grows) { grows ->
                    div(fomantic.field).new {
                        if (grows) {
                            label().text("Growth rate")
                            render(growthTypeUnit) { type ->
                                if (type == "$") {
                                    div(fomantic.ui.left.labeled.input).new {
                                        div(fomantic.ui.label).text(type)
                                        input(
                                            type = InputType.number,
                                            placeholder = "Growth rate",
                                            kvarUpdateEvent = "change"
                                        ).value = kGrowth
                                    }
                                } else {
                                    div(fomantic.ui.right.labeled.input).new {
                                        input(
                                            type = InputType.number,
                                            placeholder = "Growth rate",
                                            kvarUpdateEvent = "change"
                                        ).value = kGrowth
                                        div(fomantic.ui.label).text(type)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            div(fomantic.field).new {
                render(grows) { grows ->
                    div(fomantic.field).new {
                        if (grows) {
                            label().text("Growth frequency")
                            input(
                                type = InputType.number,
                                placeholder = "Growth frequency",
                                kvarUpdateEvent = "change"
                            ).value = kGrowthFrequency
                        }
                    }
                }
            }
        }
        render(grows) { grows ->
            if (grows) {
                render(flowFrequencyString) { frequency ->
                    render(kBaseFlow) { base ->
                        if (base != "") {
                            render(kGrowth) { rate ->
                                if (rate != "") {
                                    render(kGrowthFrequency) { growthFrequency ->
                                        val formatter =
                                            RuleBasedNumberFormat(
                                                Locale.ENGLISH,
                                                RuleBasedNumberFormat.ORDINAL
                                            )
                                        val frequencyNumber = growthFrequency.toInt()
                                        val frequencyOrdinal = if (frequencyNumber > 1) {
                                            "${formatter.format(frequencyNumber)} $frequency"
                                        } else {
                                            frequency
                                        }
                                        render(growthTypeUnit) { type ->
                                            div(fomantic.ui.message.small).new {
                                                if (type == "%") {
                                                    p().text("This stream will start at \$$base per $frequency, and increase by $rate% every $frequencyOrdinal.")
                                                } else {
                                                    p().text("This stream will start at \$$base per $frequency, and increase by \$$rate every $frequencyOrdinal.")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
