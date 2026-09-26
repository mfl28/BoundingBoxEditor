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

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.mfl28.boundingboxeditor.model.io.restclients.RestRequests.ServerErrorMessages;

import static com.github.mfl28.boundingboxeditor.model.io.restclients.RestRequests.createTarget;
import static com.github.mfl28.boundingboxeditor.model.io.restclients.RestRequests.readEntity;
import static com.github.mfl28.boundingboxeditor.model.io.restclients.RestRequests.toRequestException;

public class TorchServeRestClient implements BoundingBoxPredictorClient {
    private static final String MODELS_RESOURCE_NAME = "models";
    private static final String DATA_BODY_PART_NAME = "data";
    private static final String PREDICTIONS_RESOURCE_NAME = "predictions";
    private static final String TORCH_SERVE_NAME = "Torch serve";
    private static final ServerErrorMessages INFERENCE_SERVER_ERROR_MESSAGES = new ServerErrorMessages(
            "Invalid inference address or port.",
            "Could not connect to inference server.",
            "Inference server did not respond in time.",
            "Could not get prediction from inference server.",
            "Invalid inference server response format for resource \"" + PREDICTIONS_RESOURCE_NAME + "\".");
    private static final ServerErrorMessages MANAGEMENT_SERVER_ERROR_MESSAGES = new ServerErrorMessages(
            "Invalid management address or port.",
            "Could not connect to management server.",
            "Management server did not respond in time.",
            "Could not fetch models from management server.",
            "Invalid management server response format for resource \"" + MODELS_RESOURCE_NAME + "\".");
    private static final String PING_RESOURCE_NAME = "ping";
    private static final ServerErrorMessages HEALTH_CHECK_ERROR_MESSAGES = new ServerErrorMessages(
            "Invalid inference address or port.",
            "Could not connect to inference server.",
            "Inference server did not respond in time.",
            "Inference server is not healthy.",
            "Invalid inference server response format for resource \"" + PING_RESOURCE_NAME + "\".");
    private final Client client;
    private final BoundingBoxPredictorClientConfig clientConfig;

    public TorchServeRestClient(Client client, BoundingBoxPredictorClientConfig clientConfig) {
        this.client = client;
        this.clientConfig = clientConfig;
    }

    @Override
    public List<BoundingBoxPredictionEntry> predict(InputStream input) throws PredictionClientException {
        final WebTarget predictionTarget =
                createTarget(client, clientConfig.getInferenceAddress(), INFERENCE_SERVER_ERROR_MESSAGES);

        Invocation.Builder invocationBuilder;

        try {
            invocationBuilder = predictionTarget.path(PREDICTIONS_RESOURCE_NAME)
                                                .path(clientConfig.getInferenceModelName())
                                                .request(MediaType.APPLICATION_JSON);
        } catch(IllegalArgumentException | IllegalStateException | ProcessingException e) {
            throw toRequestException(e, INFERENCE_SERVER_ERROR_MESSAGES);
        }

        Response response;

        try(final MultiPart multiPart = new FormDataMultiPart()
                .bodyPart(new StreamDataBodyPart(DATA_BODY_PART_NAME, input))) {
            response = invocationBuilder.post(Entity.entity(multiPart, multiPart.getMediaType()));
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException | IOException e) {
            throw toRequestException(e, INFERENCE_SERVER_ERROR_MESSAGES);
        }

        return readEntity(response, r -> r.readEntity(RestRequests.PREDICTIONS_TYPE), INFERENCE_SERVER_ERROR_MESSAGES);
    }

    @Override
    public List<ModelEntry> models() throws PredictionClientException {
        final WebTarget managementTarget =
                createTarget(client, clientConfig.getManagementAddress(), MANAGEMENT_SERVER_ERROR_MESSAGES);

        Response response;

        try {
            response = managementTarget
                    .path(MODELS_RESOURCE_NAME)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException e) {
            throw toRequestException(e, MANAGEMENT_SERVER_ERROR_MESSAGES);
        }

        return readEntity(response, r -> r.readEntity(ModelsWrapper.class).getModels(),
                MANAGEMENT_SERVER_ERROR_MESSAGES);
    }

    @Override
    public void checkConnection() throws PredictionClientException {
        final WebTarget inferenceTarget =
                createTarget(client, clientConfig.getInferenceAddress(), HEALTH_CHECK_ERROR_MESSAGES);

        Response response;

        try {
            response = inferenceTarget.path(PING_RESOURCE_NAME).request(MediaType.APPLICATION_JSON).get();
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException e) {
            throw toRequestException(e, HEALTH_CHECK_ERROR_MESSAGES);
        }

        readEntity(response, r -> null, HEALTH_CHECK_ERROR_MESSAGES);
    }

    @Override
    public String getName() {
        return TORCH_SERVE_NAME;
    }

    public static class ModelsWrapper {
        private List<ModelEntry> models = new ArrayList<>();

        public ModelsWrapper() {}

        public ModelsWrapper(List<ModelEntry> modelEntries) {
            this.models = modelEntries;
        }

        public List<ModelEntry> getModels() {
            return models;
        }
    }
}
