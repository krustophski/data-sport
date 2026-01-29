package com.krustophski.data.sport

import com.krustophski.data.sport.util.Centimeter
import com.krustophski.data.sport.util.Day
import com.krustophski.data.sport.util.Foot
import com.krustophski.data.sport.util.Hour
import com.krustophski.data.sport.util.Inch
import com.krustophski.data.sport.util.Kilo
import com.krustophski.data.sport.util.Kilogram
import com.krustophski.data.sport.util.Kilometer
import com.krustophski.data.sport.util.KilometersPerHour
import com.krustophski.data.sport.util.Length
import com.krustophski.data.sport.util.Mass
import com.krustophski.data.sport.util.Meter
import com.krustophski.data.sport.util.MetersPerSecond
import com.krustophski.data.sport.util.Mile
import com.krustophski.data.sport.util.MilesPerHour
import com.krustophski.data.sport.util.Minute
import com.krustophski.data.sport.util.Pound
import com.krustophski.data.sport.util.Quantity
import com.krustophski.data.sport.util.Second
import com.krustophski.data.sport.util.Time
import com.krustophski.data.sport.util.Week
import com.krustophski.data.sport.util.Yard
import com.krustophski.data.sport.util.Year
import org.junit.Assert
import org.junit.Test
import com.krustophski.data.sport.util.Unit as Units

class UnitsTest {
    @Test
    fun validObjects() {
        Assert.assertEquals(Meter, Mile.baseUnit)
        Assert.assertEquals(Meter, Meter.baseUnit)
        Assert.assertEquals(Second, Year.baseUnit)
        Assert.assertEquals(Second, Minute.baseUnit)
        Assert.assertEquals(Second, Hour.baseUnit)
    }

    @Test
    fun convertFeetToInches() {
        val testQuantity = Quantity(3.0, Foot)
        Assert.assertEquals(36.0, testQuantity.convertTo(Inch).value, 1e-14)
    }

    @Test
    fun convertFeetToMiles() {
        val testQuantity = Quantity(5280.0, Foot)
        Assert.assertEquals(1.0, testQuantity.convertTo(Mile).value, 0.0)
    }

    @Test
    fun convertFeetToMeters() {
        val testQuantity = Quantity(3.0, Foot)
        Assert.assertEquals(0.9144, testQuantity.convertTo(Meter).value, 1e-6)
    }

    @Test
    fun convertYardToMeters() {
        val testQuantity = Quantity(1.0, Yard)
        Assert.assertEquals(0.9144, testQuantity.convertTo(Meter).value, 1e-6)
    }

    @Test
    fun convertMileToMeters() {
        val testQuantity = Quantity(1.0, Mile)
        Assert.assertEquals(1609.344, testQuantity.convertTo(Meter).value, 1e-6)
    }

    @Test
    fun convertMetersToMiles() {
        val testQuantity = Quantity(1609.344, Meter)
        Assert.assertEquals(1.0, testQuantity.convertTo(Mile).value, 1e-6)
    }

    @Test
    fun metersToKilometers() {
        Assert.assertEquals(2.0, Quantity(2000.0, Meter).convertTo(Kilo(Meter)).value, 1e-6)
    }

    @Test
    fun kilometersToMeters() {
        Assert.assertEquals(1000.0, Quantity(1.0, Kilo(Meter)).convertTo(Meter).value, 1e-6)
    }

    @Test
    fun inchesToCentimeters() {
        Assert.assertEquals(11.81102362, Quantity(30.0, Centimeter).convertTo(Inch).value, 1e-6)
    }

    @Test
    fun convertSecondsToMinutes() {
        Assert.assertEquals(1.0, Quantity(60.0, Second).convertTo(Minute).value, 1e-6)
    }

    @Test
    fun convertSecondsToHours() {
        Assert.assertEquals(1.0, Quantity(3600.0, Second).convertTo(Hour).value, 1e-6)
    }

    @Test
    fun convertHoursToSeconds() {
        Assert.assertEquals(3600.0, Quantity(1.0, Hour).convertTo(Second).value, 1e-6)
    }

    @Test
    fun addQuantities() {
        Assert.assertEquals(
            Quantity(2.0, Mile).value,
            (Quantity(1.0, Mile) + Quantity(1.0, Mile)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(24.0, Inch).value,
            (Quantity(12.0, Inch) + Quantity(1.0, Foot)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(61.0, Second).value,
            (Quantity(1.0, Second) + Quantity(1.0, Minute)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(3218.688, Mile).value,
            (Quantity(1609.344, Meter) + Quantity(1.0, Mile)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(2.0, Mile).value,
            (Quantity(1.0, Mile) + Quantity(1609.344, Meter)).value, 1e-6
        )
        Assert.assertThrows(Exception::class.java) {
            Quantity(1.0, Meter) + Quantity(1.0, Second)
        }
    }

    @Test
    fun subtractQuantities() {
        Assert.assertEquals(
            Quantity(0.0, Mile).value,
            (Quantity(1.0, Mile) - Quantity(1.0, Mile)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(0.0, Inch).value,
            (Quantity(12.0, Inch) - Quantity(1.0, Foot)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(-59.0, Second).value,
            (Quantity(1.0, Second) - Quantity(1.0, Minute)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(0.0, Mile).value,
            (Quantity(1609.344, Meter) - Quantity(1.0, Mile)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(0.0, Mile).value,
            (Quantity(1.0, Mile) - Quantity(1609.344, Meter)).value, 1e-6
        )
        Assert.assertThrows(Exception::class.java) {
            Quantity(1.0, Meter) - Quantity(1.0, Second)
        }
    }

    @Test
    fun multiplyQuantities() {
        Assert.assertEquals(
            Quantity(1.0, Mile).value,
            (Quantity(1.0, Mile) * Quantity(1.0, Mile)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(144.0, Inch).value,
            (Quantity(12.0, Inch) * Quantity(1.0, Foot)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(60.0, Second).value,
            (Quantity(1.0, Second) * Quantity(1.0, Minute)).value, 1e-6
        )
        Assert.assertEquals(
            1609.344 * 1609.344,
            (Quantity(1609.344, Meter) * Quantity(1.0, Mile)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(1.0, Mile).value,
            (Quantity(1.0, Mile) * Quantity(1609.344, Meter)).value, 1e-6
        )
        Assert.assertThrows(Exception::class.java) {
            Quantity(1.0, Meter) * Quantity(1.0, Second)
        }
    }

    @Test
    fun divideQuantities() {
        Assert.assertEquals(
            Quantity(1.0, Mile).value,
            (Quantity(1.0, Mile) / Quantity(1.0, Mile)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(1.0, Inch).value,
            (Quantity(12.0, Inch) / Quantity(1.0, Foot)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(1 / 60.0, Second).value,
            (Quantity(1.0, Second) / Quantity(1.0, Minute)).value, 1e-6
        )
        Assert.assertEquals(
            1.0,
            (Quantity(1609.344, Meter) / Quantity(1.0, Mile)).value, 1e-6
        )
        Assert.assertEquals(
            Quantity(1.0, Mile).value,
            (Quantity(1.0, Mile) / Quantity(1609.344, Meter)).value, 1e-6
        )
        Assert.assertThrows(Exception::class.java) {
            Quantity(1.0, Meter) / Quantity(1.0, Second)
        }
    }

    @Test
    fun lengthInequalities() {
        Assert.assertEquals(true, Quantity(1.0, Mile) > Quantity(1.0, Meter))
        Assert.assertEquals(true, Quantity(1.0, Meter) < Quantity(1.0, Mile))
        Assert.assertEquals(true, Quantity(1.0, Meter) != Quantity(1.0, Mile))
        Assert.assertEquals(true, Quantity(1.0, Mile) == Quantity(1.0, Mile))
        Assert.assertEquals(true, Quantity(1.0, Foot) == Quantity(12.0, Inch))
        Assert.assertEquals(true, Quantity(1.0, Foot) >= Quantity(12.0, Inch))
        Assert.assertEquals(true, Quantity(1.0, Foot) <= Quantity(12.0, Inch))
        Assert.assertEquals(true, Quantity(1.0, Foot) >= Quantity(11.9, Inch))
        Assert.assertEquals(true, Quantity(1.0, Foot) <= Quantity(12.1, Inch))

        Assert.assertEquals(false, Quantity(1.0, Mile) < Quantity(1.0, Meter))
        Assert.assertEquals(false, Quantity(1.0, Meter) > Quantity(1.0, Mile))
        Assert.assertEquals(false, Quantity(1.0, Meter) == Quantity(1.0, Mile))
        Assert.assertEquals(false, Quantity(1.0, Mile) != Quantity(1.0, Mile))
        Assert.assertEquals(false, Quantity(1.0, Foot) != Quantity(12.0, Inch))
        Assert.assertEquals(false, Quantity(1.0, Foot) <= Quantity(11.9, Inch))
        Assert.assertEquals(false, Quantity(1.0, Foot) >= Quantity(12.1, Inch))
    }

    @Test
    fun timeLessThan() {
        Assert.assertEquals(
            true,
            Quantity(60.0, Second) < Quantity(2.0, Minute)
        )
        Assert.assertEquals(
            true,
            Quantity(60.0, Minute) < Quantity(2.0, Hour)
        )
        Assert.assertEquals(
            true,
            Quantity(6.0, Hour) < Quantity(2.0, Day)
        )
        Assert.assertEquals(
            true,
            Quantity(6.0, Day) < Quantity(1.0, Week)
        )
        Assert.assertEquals(
            true,
            Quantity(6.0, Week) < Quantity(1.0, Year)
        )
    }

    @Test
    fun timeGreaterThan() {
        Assert.assertEquals(
            true,
            Quantity(140.0, Second) > Quantity(2.0, Minute)
        )
        Assert.assertEquals(
            true,
            Quantity(140.0, Minute) > Quantity(2.0, Hour)
        )
        Assert.assertEquals(
            true,
            Quantity(72.0, Hour) > Quantity(2.0, Day)
        )
        Assert.assertEquals(
            true,
            Quantity(8.0, Day) > Quantity(1.0, Week)
        )
        Assert.assertEquals(
            true,
            Quantity(60.0, Week) > Quantity(1.0, Year)
        )
    }

    @Test
    fun timeEquality() {
        Assert.assertEquals(
            true,
            Quantity(120.0, Second) == Quantity(2.0, Minute)
        )
        Assert.assertEquals(
            true,
            Quantity(120.0, Minute) == Quantity(2.0, Hour)
        )
        Assert.assertEquals(
            true,
            Quantity(48.0, Hour) == Quantity(2.0, Day)
        )
        Assert.assertEquals(
            true,
            Quantity(7.0, Day) == Quantity(1.0, Week)
        )
        Assert.assertEquals(
            true,
            Quantity(52.17857142857143, Week) == Quantity(1.0, Year)
        )
    }

    @Test
    fun weekToYear() {
        Assert.assertEquals(52.17857142857143, Quantity(1.0, Year).convertTo(Week).value, 1e-12)
    }

    @Test
    fun mphToMetersPerSecond() {
        Assert.assertEquals(
            8.9408,
            Quantity(20.0, MilesPerHour).convertTo(MetersPerSecond).value,
            1e-6
        )
    }

    @Test
    fun metersPerSecondToMph() {
        Assert.assertEquals(
            20.0,
            Quantity(8.9408, MetersPerSecond).convertTo(MilesPerHour).value,
            1e-6
        )
    }

    @Test
    fun kphToMetersPerSecond() {
        Assert.assertEquals(
            5.5555556,
            Quantity(20.0, KilometersPerHour).convertTo(MetersPerSecond).value,
            1e-6
        )
    }

    @Test
    fun metersPerSecondToKph() {
        Assert.assertEquals(
            20.0,
            Quantity(5.5555556, MetersPerSecond).convertTo(KilometersPerHour).value,
            1e-6
        )
    }

    @Test
    fun unitsFromString() {
        Assert.assertEquals(null, Units.fromString(""))
        Assert.assertEquals(Mile, Units.fromString("miles"))
        Assert.assertEquals(Mile, Units.fromString("mile"))
        Assert.assertEquals(Mile, Units.fromString("mi"))
        Assert.assertNotEquals(Mile, Units.fromString("m"))
        Assert.assertEquals(Meter, Units.fromString("m"))
        Assert.assertEquals(Kilometer, Units.fromString("km"))
        Assert.assertEquals(Kilometer, Units.fromString("kilometer"))
        Assert.assertEquals(Kilometer, Units.fromString("kilometers"))
        Assert.assertEquals(Inch, Units.fromString("in"))
        Assert.assertEquals(Foot, Units.fromString("ft"))
        Assert.assertNotEquals(Foot, Units.fromString("f"))
        Assert.assertEquals(MetersPerSecond, Units.fromString("m/s"))
        Assert.assertEquals(Kilogram, Units.fromString("kg"))
        Assert.assertEquals(Pound, Units.fromString("pound"))
        Assert.assertEquals(Pound, Units.fromString("pounds"))
        Assert.assertEquals(Pound, Units.fromString("lb"))
    }

    @Test
    fun unitsFromMeasurementSystem() {
        Assert.assertEquals(Mile, Length.fromMeasurementSystem("1"))
        Assert.assertEquals(Kilometer, Length.fromMeasurementSystem("2"))
        Assert.assertEquals(Meter, Length.fromMeasurementSystem("0"))
    }

    @Suppress("USELESS_IS_CHECK")
    @Test
    fun unitTypeChecks() {
        Assert.assertEquals(Mile, Mile)
        Assert.assertTrue(Mile is Length)
        Assert.assertTrue(Inch is Length)
        Assert.assertTrue(Kilo(Meter).baseUnit is Length)
        Assert.assertTrue(Kilometer.baseUnit is Length)
        Assert.assertTrue(Kilo(Meter).baseUnit is Meter)
        Assert.assertFalse(Kilo(Meter).baseUnit is Time)
        Assert.assertTrue(Pound is Mass)
        Assert.assertTrue(Pound.baseUnit is Mass)
    }
}
