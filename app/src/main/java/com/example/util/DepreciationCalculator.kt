package com.example.util

import com.example.model.AssetEntity
import com.example.model.AssetType
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

data class DepreciationResult(
    val acquisitionCost: Double,
    val salvageValue: Double,
    val depreciableBase: Double,
    val usefulLifeYears: Int,
    val elapsedMonths: Int,
    val remainingMonths: Int,
    val annualDepreciation: Double,
    val monthlyDepreciation: Double,
    val accumulatedDepreciation: Double,
    val currentBookValue: Double,
    val depreciationPercentage: Float // 0.0f to 1.0f
)

data class YearlyDepreciationSchedule(
    val yearNumber: Int,
    val calendarYear: Int,
    val startingBookValue: Double,
    val annualDepreciation: Double,
    val accumulatedDepreciation: Double,
    val endingBookValue: Double
)

object DepreciationCalculator {

    fun calculate(
        asset: AssetEntity,
        asOfTimestamp: Long = System.currentTimeMillis()
    ): DepreciationResult {
        val cost = asset.acquisitionCost
        val salvage = asset.salvageValue.coerceAtLeast(0.0)
        val usefulYears = asset.usefulLifeYears

        // Land (Tanah) does not depreciate under Indonesian PSAK / standard accounting
        if (asset.type == AssetType.TANAH || usefulYears <= 0) {
            return DepreciationResult(
                acquisitionCost = cost,
                salvageValue = cost,
                depreciableBase = 0.0,
                usefulLifeYears = 0,
                elapsedMonths = 0,
                remainingMonths = 0,
                annualDepreciation = 0.0,
                monthlyDepreciation = 0.0,
                accumulatedDepreciation = 0.0,
                currentBookValue = cost,
                depreciationPercentage = 0.0f
            )
        }

        val depreciableBase = max(0.0, cost - salvage)
        val annualDepreciation = depreciableBase / usefulYears.toDouble()
        val monthlyDepreciation = annualDepreciation / 12.0

        val totalUsefulMonths = usefulYears * 12
        val elapsedMonths = getElapsedMonths(asset.acquisitionDate, asOfTimestamp)
        val boundedElapsedMonths = elapsedMonths.coerceIn(0, totalUsefulMonths)
        val remainingMonths = max(0, totalUsefulMonths - boundedElapsedMonths)

        val accumulated = (monthlyDepreciation * boundedElapsedMonths).coerceIn(0.0, depreciableBase)
        val currentBookValue = max(salvage, cost - accumulated)
        val ratio = if (depreciableBase > 0) (accumulated / depreciableBase).toFloat().coerceIn(0f, 1f) else 0f

        return DepreciationResult(
            acquisitionCost = cost,
            salvageValue = salvage,
            depreciableBase = depreciableBase,
            usefulLifeYears = usefulYears,
            elapsedMonths = boundedElapsedMonths,
            remainingMonths = remainingMonths,
            annualDepreciation = annualDepreciation,
            monthlyDepreciation = monthlyDepreciation,
            accumulatedDepreciation = accumulated,
            currentBookValue = currentBookValue,
            depreciationPercentage = ratio
        )
    }

    fun generateYearlySchedule(
        asset: AssetEntity
    ): List<YearlyDepreciationSchedule> {
        val result = calculate(asset)
        if (asset.usefulLifeYears <= 0 || asset.type == AssetType.TANAH) {
            return emptyList()
        }

        val startCal = Calendar.getInstance().apply {
            timeInMillis = asset.acquisitionDate
        }
        val startYear = startCal.get(Calendar.YEAR)

        val list = mutableListOf<YearlyDepreciationSchedule>()
        var currentStartValue = asset.acquisitionCost
        var accumulated = 0.0

        for (i in 1..asset.usefulLifeYears) {
            val annual = if (i == asset.usefulLifeYears) {
                // Adjust for residual rounding in the final year
                currentStartValue - asset.salvageValue
            } else {
                result.annualDepreciation
            }
            accumulated += annual
            val endingValue = (asset.acquisitionCost - accumulated).coerceAtLeast(asset.salvageValue)

            list.add(
                YearlyDepreciationSchedule(
                    yearNumber = i,
                    calendarYear = startYear + (i - 1),
                    startingBookValue = currentStartValue,
                    annualDepreciation = annual,
                    accumulatedDepreciation = accumulated,
                    endingBookValue = endingValue
                )
            )
            currentStartValue = endingValue
        }

        return list
    }

    private fun getElapsedMonths(startDate: Long, endDate: Long): Int {
        if (endDate <= startDate) return 0
        val start = Calendar.getInstance().apply { timeInMillis = startDate }
        val end = Calendar.getInstance().apply { timeInMillis = endDate }

        val diffYears = end.get(Calendar.YEAR) - start.get(Calendar.YEAR)
        val diffMonths = end.get(Calendar.MONTH) - start.get(Calendar.MONTH)
        val dayDiff = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH)

        var totalMonths = diffYears * 12 + diffMonths
        if (dayDiff > 15) {
            totalMonths += 1
        }
        return max(0, totalMonths)
    }
}
