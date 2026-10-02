package com.example.util

import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderPlannerTest {

    private val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 2, 10, 0, 0) }.timeInMillis
    private fun day(offset: Int) = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, offset) }.timeInMillis

    private fun vehicle(
        id: Long = 1,
        tax: Long? = null,
        plate5: Long? = null,
        service: Long? = null,
        status: AssetStatus = AssetStatus.DIGUNAKAN,
        type: AssetType = AssetType.KENDARAAN
    ) = AssetEntity(
        id = id, name = "Innova", code = "K$id", type = type, acquisitionCost = 1.0, acquisitionDate = now,
        usefulLifeYears = 8, location = "", pic = "", status = status, licensePlate = "B 1 X",
        annualTaxDueDate = tax, fiveYearPlateDueDate = plate5, lastServiceDate = service
    )

    @Test
    fun levelsFollowTheThresholds() {
        assertEquals(0, ReminderPlanner.levelFor(31))
        assertEquals(1, ReminderPlanner.levelFor(30))
        assertEquals(1, ReminderPlanner.levelFor(15))
        assertEquals(2, ReminderPlanner.levelFor(14))
        assertEquals(3, ReminderPlanner.levelFor(7))
        assertEquals(4, ReminderPlanner.levelFor(0))
        assertEquals(4, ReminderPlanner.levelFor(-20))
    }

    @Test
    fun daysUntilCountsCalendarDaysNotHours() {
        assertEquals(0, ReminderPlanner.daysUntil(day(0) + 3_600_000 * 8, now))
        assertEquals(1, ReminderPlanner.daysUntil(day(1), now))
        assertEquals(-1, ReminderPlanner.daysUntil(day(-1), now))
    }

    @Test
    fun onlyVehiclesDueSoonAreReported() {
        val items = ReminderPlanner.plan(
            listOf(
                vehicle(1, tax = day(10)),             // 10 hari -> level 2
                vehicle(2, tax = day(90)),             // aman
                vehicle(3, tax = day(-3)),             // terlewat -> level 4
                vehicle(4, tax = day(5), status = AssetStatus.DIHAPUSBUKUKAN), // sudah dijual
                vehicle(5, tax = day(5), type = AssetType.INVENTARIS)          // bukan kendaraan
            ),
            now
        )
        assertEquals(listOf(1L, 3L), items.map { it.assetId })
        assertEquals(2, items[0].level)
        assertEquals(4, items[1].level)
        assertTrue(items[1].message, items[1].message.contains("terlewat 3 hari"))
    }

    @Test
    fun deletedVehiclesAreSkipped() {
        val items = ReminderPlanner.plan(listOf(vehicle(1, tax = day(3)).copy(syncState = SyncState.DELETED)), now)
        assertTrue(items.isEmpty())
    }

    @Test
    fun taxPlateAndServiceAreSeparateReminders() {
        val sixMonthsAgoPlus5Days = Calendar.getInstance().apply { timeInMillis = day(5); add(Calendar.MONTH, -ReminderPlanner.SERVICE_INTERVAL_MONTHS) }.timeInMillis
        val items = ReminderPlanner.plan(listOf(vehicle(1, tax = day(20), plate5 = day(2), service = sixMonthsAgoPlus5Days)), now)

        assertEquals(setOf("tax", "plate", "service"), items.map { it.key.split("|")[1] }.toSet())
        assertEquals(3, items.single { it.key.contains("|service|") }.level) // 5 hari lagi
    }

    @Test
    fun noServiceDateMeansNoServiceReminder() {
        assertTrue(ReminderPlanner.plan(listOf(vehicle(1, service = null)), now).isEmpty())
    }

    @Test
    fun renewingChangesTheKeySoRemindersRestart() {
        val before = ReminderPlanner.plan(listOf(vehicle(1, tax = day(3))), now).single().key
        val after = ReminderPlanner.plan(listOf(vehicle(1, tax = day(10))), now).single().key
        assertFalse(before == after)
    }

    @Test
    fun notifiesOnlyWhenUrgencyRisesAndRepeatsWeeklyWhenOverdue() {
        val week = 7L * 24 * 60 * 60 * 1000
        assertTrue(ReminderPlanner.shouldNotify(0, 0, 1, now))                 // pertama kali
        assertFalse(ReminderPlanner.shouldNotify(1, now - 1000, 1, now))       // level sama, sudah diberi tahu
        assertTrue(ReminderPlanner.shouldNotify(1, now - 1000, 2, now))        // makin dekat
        assertFalse(ReminderPlanner.shouldNotify(3, now - 1000, 2, now))       // tidak mundur
        assertFalse(ReminderPlanner.shouldNotify(4, now - week + 1000, 4, now)) // terlewat, belum 7 hari
        assertTrue(ReminderPlanner.shouldNotify(4, now - week, 4, now))         // terlewat, sudah 7 hari
    }
}
