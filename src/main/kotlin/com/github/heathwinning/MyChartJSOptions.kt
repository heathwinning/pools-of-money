package com.github.heathwinning

import com.beust.klaxon.JsonObject
import com.github.heathwinning.plugins.chartjs.JSFunction

fun myChartJSOptions(tooltipClassName: String, reverseY: Boolean = false) = JsonObject(
    mapOf(
        "maintainAspectRatio" to false,
        "hover" to mapOf(
            "mode" to "nearest",
            "axis" to "x",
            "intersect" to false
        ),
        "tooltips" to mapOf(
            "mode" to "nearest",
            "axis" to "x",
            "intersect" to false,
            "enabled" to false,
            "custom" to JSFunction(
                """function (tooltip) {
        // Tooltip Element
        const tooltipElement = document.getElementsByClassName('${tooltipClassName}')[0];
        if (tooltipElement.lastChild) {
          tooltipElement.removeChild(tooltipElement.lastChild);
        }
        // Hide if no tooltip
        if (tooltip.opacity === 0) {
          tooltipElement.style.opacity = '0';
          return;
        }
        // Set caret Position
        tooltipElement.classList.remove('above', 'below', 'no-transform');
        if (tooltip.yAlign) {
          tooltipElement.classList.add(tooltip.yAlign);
        } else {
          tooltipElement.classList.add('no-transform');
        }
        if (tooltip.body) {
          const titleLines = tooltip.title || [];
          const tooltipTable = document.createElement('table');
          tooltipTable.style.borderSpacing = '5px 2px';
          tooltipTable.style.fontFamily = 'Arial, Helvetica, sans-serif';
          tooltipElement.appendChild(tooltipTable);
          const tooltipHead = document.createElement('thead');
          titleLines.forEach(function (title) {
            const titleRow = document.createElement('th');
            const titleNode = document.createTextNode(title);
            titleRow.appendChild(titleNode);
            titleRow.colSpan = 3;
            titleRow.style.whiteSpace = 'nowrap';
            tooltipHead.appendChild(document.createElement('tr')).appendChild(titleRow);
          });
          tooltipTable.appendChild(tooltipHead);

          const tooltipBody = document.createElement('tbody');
          var total = 0;
          tooltip.body.forEach((line, i) => {
            const colours = tooltip.labelColors[i];
            const lineComponents = line.lines[0].split(': ');
            const amount = lineComponents.pop()
            total += parseInt(amount);
            const rowValue = document.createTextNode(new Intl.NumberFormat('en-US', { style: 'currency', currency: "USD" }).format(amount));
            const rowLabel = document.createTextNode(lineComponents.join(': '));

            const lineRow = document.createElement('tr');

            const keyEntry = document.createElement('td');
            keyEntry.style.whiteSpace = 'nowrap';
            const key = document.createElement('span');
            key.style.background = colours.backgroundColor;
            key.style.borderColor = colours.borderColor;
            key.style.display = 'inline-block';
            key.style.borderWidth = '2px';
            key.style.width = '10px';
            key.style.height = '10px';
            keyEntry.appendChild(key);
            lineRow.appendChild(keyEntry);

            const label = document.createElement('td');
            label.style.whiteSpace = 'nowrap';
            label.appendChild(rowLabel);
            lineRow.appendChild(label);

            const value = document.createElement('td');
            value.style.whiteSpace = 'nowrap';
            value.style.textAlign = 'right';
            value.appendChild(rowValue);
            lineRow.appendChild(value);

            tooltipBody.appendChild(lineRow);
          });
            const rowValue = document.createTextNode(new Intl.NumberFormat('en-US', { style: 'currency', currency: "USD" }).format(total));
            const rowLabel = document.createTextNode("Total");

            const lineRow = document.createElement('tr');

            const keyEntry = document.createElement('td');
            keyEntry.style.whiteSpace = 'nowrap';
            const key = document.createElement('span');
            key.style.display = 'inline-block';
            key.style.borderWidth = '2px';
            key.style.width = '10px';
            key.style.height = '10px';
            keyEntry.appendChild(key);
            lineRow.appendChild(keyEntry);

            const label = document.createElement('td');
            label.style.whiteSpace = 'nowrap';
            label.appendChild(rowLabel);
            lineRow.appendChild(label);

            const value = document.createElement('td');
            value.style.whiteSpace = 'nowrap';
            value.style.textAlign = 'right';
            value.appendChild(rowValue);
            lineRow.appendChild(value);

            tooltipBody.appendChild(lineRow);
            
          tooltipTable.appendChild(tooltipBody);
        }
        tooltipElement.style.opacity = '1';
        tooltipElement.style.zIndex = '10';
        tooltipElement.style.fontFamily = tooltip._bodyFontFamily;
        tooltipElement.style.fontSize = tooltip.bodyFontSize;
        tooltipElement.style.fontStyle = tooltip._bodyFontStyle;
        tooltipElement.style.padding = tooltip.yPadding + 'px ' + tooltip.xPadding + 'px';
        tooltipElement.style.position = 'absolute';
        tooltipElement.style.left = this._chart.canvas.offsetLeft + tooltip.caretX + 'px';
        tooltipElement.style.top = this._chart.canvas.offsetTop + tooltip.caretY + 'px';
        console.log(this._chart)
        console.log("top: "+this._chart.canvas.offsetTop)
        console.log("left: "+this._chart.canvas.offsetLeft)
      }
                                                        """.trimIndent()
            )
        ),
        "scales" to mapOf(
            "xAxes" to listOf(
                mapOf(
                    "stacked" to true,
                    "barPercentage" to 1,
                    "categoryPercentage" to 1,
                    "scaleLabel" to mapOf(
                        "display" to true,
                        "labelString" to "Date"
                    )
                )
            ),
            "yAxes" to listOf(
                mapOf(
                    "stacked" to true,
                    "ticks" to mapOf(
                        "reverse" to reverseY,
                        "callback" to JSFunction(
                            """function(value, index, values) { return new Intl.NumberFormat('en-US', { style: 'currency', currency: "USD" }).format(value) }""".trimIndent()
                        )
                    )
                )
            )
        ),
        "elements" to mapOf(
            "line" to mapOf(
                "fill" to -1
            )
        )
    )
)
