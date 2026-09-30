package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AssetEntity
import com.example.model.AssetType
import com.example.model.TaxStatus
import com.example.model.VehicleType
import com.example.util.DepreciationCalculator
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Asset Manajemen", appName)
    }

    @Test
    fun `land does not depreciate`() {
        val land = AssetEntity(
            name = "Tanah Kawasan Industri",
            code = "AST-TNH-001",
            type = AssetType.TANAH,
            acquisitionCost = 1_000_000_000.0,
            acquisitionDate = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 365 * 2,
            usefulLifeYears = 0,
            location = "Karawang",
            pic = "Legal"
        )
        val result = DepreciationCalculator.calculate(land)
        assertEquals(0.0, result.accumulatedDepreciation, 0.001)
        assertEquals(1_000_000_000.0, result.currentBookValue, 0.001)
    }

    @Test
    fun `vehicle straight line depreciation calculates correctly`() {
        val oneYearAgo = Calendar.getInstance().apply {
            add(Calendar.YEAR, -1)
        }.timeInMillis

        val car = AssetEntity(
            name = "Toyota Innova",
            code = "AST-KND-001",
            type = AssetType.KENDARAAN,
            vehicleType = VehicleType.MOBIL,
            acquisitionCost = 500_000_000.0,
            acquisitionDate = oneYearAgo,
            usefulLifeYears = 5,
            salvageValue = 100_000_000.0, // Depreciable base = 400M -> 80M per year
            location = "Jakarta",
            pic = "Driver"
        )
        val result = DepreciationCalculator.calculate(car)
        assertEquals(80_000_000.0, result.annualDepreciation, 1000.0)
        assertTrue(result.currentBookValue < 500_000_000.0)
        assertTrue(result.currentBookValue >= 100_000_000.0)
    }

    @Test
    fun `tax status urgency detection`() {
        val now = System.currentTimeMillis()
        val expiredDate = now - (5L * 24 * 60 * 60 * 1000)
        val criticalDate = now + (10L * 24 * 60 * 60 * 1000)
        val safeDate = now + (120L * 24 * 60 * 60 * 1000)

        assertEquals(TaxStatus.EXPIRED, FormatUtils.getTaxStatus(expiredDate))
        assertEquals(TaxStatus.CRITICAL, FormatUtils.getTaxStatus(criticalDate))
        assertEquals(TaxStatus.SAFE, FormatUtils.getTaxStatus(safeDate))
    }
}
