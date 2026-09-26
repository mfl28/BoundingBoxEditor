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

import com.github.mfl28.boundingboxeditor.model.io.restclients.RestRequests.ServerErrorMessages;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.github.mfl28.boundingboxeditor.model.io.restclients.RestRequests.createTarget;
import static com.github.mfl28.boundingboxeditor.model.io.restclients.RestRequests.readEntity;
import static com.github.mfl28.boundingboxeditor.model.io.restclients.RestRequests.toRequestException;

/**
 * The client of a <a href="https://github.com/Lightning-AI/LitServe">LitServe</a> server. LitServe does not fix a
 * request or response format, so the server's {@code LitAPI} is expected to read the image from the multipart field
 * {@value #DATA_BODY_PART_NAME} and to respond with the same prediction format as Torch serve's object detector.
 */
public class LitServeRestClient implements BoundingBoxPredictorClient {
    static final String DATA_BODY_PART_NAME = "data";
    static final String API_KEY_HEADER_NAME = "X-API-Key";
    // LitServe only passes a multipart part to the LitAPI as a file if the part has a file name.
    private static final String DATA_BODY_PART_FILE_NAME = "image";
    private static final String HEALTH_RESOURCE_NAME = "health";
    private static final String LIT_SERVE_NAME = "LitServe";
    private static final String NO_MODEL_LIST_ERROR_MESSAGE = "LitServe does not provide a model list.";
    private static final String SERVER_NOT_READY_ERROR_MESSAGE = "LitServe server is not ready yet.";
    private static final String INVALID_API_KEY_ERROR_MESSAGE = "Missing or invalid API key.";
    private static final ServerErrorMessages PREDICTION_ERROR_MESSAGES = new ServerErrorMessages(
            "Invalid inference address or port.",
            "Could not connect to inference server.",
            "Inference server did not respond in time.",
            "Could not get prediction from inference server.",
            "Invalid inference server response format for the prediction endpoint.");
    private static final ServerErrorMessages HEALTH_CHECK_ERROR_MESSAGES = new ServerErrorMessages(
            "Invalid inference address or port.",
            "Could not connect to inference server.",
            "Inference server did not respond in time.",
            "Could not check the inference server's health.",
            "Invalid inference server response format for resource \"" + HEALTH_RESOURCE_NAME + "\".");
    private final Client client;
    private final BoundingBoxPredictorClientConfig clientConfig;

    public LitServeRestClient(Client client, BoundingBoxPredictorClientConfig clientConfig) {
        this.client = client;
        this.clientConfig = clientConfig;
    }

    @Override
    public List<BoundingBoxPredictionEntry> predict(InputStream input) throws PredictionClientException {
        final Invocation.Builder invocationBuilder;

        try {
            invocationBuilder = withApiKey(createTarget(client, clientConfig.getInferenceAddress(),
                    PREDICTION_ERROR_MESSAGES)
                    .path(clientConfig.getPredictionPath())
                    .request(MediaType.APPLICATION_JSON));
        } catch(IllegalArgumentException | IllegalStateException | ProcessingException e) {
            throw toRequestException(e, PREDICTION_ERROR_MESSAGES);
        }

        final Response response;

        try(final MultiPart multiPart = new FormDataMultiPart()
                .bodyPart(new StreamDataBodyPart(DATA_BODY_PART_NAME, input, DATA_BODY_PART_FILE_NAME))) {
            response = invocationBuilder.post(Entity.entity(multiPart, multiPart.getMediaType()));
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException | IOException e) {
            throw toRequestException(e, PREDICTION_ERROR_MESSAGES);
        }

        throwIfUnauthorized(response);

        return readEntity(response, r -> r.readEntity(RestRequests.PREDICTIONS_TYPE), PREDICTION_ERROR_MESSAGES);
    }

    @Override
    public List<ModelEntry> models() throws PredictionClientException {
        throw new PredictionClientException(NO_MODEL_LIST_ERROR_MESSAGE);
    }

    @Override
    public void checkConnection() throws PredictionClientException {
        final Response response;

        try {
            response = withApiKey(createTarget(client, clientConfig.getInferenceAddress(),
                    HEALTH_CHECK_ERROR_MESSAGES)
                    .path(HEALTH_RESOURCE_NAME)
                    .request(MediaType.TEXT_PLAIN))
                    .get();
        } catch(ProcessingException | IllegalArgumentException | IllegalStateException e) {
            throw toRequestException(e, HEALTH_CHECK_ERROR_MESSAGES);
        }

        throwIfUnauthorized(response);

        if(response.getStatus() == Response.Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            response.close();
            throw new PredictionClientException(SERVER_NOT_READY_ERROR_MESSAGE);
        }

        readEntity(response, r -> null, HEALTH_CHECK_ERROR_MESSAGES);
    }

    @Override
    public String getName() {
        return LIT_SERVE_NAME;
    }

    private Invocation.Builder withApiKey(Invocation.Builder invocationBuilder) {
        final String apiKey = clientConfig.getApiKey();

        if(apiKey != null && !apiKey.isBlank()) {
            return invocationBuilder.header(API_KEY_HEADER_NAME, apiKey);
        }

        return invocationBuilder;
    }

    private static void throwIfUnauthorized(Response response) throws PredictionClientException {
        final int status = response.getStatus();

        if(status == Response.Status.UNAUTHORIZED.getStatusCode()
                || status == Response.Status.FORBIDDEN.getStatusCode()) {
            response.close();
            throw new PredictionClientException(INVALID_API_KEY_ERROR_MESSAGE);
        }
    }
}
