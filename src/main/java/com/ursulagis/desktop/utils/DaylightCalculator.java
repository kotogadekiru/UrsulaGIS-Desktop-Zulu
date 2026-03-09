package com.ursulagis.desktop.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates total daylight (hours the sun is above the horizon) between two
 * dates at a given latitude. Uses standard solar geometry: declination and
 * sunrise/sunset hour angle.
 */
public class DaylightCalculator {

    /** Earth's axial tilt in degrees (obliquity). */
    private static final double OBLIQUITY_DEG = 23.44;

    /** Days in a standard year for declination approximation. */
    private static final double DAYS_PER_YEAR = 365.25;

    /** Day-of-year offset so that sin curve matches equinox/solstice (approx. March 21). */
    private static final int DECLINATION_OFFSET = 81;

    /** Solar constant in MJ/(m²·min) for daily extraterrestrial radiation formula. */
    private static final double SOLAR_CONSTANT_MJ_M2_MIN = 0.0820;

    private final double latitudeDeg;

    /**
     * @param latitudeDeg latitude in degrees, -90 (South Pole) to 90 (North Pole)
     */
    public DaylightCalculator(double latitudeDeg) {
        if (latitudeDeg < -90 || latitudeDeg > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        this.latitudeDeg = latitudeDeg;
    }

    /**
     * Solar declination in radians for a given day of year (1–366).
     * Approximate formula; ignores nutation and small long-term effects.
     */
    private static double solarDeclinationRad(int dayOfYear) {
        double angleRad = 2 * Math.PI * (dayOfYear - DECLINATION_OFFSET) / DAYS_PER_YEAR;
        return Math.toRadians(OBLIQUITY_DEG) * Math.sin(angleRad);
    }

    /**
     * Daylight duration for a single day at this latitude.
     *
     * @param date the date (time zone is not used; calculation is for the day at the given latitude)
     * @return daylight duration in hours (0–24)
     */
    public double getDaylightHours(LocalDate date) {
        int dayOfYear = date.getDayOfYear();
        double declinationRad = solarDeclinationRad(dayOfYear);
        double latRad = Math.toRadians(latitudeDeg);

        double tanLat = Math.tan(latRad);
        double tanDec = Math.tan(declinationRad);
        double cosHourAngle = -tanLat * tanDec;

        if (cosHourAngle >= 1) {
            return 0; // polar night
        }
        if (cosHourAngle <= -1) {
            return 24; // polar day
        }

        double hourAngleRad = Math.acos(cosHourAngle);
        // Day length = (24 / π) * hourAngle (half-day in radians → hours)
        return (24.0 / Math.PI) * hourAngleRad;
    }

    /**
     * Maximum angular height of the sun (solar elevation at solar noon) for a given date
     * at this latitude.
     * <p>
     * Uses the same declination model as daylight calculations. Result is in degrees
     * above the horizon; may be negative when the sun does not rise (polar night).
     *
     * @param date the date (time zone is not used)
     * @return maximum solar elevation in degrees (−90 to 90)
     */
    public double getMaxSolarElevationDeg(LocalDate date) {
        int dayOfYear = date.getDayOfYear();
        double declinationRad = solarDeclinationRad(dayOfYear);
        double latRad = Math.toRadians(latitudeDeg);
        // At solar noon: sin(altitude) = cos(lat - decl) => altitude = 90° - (lat - decl)
        double altitudeRad = (Math.PI / 2) - (latRad - declinationRad);
        return Math.toDegrees(altitudeRad);
    }

    /**
     * Total solar radiation for a day at this latitude (extraterrestrial, top of atmosphere).
     * <p>
     * Uses the standard formula: daily integral of solar irradiance on a horizontal surface
     * at the top of the atmosphere, including the Earth–Sun distance (eccentricity) factor.
     * Same declination and sunset hour angle as daylight calculations.
     *
     * @param date the date (time zone is not used)
     * @return daily extraterrestrial radiation in MJ/(m²·day); 0 on polar night
     */
    public double getTotalSolarRadiationMjPerM2(LocalDate date) {
        int dayOfYear = date.getDayOfYear();
        double declinationRad = solarDeclinationRad(dayOfYear);
        double latRad = Math.toRadians(latitudeDeg);

        double tanLat = Math.tan(latRad);
        double tanDec = Math.tan(declinationRad);
        double cosHourAngle = -tanLat * tanDec;

        double omegaS; // sunset hour angle in radians
        if (cosHourAngle >= 1) {
            return 0; // polar night
        }
        if (cosHourAngle <= -1) {
            omegaS = Math.PI; // polar day: sun never sets
        } else {
            omegaS = Math.acos(cosHourAngle);
        }

        // Inverse relative Earth–Sun distance (eccentricity correction)
        double dr = 1 + 0.033 * Math.cos(2 * Math.PI * dayOfYear / 365);

        // Ra = (24*60/π) * Gsc * dr * [ωs*sin(φ)*sin(δ) + cos(φ)*cos(δ)*sin(ωs)]
        double sinLat = Math.sin(latRad);
        double sinDec = Math.sin(declinationRad);
        double cosLat = Math.cos(latRad);
        double cosDec = Math.cos(declinationRad);
        double bracket = omegaS * sinLat * sinDec + cosLat * cosDec * Math.sin(omegaS);

        return (24 * 60 / Math.PI) * SOLAR_CONSTANT_MJ_M2_MIN * dr * bracket;
    }

    /**
     * Total solar radiation (extraterrestrial) between two dates (inclusive) at this latitude.
     * Sum of daily {@link #getTotalSolarRadiationMjPerM2(LocalDate)} for each day in the range.
     *
     * @param startInclusive start date (inclusive)
     * @param endInclusive   end date (inclusive)
     * @return total radiation in MJ/m²; 0 if end is before start
     */
    public double getTotalSolarRadiationMjBetween(LocalDate startInclusive, LocalDate endInclusive) {
        if (endInclusive.isBefore(startInclusive)) {
            return 0;
        }
        double total = 0;
        LocalDate d = startInclusive;
        while (!d.isAfter(endInclusive)) {
            total += getTotalSolarRadiationMjPerM2(d);
            d = d.plusDays(1);
        }
        return total;
    }

    /**
     * Total daylight in hours between two dates (inclusive) at this latitude.
     *
     * @param startInclusive start date (inclusive)
     * @param endInclusive   end date (inclusive)
     * @return total daylight in hours
     */
    public double getTotalDaylightHours(LocalDate startInclusive, LocalDate endInclusive) {
        if (!endInclusive.isBefore(startInclusive)) {
            long days = ChronoUnit.DAYS.between(startInclusive, endInclusive) + 1;
            double total = 0;
            LocalDate d = startInclusive;
            for (long i = 0; i < days; i++) {
                total += getDaylightHours(d);
                d = d.plusDays(1);
            }

            return total;
        }
        return 0;
    }

    /**
     * Daylight per day between two dates (inclusive).
     *
     * @param startInclusive start date (inclusive)
     * @param endInclusive   end date (inclusive)
     * @return list of daylight hours for each day, in order
     */
    public List<DaylightResult> getDaylightPerDay(LocalDate startInclusive, LocalDate endInclusive) {
        List<DaylightResult> results = new ArrayList<>();
        if (endInclusive.isBefore(startInclusive)) {
            return results;
        }
        LocalDate d = startInclusive;
        while (!d.isAfter(endInclusive)) {
            results.add(new DaylightResult(d, getDaylightHours(d)));
            d = d.plusDays(1);
        }
        return results;
    }

    public double getLatitudeDeg() {
        return latitudeDeg;
    }

    /** Result for a single day: date and daylight hours. */
    public static final class DaylightResult {
        private final LocalDate date;
        private final double daylightHours;

        public DaylightResult(LocalDate date, double daylightHours) {
            this.date = date;
            this.daylightHours = daylightHours;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getDaylightHours() {
            return daylightHours;
        }
    }
}
