package com.ursulagis.desktop.utils;

import java.time.LocalDate;
import java.util.function.ToDoubleFunction;

/**
 * Utilidad para estimar la radiación fotosintéticamente activa absorbida (APAR)
 * a partir de las relaciones propuestas por Monteith (1972, 1977) y Ruimy et al. (1994),
 * tal como se describen en el trabajo "Estimación de la APAR y la NPP mediante
 * sensoramiento remoto en tres sectores de la Pampa Húmeda, Argentina".
 *
 * Todas las fórmulas están expresadas en base diaria.
 */
public final class AparCalculator {

    /**
     * Coeficiente "a" de Angström‑Prescott (Pennman, 1948) para estimar radiación global.
     */
    public static final double DEFAULT_A_COEFF = 0.18d;

    /**
     * Coeficiente "b" de Angström‑Prescott (Pennman, 1948) para estimar radiación global.
     */
    public static final double DEFAULT_B_COEFF = 0.55d;

    /**
     * Fracción típica de la radiación global que corresponde a PAR.
     * En el trabajo se adopta PAR ≈ 47 % de Rg.
     */
    public static final double DEFAULT_PAR_FRACTION_OF_RG = 0.47d;

    private AparCalculator() {
        // Clase de utilidades: no instanciable
    }

    /**
     * Estima la radiación global diaria Rg (MJ·m⁻²·día⁻¹) a partir de:
     * - Ra: radiación astronómica diaria (MJ·m⁻²·día⁻¹)
     * - relativeSunshine (hOverH): heliofanía relativa h/H (adimensional, entre 0 y 1)
     * usando la ecuación de Angström‑Prescott:
     * Rg = Ra * (a + b * h/H)
     */
    public static double estimateGlobalRadiation(double ra,
                                                 double relativeSunshine,
                                                 double aCoeff,
                                                 double bCoeff) {
        if (ra < 0.0) {
            throw new IllegalArgumentException("Ra (radiación astronómica) no puede ser negativa.");
        }
        if (relativeSunshine < 0.0 || relativeSunshine > 1.0) {
            throw new IllegalArgumentException("La heliofanía relativa (h/H) debe estar entre 0 y 1.");
        }
        return ra * (aCoeff + bCoeff * relativeSunshine);
    }

    /**
     * Variante conveniente de {@link #estimateGlobalRadiation(double, double, double, double)}
     * usando los coeficientes por defecto de Pennman (a = 0.18, b = 0.55).
     */
    public static double estimateGlobalRadiation(double ra, double relativeSunshine) {
        return estimateGlobalRadiation(ra, relativeSunshine, DEFAULT_A_COEFF, DEFAULT_B_COEFF);
    }

    /**
     * Estima la radiación fotosintéticamente activa PAR (MJ·m⁻²·día⁻¹)
     * a partir de la radiación global Rg (MJ·m⁻²·día⁻¹), asumiendo que:
     * PAR ≈ 0.47 * Rg
     */
    public static double estimateParFromGlobalRadiation(double rg) {
        if (rg < 0.0) {
            throw new IllegalArgumentException("La radiación global (Rg) no puede ser negativa.");
        }
        return rg * DEFAULT_PAR_FRACTION_OF_RG;
    }

    /**
     * Estima la fracción de PAR absorbida por la cubierta vegetal (fPAR, adimensional)
     * usando la relación lineal propuesta por Ruimy et al. (1994) que se cita en el paper:
     * fPAR = 1.25 * NDVI - 0.025
     *
     * El resultado se acota al rango [0, 1].
     *
     * @param ndvi Índice Verde de Diferencias Normalizadas (NDVI), normalmente en [-1, 1].
     */
    public static double estimateFparFromNdvi(double ndvi) {
        double fpar = 1.25d * ndvi - 0.025d;
        if (fpar < 0.0) {
            fpar = 0.0;
        } else if (fpar > 1.0) {
            fpar = 1.0;
        }
        return fpar;
    }

    /**
     * Calcula APAR (MJ·m⁻²·día⁻¹) a partir de PAR (MJ·m⁻²·día⁻¹) y fPAR (adimensional):
     * APAR = PAR * fPAR
     */
    public static double calculateApar(double par, double fpar) {
        if (par < 0.0) {
            throw new IllegalArgumentException("PAR no puede ser negativa.");
        }
        if (fpar < 0.0 || fpar > 1.0) {
            throw new IllegalArgumentException("fPAR debe estar entre 0 y 1.");
        }
        return par * fpar;
    }

    /**
     * Calcula APAR (MJ·m⁻²·día⁻¹) a partir de:
     * - NDVI (adimensional)
     * - radiación global Rg (MJ·m⁻²·día⁻¹)
     *
     * Pasos:
     * 1) PAR = 0.47 * Rg
     * 2) fPAR = 1.25 * NDVI - 0.025  (acotado a [0,1])
     * 3) APAR = PAR * fPAR
     */
    public static double calculateAparFromNdviAndGlobalRadiation(double ndvi, double rg) {
        double par = estimateParFromGlobalRadiation(rg);
        double fpar = estimateFparFromNdvi(ndvi);
        return calculateApar(par, fpar);
    }

    /**
     * Calcula APAR (MJ·m⁻²·día⁻¹) a partir de:
     * - NDVI
     * - radiación astronómica diaria Ra (MJ·m⁻²·día⁻¹)
     * - heliofanía relativa h/H (entre 0 y 1)
     *
     * Se encadenan las ecuaciones:
     * 1) Rg = Ra * (a + b * h/H)
     * 2) PAR = 0.47 * Rg
     * 3) fPAR = 1.25 * NDVI - 0.025 (acotado a [0,1])
     * 4) APAR = PAR * fPAR
     */
    public static double calculateAparFromNdviRaAndSunshine(double ndvi,
                                                            double ra,
                                                            double relativeSunshine) {
        double rg = estimateGlobalRadiation(ra, relativeSunshine);
        return calculateAparFromNdviAndGlobalRadiation(ndvi, rg);
    }

    /**
     * Calcula la APAR total (MJ·m⁻²) entre dos fechas (inclusive), sumando la APAR diaria
     * de cada día del intervalo. Usa radiación astronómica diaria (Ra) según la latitud
     * y una heliofanía relativa y un NDVI constantes para todo el período.
     *
     * @param startInclusive fecha inicial (inclusive)
     * @param endInclusive   fecha final (inclusive)
     * @param latitudeDeg    latitud en grados (-90 a 90) para calcular Ra
     * @param ndvi           NDVI constante en el período (adimensional)
     * @param relativeSunshine heliofanía relativa h/H constante (0–1)
     * @return APAR acumulada en MJ·m⁻²; 0 si end es anterior a start
     */
    public static double totalAparBetweenDates(LocalDate startInclusive,
                                               LocalDate endInclusive,
                                               double latitudeDeg,
                                               double ndvi,
                                               double relativeSunshine) {
        if (endInclusive.isBefore(startInclusive)) {
            return 0.0;
        }
        DaylightCalculator daylight = new DaylightCalculator(latitudeDeg);
        double total = 0.0;
        LocalDate d = startInclusive;
        while (!d.isAfter(endInclusive)) {
            double ra = daylight.getTotalSolarRadiationMjPerM2(d);
            total += calculateAparFromNdviRaAndSunshine(ndvi, ra, relativeSunshine);
            d = d.plusDays(1);
        }
        return total;
    }

    /**
     * Calcula la APAR total (MJ·m⁻²) entre dos fechas usando PAR diaria y NDVI
     * proporcionados por día (p. ej. desde series temporales o imágenes).
     *
     * @param startInclusive fecha inicial (inclusive)
     * @param endInclusive   fecha final (inclusive)
     * @param parProvider   PAR diaria (MJ·m⁻²·día⁻¹) para cada fecha; no null
     * @param ndviProvider  NDVI para cada fecha; no null
     * @return APAR acumulada en MJ·m⁻²; 0 si end es anterior a start
     */
    public static double totalAparBetweenDates(LocalDate startInclusive,
                                               LocalDate endInclusive,
                                               ToDoubleFunction<LocalDate> parProvider,
                                               ToDoubleFunction<LocalDate> ndviProvider) {
        if (endInclusive.isBefore(startInclusive)) {
            return 0.0;
        }
        double total = 0.0;
        LocalDate d = startInclusive;
        while (!d.isAfter(endInclusive)) {
            double par = parProvider.applyAsDouble(d);
            double ndvi = ndviProvider.applyAsDouble(d);
            double fpar = estimateFparFromNdvi(ndvi);
            total += calculateApar(par, fpar);
            d = d.plusDays(1);
        }
        return total;
    }
}

