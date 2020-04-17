package com.github.heathwinning

import io.kweb.Kweb
import io.kweb.dom.element.creation.ElementCreator
import io.kweb.dom.element.creation.tags.InputType
import io.kweb.dom.element.creation.tags.div
import io.kweb.dom.element.creation.tags.label
import io.kweb.dom.element.creation.tags.select
import io.kweb.dom.element.new
import io.kweb.plugins.fomanticUI.fomantic
import io.kweb.plugins.fomanticUI.fomanticUIPlugin
import io.kweb.state.KVar

fun main() {
    val poolForm = PoolForm()
        Kweb(port = 8080, plugins = listOf(fomanticUIPlugin)) {
            doc.body.new {
                div(fomantic.ui.main.container).new {
                div(fomantic.ui.form.vertical.segment).new {
                    div(fomantic.ui.raised.segment).new {
                        poolForm.form(this)
                    }
                }
            }
        }
    }
}

class PoolForm {
    val kName: KVar<String> = KVar("")
    val kGrowthRate: KVar<String> = KVar("")
    val kCompoundFrequency: KVar<String> = KVar("1D")
    val kStartVolume: KVar<String> = KVar("")

    fun toPool(): Pool {
        return Pool(kName.value, kStartVolume.value.toDouble())
    }

    fun toPoolInterestStream(pool: Pool, reportDates: DateProgression): PoolInterestStream {
        return PoolInterestStream(
            "${pool.name} interest/growth",
            pool,
            pool,
            reportDates step DateStep.fromString(kCompoundFrequency.value),
            kGrowthRate.value.toDouble()
        )
    }

    fun form(elementCreator: ElementCreator<*>) = elementCreator.div().new {
        div(fomantic.three.fields).new {
            div(fomantic.field).new {
                label().text("Pool name")
                input(type = InputType.text, placeholder = "Pool name", kvarUpdateEvent = "change").value = kName
            }
            div(fomantic.field).new {
                label().text("Initial deposit")
                div(fomantic.ui.left.labeled.input).new {
                    div(fomantic.ui.label).text("$")
                    input(type = InputType.number, placeholder = "Initial deposit", kvarUpdateEvent = "change").value =
                        kStartVolume
                }
            }
            div(fomantic.field).new {
                label().text("Interest/growth rate")
                div(fomantic.ui.right.labeled.input).new {
                    input(
                        type = InputType.number,
                        placeholder = "Interest/growth rate",
                        kvarUpdateEvent = "change"
                    ).value = kGrowthRate
                    div(fomantic.ui.label).text("% per year")
                }
            }
            div(fomantic.field).new {
                label().text("Growth frequency")
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
                    attributes = fomantic.ui.dropdown.fluid
                ).value = kCompoundFrequency
            }
        }
    }
}