/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import jakarta.ws.rs.client.Client;
import java.io.InputStream;
import java.util.List;

public interface BoundingBoxPredictorClient {
    static BoundingBoxPredictorClient create(Client client, BoundingBoxPredictorClientConfig clientConfig) {
        return switch(clientConfig.getServiceType()) {
            case TORCH_SERVE -> new TorchServeRestClient(client, clientConfig);
            case LIT_SERVE -> new LitServeRestClient(client, clientConfig);
        };
    }

    List<BoundingBoxPredictionEntry> predict(InputStream input) throws PredictionClientException;

    List<ModelEntry> models() throws PredictionClientException;

    /**
     * Checks that the inference server is reachable and ready to make predictions.
     *
     * @throws PredictionClientException if the server is not reachable or not ready
     */
    void checkConnection() throws PredictionClientException;

    String getName();

    /**
     * The supported inference servers.
     */
    enum ServiceType {
        TORCH_SERVE("TorchServe", 8080),
        LIT_SERVE("LitServe", 8000);

        private final String displayName;
        private final int defaultInferencePort;

        ServiceType(String displayName, int defaultInferencePort) {
            this.displayName = displayName;
            this.defaultInferencePort = defaultInferencePort;
        }

        /**
         * Returns the port the server listens on for predictions by default.
         *
         * @return the default port
         */
        public int getDefaultInferencePort() {
            return defaultInferencePort;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
