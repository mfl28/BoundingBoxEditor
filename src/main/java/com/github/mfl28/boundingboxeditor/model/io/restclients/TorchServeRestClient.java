/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.google.gson.JsonSyntaxException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

public class TorchServeRestClient implements BoundingBoxPredictorClient {
    private static final String MODELS_RESOURCE_NAME = "models";
    private static final String SERVER_MODELS_READ_ERROR_MESSAGE =
            "Could not fetch models from management server.";
    private static final String DATA_BODY_PART_NAME = "data";
    private static final String PREDICTIONS_RESOURCE_NAME = "predictions";
    private static final String SERVER_PREDICTION_POST_ERROR_MESSAGE =
            "Could not get prediction from inference server.";
    private static final String MANAGEMENT_ADDRESS_ERROR_MESSAGE = "Invalid management address or port.";
    private static final String MANAGEMENT_SERVER_CONNECTION_ERROR_MESSAGE = "Could not connect to management server.";
    private static final String MANAGEMENT_SERVER_MODELS_RESPONSE_FORMAT_ERROR_MESSAGE = "Invalid management server " +
            "response format for resource \"" + MODELS_RESOURCE_NAME + "\".";
    private static final String INFERENCE_SERVER_PREDICTION_RESPONSE_FORMAT_ERROR_MESSAGE = "Invalid inference server "
            + "response format for resource \"" + PREDICTIONS_RESOURCE_NAME + "\".";
    private static final String TORCH_SERVE_NAME = "Torch serve";
    private static final String INFERENCE_ADDRESS_ERROR_MESSAGE = "Invalid inference address or port.";
    private static final String INFERENCE_SERVER_CONNECTION_ERROR_MESSAGE = "Could not connect to inference server.";
    private static final String SERVER_ERROR_REASON = " Reason: ";
    private final Client client;
    private final BoundingBoxPredictorClientConfig clientConfig;

    public TorchServeRestClient(Client client, BoundingBoxPredictorClientConfig clientConfig) {
        this.client = client;
        this.clientConfig = clientConfig;
    }

    @Override
    public List<BoundingBoxPredictionEntry> predict(InputStream input) throws PredictionClientException {
        WebTarget predictionTarget;

        try {
            predictionTarget = client.target(clientConfig.getInferenceAddress());
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new PredictionClientException(INFERENCE_ADDRESS_ERROR_MESSAGE);
        }

        Invocation.Builder invocationBuilder;

        try {
            invocationBuilder = predictionTarget.path(PREDICTIONS_RESOURCE_NAME)
                                                .path(clientConfig.getInferenceModelName())
                                                .request(MediaType.APPLICATION_JSON);
        } catch(IllegalArgumentException | IllegalStateException | ProcessingException e) {
            if(e.getCause() instanceof ConnectException) {
                throw new PredictionClientException(INFERENCE_SERVER_CONNECTION_ERROR_MESSAGE);
            } else {
                throw new PredictionClientException(SERVER_PREDICTION_POST_ERROR_MESSAGE);
            }
        }

        Response response;

        try(final MultiPart multiPart = new FormDataMultiPart()
                .bodyPart(new StreamDataBodyPart(DATA_BODY_PART_NAME, input))) {
            response = invocationBuilder.post(Entity.entity(multiPart, multiPart.getMediaType()));
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException | IOException e) {
            if(e.getCause() instanceof ConnectException) {
                throw new PredictionClientException(INFERENCE_SERVER_CONNECTION_ERROR_MESSAGE);
            } else {
                throw new PredictionClientException(SERVER_PREDICTION_POST_ERROR_MESSAGE);
            }
        }

        if(!response.getStatusInfo().equals(Response.Status.OK)) {
            final String reason = response.getStatusInfo().getReasonPhrase();
            response.close();
            throw new PredictionClientException(SERVER_PREDICTION_POST_ERROR_MESSAGE + SERVER_ERROR_REASON + reason);
        }

        try {
            return response.readEntity(new GenericType<>() {});
        } catch(ProcessingException | IllegalStateException e) {
            throw new PredictionClientException(SERVER_PREDICTION_POST_ERROR_MESSAGE);
        } catch(JsonSyntaxException e) {
            throw new PredictionClientException(INFERENCE_SERVER_PREDICTION_RESPONSE_FORMAT_ERROR_MESSAGE);
        } finally {
            response.close();
        }
    }

    @Override
    public List<ModelEntry> models() throws PredictionClientException {
        WebTarget managementTarget;

        try {
            managementTarget = client.target(clientConfig.getManagementAddress());
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new PredictionClientException(MANAGEMENT_ADDRESS_ERROR_MESSAGE);
        }

        Response response;

        try {
            response = managementTarget
                    .path(MODELS_RESOURCE_NAME)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException e) {
            if(e.getCause() instanceof ConnectException) {
                throw new PredictionClientException(MANAGEMENT_SERVER_CONNECTION_ERROR_MESSAGE);
            } else {
                throw new PredictionClientException(SERVER_MODELS_READ_ERROR_MESSAGE);
            }
        }

        if(!response.getStatusInfo().equals(Response.Status.OK)) {
            final String reason = response.getStatusInfo().getReasonPhrase();
            response.close();
            throw new PredictionClientException(SERVER_MODELS_READ_ERROR_MESSAGE + SERVER_ERROR_REASON + reason);
        }

        try {
            return response.readEntity(ModelsWrapper.class).getModels();
        } catch(ProcessingException | IllegalStateException e) {
            throw new PredictionClientException(SERVER_MODELS_READ_ERROR_MESSAGE);
        } catch(JsonSyntaxException e) {
            throw new PredictionClientException(MANAGEMENT_SERVER_MODELS_RESPONSE_FORMAT_ERROR_MESSAGE);
        } finally {
            response.close();
        }
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
