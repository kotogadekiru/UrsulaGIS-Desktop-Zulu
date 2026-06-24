package com.ursulagis.desktop.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ManualContextBuilderTest {

	@Test
	@DisplayName("NDVI query includes ObtenerNDVI manual excerpt")
	void ndviQueryIncludesManual() {
		String context = ManualContextBuilder.buildForQuery("como descargar ndvi para un poligono");

		assertFalse(context.isBlank());
		assertTrue(context.contains("Obtener NDVI"));
		assertTrue(context.contains("ObtenerNDVI.pdf"));
	}

	@Test
	@DisplayName("margin query includes margin manual excerpt")
	void marginQueryIncludesManual() {
		String context = ManualContextBuilder.buildForQuery("como genero un mapa de margenes");

		assertFalse(context.isBlank());
		assertTrue(context.contains("Mapa de márgenes") || context.contains("MapaDeMargenesUrsulaGIS.pdf"));
	}

	@Test
	@DisplayName("loads PDF text from docs folder")
	void loadsPdfText() {
		String text = ManualContextBuilder.loadDocumentText("ModoDeUso_0.2.18.pdf");

		assertFalse(text.isBlank());
		assertTrue(text.length() > 500);
	}

	@Test
	@DisplayName("selectExcerpts prefers paragraphs matching query terms")
	void selectExcerptsPrefersMatchingParagraphs() {
		String fullText = "Intro general.\n\n"
				+ "Para obtener NDVI abra el menu Poligonos y seleccione Obtener NDVI.\n\n"
				+ "Otro tema sin relacion.";

		String excerpt = ManualContextBuilder.selectExcerpts(fullText, "obtener ndvi poligono", 500);

		assertTrue(excerpt.toLowerCase().contains("ndvi"));
	}

	@Test
	@DisplayName("filename stem becomes tutorial title and keywords")
	void filenameParsingForTranscripts() {
		assertTrue(ManualContextBuilder.titleFromFilename("importar_cosecha.txt").contains("Importar"));
		assertTrue(ManualContextBuilder.titleFromFilename("importar_cosecha.txt").contains("videotutorial"));
		assertTrue(ManualContextBuilder.keywordsFromFilename("siembra_fertilizada.txt").contains("siembra"));
		assertTrue(ManualContextBuilder.keywordsFromFilename("MapaDeMargenes.txt").contains("margenes"));
	}
}
