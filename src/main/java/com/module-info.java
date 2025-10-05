module com.ursulagis.desktop {
    requires java.base; // Implicitly required, but good practice to include
	requires java.desktop; // For java.awt access
	requires javafx.controls;
	requires javafx.base;
	requires javafx.swing;
	requires javafx.media;
	requires javafx.web;
	requires javafx.fxml;
	requires worldwind;
	requires worldwindx;
	requires jogl.all;
	requires gluegen.rt;
	requires org.locationtech.jts;
	// GeoTools modules are not modularized, so we need to use automatic modules
	// These will be available through the classpath dependencies
	requires java.sql;
	// requires java.persistence; // Commented out - using javax.persistence-api dependency instead
	requires lombok;
	requires com.google.api.client;
	requires com.google.zxing;
	requires org.apache.pdfbox;

	
	opens com.ursulagis.desktop.api;
	opens com.ursulagis.desktop.dao;
	opens com.ursulagis.desktop.gui;
	opens com.ursulagis.desktop.tasks;
	opens com.ursulagis.desktop.utils;
	
	// Equivalent to --add-opens JVM directives
	opens javafx.controls to com.ursulagis.desktop;
	opens javafx.graphics to com.ursulagis.desktop;
	opens javafx.base to com.ursulagis.desktop;	
}