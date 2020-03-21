package com.github.heathwinning

import com.beust.klaxon.JsonObject
import com.github.heathwinning.plugins.googleAdsense.GoogleAdsensePlugin
import com.github.ajalt.colormath.toCssRgb
import com.github.heathwinning.plugins.chartjs.*
import com.ibm.icu.text.RuleBasedNumberFormat
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import io.ktor.websocket.WebSockets
import io.kweb.*
import io.kweb.dom.element.creation.ElementCreator
import io.kweb.dom.element.creation.tags.*
import io.kweb.dom.element.new
import io.kweb.dom.title
import io.kweb.plugins.fomanticUI.fomantic
import io.kweb.plugins.fomanticUI.fomanticUIPlugin
import io.kweb.routing.route
import io.kweb.state.KVar
import io.kweb.state.render.render
import koma.*
import koma.extensions.get
import koma.extensions.mapIndexed
import koma.extensions.toTypedArray
import koma.matrix.Matrix
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.pow
import java.time.Duration

fun main() {
    embeddedServer(Jetty, port = 8080, module = Application::kweb).start()
}

fun Application.kweb() {
    install(DefaultHeaders)
    install(Compression)
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(10)
        timeout = Duration.ofSeconds(30)
    }
    install(Kweb) {
        plugins = listOf(
            fomanticUIPlugin, ChartJsPlugin("2.8.0")
            , GoogleAdsensePlugin("ca-pub-2691768896144534")
        )
        buildPage = {
            val config = Config.newConfig()
            doc.head.new {
                title().text("Pools of Money | Compound Interest Calculator")
            }

            doc.body.new {
                div(fomantic.ui.main.container).new {
                    div(fomantic.ui.vertical.segment).new {
                        h1(fomantic.ui.header).text("Pools of Money")
                        p().text("This tool is a compound interest calculator with some bells and whistles to model your entire financial life; e.g. living costs after retirement, mortgage/loan repayments, multiple financial assets with different expected returns, etc.")
                        h2(fomantic.ui.header).text("Terminology")
                        p().innerHTML("Your finances consist of different assets, each of which we imagine as a separate <i>pool</i> of money,")
                        div(fomantic.ui.message).new {
                            p().text("e.g. a bank account, a mortgage, or an investment portfolio.")
                        }
                        p().innerHTML("Money flows into and out of these pools in <i>streams</i>,")
                        div(fomantic.ui.message).new {
                            p().text("e.g. monthly income flows into a bank account, expenses flow out of a bank account, loan repayments flow into a mortgage account.")
                        }

                        p().innerHTML("A pool may also grow based on the amount of money in it,")
                        div(fomantic.ui.message).new {
                            p().text("e.g. a bank account grows at 2% p.a., the principal part of a mortgage grows at 3.5% p.a. (this pool has negative volume), an investment portfolio grows at 6%.")
                        }

                        h2(fomantic.ui.header).text("Examples")
                        div(fomantic.ui.list).new {
                            div(fomantic.item).new {
                                p().innerHTML(
                                    """
                            
                        """.trimIndent()
                                )
                            }
                        }
                    }
                    div(fomantic.ui.form.vertical.segment).new {
                        route {
                            path("/{config}") { params ->
                            }
                            path("/") {
                            }
                        }
                        config.form(this)

                    }
                    div(fomantic.ui.vertical.segment).new {
                        val calculateButton = button(fomantic.ui.button).text("Calculate")
                        val messages = div(fomantic.ui.basic.segment)
                        messages.setAttributeRaw("style", "display: none;")
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
                        ).addClasses("chartjs-tooltip")
                        val chartHolder = div()
                        calculateButton.on.click {
                            val (labels, datasets, negativePools) = config.calculate()
                            messages.removeChildren()
                            for (pool in negativePools) {
                                messages.setAttributeRaw("style", "display: block;")
                                messages.new {
                                    div(fomantic.ui.warning.tiny.message).new {
                                        div(fomantic.ui.header).text(""""$pool" becomes negative in the selected timeframe""")
                                        div().text(
                                            """
                                         Although this is mathematically possible, most banks don't accept negative money.
                                    """.trimIndent()
                                        )
                                    }
                                }
                            }
                            chartHolder.setAttributeRaw("style", "height: 70vh;")
                            execute(
                                """
                                    window.scrollBy({left:0,top:window.innerHeight*0.8,behaviour:"smooth"})
                                """.trimIndent()
                            )
                            chartHolder.removeChildren().new {
                                Chart(
                                    canvas(2, 1),
                                    ChartJSConfig(
                                        type = ChartType.bar,
                                        data = ChartJSData(
                                            labels = labels,
                                            datasets = datasets
                                        ),
                                        options = JsonObject(
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
                                                        """
                                                           function (tooltip) {
        // Tooltip Element
        const tooltipElement = document.getElementsByClassName('chartjs-tooltip')[0];
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
        const position = this._chart.canvas.getBoundingClientRect();
        // Display, position, and set styles for font
        tooltipElement.style.opacity = '1';
        tooltipElement.style.left = tooltip.x + 'px';
        tooltipElement.style.top = tooltip.y + 'px';
        tooltipElement.style.fontFamily = tooltip._fontFamily;
        tooltipElement.style.fontSize = tooltip.fontSize;
        tooltipElement.style.fontStyle = tooltip._fontStyle;
        tooltipElement.style.padding = tooltip.yPadding + 'px ' + tooltip.xPadding + 'px';
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
                                                                "callback" to JSFunction(
                                                                    """
                                                                        function(value, index, values) { return new Intl.NumberFormat('en-US', { style: 'currency', currency: "USD" }).format(value) }
                                                                    """.trimIndent()
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
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Config(
    val kStartDate: KVar<String>,
    val kEndDate: KVar<String>,
    val kDuration: KVar<String>,
    val kFrequency: KVar<String>,
    val kPools: KVar<List<PoolConfig>>
) {
    companion object {
        fun newConfig(): Config {
            val dateToday = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val config = Config(
                KVar(dateToday),
                KVar(""),
                KVar("10"),
                KVar("1A"),
                KVar(listOf())
            )
            config.addPool()
            return config
        }
    }

    private fun bindInput(input: ValueElement, prop: KVar<String>) {
        input.value = prop
    }

    fun addPool() {
        val mutablePools = kPools.value.toMutableList()
        mutablePools.add(PoolConfig.newPool())
        kPools.value = mutablePools
    }

    fun removePool(poolConfig: PoolConfig) {
        val mutablePools = kPools.value.toMutableList()
        mutablePools.remove(poolConfig)
        kPools.value = mutablePools
    }

    fun calculate(): Triple<List<String>, List<DataSetJS>, List<String>> {
        val startDate = LocalDate.parse(kStartDate.value, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endDate = startDate.plusYears(kDuration.value.toLong())
        val dateRange = (startDate..endDate).toList().toTypedArray()
        val sampledDates = (startDate..endDate step DateStep.fromString(
            kFrequency.value
        )).toList().toTypedArray()
        val sampledDateIndices = sampledDates.map { date ->
            dateRange.indexOf(date)
        }
        val pools = kPools.value
        val colours = colourPalette().take(pools.size * 2).toList()
        val negativeTotalPools = mutableListOf<String>()
        val report = pools.withIndex().flatMap { (index, pool) ->
            val name = pool.kName.value
            val (contributions, interest) = pool.calculate(dateRange)
            val total = contributions + interest
            if (min(total) < 0) {
                negativeTotalPools.add(name)
            }
            val sampledContributions = sampledDateIndices.map { dateIndex -> contributions[dateIndex] }
            val sampledInterest = sampledDateIndices.map { dateIndex -> interest[dateIndex] }

            listOf(
                DataSetJS(
                    label = "$name input",
                    dataList = DataList.Numbers(*sampledContributions.toTypedArray()),
                    backgroundColor = colours[2 * index].toCssRgb()
                ), DataSetJS(
                    label = "$name interest",
                    dataList = DataList.Numbers(*sampledInterest.toTypedArray()),
                    //fill = "-1",
                    backgroundColor = colours[2 * index + 1].toCssRgb()
                )
            )
        }
        return Triple(
            sampledDates.map { date -> date.format(DateTimeFormatter.ofPattern("MM-yyyy")) },
            report,
            negativeTotalPools
        )
    }

    fun form(elementCreator: ElementCreator<*>) = elementCreator.div(fomantic.ui).new() {
        div(fomantic.field.ui.raised.segment).new {
            label().text("Report")
            div(fomantic.three.fields).new {
                div(fomantic.field).new {
                    label().text("Start date")
                    bindInput(input(type = InputType.date, placeholder = "Start date", kvarUpdateEvent = "change"), kStartDate)
                }
                div(fomantic.field).new {
                    label().text("Duration")
                    div(fomantic.ui.right.labeled.input).new {
                        bindInput(input(type = InputType.number, placeholder = "Duration", kvarUpdateEvent = "change"), kDuration)
                        div(fomantic.ui.label).text("Years")
                    }
                }
                div(fomantic.field).new {
                    label().text("Frequency")
                    bindInput(select(
                        listOf(
                            Pair("1M", "Monthly"),
                            Pair("1Q", "Quarterly"),
                            Pair("2Q", "Biannually"),
                            Pair("1A", "Annually")
                        ),
                        attributes = fomantic.ui.dropdown.fluid
                    ), kFrequency)
                }
            }
        }
        render(kPools) { pools ->
            for (pool in pools) {
                div(fomantic.ui.raised.segment).new {
                    if (pools.size > 1) {
                        div(fomantic.ui.top.attached.label.button).apply {
                            on.click {
                                removePool(pool)
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
                    addPool()
                }
            }
        }
    }
}
data class PoolConfig(
    val kName: KVar<String>,
    val kGrowthRate: KVar<String>,
    val kCompoundFrequency: KVar<String>,
    val kStartVolume: KVar<String>,
    val kStreams: KVar<List<StreamConfig>>
) {
    companion object {
        fun newPool(): PoolConfig {
            return PoolConfig(
                KVar(""),
                KVar(""),
                KVar("1D"),
                KVar(""),
                KVar(listOf())
            )
        }
    }
    private fun bindInput(input: ValueElement, prop: KVar<String>) {
        input.value = prop
    }
    fun addStream() {
        val mutableStreams = kStreams.value.toMutableList()
        mutableStreams.add(StreamConfig.newStream())
        kStreams.value = mutableStreams
    }

    fun removeStream(stream: StreamConfig) {
        val mutableStreams = kStreams.value.toMutableList()
        mutableStreams.remove(stream)
        kStreams.value = mutableStreams
    }

    fun calculate(dates: Array<LocalDate>): Pair<Matrix<Double>, Matrix<Double>> {
        val startVolume = kStartVolume.value.toDouble()
        var streamContributions = zeros(1, dates.size)
        kStreams.value.forEach { streamConfig ->
            streamContributions += streamConfig.contribution(dates)
        }
        val poolContributions = streamContributions.mapIndexed { row: Int, col: Int, ele: Double -> if (col == 0) ele + startVolume else ele }

        val compoundFrequency =
            DateStep.fromString("${kCompoundFrequency.value}E")
        val compoundDates = (dates.first()..dates.last() step compoundFrequency).toList().toTypedArray()
        val growthRate = kGrowthRate.value.toDouble() * 0.01 / compoundFrequency.frequencyPerYear()
        val poolGrowthFactors = dates.map { date ->
            if(date in compoundDates) {
                1.0 + growthRate
            } else {
                1.0
            }
        }
        val poolTotal = create(((poolContributions.toTypedArray() zip poolGrowthFactors).fold(mutableListOf<Double>()) { acc, (contribution, growth) ->
            if(acc.isEmpty()) acc.add(contribution * growth) else acc.add((acc.last() + contribution) * growth)
            acc
        }).toDoubleArray())
        val poolContributionsCumulative = poolContributions.cumSum()
        val poolInterest = poolTotal - poolContributionsCumulative
        return Pair(round(poolContributionsCumulative), round(poolInterest))
    }

    fun form(elementCreator: ElementCreator<*>) = elementCreator.div().new {
        div(fomantic.three.fields).new {
            div(fomantic.field).new {
                label().text("Pool name")
                bindInput(input(type = InputType.text, placeholder = "Pool name", kvarUpdateEvent = "change"), kName)
            }
            div(fomantic.field).new {
                label().text("Initial deposit")
                div(fomantic.ui.left.labeled.input).new {
                    div(fomantic.ui.label).text("$")
                    bindInput(input(type = InputType.number, placeholder = "Initial deposit", kvarUpdateEvent = "change"), kStartVolume)
                }
            }
            div(fomantic.field).new {
                label().text("Interest/growth rate")
                div(fomantic.ui.right.labeled.input).new {
                    bindInput(input(type = InputType.number, placeholder = "Interest/growth rate", kvarUpdateEvent = "change"), kGrowthRate)
                    div(fomantic.ui.label).text("% per year")
                }
            }
            div(fomantic.field).new {
                label().text("Growth frequency")
                bindInput(select(
                    listOf(Pair("1D", "Daily"),
                        Pair("7D", "Weekly"),
                        Pair("14D", "Fortnightly"),
                        Pair("1M", "Monthly"),
                        Pair("1Q", "Quarterly"),
                        Pair("2Q", "Biannually"),
                        Pair("1A", "Annually")
                    ),
                    attributes = fomantic.ui.dropdown.fluid
                ), kCompoundFrequency)
            }
        }
        render(kStreams) { streams ->
            for (stream in streams) {
                div(fomantic.ui.raised.segment).new {
                    div(fomantic.ui.top.attached.label.button).apply {
                        on.click {
                            removeStream(stream)
                        }
                    }.new {
                        i(fomantic.icon.trash)
                        span().text("Remove stream")
                    }
                    stream.form(this)
                }
            }
            button(fomantic.ui.button).text("Add a stream").apply {
                on.click {
                    addStream()
                }
            }
        }
    }
}
data class StreamConfig(
    val kName: KVar<String>,
    val kDirection: KVar<String>,
    val kFlowFrequency: KVar<String>,
    val kBaseFlow: KVar<String>,
    val kGrowthStrategy: KVar<String>,
    val kGrowth: KVar<String>,
    val kGrowthFrequency: KVar<String>,
    val kStartDate: KVar<String>,
    val kEndAfter: KVar<String>,
    val kRunsForever: KVar<String>
) {
    companion object {
        fun newStream(): StreamConfig {
            return StreamConfig(
                KVar(""),
                KVar("1"),
                KVar("1M"),
                KVar(""),
                KVar("none"),
                KVar(""),
                KVar("1"),
                KVar(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))),
                KVar(""),
                KVar("indefinite")
            )
        }
    }

    private fun bindInput(input: ValueElement, prop: KVar<String>) {
        input.value = prop
    }
    val grows = kGrowthStrategy.map { strategy ->
        strategy != "none"
    }
    val growthType = kGrowthStrategy.map { strategy ->
        when(strategy) {
            "fixed" -> "$"
            "percentage" -> "%"
            else -> ""
        }
    }
    val flowFrequencyString = kFlowFrequency.map { freq ->
        when(freq) {
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
    val flowFrequencyPlural = flowFrequencyString.map { freq -> "${freq}s"}

    fun contribution(dates: Array<LocalDate>): Matrix<Double> {
        val startDate = LocalDate.parse(kStartDate.value, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endDate = if(kRunsForever.value == "indefinite") {
            dates.last()
        } else {
            when(kFlowFrequency.value) {
                "1D" -> startDate.plusDays(kEndAfter.value.toLong())
                "7D" -> startDate.plusDays(7*kEndAfter.value.toLong())
                "14D" -> startDate.plusDays(14*kEndAfter.value.toLong())
                "1M" -> startDate.plusMonths(kEndAfter.value.toLong())
                "1Q" -> startDate.plusMonths(3*kEndAfter.value.toLong())
                "2Q" -> startDate.plusMonths(6*kEndAfter.value.toLong())
                "1A" -> startDate.plusYears(kEndAfter.value.toLong())
                else -> startDate
            }.minusDays(1)
        }
        val flowFrequency = kFlowFrequency.value
        val flowDates = (startDate..endDate step DateStep.fromString(
            "${flowFrequency}S"
        )).toList().toTypedArray()
        val direction = kDirection.value.toInt()
        val baseFlow = kBaseFlow.value.toDouble()
        val growth = kGrowth.value.let {
            if(it.isEmpty()) 0.0 else it.toDouble()
        }
        val growthFrequency = kGrowthFrequency.value.toInt()
        val growthStrategy = when(kGrowthStrategy.value) {
            "fixed" -> { period: Int -> (baseFlow + (period/growthFrequency) * growth) * direction }
            "percentage" -> { period: Int -> (baseFlow * (1 + growth * 0.01).pow(period/growthFrequency)) * direction }
            else -> { period: Int -> baseFlow * direction }
        }
        val flowArray = dates.map { date ->
            val index = flowDates.indexOf(date)
            if(index > -1) {
                growthStrategy(index)
            } else {
                0.0
            }
        }
        return create(arrayOf(flowArray.toDoubleArray()))
    }
    fun form(elementCreator: ElementCreator<*>) = elementCreator.div().new {
        div(fomantic.three.fields).new {
            div(fomantic.field).new {
                label().text("Stream name")
                bindInput(input(type = InputType.text, placeholder = "Stream name", kvarUpdateEvent = "change"), kName)
            }
            div(fomantic.field).new {
                label().text("Flow direction")
                bindInput(select(
                    listOf(Pair("1", "Incoming"), Pair("-1", "Outgoing")),
                    attributes = fomantic.ui.dropdown
                ), kDirection)
            }
            div(fomantic.field).new {
                label().text("Flow frequency")
                bindInput(select(
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
                ), kFlowFrequency)
            }
        }
        div(fomantic.three.fields).new {
            div(fomantic.field).new {
                label().text("Flow amount")
                div(fomantic.ui.left.labeled.input).new {
                    div(fomantic.ui.label).text("$")
                    bindInput(input(type = InputType.number, placeholder = "Flow amount", kvarUpdateEvent = "change"), kBaseFlow)
                }
            }
            div(fomantic.field).new {
                label().text("Flow growth over time")
                bindInput(select(
                    listOf(
                        Pair("none", "None"),
                        Pair("fixed", "Fixed amount"),
                        Pair("percentage", "Percentage")
                    ),
                    attributes = fomantic.ui.dropdown
                ), kGrowthStrategy)
            }
            div(fomantic.field).new {
                render(grows) { grows ->
                    div(fomantic.field).new {
                        if (grows) {
                            label().text("Growth rate")
                            render(growthType) { type ->
                                if(type == "$") {
                                    div(fomantic.ui.left.labeled.input).new {
                                        div(fomantic.ui.label).text(type)
                                        bindInput(input(type = InputType.number, placeholder = "Growth rate", kvarUpdateEvent = "change"), kGrowth)
                                    }
                                } else {
                                    div(fomantic.ui.right.labeled.input).new {
                                        bindInput(input(type = InputType.number, placeholder = "Growth rate", kvarUpdateEvent = "change"), kGrowth)
                                        div(fomantic.ui.label).text(type)
                                    }
                                }
                            }
                        }
                    }
                }}
            div(fomantic.field).new {
                render(grows) { grows ->
                    div(fomantic.field).new {
                        if (grows) {
                            label().text("Growth frequency")
                            bindInput(input(type = InputType.number, placeholder = "Growth frequency", kvarUpdateEvent = "change"), kGrowthFrequency)
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
                                            RuleBasedNumberFormat(Locale.ENGLISH, RuleBasedNumberFormat.ORDINAL)
                                        val frequencyNumber = growthFrequency.toInt()
                                        val frequencyOrdinal = if (frequencyNumber > 1) {
                                            "${formatter.format(frequencyNumber)} $frequency"
                                        } else {
                                            frequency
                                        }
                                        render(growthType) { type ->
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

        div(fomantic.two.fields).new {
            div(fomantic.field).new {
                label().text("Start date")
                bindInput(input(type = InputType.date, placeholder = "Start date", kvarUpdateEvent = "change"), kStartDate)
            }
            div(fomantic.field).new {
                label().text("Flow forever?")
                bindInput(select(
                    listOf(
                        Pair("indefinite", "Stream runs forever"),
                        Pair("definite", "Stream has an end")
                    ),
                    attributes = fomantic.ui.dropdown
                ), kRunsForever)
            }
            div(fomantic.field).new {
                render(kRunsForever) {
                    if (it == "definite") {
                        div(fomantic.field).new {
                            label().text("Run for")
                            div(fomantic.ui.right.labeled.input).new {
                                bindInput(input(type = InputType.number, kvarUpdateEvent = "change"), kEndAfter)
                                div(fomantic.ui.label).text(flowFrequencyPlural)
                            }
                        }
                    }
                }
            }
        }
    }
}
