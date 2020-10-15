package com.github.mfl28.boundingboxeditor.model.io;

import javafx.beans.property.*;

public class BoundingBoxPredictorClientConfig {
    private final ObjectProperty<BoundingBoxPredictorClient.ServiceType> serviceName = new SimpleObjectProperty<>(
            BoundingBoxPredictorClient.ServiceType.TORCH_SERVE);
    private final StringProperty inferenceUrl = new SimpleStringProperty("http://localhost");
    private final IntegerProperty inferencePort = new SimpleIntegerProperty(8080);
    private final StringProperty managementUrl = new SimpleStringProperty("http://localhost");
    private final IntegerProperty managementPort = new SimpleIntegerProperty(8081);
    private final StringProperty inferenceModelName = new SimpleStringProperty();

    public String getInferenceAddress() {
        return inferenceUrl.get() + ":" + inferencePort.get();
    }

    public String getManagementAddress() {
        return managementUrl.get() + ":" + managementPort.get();
    }

    public int getInferencePort() {
        return inferencePort.get();
    }

    public void setInferencePort(int inferencePort) {
        this.inferencePort.set(inferencePort);
    }

    public IntegerProperty inferencePortProperty() {
        return inferencePort;
    }

    public int getManagementPort() {
        return managementPort.get();
    }

    public void setManagementPort(int managementPort) {
        this.managementPort.set(managementPort);
    }

    public IntegerProperty managementPortProperty() {
        return managementPort;
    }

    public String getInferenceUrl() {
        return inferenceUrl.get();
    }

    public void setInferenceUrl(String inferenceUrl) {
        this.inferenceUrl.set(inferenceUrl);
    }

    public StringProperty inferenceUrlProperty() {
        return inferenceUrl;
    }

    public String getManagementUrl() {
        return managementUrl.get();
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl.set(managementUrl);
    }

    public StringProperty managementUrlProperty() {
        return managementUrl;
    }

    public String getInferenceModelName() {
        return inferenceModelName.get();
    }

    public void setInferenceModelName(String inferenceModelName) {
        this.inferenceModelName.set(inferenceModelName);
    }

    public StringProperty inferenceModelNameProperty() {
        return inferenceModelName;
    }

    public BoundingBoxPredictorClient.ServiceType getServiceType() {
        return serviceName.get();
    }

    public ObjectProperty<BoundingBoxPredictorClient.ServiceType> serviceNameProperty() {
        return serviceName;
    }

    public void setServiceName(BoundingBoxPredictorClient.ServiceType serviceType) {
        this.serviceName.set(serviceType);
    }
}
