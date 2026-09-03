package com.ursulagis.desktop.tasks.procesar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.api.feature.simple.SimpleFeature;

import com.ursulagis.desktop.dao.Labor;
import com.ursulagis.desktop.dao.LaborItem;
import com.ursulagis.desktop.tasks.ProcessMapTask;
import com.ursulagis.desktop.utils.ProyectionConstants;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.logging.Logger;

/**
 * Applies a gain factor to the deviation of each item from the layer mean.
 * newValue = mean + gain * (value - mean)
 */
public class AccentuateMeanLaborMapTask extends ProcessMapTask<LaborItem, Labor<LaborItem>> {
	private static final Logger logger = Logger.getLogger(AccentuateMeanLaborMapTask.class.getName());

	private final double gain;

	public AccentuateMeanLaborMapTask(Labor<LaborItem> labor, Double gain) {
		super(labor);
		this.gain = gain != null ? gain : 1.0;
		labor.setNombre(labor.getNombre() + " gain");
	}

	@Override
	protected void doProcess() throws IOException {
		double mean = computeAreaWeightedMean();
		logger.fine("mean=" + mean + " gain=" + gain);

		int initSize = labor.outCollection.size();
		final DoubleProperty done = new SimpleDoubleProperty(0);
		SimpleFeature[] arrayF = new SimpleFeature[initSize];
		labor.outCollection.toArray(arrayF);
		List<SimpleFeature> features = Arrays.asList(arrayF);

		List<SimpleFeature> processedFeatures = features.parallelStream().collect(
				() -> new ArrayList<SimpleFeature>(),
				(list, pf) -> {
					try {
						LaborItem item = labor.constructFeatureContainerStandar(pf, false);
						double amount = item.getAmount();
						double newAmount = mean + gain * (amount - mean);
						item.setAmount(newAmount);

						SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(labor.getType());
						SimpleFeature f = item.getFeature(fBuilder);
						list.add(f);

						done.set(done.get() + 1);
						updateProgress(done.get(), initSize);
					} catch (Exception e) {
						logger.warning("error in accentuateFromMean");
						e.printStackTrace();
					}
				},
				(list1, list2) -> list1.addAll(list2));

		DefaultFeatureCollection newOutCollection = new DefaultFeatureCollection("internal", labor.getType());
		newOutCollection.addAll(processedFeatures);

		labor.clearCache();
		if (initSize != newOutCollection.size()) {
			logger.warning("lost elements accentuating mean. init=" + initSize + " end=" + newOutCollection.size());
		}
		labor.setOutCollection(newOutCollection);
		featureCount = labor.outCollection.size();

		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(featureCount, featureCount);
	}

	private double computeAreaWeightedMean() {
		double sumWeighted = 0.0;
		double sumArea = 0.0;
		SimpleFeature[] arrayF = new SimpleFeature[labor.outCollection.size()];
		labor.outCollection.toArray(arrayF);
		for (SimpleFeature pf : arrayF) {
			LaborItem item = labor.constructFeatureContainerStandar(pf, false);
			double area = ProyectionConstants.A_HAS(item.getGeometry().getArea());
			if (area > 0) {
				sumWeighted += item.getAmount() * area;
				sumArea += area;
			}
		}
		return sumArea > 0 ? sumWeighted / sumArea : 0.0;
	}

	@Override
	protected int getAmountMin() {
		return 0;
	}

	@Override
	protected int gerAmountMax() {
		return 0;
	}
}
