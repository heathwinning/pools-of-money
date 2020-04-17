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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun main() {
    Kweb(port = 8080, plugins = listOf(fomanticUIPlugin)) {
        val reportingForm = ReportingForm()
        doc.body.new {
            div(fomantic.ui.main.container).new {
                div(fomantic.ui.form.vertical.segment).new {
                        reportingForm.form(this)
                }
            }
        }
    }
}

class ReportingForm {
    val kStartDate: KVar<String> = KVar(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    val kDuration: KVar<String> = KVar("10")
    val kFrequency: KVar<String> = KVar("1A")

    fun form(elementCreator: ElementCreator<*>) = elementCreator.div(fomantic.ui).new() {
        div(fomantic.field.ui.raised.segment).new {
            label().text("Report")
            div(fomantic.three.fields).new {
                div(fomantic.field).new {
                    label().text("Start date")
                    input(type = InputType.date, placeholder = "Start date", kvarUpdateEvent = "change").value =
                        kStartDate
                }
                div(fomantic.field).new {
                    label().text("Duration")
                    div(fomantic.ui.right.labeled.input).new {
                        input(type = InputType.number, placeholder = "Duration", kvarUpdateEvent = "change").value =
                            kDuration
                        div(fomantic.ui.label).text("Years")
                    }
                }
                div(fomantic.field).new {
                    label().text("Frequency")
                    select(
                        listOf(
                            Pair("1M", "Monthly"),
                            Pair("1Q", "Quarterly"),
                            Pair("2Q", "Biannually"),
                            Pair("1A", "Annually")
                        ),
                        attributes = fomantic.ui.dropdown.fluid
                    ).value = kFrequency
                }
            }
        }
    }
}