package com.ursulagis.desktop.chat;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ursulagis.desktop.dao.Poligono;
import com.ursulagis.desktop.dao.config.Asignacion;
import com.ursulagis.desktop.dao.config.Campania;
import com.ursulagis.desktop.dao.config.Cultivo;
import com.ursulagis.desktop.utils.DAH;

/**
 * Parses campaign, crop, and NDVI date-range filters from chat text (and optional
 * structured intent fields), then finds matching {@link Asignacion} contours for
 * bulk NDVI download. Also used to extract crop/campaign keywords for recorrida loads.
 *
 * @param campaniaName resolved campaign name (may come from text, intent, or latest DB campaign)
 * @param cultivoName  resolved crop name, if any
 * @param begin        inclusive start of the imagery window
 * @param end          inclusive end of the imagery window
 */
public record AsignacionNdviRequest(
		String campaniaName,
		String cultivoName,
		LocalDate begin,
		LocalDate end) {

	private static final Pattern CAMPAIGN_YY = Pattern.compile("\\b(\\d{2})\\s*[/-]\\s*(\\d{2})\\b");
	private static final Pattern CAMPAIGN_COMPACT = Pattern.compile("\\b(\\d{2})(\\d{2})\\b");
	private static final Pattern ISO_DATE = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
	private static final Pattern DMY_DATE = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b");
	private static final Pattern DESDE_HASTA = Pattern.compile(
			"desde\\s+(.+?)\\s+hasta\\s+(.+?)(?:\\s|$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern MONTH_RANGE = Pattern.compile(
			"de\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)"
					+ "\\s+a\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)"
					+ "(?:\\s+(?:de\\s+)?(\\d{4}))?",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern CAMPANIA_PHRASE = Pattern.compile(
			"campa[nñ]a\\s+[\"']?([^\"'\\n,]+?)[\"']?(?=\\s+(?:cultivo|soja|ma[ií]z|trigo|girasol|ndvi|desde|de\\s+|periodo|per[ií]odo|$)|$)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final String[] CROPS = {
			"soja", "maiz", "maíz", "trigo", "girasol", "cebada", "sorgo", "alfalfa", "colza", "avena"
	};

	/** Parses filters from free text only. */
	public static AsignacionNdviRequest parse(String userText) {
		return parse(userText, null, null, null, null);
	}

	/**
	 * Merges structured intent fields with text extraction; fills missing period
	 * from campaign entity, “últimas imágenes”, season tokens, or last-month default.
	 */
	public static AsignacionNdviRequest parse(
			String userText,
			String campaniaFromIntent,
			String cultivoFromIntent,
			LocalDate beginFromIntent,
			LocalDate endFromIntent) {
		String text = userText == null ? "" : userText;
		String lower = normalize(text);

		String campania = firstNonBlank(campaniaFromIntent, extractCampaniaName(text, lower));
		if (campania == null || campania.isBlank()) {
			campania = resolveLatestCampaniaName().orElse(null);
		}
		String cultivo = firstNonBlank(cultivoFromIntent, extractCultivoName(lower));

		LocalDate begin = beginFromIntent;
		LocalDate end = endFromIntent;
		boolean explicitPeriod = begin != null && end != null && end.isAfter(begin);
		if (!explicitPeriod) {
			DateRange fromText = extractDateRange(text, lower, campania);
			if (isMeaningfulPeriod(fromText.begin(), fromText.end())) {
				begin = fromText.begin();
				end = fromText.end();
				explicitPeriod = true;
			}
		}

		DateRange campaignPeriod = campania != null ? datesFromCampaniaEntity(campania) : DateRange.empty();
		boolean hasCampaignPeriod = isMeaningfulPeriod(campaignPeriod.begin(), campaignPeriod.end());

		if (!explicitPeriod && hasCampaignPeriod) {
			if (mentionsLatestImages(lower)) {
				DateRange clipped = clipLatestInsideCampaign(campaignPeriod.begin(), campaignPeriod.end());
				begin = clipped.begin();
				end = clipped.end();
			} else {
				begin = campaignPeriod.begin();
				end = campaignPeriod.end();
			}
			explicitPeriod = isMeaningfulPeriod(begin, end);
		}

		if (!explicitPeriod && mentionsLatestImages(lower)) {
			end = LocalDate.now();
			begin = end.minusMonths(1);
			explicitPeriod = true;
		}

		boolean hasCampaignToken = CAMPAIGN_YY.matcher(text).find()
				|| CAMPAIGN_COMPACT.matcher(text).find()
				|| (campania != null && (CAMPAIGN_YY.matcher(campania).find() || CAMPAIGN_COMPACT.matcher(campania).find()));
		if (!explicitPeriod && hasCampaignToken) {
			DateRange season = seasonFromCampaignToken(campania != null ? campania : lower);
			if (isMeaningfulPeriod(season.begin(), season.end())) {
				begin = season.begin();
				end = season.end();
				explicitPeriod = true;
			}
		}

		if (!explicitPeriod) {
			end = LocalDate.now();
			begin = end.minusMonths(1);
		}

		return new AsignacionNdviRequest(campania, cultivo, begin, end);
	}

	/** Last month of imagery, clipped to the campaign window and not after today. */
	private static DateRange clipLatestInsideCampaign(LocalDate campaignBegin, LocalDate campaignEnd) {
		LocalDate today = LocalDate.now();
		LocalDate end = campaignEnd.isBefore(today) ? campaignEnd : today;
		LocalDate begin = end.minusMonths(1);
		if (begin.isBefore(campaignBegin)) {
			begin = campaignBegin;
		}
		if (!begin.isBefore(end)) {
			begin = campaignBegin;
			end = campaignEnd.isBefore(today) ? campaignEnd : today;
		}
		if (!begin.isBefore(end)) {
			return new DateRange(campaignBegin, campaignEnd);
		}
		return new DateRange(begin, end);
	}

	/**
	 * True when the period spans at least 7 days (rejects Campania defaults where
	 * inicio/fin are both “today”).
	 */
	static boolean isMeaningfulPeriod(LocalDate begin, LocalDate end) {
		if (begin == null || end == null || !end.isAfter(begin)) {
			return false;
		}
		// Reject Campania defaults where inicio/fin are both "today".
		return java.time.temporal.ChronoUnit.DAYS.between(begin, end) >= 7;
	}

	/** Whether begin/end form a usable open interval (end strictly after begin). */
	public boolean hasPeriod() {
		return begin != null && end != null && end.isAfter(begin);
	}

	/**
	 * Assignments matching campaign/crop that have a resolvable polygon contour.
	 */
	public List<Asignacion> findAsignaciones() {
		List<Asignacion> all = DAH.getAllAsignaciones();
		return all.stream()
				.filter(a -> matchesCampania(a, campaniaName))
				.filter(a -> matchesCultivo(a, cultivoName))
				.filter(a -> resolveContorno(a) != null)
				.toList();
	}

	/** Distinct polygons (lot or assignment contornos) for the matched assignments. */
	public List<Poligono> findContornos() {
		return findAsignaciones().stream()
				.map(AsignacionNdviRequest::resolveContorno)
				.distinct()
				.toList();
	}

	/** Contorno on the assignment itself, or the lot's contorno as fallback. */
	public static Poligono resolveContorno(Asignacion a) {
		if (a == null) {
			return null;
		}
		if (a.getContorno() != null) {
			return a.getContorno();
		}
		if (a.getLote() != null) {
			return a.getLote().getContorno();
		}
		return null;
	}

	/** True when the assignment's campaign matches {@code campaniaName} (or filter is blank). */
	private static boolean matchesCampania(Asignacion a, String campaniaName) {
		if (campaniaName == null || campaniaName.isBlank()) {
			return true;
		}
		if (a.getCampania() == null || a.getCampania().getNombre() == null) {
			return false;
		}
		return campaignNamesMatch(campaniaName, a.getCampania().getNombre());
	}

	/**
	 * Loose campaign-name equality (substring or shared {@code YY/YY} / {@code YYYY} key).
	 */
	static boolean campaignNamesMatch(String a, String b) {
		if (a == null || b == null) {
			return false;
		}
		String want = normalize(a);
		String have = normalize(b);
		if (have.equals(want) || have.contains(want) || want.contains(have)) {
			return true;
		}
		String keyA = campaignSortKey(a);
		String keyB = campaignSortKey(b);
		return keyA != null && keyA.equals(keyB);
	}

	/** True when the assignment's crop matches {@code cultivoName} (or filter is blank). */
	private static boolean matchesCultivo(Asignacion a, String cultivoName) {
		if (cultivoName == null || cultivoName.isBlank()) {
			return true;
		}
		if (a.getCultivo() == null || a.getCultivo().getNombre() == null) {
			return false;
		}
		String want = normalize(cultivoName);
		String have = normalize(a.getCultivo().getNombre());
		return have.equals(want) || have.contains(want) || want.contains(have);
	}

	/**
	 * Pulls a campaign name from phrase, {@code YY/YY}, compact {@code YYXX}, or known DB names.
	 */
	private static String extractCampaniaName(String text, String lower) {
		Matcher phrase = CAMPANIA_PHRASE.matcher(text);
		if (phrase.find()) {
			return phrase.group(1).trim();
		}
		Matcher yy = CAMPAIGN_YY.matcher(text);
		if (yy.find()) {
			return yy.group(1) + "/" + yy.group(2);
		}
		Matcher compact = CAMPAIGN_COMPACT.matcher(text);
		while (compact.find()) {
			if (!looksLikeCampaignCompact(compact.group(1), compact.group(2))) {
				continue;
			}
			String token = compact.group(1) + compact.group(2);
			Optional<String> known = resolveCampaniaNameForToken(token);
			return known.orElse(compact.group(1) + "/" + compact.group(2));
		}
		Optional<Campania> known = matchKnownCampania(lower);
		return known.map(Campania::getNombre).orElse(null);
	}

	/** Rejects 4-digit calendar years (e.g. 2025) mistaken for campaign {@code 20/25}. */
	private static boolean looksLikeCampaignCompact(String y1, String y2) {
		try {
			int a = Integer.parseInt(y1);
			int b = Integer.parseInt(y2);
			// Avoid treating calendar years like 2025 as campaign 20/25.
			if (a == 20 && b >= 20) {
				return false;
			}
			return b == a + 1 || b == (a + 1) % 100;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/** Latest campaign name in the DB by year-token / start-date ranking. */
	static Optional<String> resolveLatestCampaniaName() {
		try {
			return DAH.getAllCampanias().stream()
					.filter(c -> c.getNombre() != null && !c.getNombre().isBlank())
					.max(AsignacionNdviRequest::compareCampaniaRecency)
					.map(Campania::getNombre);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/** Best DB campaign whose name loosely matches {@code token}, preferring the newest. */
	private static Optional<String> resolveCampaniaNameForToken(String token) {
		try {
			return DAH.getAllCampanias().stream()
					.filter(c -> c.getNombre() != null && campaignNamesMatch(token, c.getNombre()))
					.max(AsignacionNdviRequest::compareCampaniaRecency)
					.map(Campania::getNombre);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/** Comparator for {@link java.util.stream.Stream#max}: older campaigns rank lower. */
	static int compareCampaniaRecency(Campania a, Campania b) {
		// Natural order: older < newer (so Stream.max picks the latest).
		int byKey = Integer.compare(campaignYearRank(a.getNombre()), campaignYearRank(b.getNombre()));
		if (byKey != 0) {
			return byKey;
		}
		LocalDate aIni = toLocalDate(a.getInicio());
		LocalDate bIni = toLocalDate(b.getInicio());
		if (aIni != null && bIni != null) {
			int byDate = aIni.compareTo(bIni);
			if (byDate != 0) {
				return byDate;
			}
		}
		return String.valueOf(a.getNombre()).compareToIgnoreCase(String.valueOf(b.getNombre()));
	}

	/** Higher = more recent. Supports 2627, 26/27, 26-27. */
	static int campaignYearRank(String name) {
		String key = campaignSortKey(name);
		if (key == null || key.length() < 4) {
			return Integer.MIN_VALUE / 4;
		}
		try {
			int y1 = Integer.parseInt(key.substring(0, 2));
			int y2 = Integer.parseInt(key.substring(2, 4));
			return y1 * 100 + y2;
		} catch (NumberFormatException e) {
			return Integer.MIN_VALUE / 4;
		}
	}

	/** Four-digit sort key ({@code YY} + {@code YY}) extracted from a campaign name, or null. */
	static String campaignSortKey(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		Matcher yy = CAMPAIGN_YY.matcher(name);
		if (yy.find()) {
			return yy.group(1) + yy.group(2);
		}
		Matcher compact = CAMPAIGN_COMPACT.matcher(name);
		if (compact.find()) {
			return compact.group(1) + compact.group(2);
		}
		String digits = name.replaceAll("\\D+", "");
		if (digits.length() >= 4) {
			return digits.substring(0, 4);
		}
		return null;
	}

	/** True for “últimas imágenes” / “latest” style phrasing in normalized text. */
	private static boolean mentionsLatestImages(String lower) {
		return lower.contains("ultima") || lower.contains("ultimo")
				|| lower.contains("ultimas") || lower.contains("ultimos")
				|| lower.contains("latest") || lower.contains("most recent");
	}

	/** Newest DB campaign whose normalized name appears as a substring of {@code lower}. */
	private static Optional<Campania> matchKnownCampania(String lower) {
		try {
			return DAH.getAllCampanias().stream()
					.filter(c -> c.getNombre() != null && lower.contains(normalize(c.getNombre())))
					.max(AsignacionNdviRequest::compareCampaniaRecency);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/** Crop name from known crop tokens or a {@code cultivo …} phrase; otherwise null. */
	private static String extractCultivoName(String lower) {
		for (String crop : CROPS) {
			if (lower.contains(normalize(crop))) {
				try {
					Cultivo known = DAH.getCultivo(crop.replace("í", "i").replace("á", "a"));
					if (known != null) {
						return known.getNombre();
					}
				} catch (Exception ignored) {
					// fall through
				}
				return crop.replace("í", "i").replace("á", "a");
			}
		}
		Matcher m = Pattern.compile("cultivo\\s+[\"']?([^\"'\\n,]+)[\"']?", Pattern.CASE_INSENSITIVE)
				.matcher(lower);
		if (m.find()) {
			return m.group(1).trim();
		}
		return null;
	}

	/**
	 * Parses desde/hasta, Spanish month ranges, or the first two ISO/DMY dates in the text.
	 */
	private static DateRange extractDateRange(String text, String lower, String campaniaName) {
		Matcher desdeHasta = DESDE_HASTA.matcher(text);
		if (desdeHasta.find()) {
			LocalDate b = parseFlexibleDate(desdeHasta.group(1).trim());
			LocalDate e = parseFlexibleDate(desdeHasta.group(2).trim());
			if (b != null && e != null) {
				return new DateRange(b, e);
			}
		}

		Matcher monthRange = MONTH_RANGE.matcher(lower);
		if (monthRange.find()) {
			Month startMonth = monthOf(monthRange.group(1));
			Month endMonth = monthOf(monthRange.group(2));
			int year = monthRange.group(3) != null
					? Integer.parseInt(monthRange.group(3))
					: inferSeasonStartYear(campaniaName, lower);
			LocalDate begin = LocalDate.of(year, startMonth, 1);
			int endYear = endMonth.getValue() < startMonth.getValue() ? year + 1 : year;
			LocalDate end = LocalDate.of(endYear, endMonth, endMonth.length(LocalDate.of(endYear, endMonth, 1).isLeapYear()));
			return new DateRange(begin, end);
		}

		List<LocalDate> isos = ISO_DATE.matcher(text).results()
				.map(r -> LocalDate.parse(r.group(1)))
				.toList();
		if (isos.size() >= 2) {
			return new DateRange(isos.get(0), isos.get(1));
		}

		List<LocalDate> dmys = DMY_DATE.matcher(text).results()
				.map(r -> toLocalDate(r.group(1), r.group(2), r.group(3)))
				.filter(d -> d != null)
				.toList();
		if (dmys.size() >= 2) {
			return new DateRange(dmys.get(0), dmys.get(1));
		}

		return DateRange.empty();
	}

	/** Inclusive inicio/fin from the matching {@link Campania} entity when meaningful. */
	private static DateRange datesFromCampaniaEntity(String campaniaName) {
		try {
			Optional<Campania> match = DAH.getAllCampanias().stream()
					.filter(c -> c.getNombre() != null && campaignNamesMatch(campaniaName, c.getNombre()))
					.max(AsignacionNdviRequest::compareCampaniaRecency);
			if (match.isEmpty()) {
				return DateRange.empty();
			}
			Campania c = match.get();
			LocalDate begin = toLocalDate(c.getInicio());
			LocalDate end = toLocalDate(c.getFin());
			if (isMeaningfulPeriod(begin, end)) {
				return new DateRange(begin, end);
			}
		} catch (Exception ignored) {
			// ignore DB issues while parsing chat
		}
		return DateRange.empty();
	}

	/** Nov–Apr season window inferred from a {@code YY/YY} or compact campaign token. */
	private static DateRange seasonFromCampaignToken(String lower) {
		Matcher yy = CAMPAIGN_YY.matcher(lower);
		if (yy.find()) {
			int y1 = 2000 + Integer.parseInt(yy.group(1));
			int y2 = 2000 + Integer.parseInt(yy.group(2));
			if (y2 < y1) {
				y2 += 100;
			}
			return new DateRange(LocalDate.of(y1, Month.NOVEMBER, 1), LocalDate.of(y2, Month.APRIL, 30));
		}
		Matcher compact = CAMPAIGN_COMPACT.matcher(lower);
		while (compact.find()) {
			if (!looksLikeCampaignCompact(compact.group(1), compact.group(2))) {
				continue;
			}
			int y1 = 2000 + Integer.parseInt(compact.group(1));
			int y2 = 2000 + Integer.parseInt(compact.group(2));
			if (y2 < y1) {
				y2 += 100;
			}
			return new DateRange(LocalDate.of(y1, Month.NOVEMBER, 1), LocalDate.of(y2, Month.APRIL, 30));
		}
		String key = campaignSortKey(lower);
		if (key != null && key.length() == 4) {
			int y1 = 2000 + Integer.parseInt(key.substring(0, 2));
			int y2 = 2000 + Integer.parseInt(key.substring(2, 4));
			if (y2 < y1) {
				y2 += 100;
			}
			return new DateRange(LocalDate.of(y1, Month.NOVEMBER, 1), LocalDate.of(y2, Month.APRIL, 30));
		}
		return DateRange.empty();
	}

	/** Calendar year for the start of a month-range when the user omitted an explicit year. */
	private static int inferSeasonStartYear(String campaniaName, String lower) {
		Matcher yy = CAMPAIGN_YY.matcher(campaniaName != null ? campaniaName : lower);
		if (yy.find()) {
			return 2000 + Integer.parseInt(yy.group(1));
		}
		return LocalDate.now().getMonthValue() >= 7 ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1;
	}

	/** ISO, DMY, or Spanish month-name parse; null when unrecognized. */
	private static LocalDate parseFlexibleDate(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String s = raw.trim();
		try {
			return LocalDate.parse(s, ISO);
		} catch (DateTimeParseException ignored) {
			// continue
		}
		Matcher dmy = DMY_DATE.matcher(s);
		if (dmy.find()) {
			return toLocalDate(dmy.group(1), dmy.group(2), dmy.group(3));
		}
		Month month = monthOf(normalize(s));
		if (month != null) {
			int year = LocalDate.now().getYear();
			return LocalDate.of(year, month, 1);
		}
		return null;
	}

	/** Builds a {@link LocalDate} from day/month/year strings (2-digit years → 20xx). */
	private static LocalDate toLocalDate(String day, String month, String year) {
		try {
			int y = Integer.parseInt(year);
			if (y < 100) {
				y += 2000;
			}
			return LocalDate.of(y, Integer.parseInt(month), Integer.parseInt(day));
		} catch (Exception e) {
			return null;
		}
	}

	/** Converts a legacy {@link Calendar} field from {@link Campania} into {@link LocalDate}. */
	private static LocalDate toLocalDate(Calendar cal) {
		if (cal == null) {
			return null;
		}
		return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
	}

	/** Maps Spanish month names (including setiembre) to {@link Month}. */
	private static Month monthOf(String token) {
		if (token == null) {
			return null;
		}
		String t = normalize(token);
		return switch (t) {
			case "enero" -> Month.JANUARY;
			case "febrero" -> Month.FEBRUARY;
			case "marzo" -> Month.MARCH;
			case "abril" -> Month.APRIL;
			case "mayo" -> Month.MAY;
			case "junio" -> Month.JUNE;
			case "julio" -> Month.JULY;
			case "agosto" -> Month.AUGUST;
			case "septiembre", "setiembre" -> Month.SEPTEMBER;
			case "octubre" -> Month.OCTOBER;
			case "noviembre" -> Month.NOVEMBER;
			case "diciembre" -> Month.DECEMBER;
			default -> null;
		};
	}

	/** Equality or mutual substring match after accent-stripping normalization. */
	private static boolean matchesLoose(String a, String b) {
		String na = normalize(a);
		String nb = normalize(b);
		return na.equals(nb) || na.contains(nb) || nb.contains(na);
	}

	/** Prefers the first non-blank trimmed value (intent fields before text extraction). */
	private static String firstNonBlank(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a.trim();
		}
		if (b != null && !b.isBlank()) {
			return b.trim();
		}
		return null;
	}

	/** Lowercases and strips accents/extra spaces for tolerant name matching. */
	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		String n = Normalizer.normalize(value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "")
				.toLowerCase(Locale.ROOT);
		return n.replaceAll("\\s+", " ").trim();
	}

	/** Inclusive date pair used while resolving periods from text or entities. */
	private record DateRange(LocalDate begin, LocalDate end) {
		/** Sentinel with both bounds unset (no period resolved yet). */
		static DateRange empty() {
			return new DateRange(null, null);
		}
	}
}
