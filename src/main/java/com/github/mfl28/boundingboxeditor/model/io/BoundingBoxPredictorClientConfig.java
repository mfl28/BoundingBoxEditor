package com.github.mfl28.boundingboxeditor.model.io;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BoundingBoxPredictorClientConfig {
    private final ObjectProperty<BoundingBoxPredictorClient.ServiceType> serviceName = new SimpleObjectProperty<>(
            BoundingBoxPredictorClient.ServiceType.TORCH_SERVE);
    private final StringProperty inferenceAddress = new SimpleStringProperty("http://localhost:8080");
    private final StringProperty managementAddress = new SimpleStringProperty("http://localhost:8081");
    private final StringProperty inferenceModelName = new SimpleStringProperty();

    public String getInferenceAddress() {
        return inferenceAddress.get();
    }

    public StringProperty inferenceAddressProperty() {
        return inferenceAddress;
    }

    public void setInferenceAddress(String inferenceAddress) {
        this.inferenceAddress.set(inferenceAddress);
    }

    public String getManagementAddress() {
        return managementAddress.get();
    }

    public StringProperty managementAddressProperty() {
        return managementAddress;
    }

    public void setManagementAddress(String managementAddress) {
        this.managementAddress.set(managementAddress);
    }

    public String getInferenceModelName() {
        return inferenceModelName.get();
    }

    public StringProperty inferenceModelNameProperty() {
        return inferenceModelName;
    }

    public void setInferenceModelName(String inferenceModelName) {
        this.inferenceModelName.set(inferenceModelName);
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
