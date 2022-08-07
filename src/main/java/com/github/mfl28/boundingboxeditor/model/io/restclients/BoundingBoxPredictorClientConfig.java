/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.model.io.restclients;

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

    public int getManagementPort() {
        return managementPort.get();
    }

    public void setManagementPort(int managementPort) {
        this.managementPort.set(managementPort);
    }

    public String getInferenceUrl() {
        return inferenceUrl.get();
    }

    public void setInferenceUrl(String inferenceUrl) {
        this.inferenceUrl.set(inferenceUrl);
    }

    public String getManagementUrl() {
        return managementUrl.get();
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl.set(managementUrl);
    }

    public String getInferenceModelName() {
        return inferenceModelName.get();
    }

    public void setInferenceModelName(String inferenceModelName) {
        this.inferenceModelName.set(inferenceModelName);
    }

    public BoundingBoxPredictorClient.ServiceType getServiceType() {
        return serviceName.get();
    }
}
