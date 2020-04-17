package com.github.heathwinning

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class DateIterator(val startDate: LocalDate,
                   val endDateInclusive: LocalDate,
                   val stepDays: DateStep
): Iterator<LocalDate> {
    private var currentDate = startDate
    override fun hasNext() = currentDate <= endDateInclusive
    override fun next(): LocalDate {
        val next = currentDate
        currentDate = when(stepDays.dateOffset) {
            DateOffset.Day -> currentDate.plusDays(stepDays.offsetModifier)
            DateOffset.Week -> currentDate.plusWeeks(stepDays.offsetModifier)
            DateOffset.Month -> currentDate.plusMonths(stepDays.offsetModifier)
            DateOffset.Quarter -> currentDate.plusMonths(3 * stepDays.offsetModifier)
            DateOffset.Year -> currentDate.plusYears(stepDays.offsetModifier)
        }
        return next
    }
}

enum class DateOffset {
    Day, Week, Month, Quarter, Year
}

enum class DateAlignment {
    NONE, START, END
}
data class DateStep(
    val dateOffset: DateOffset,
    val offsetModifier: Long = 1,
    val alignment: DateAlignment = DateAlignment.NONE
) {
    companion object {
        fun fromString(dString: String): DateStep {
            val match = "(\\d*)([D|W|M|Q|A])(S|E)?".toRegex().matchEntire(dString)
            match?.let {
                val (modifier, offset, alignment) = match.destructured
                val dateOffset = when(offset) {
                    "D" -> DateOffset.Day
                    "W" -> DateOffset.Week
                    "M" -> DateOffset.Month
                    "Q" -> DateOffset.Quarter
                    "A" -> DateOffset.Year
                    else -> throw IllegalArgumentException("Unknown date offset: $offset")
                }
                val offsetModifier = if(modifier.isNotEmpty()) modifier.toLong() else 1
                val dateAlignment = when(alignment) {
                    "S" -> DateAlignment.START
                    "E" -> DateAlignment.END
                    else -> DateAlignment.NONE
                }
                return DateStep(
                    dateOffset,
                    offsetModifier,
                    dateAlignment
                )
            }
            throw IllegalArgumentException("Cannot parse string: $dString")
        }
    }

    fun frequencyPerYear(): Double {
        return when(dateOffset) {
            DateOffset.Day -> 365.0 / offsetModifier
            DateOffset.Week -> 52.0 / offsetModifier
            DateOffset.Month -> 12.0 / offsetModifier
            DateOffset.Quarter -> 4.0 / offsetModifier
            DateOffset.Year -> 1.0 / offsetModifier
        }
    }
}

fun firstDayOfNextMonthOrSame() = TemporalAdjusters.ofDateAdjuster { date -> if(date.dayOfMonth == 1) date else date.with(TemporalAdjusters.firstDayOfNextMonth()) }
fun firstDayOfNextQuarterOrSame() = TemporalAdjusters.ofDateAdjuster { date ->
    if(date.dayOfMonth == 1){
        date
    } else {
        date.withMonth(((date.monthValue - 1)/ 3) * 3 + 1).with(TemporalAdjusters.firstDayOfMonth())
    }
}
fun firstDayOfNextYearOrSame() = TemporalAdjusters.ofDateAdjuster { date -> if(date.dayOfYear == 1) date else date.with(TemporalAdjusters.firstDayOfNextYear()) }
fun lastDayOfQuarter() = TemporalAdjusters.ofDateAdjuster { date ->
    date.withMonth(((date.monthValue - 1)/ 3) * 3 + 3).with(TemporalAdjusters.lastDayOfMonth())
}

class DateProgression(override var start: LocalDate,
                      override var endInclusive: LocalDate,
                      val stepDays: DateStep = DateStep(
                          DateOffset.Day
                      )
) : Iterable<LocalDate>, ClosedRange<LocalDate> {
    init {
        when(stepDays.alignment) {
           DateAlignment.START -> start = when(stepDays.dateOffset) {
               DateOffset.Day -> start
               DateOffset.Week -> start.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
               DateOffset.Month -> start.with(firstDayOfNextMonthOrSame())
               DateOffset.Quarter -> start.with(firstDayOfNextQuarterOrSame())
               DateOffset.Year -> start.with(firstDayOfNextYearOrSame())
           }
            DateAlignment.END -> start = when(stepDays.dateOffset) {
                DateOffset.Day -> start
                DateOffset.Week -> start.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
                DateOffset.Month -> start.with(TemporalAdjusters.lastDayOfMonth())
                DateOffset.Quarter -> start.with(lastDayOfQuarter())
                DateOffset.Year -> start.with(TemporalAdjusters.lastDayOfYear())
            }
            DateAlignment.NONE -> {}
        }
    }
    override fun iterator(): Iterator<LocalDate> =
        DateIterator(start, endInclusive, stepDays)
    infix fun step(days: DateStep) =
        DateProgression(start, endInclusive, days)
}

operator fun LocalDate.rangeTo(other: LocalDate) =
    DateProgression(this, other)
