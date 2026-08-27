package com.ursulagis.desktop.chat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ursulagis.desktop.dao.Ndvi;
import com.ursulagis.desktop.dao.Poligono;

/**
 * Detects “siembra fertilizada” / environment-based fertilization questions and
 * builds Spanish step-by-step guidance (NDVI → cosecha → P recommendation →
 * siembras por ambiente → siembra fertilizada), tailored with names and yields
 * found in the user text and currently loaded map layers.
 */
public final class SiembraFertilizadaWorkflowGuide {

	private static final Pattern YIELD_KG_PATTERN = Pattern.compile("(\\d{3,5})\\s*kg\\s*/?\\s*ha", Pattern.CASE_INSENSITIVE);
	private static final Pattern YIELD_TN_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*t(?:ns?)?\\s*/?\\s*ha", Pattern.CASE_INSENSITIVE);

	/** Prevents instantiation. */
	private SiembraFertilizadaWorkflowGuide() {
	}

	/**
	 * Whether the query looks like the fertilized-seeding-by-environments workflow
	 * (siembra+fert, NDVI+fósforo, variety/ambiente keywords, etc.).
	 */
	public static boolean matches(String userQuery) {
		if (userQuery == null || userQuery.isBlank()) {
			return false;
		}
		String n = normalize(userQuery);
		boolean siembraFert = n.contains("siembra") && (n.contains("fertiliz") || n.contains("fert"));
		boolean ndviFosforo = n.contains("ndvi") && (n.contains("fosforo") || n.contains("fosfato") || n.contains("repon"));
		boolean ambientes = n.contains("baguette") || n.contains("baguete") || n.contains("pehuen") || n.contains("lomas")
				|| n.contains("ambiente");
		boolean campo = n.contains("regalada") || n.contains("lote") || n.contains("poligono");
		return siembraFert || (ndviFosforo && (ambientes || campo)) || (siembraFert && ndviFosforo);
	}

	/**
	 * Numbered Spanish guide for the full workflow, filling in polygon/NDVI
	 * suggestions from {@code layerContext} and crop/yield/fert from the query.
	 */
	public static String buildGuidance(String userQuery, MapLayerContext layerContext) {
		StringBuilder sb = new StringBuilder();
		sb.append("Flujo de siembra fertilizada por ambientes — te guío paso a paso:\n\n");

		String fieldName = extractFieldName(userQuery);
		double yieldTn = extractYieldTn(userQuery);
		String crop = extractCrop(userQuery);
		String fertSource = extractFertSource(userQuery);

		// 1. Polígono del campo
		sb.append("1. **Polígono del campo**");
		appendPolygonStep(sb, layerContext, fieldName, "contorno para descargar NDVI");

		// 2. NDVI
		sb.append("\n2. **Descargar NDVI**");
		sb.append("\n   - Activá el polígono del paso 1 en el árbol de capas.");
		sb.append("\n   - Clic derecho en el nodo **Polígonos** → **Obtener NDVI** (o Herramientas → descarga masiva de NDVI).");
		if (userQuery.toLowerCase(Locale.ROOT).contains("25/26") || userQuery.contains("25-26")) {
			sb.append("\n   - Filtrá fechas de la campaña **soja 25/26**.");
		}
		appendNdviStep(sb, layerContext);

		// 3. NDVI → cosecha
		sb.append("\n3. **Convertir NDVI a cosecha** (el de mayor NDVI promedio)");
		sb.append("\n   - Seleccioná la capa NDVI con **mayor valor promedio**");
		appendBestNdviHint(sb, layerContext);
		sb.append("\n   - Clic derecho → **Convertir NDVI a Cosecha**.");
		sb.append("\n   - Cultivo: **").append(crop.isBlank() ? "soja" : crop).append("**.");
		sb.append("\n   - Rinde promedio: **").append(formatYield(yieldTn)).append(" t/ha**");
		if (yieldTn > 0 && yieldTn < 100) {
			sb.append(" (").append(Math.round(yieldTn * 1000)).append(" kg/ha)");
		}
		sb.append(" — *Convertir a cosecha usa toneladas por ha, no kg/ha.*");

		// 4. Recomendar Fert P
		sb.append("\n4. **Recomendar fertilización P (reposición)**");
		sb.append("\n   - Activá la cosecha generada en el paso 3.");
		sb.append("\n   - Clic derecho sobre la cosecha → **Recomendar Fert. P Reposición**.");
		sb.append("\n   - Fuente de P: **").append(fertSource.isBlank() ? "fosfato monoamónico (confirmá en el diálogo)" : fertSource).append("**.");
		sb.append("\n   - Esto calcula la dosis según extracción de la cosecha de soja.");

		// 5. Siembra lomas
		sb.append("\n5. **Siembra en lomas** (ambiente bajo rendimiento)");
		appendPolygonStep(sb, layerContext, "lomas", "polígono de lomas");
		sb.append("\n   - Clic derecho en **Polígonos** → **Convertir a Siembra** (solo el ambiente lomas activo).");
		sb.append("\n   - Cultivo: **trigo** | Entresurco: **0,19 m** (confirmá en diálogo).");
		sb.append("\n   - Semilla: **Baguette 620 2627** (elegila en configuración de siembra).");

		// 6. Siembra zonas buenas
		sb.append("\n6. **Siembra en zonas buenas** (resto de ambientes, no lomas)");
		appendPolygonStep(sb, layerContext, "buena", "polígonos de zonas altas/productivas");
		sb.append("\n   - Repetí **Convertir a Siembra** para cada ambiente restante.");
		sb.append("\n   - Semilla sugerida: **Pehuen** (confirmá variedad en el diálogo).");

		// 7. Siembra fertilizada
		sb.append("\n7. **Generar siembra fertilizada** (por cada ambiente)");
		sb.append("\n   - Activá **una siembra** y **la fertilización P** del paso 4.");
		sb.append("\n   - Clic derecho en el nodo **Siembras** → **Generar Siembra Fertilizada**.");
		sb.append("\n   - Indicá si la fertilización va **en línea** (OK) o al costado (Cancelar).");
		sb.append("\n   - Repetí para cada par siembra + fertilización de ambiente.");

		sb.append("\n\n*Hoy el chat guía este flujo; algunos pasos abren diálogos de configuración que debés confirmar.*");
		if (layerContext != null && !layerContext.getLayers().isEmpty()) {
			sb.append("\n\n").append(layerContext.formatLayerList());
		}
		return sb.toString();
	}

	/** Appends polygon match hints for a named field/ambiente. */
	private static void appendPolygonStep(StringBuilder sb, MapLayerContext ctx, String nameHint, String role) {
		if (ctx == null) {
			sb.append("\n   - Buscá el ").append(role).append(" con nombre parecido a **").append(nameHint).append("**.");
			return;
		}
		List<LoadedLayerInfo> matches = ctx.findPolygonsMatching(nameHint);
		if (matches.isEmpty()) {
			sb.append("\n   - No encontré polígonos con **").append(nameHint).append("** cargados.");
			sb.append(" Importá polígonos o revisá nombres en el árbol de capas.");
			List<LoadedLayerInfo> all = ctx.findPolygons();
			if (!all.isEmpty()) {
				sb.append("\n   - Polígonos disponibles: ")
						.append(all.stream().map(LoadedLayerInfo::getName).collect(Collectors.joining(", ")));
			}
			return;
		}
		if (matches.size() == 1) {
			sb.append("\n   - Usá el polígono **").append(matches.get(0).getName()).append("** (").append(role).append(").");
			return;
		}
		sb.append("\n   - Hay varios candidatos para ").append(role).append(": ");
		sb.append(matches.stream().map(LoadedLayerInfo::getName).collect(Collectors.joining(", ")));
		sb.append("\n   - Elegí el más adecuado o el más cercano al nombre **").append(nameHint).append("**.");
	}

	/** Lists already-loaded NDVI layer names under the download step when present. */
	private static void appendNdviStep(StringBuilder sb, MapLayerContext ctx) {
		if (ctx == null) {
			return;
		}
		List<LoadedLayerInfo> ndvis = ctx.findNdviLayers();
		if (!ndvis.isEmpty()) {
			sb.append("\n   - NDVI ya cargados: ")
					.append(ndvis.stream().map(LoadedLayerInfo::getName).collect(Collectors.joining(", ")));
		}
	}

	/** Suggests the loaded NDVI with the highest mean, or a generic pick tip. */
	private static void appendBestNdviHint(StringBuilder sb, MapLayerContext ctx) {
		if (ctx == null) {
			sb.append(" (compará el NDVI promedio en el histograma o propiedades de cada capa).");
			return;
		}
		Optional<LoadedLayerInfo> best = ctx.findNdviWithHighestMean();
		if (best.isPresent()) {
			Double mean = ndviMean(best.get());
			sb.append("\n   - Sugerencia según capas cargadas: **").append(best.get().getName()).append("**");
			if (mean != null) {
				sb.append(" (NDVI promedio ≈ ").append(String.format(Locale.ROOT, "%.3f", mean)).append(")");
			}
		} else {
			sb.append(" (elegí el de mayor NDVI promedio tras descargar).");
		}
	}

	/** Mean NDVI from the layer entity when it is an {@link Ndvi}; otherwise null. */
	private static Double ndviMean(LoadedLayerInfo info) {
		if (info.getEntity() instanceof Ndvi ndvi) {
			return ndvi.getMeanNDVI();
		}
		return null;
	}

	/** Field/lote token from the query (“regalada” or after para/del/de la). */
	private static String extractFieldName(String query) {
		String n = normalize(query);
		if (n.contains("regalada")) {
			return "regalada";
		}
		Matcher m = Pattern.compile("(?:para|del|de la)\\s+([a-z0-9]+)").matcher(n);
		return m.find() ? m.group(1) : "";
	}

	/** First crop keyword found in the query (soja/trigo/maíz), or empty. */
	private static String extractCrop(String query) {
		String lower = query.toLowerCase(Locale.ROOT);
		if (lower.contains("soja")) {
			return "soja";
		}
		if (lower.contains("trigo")) {
			return "trigo";
		}
		if (lower.contains("maiz") || lower.contains("maíz")) {
			return "maíz";
		}
		return "";
	}

	/** Phosphorus source hint from MAP/fosfato wording in the query. */
	private static String extractFertSource(String query) {
		String lower = query.toLowerCase(Locale.ROOT);
		if (lower.contains("fosfato monoamonico") || lower.contains("fosfato monoamónico")
				|| lower.contains("map")) {
			return "fosfato monoamónico";
		}
		if (lower.contains("fosforo") || lower.contains("fosfato")) {
			return "fosfato (confirmá fuente en diálogo)";
		}
		return "";
	}

	/**
	 * Parses yield from the query as t/ha (converts kg/ha when needed).
	 * Returns {@code 0} when no yield is mentioned (caller may use a default).
	 */
	public static double extractYieldTn(String query) {
		Matcher kg = YIELD_KG_PATTERN.matcher(query);
		if (kg.find()) {
			return Double.parseDouble(kg.group(1)) / 1000.0;
		}
		Matcher tn = YIELD_TN_PATTERN.matcher(query);
		if (tn.find()) {
			return Double.parseDouble(tn.group(1).replace(',', '.'));
		}
		if (query.contains("4600")) {
			return 4.6;
		}
		return 0;
	}

	/** Spanish-formatted t/ha string for the guide (default 4,6 when yield is unknown). */
	private static String formatYield(double yieldTn) {
		if (yieldTn <= 0) {
			return "4,6";
		}
		return String.format(Locale.ROOT, "%.1f", yieldTn).replace('.', ',');
	}

	/** Lowercases and strips accents for keyword matching. */
	public static String normalize(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		return Normalizer.normalize(lower, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
	}
}
