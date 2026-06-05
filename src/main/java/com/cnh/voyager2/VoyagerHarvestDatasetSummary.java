package com.cnh.voyager2;

/**
 * Metadata for a harvest dataset on a Voyager 2 card.
 * {@link #selectionKey} uniquely identifies a dataset (fieldSuid|taskSuid|datasetId).
 */
public class VoyagerHarvestDatasetSummary {

    private String selectionKey;
    private String datasetId;
    private String growerName;
    private String farmName;
    private String fieldName;
    private String taskName;
    private String date;

    public String getSelectionKey() {
        return selectionKey;
    }

    public void setSelectionKey(String selectionKey) {
        this.selectionKey = selectionKey;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getGrowerName() {
        return growerName;
    }

    public void setGrowerName(String growerName) {
        this.growerName = growerName;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDisplayLabel() {
        return String.format("%s | %s | %s | %s | %s | %s",
                nullToEmpty(taskName),
                nullToEmpty(fieldName),
                nullToEmpty(datasetId),
                nullToEmpty(growerName),
                nullToEmpty(farmName),
                nullToEmpty(date));
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
