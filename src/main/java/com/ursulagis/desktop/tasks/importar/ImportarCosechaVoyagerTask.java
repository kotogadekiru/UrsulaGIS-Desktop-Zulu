package com.ursulagis.desktop.tasks.importar;



import java.io.File;

import java.util.ArrayList;

import java.util.Collections;

import java.util.List;



import org.geotools.feature.DefaultFeatureCollection;

import org.geotools.feature.simple.SimpleFeatureBuilder;

import org.geotools.api.feature.simple.SimpleFeature;

import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;

import org.locationtech.jts.geom.GeometryFactory;

import org.locationtech.jts.geom.Point;

import org.locationtech.jts.geom.PrecisionModel;



import com.cnh.voyager2.CNHVoyager2;

import com.cnh.voyager2.CNHVoyager2Exception;

import com.cnh.voyager2.VoyagerHarvestDatasetSummary;

import com.cnh.voyager2.VoyagerHarvestSample;

import com.ursulagis.desktop.dao.Labor;

import com.ursulagis.desktop.gui.VoyagerHarvestDatasetDialog;

import com.ursulagis.desktop.dao.cosecha.CosechaLabor;

import com.ursulagis.desktop.dao.cosecha.CosechaLabor.CosechaLaborConstants;

import com.ursulagis.desktop.gui.JFXMain;

import com.ursulagis.desktop.tasks.ProgresibleTask;

import com.ursulagis.desktop.utils.Voyager2NativeLoader;

import com.ursulagis.desktop.utils.Voyager2Settings;



/**

 * Imports harvest points from a Case IH Voyager 2 card (.vy1 directory) using the CNHVoyager2 Java wrapper.

 * The result is stored on {@link CosechaLabor#getInCollection()} for {@link ProcessHarvestMapTask}.

 */

public class ImportarCosechaVoyagerTask extends ProgresibleTask<CosechaLabor> {



    private final CosechaLabor labor;

    private final File cardDirectory;

    /** Optional selection keys (fieldSuid|taskSuid|datasetId); null to auto-resolve via dialog. */

    private final List<String> selectionKeys;

    private final Voyager2Settings settings;



    public ImportarCosechaVoyagerTask(CosechaLabor labor, File cardDirectory, List<String> selectionKeys) {

        this(labor, cardDirectory, selectionKeys, Voyager2Settings.fromConfig(JFXMain.config));

    }



    public ImportarCosechaVoyagerTask(

            CosechaLabor labor,

            File cardDirectory,

            List<String> selectionKeys,

            Voyager2Settings settings) {

        super();

        this.labor = labor;

        this.cardDirectory = cardDirectory;

        this.selectionKeys = selectionKeys;

        this.settings = settings;

        this.taskName = labor.getNombre() != null ? labor.getNombre() : cardDirectory.getName();

        updateTitle(taskName);

    }



    @Override

    protected CosechaLabor call() throws Exception {

        settings.validateForImport();

        System.out.println("Validating Voyager 2 settings");

        Voyager2NativeLoader.ensureLoaded(settings);

        System.out.println("Ensuring Voyager 2 native libraries are loaded");



        long cardHandle = 0;

        try {

            updateMessage("Initializing Voyager 2 SDK…");

            System.out.println("Initializing Voyager 2 SDK from " + settings.getSdkBasePath());

            CNHVoyager2.initialize(settings.getSdkBasePath());

            System.out.println("Creating Voyager 2 card with license key " + settings.getLicenseKey());

            cardHandle = CNHVoyager2.createCard(settings.getLicenseKey());



            updateMessage("Opening card…");

            CNHVoyager2.openCard(cardHandle, cardDirectory.getAbsolutePath());

            System.out.println("Card opened successfully");



            List<String> resolvedKeys = selectionKeys;

            if (resolvedKeys == null || resolvedKeys.isEmpty()) {

                resolvedKeys = resolveHarvestSelectionKeys(cardHandle);

            }

            if (resolvedKeys.isEmpty()) {

                System.out.println("No harvest dataset found on card: " + cardDirectory);

                throw new CNHVoyager2Exception("No harvest dataset found on card: " + cardDirectory);

            }



            List<VoyagerHarvestSample> samples = loadSamplesFromDatasets(cardHandle, resolvedKeys);

            System.out.println("Harvest samples loaded: " + samples.size()

                    + " from " + resolvedKeys.size() + " dataset(s)");

            if (samples.isEmpty()) {

                throw new CNHVoyager2Exception("Selected harvest datasets have no samples");

            }



            populateInCollection(samples);

            return labor;

        } finally {

            if (cardHandle != 0 && CNHVoyager2.isInitialized()) {

                try {

                    CNHVoyager2.releaseCard(cardHandle);

                } catch (CNHVoyager2Exception e) {

                    System.err.println("Failed to release Voyager card: " + e.getMessage());

                }

            }

            // Keep the Voyager SDK initialized for subsequent imports; shutting down the

            // .NET host after each card breaks the second initialize() in the same process.

        }

    }



    private List<String> resolveHarvestSelectionKeys(long cardHandle) throws CNHVoyager2Exception {

        List<VoyagerHarvestDatasetSummary> datasets = CNHVoyager2.listHarvestDatasets(cardHandle);

        if (datasets.isEmpty()) {

            return Collections.emptyList();

        }

        if (datasets.size() == 1) {

            String key = datasets.get(0).getSelectionKey();

            System.out.println("Single harvest dataset: " + key);

            return Collections.singletonList(key);

        }

        updateMessage("Selecting harvest datasets…");

        List<VoyagerHarvestDatasetSummary> chosen = VoyagerHarvestDatasetDialog.choose(datasets);

        if (chosen.isEmpty()) {

            throw new CNHVoyager2Exception("Harvest dataset selection cancelled");

        }

        List<String> keys = new ArrayList<>(chosen.size());

        for (VoyagerHarvestDatasetSummary summary : chosen) {

            keys.add(summary.getSelectionKey());

            System.out.println("Selected harvest dataset: " + summary.getSelectionKey());

        }

        return keys;

    }



    private List<VoyagerHarvestSample> loadSamplesFromDatasets(long cardHandle, List<String> keys)

            throws CNHVoyager2Exception {

        List<VoyagerHarvestSample> allSamples = new ArrayList<>();

        int datasetIndex = 0;

        for (String key : keys) {

            datasetIndex++;

            updateMessage("Reading harvest samples (" + datasetIndex + "/" + keys.size() + ")…");

            List<VoyagerHarvestSample> batch = CNHVoyager2.loadHarvestDatasetSamples(cardHandle, key);

            System.out.println("Dataset " + key + ": " + batch.size() + " samples");

            allSamples.addAll(batch);

        }

        return allSamples;

    }



    private void populateInCollection(List<VoyagerHarvestSample> samples) throws InterruptedException {

        SimpleFeatureType pointType = labor.getPointType();

        DefaultFeatureCollection collection = new DefaultFeatureCollection("voyager-harvest", pointType);

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(pointType);

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);



        int total = samples.size();

        int index = 0;

        for (VoyagerHarvestSample sample : samples) {

            checkCancelled();

            index++;

            updateProgress(index, total);



            Coordinate coordinate = new Coordinate(sample.getLongitude(), sample.getLatitude(), sample.getElevation());

            Point point = geometryFactory.createPoint(coordinate);



            Double yieldValue = sample.getCropFlow() != null ? sample.getCropFlow() : 0.0;



            builder.set(pointType.getGeometryDescriptor().getLocalName(), point);

            builder.set(Labor.COLUMNA_DISTANCIA, sample.getDistance());

            builder.set(Labor.COLUMNA_CURSO, sample.getHeading());

            builder.set(Labor.COLUMNA_ANCHO, sample.getWidth());

            builder.set(Labor.COLUMNA_ELEVACION, sample.getElevation());

            builder.set("Categoria", 0);

            builder.set(CosechaLaborConstants.COLUMNA_RENDIMIENTO, yieldValue);

            builder.set(CosechaLaborConstants.COLUMNA_DESVIO_REND, 0.0);

            builder.set(CosechaLaborConstants.COLUMNA_COSTO_LB_HA, 0.0);

            builder.set(CosechaLaborConstants.COLUMNA_COSTO_LB_TN, 0.0);

            builder.set(CosechaLaborConstants.COLUMNA_PRECIO, labor.getPrecioInsumo() != null ? labor.getPrecioInsumo() : 0.0);

            builder.set(CosechaLaborConstants.COLUMNA_IMPORTE_HA, 0.0);



            SimpleFeature feature = builder.buildFeature(String.valueOf(index));

            collection.add(feature);

        }



        labor.setInCollection(collection);

        labor.setInStore(null);

        featureCount = total;

    }

}


