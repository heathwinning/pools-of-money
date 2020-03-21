package com.github.heathwinning

import java.time.LocalDate
import kotlin.math.pow

fun main() {
    val externalPool = Pool("external")
    val bankPool = Pool("bank", 1000.0)
    val shares = Pool("shares", 5000.0)

    val today = LocalDate.now()
    val endDate = today.plusYears(5)

    val bankInterest = PoolInterestStream(
        "bank interest",
        bankPool,
        bankPool,
        today..endDate step DateStep.fromString("1D"),
        1.5
    )
    val income = ConstantStream(
        "income",
        externalPool,
        bankPool,
        today..endDate step DateStep.fromString("2WE"),
        1000.0
    )
    val expenses = ConstantStream(
        "expenses",
        bankPool,
        externalPool,
        today..endDate step DateStep.fromString("1MS"),
        1000.0
    )
    val investing = ConstantStream(
        "investment",
        bankPool,
        shares,
        today..endDate step DateStep.fromString("1QE"),
        2000.0
    )
    val dividends = PercentageStream(
        "dividends",
        shares,
        bankPool,
        today..endDate step DateStep.fromString("2QS"),
        4.0
    )
    val shareGrowth = PoolInterestStream(
        "share growth",
        shares,
        shares,
        today..endDate step DateStep.fromString("1A"),
        4.0
    )

    val poolNetwork = PoolNetwork(
        listOf(bankPool, shares),
        listOf(income, expenses, bankInterest, investing, dividends, shareGrowth)
    )
    poolNetwork.simulate(today..endDate)
    println(bankPool.history.toList())
    println(dividends.targetHistory.toList())
}

data class PoolNetwork(val pools: List<Pool>, val streams: List<Stream>) {
    fun simulate(dateProgression: DateProgression) {
        for(pool in pools) pool.reset(dateProgression)
        for(stream in streams) stream.reset(dateProgression)

        for((index, date) in dateProgression.withIndex()) {
            for(stream in streams) {
                stream.flowIfInProgression(date)
                stream.recordFlow(index)
            }
            for(pool in pools) pool.recordVolume(index)
        }
    }

    fun streamsIn(pool: Pool): List<Stream> = streams.filter { stream -> stream.target == pool && stream.targetIncreases }

    fun streamsOut(pool: Pool): List<Stream> = streams.filter { stream -> stream.source == pool && stream.sourceDecreases }
}

data class Pool(var name: String, val startVolume: Double = 0.0) {
    var volume = startVolume
    var history = arrayOf<Double>()
    fun decreaseBy(amount: Double) {
        volume -= amount
    }

    fun increaseBy(amount: Double) {
        volume += amount
    }

    fun reset(reportingDates: DateProgression) {
        volume = startVolume
        history = reportingDates.map { 0.0 }.toTypedArray()
    }
    fun recordVolume(step: Int) {
        history[step] = volume
    }
}

abstract class Stream(val name: String, val source: Pool, val target: Pool, val dateProgression: DateProgression, val sourceDecreases: Boolean = true, val targetIncreases: Boolean = true) {
    var currentFlowAmount = 0.0
    var sourceHistory = arrayOf<Double>()
    var targetHistory = arrayOf<Double>()

    fun reset(reportingDates: DateProgression) {
        currentFlowAmount = 0.0
        sourceHistory = reportingDates.map { 0.0 }.toTypedArray()
        targetHistory = reportingDates.map { 0.0 }.toTypedArray()
    }

    fun recordFlow(step: Int) {
        val previousSourceAmount = if(step == 0) 0.0 else sourceHistory[step-1]
        sourceHistory[step] = previousSourceAmount + if(sourceDecreases) {
             currentFlowAmount
        } else {
            0.0
        }
        val previousTargetAmount = if(step == 0) 0.0 else targetHistory[step-1]
        targetHistory[step] = previousTargetAmount + if(targetIncreases) {
            currentFlowAmount
        } else {
            0.0
        }
    }

    abstract fun flowAmount(step: Int): Double

    fun flow() {
        if(sourceDecreases) {
            source.decreaseBy(currentFlowAmount)
        }
        if(targetIncreases) {
            target.increaseBy(currentFlowAmount)
        }
    }

    fun flowIfInProgression(date: LocalDate) {
        val step = dateProgression.indexOf(date)
        if(step > -1) {
            currentFlowAmount = flowAmount(step)
            flow()
        } else {
            currentFlowAmount = 0.0
        }
    }
}

class PercentageStream(name: String, source: Pool, target: Pool, dateProgression: DateProgression, percentagePerYear: Double) : Stream(name, source, target, dateProgression) {
    val percentage = percentagePerYear / dateProgression.stepDays.frequencyPerYear()
    override fun flowAmount(step: Int) = source.volume * percentage / 100
}

class PoolInterestStream(name: String, source: Pool, target: Pool, dateProgression: DateProgression, percentagePerYear: Double) : Stream(name, source, target, dateProgression, sourceDecreases = false) {
    val percentage = percentagePerYear / dateProgression.stepDays.frequencyPerYear()
    override fun flowAmount(step: Int) = source.volume * percentage / 100
}

class ConstantStream(name: String, source: Pool, target: Pool, dateProgression: DateProgression, val amount: Double) : Stream(name, source, target, dateProgression) {
    override fun flowAmount(step: Int) = amount
}

class PercentageIncreaseStream(name: String, source: Pool, target: Pool, dateProgression: DateProgression, val base: Double, val percentage: Double, val increasePeriod: Int) : Stream(name, source, target, dateProgression) {
    override fun flowAmount(step: Int) = base * (1 + percentage / 100).pow(step / increasePeriod)
}

class ConstantIncreaseStream(name: String, source: Pool, target: Pool, dateProgression: DateProgression, val base: Double, val constant: Double, val increasePeriod: Int) : Stream(name, source, target, dateProgression) {
    override fun flowAmount(step: Int) = base + step / increasePeriod * constant
}