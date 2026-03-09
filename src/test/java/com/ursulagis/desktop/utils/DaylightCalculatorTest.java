package com.ursulagis.desktop.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import javax.measure.quantity.Time;

import static org.junit.jupiter.api.Assertions.*;

class DaylightCalculatorTest {

    private static final double TOLERANCE_HOURS = 0.5;

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("accepts valid latitude range")
        void acceptsValidLatitude() {
            assertDoesNotThrow(() -> new DaylightCalculator(0));
            assertDoesNotThrow(() -> new DaylightCalculator(90));
            assertDoesNotThrow(() -> new DaylightCalculator(-90));
            assertDoesNotThrow(() -> new DaylightCalculator(45));
        }

        @Test
        @DisplayName("rejects latitude below -90")
        void rejectsLatitudeBelowMinus90() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new DaylightCalculator(-91));
            assertTrue(e.getMessage().contains("Latitude"));
        }

        @Test
        @DisplayName("rejects latitude above 90")
        void rejectsLatitudeAbove90() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new DaylightCalculator(91));
            assertTrue(e.getMessage().contains("Latitude"));
        }
    }

    @Nested
    @DisplayName("getDaylightHours (single day)")
    class SingleDayTests {

        @Test
        @DisplayName("equator has ~12h on equinox")
        void equatorEquinoxApprox12Hours() {
            DaylightCalculator calc = new DaylightCalculator(0);
            // March 21 and Sept 22 are approximate equinoxes
            double march21 = calc.getDaylightHours(LocalDate.of(2025, 3, 21));
            double sept22 = calc.getDaylightHours(LocalDate.of(2025, 9, 22));
            assertEquals(12, march21, TOLERANCE_HOURS);
            assertEquals(12, sept22, TOLERANCE_HOURS);
        }

        @Test
        @DisplayName("mid-latitude summer day longer than winter")
        void midLatitudeSummerLongerThanWinter() {
            DaylightCalculator calc = new DaylightCalculator(40); // e.g. Madrid
            double summer = calc.getDaylightHours(LocalDate.of(2025, 6, 21));
            double winter = calc.getDaylightHours(LocalDate.of(2025, 12, 21));
            assertTrue(summer > 14 && summer < 16);
            assertTrue(winter > 9 && winter < 10);
            assertTrue(summer > winter);
        }

        @Test
        @DisplayName("daylight is between 0 and 24 hours")
        void daylightInValidRange() {
            DaylightCalculator calc = new DaylightCalculator(-34); // e.g. Buenos Aires
            for (int month = 1; month <= 12; month++) {
                double hours = calc.getDaylightHours(LocalDate.of(2025, month, 15));
                assertTrue(hours >= 0 && hours <= 24, "Month " + month + " had " + hours);
            }
        }

        @Test
        @DisplayName("Southern Hemisphere: December longer than June")
        void southernHemisphereDecemberLonger() {
            DaylightCalculator calc = new DaylightCalculator(-34);
            double dec = calc.getDaylightHours(LocalDate.of(2025, 12, 21));
            double june = calc.getDaylightHours(LocalDate.of(2025, 6, 21));
            assertTrue(dec > june);
        }
    }

    @Nested
    @DisplayName("getTotalDaylightHours (date range)")
    class TotalDaylightTests {

        @Test
        @DisplayName("single day equals getDaylightHours")
        void singleDayMatchesGetDaylightHours() {
            DaylightCalculator calc = new DaylightCalculator(0);
            LocalDate date = LocalDate.of(2025, 6, 15);
            double single = calc.getDaylightHours(date);
            double total = calc.getTotalDaylightHours(date, date);
            assertEquals(single, total, 0.001);
        }

        @Test
        @DisplayName("total over range is sum of daily daylight")
        void totalIsSumOfDays() {
            DaylightCalculator calc = new DaylightCalculator(45);
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 1, 7);
            double total = calc.getTotalDaylightHours(start, end);
            double manualSum = 0;
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                manualSum += calc.getDaylightHours(d);
            }
            assertEquals(manualSum, total, 0.001);
        }

        @Test
        @DisplayName("reversed date range returns zero")
        void reversedRangeReturnsZero() {
            DaylightCalculator calc = new DaylightCalculator(0);
            double total = calc.getTotalDaylightHours(
                    LocalDate.of(2025, 1, 10),
                    LocalDate.of(2025, 1, 5));
            assertEquals(0, total, 0.001);
        }
    }

    @Nested
    @DisplayName("getDaylightPerDay")
    class DaylightPerDayTests {

        @Test
        @DisplayName("returns one entry per day in range")
        void oneEntryPerDay() {
            DaylightCalculator calc = new DaylightCalculator(0);
            LocalDate start = LocalDate.of(2025, 2, 1);
            LocalDate end = LocalDate.of(2025, 2, 28);
            List<DaylightCalculator.DaylightResult> results = calc.getDaylightPerDay(start, end);
            assertEquals(28, results.size());
            assertEquals(start, results.get(0).getDate());
            assertEquals(end, results.get(results.size() - 1).getDate());
        }

        @Test
        @DisplayName("each result has correct date and positive hours")
        void resultsHaveDateAndHours() {
            DaylightCalculator calc = new DaylightCalculator(50);
            List<DaylightCalculator.DaylightResult> results =
                    calc.getDaylightPerDay(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3));
            assertEquals(3, results.size());
            for (int i = 0; i < 3; i++) {
                assertEquals(LocalDate.of(2025, 7, 1 + i), results.get(i).getDate());
                assertTrue(results.get(i).getDaylightHours() > 0 && results.get(i).getDaylightHours() <= 24);
            }
        }

        @Test
        @DisplayName("reversed range returns empty list")
        void reversedRangeReturnsEmpty() {
            DaylightCalculator calc = new DaylightCalculator(0);
            List<DaylightCalculator.DaylightResult> results = calc.getDaylightPerDay(
                    LocalDate.of(2025, 1, 10),
                    LocalDate.of(2025, 1, 5));
            assertTrue(results.isEmpty());
        }
    }

    @Test
    @DisplayName("getLatitudeDeg returns constructor latitude")
    void getLatitudeReturnsConstructorValue() {
        assertEquals(33.5, new DaylightCalculator(33.5).getLatitudeDeg(), 0.001);
        assertEquals(-60, new DaylightCalculator(-60).getLatitudeDeg(), 0.001);
    }

    @Test
    @DisplayName("checkDaylightForLaMargarita")
    void checkDaylightForLaMargarita() {
        double latMargarita = -33.671404;//lat: -33.671404, lon: -61.915194
        LocalDate ini = LocalDate.of(2026,03,05);//05 Thu	sunrise: 04:59:08	sunset: 17:38:30	daylight: 12:39:22
        Duration twelveHours = Duration.ofHours(12);
        Duration thirtyNineMinutes = Duration.ofMinutes(39);
        Duration twentyTwoSeconds = Duration.ofSeconds(22); 
        assertEquals(12, twelveHours.toHours());
        assertEquals(39, thirtyNineMinutes.toMinutes());
        assertEquals(22, twentyTwoSeconds.toSeconds());
        //Duration therteenHours = twelveHours.plusMinutes(60);
        //assertEquals(13, therteenHours.toHours());
        //System.out.println("therteenHours: " + therteenHours.toHours());
        //Duration fourteenHours = therteenHours.plusSeconds(30*60);
        //assertEquals(13.5, getHours(fourteenHours));
        //System.out.println("fourteenHours: " + getHours(fourteenHours));
        Duration dayLightHours = twelveHours.plus(thirtyNineMinutes).plus(twentyTwoSeconds);
        double hours = getHours(dayLightHours);
        System.out.println("hours: " + hours);
        double margaritaHours = new DaylightCalculator(latMargarita).getDaylightHours(ini);
        System.out.println("margaritaHours: " + margaritaHours);
        assertEquals(getHours(dayLightHours), margaritaHours, 0.1);        
        LocalDate end = LocalDate.of(2026,03,06);
        double totalHours = new DaylightCalculator(latMargarita).getTotalDaylightHours(ini, end);
        System.out.println("totalHours: " + totalHours);
        assertEquals(25.26, totalHours, 0.1);
    }

    public double getHours(Duration duration) {
        return duration.getSeconds() / 3600.0;
    }
}
