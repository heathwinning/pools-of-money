package com.example

import io.kweb.dom.attributes.attr
import io.kweb.dom.attributes.set
import io.kweb.dom.element.Element
import io.kweb.dom.element.creation.ElementCreator
import io.kweb.dom.element.creation.tags.InputElement
import io.kweb.dom.element.creation.tags.InputType
import io.kweb.dom.element.creation.tags.ValueElement

fun ElementCreator<Element>.input(type: InputType? = null, name: String? = null, initialValue: String? = null, size: Int? = null, placeholder: String? = null, attributes: Map<String, Any> = attr, kvarUpdateEvent: String = "input"): CustomInputElement {
    return CustomInputElement(element("input", attributes = attributes
        .set("type", type?.name)
        .set("name", name)
        .set("value", initialValue)
        .set("placeholder", placeholder)
        .set("size", size)
    ), kvarUpdateEvent)
}

// TODO: Other element types might also benefit from some of this functionality, extract a common parent Element type
open class CustomInputElement(override val element: Element, kvarUpdateEvent: String) : ValueElement(element, kvarUpdateEvent) {
    fun checked(checked: Boolean) = setAttributeRaw("checked", checked)


    fun select() = element.execute("$jsExpression.select();")

    /*
     copyText.setSelectionRange(0, 99999);
     */

    fun setSelectionRange(start : Int, end : Int) = element.execute("$jsExpression.setSelectionRange($start, $end);")

    fun setReadOnly(ro: Boolean) = element.execute("$jsExpression.readOnly = $ro;")
}