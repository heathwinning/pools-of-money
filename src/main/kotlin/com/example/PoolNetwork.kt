package com.example

import java.time.LocalDate
import kotlin.math.pow

fun main() {
    val externalPool = Pool("external")
    val bankPool = Pool("bank", 1000.0)
    val shares = Pool("shares", 5000.0)

    val today = LocalDate.now()
    val endDate = today.plusYears(5)

    val bankInterest = PoolInterestStream(bankPool, bankPool, today..endDate step DateStep.fromString("1D"), 1.5)
    val income = ConstantStream(externalPool, bankPool, today..endDate step DateStep.fromString("2WE"), 1000.0)
    val expenses = ConstantStream(bankPool, externalPool, today..endDate step DateStep.fromString("1MS"), 1000.0)
    val investing = ConstantStream(bankPool, shares, today..endDate step DateStep.fromString("1QE"), 2000.0)
    val dividends = PercentageStream(shares, bankPool, today..endDate step DateStep.fromString("2QS"), 4.0)
    val shareGrowth = PoolInterestStream(shares, shares, today..endDate step DateStep.fromString("1A"), 4.0)

    val poolNetwork = PoolNetwork(mutableListOf(bankPool, shares), mutableListOf(income, expenses, bankInterest, investing, dividends, shareGrowth))
    poolNetwork.simulate(today..endDate)
    //poolNetwork.printVolumes()
}

data class PoolNetwork(val pools: MutableList<Pool>, val streams: MutableList<Stream>) {
    fun simulate(dateProgression: DateProgression) {
        for(date in dateProgression) {
            for(stream in streams) {
                stream.flowIfInProgression(date)
            }
            println(date.toString())
            printVolumes()
        }
    }

    fun printVolumes() {
        for(pool in pools) {
            println("${pool.name} - ${pool.volume}")
        }
    }
}

data class Pool(var name: String, var volume: Double = 0.0) {
    fun decreaseBy(amount: Double) {
        volume -= amount
    }

    fun increaseBy(amount: Double) {
        volume += amount
    }
}

abstract class Stream(val source: Pool, val target: Pool, val dateProgression: DateProgression, val sourceDecreases: Boolean = true, val targetIncreases: Boolean = true) {
    abstract fun flowAmount(step: Int): Double

    fun flow(step: Int) {
        val amount = flowAmount(step)
        if(sourceDecreases) source.decreaseBy(amount)
        if(targetIncreases) target.increaseBy(amount)
    }

    fun flowIfInProgression(date: LocalDate) {
        val step = dateProgression.indexOf(date)
        if(step > -1) {
            flow(step)
        }
    }
}

class PercentageStream(source: Pool, target: Pool, dateProgression: DateProgression, percentagePerYear: Double) : Stream(source, target, dateProgression) {
    val percentage = percentagePerYear / dateProgression.stepDays.frequencyPerYear()
    override fun flowAmount(step: Int) = source.volume * percentage / 100
}

class PoolInterestStream(source: Pool, target: Pool, dateProgression: DateProgression, percentagePerYear: Double) : Stream(source, target, dateProgression, sourceDecreases = false) {
    val percentage = percentagePerYear / dateProgression.stepDays.frequencyPerYear()
    override fun flowAmount(step: Int) = source.volume * percentage / 100
}

class ConstantStream(source: Pool, target: Pool, dateProgression: DateProgression, val amount: Double) : Stream(source, target, dateProgression) {
    override fun flowAmount(step: Int) = amount
}

class PercentageIncreaseStream(source: Pool, target: Pool, dateProgression: DateProgression, val base: Double, val percentage: Double) : Stream(source, target, dateProgression) {
    override fun flowAmount(step: Int) = base * (1 + percentage / 100).pow(step)
}

class ConstantIncreaseStream(source: Pool, target: Pool, dateProgression: DateProgression, val base: Double, val constant: Double) : Stream(source, target, dateProgression) {
    override fun flowAmount(step: Int) = base + step * constant
}