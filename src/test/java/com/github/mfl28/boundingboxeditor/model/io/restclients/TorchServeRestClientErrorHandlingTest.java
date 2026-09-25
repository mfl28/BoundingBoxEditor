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

import com.google.gson.JsonSyntaxException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests how the Torch serve client turns failures of the JAX-RS client into the error messages shown to the user,
 * using a mocked {@link Client}.
 */
@Tag("unit")
class TorchServeRestClientErrorHandlingTest {
    private final Client client = mock(Client.class);
    private final WebTarget target = mock(WebTarget.class);
    private final Invocation.Builder invocationBuilder = mock(Invocation.Builder.class);
    private final Response response = mock(Response.class);
    private final BoundingBoxPredictorClientConfig clientConfig = new BoundingBoxPredictorClientConfig();
    private final TorchServeRestClient restClient = new TorchServeRestClient(client, clientConfig);

    @BeforeEach
    void setUp() {
        clientConfig.setInferenceModelName("model");

        when(client.target(anyString())).thenReturn(target);
        when(target.path(anyString())).thenReturn(target);
        when(target.request(MediaType.APPLICATION_JSON)).thenReturn(invocationBuilder);
        when(invocationBuilder.get()).thenReturn(response);
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
    }

    @Test
    void onModelsRequested_WhenServerRespondsWithModels_ShouldReturnModelsAndCloseResponse()
            throws PredictionClientException {
        final List<ModelEntry> models = List.of(new ModelEntry("model", "model.mar"));
        when(response.readEntity(TorchServeRestClient.ModelsWrapper.class))
                .thenReturn(new TorchServeRestClient.ModelsWrapper(models));

        assertEquals(models, restClient.models());
        verify(client).target("http://localhost:8081");
        verify(target).path("models");
        verify(response).close();
    }

    @Test
    void onPredictionRequested_WhenServerRespondsWithPredictions_ShouldReturnPredictionsAndCloseResponse()
            throws PredictionClientException {
        final List<BoundingBoxPredictionEntry> predictions =
                List.of(new BoundingBoxPredictionEntry(Map.of("foo", List.of(1.0, 2.0, 3.0, 4.0)), 0.9));
        doReturn(predictions).when(response).readEntity(any(GenericType.class));

        assertEquals(predictions, restClient.predict(new ByteArrayInputStream(new byte[0])));
        verify(client).target("http://localhost:8080");
        verify(target).path("predictions");
        verify(target).path("model");
        verify(response).close();
    }

    @Test
    void onRequest_WhenAddressIsInvalid_ShouldReportInvalidAddress() {
        when(client.target(anyString())).thenThrow(new IllegalArgumentException());

        assertModelsError("Invalid management address or port.");
        assertPredictionError("Invalid inference address or port.");
    }

    @Test
    void onRequest_WhenServerIsNotReachable_ShouldReportConnectionError() {
        when(invocationBuilder.get()).thenThrow(new ProcessingException(new ConnectException()));
        when(invocationBuilder.post(any(Entity.class))).thenThrow(new ProcessingException(new ConnectException()));

        assertModelsError("Could not connect to management server.");
        assertPredictionError("Could not connect to inference server.");
    }

    @Test
    void onRequest_WhenRequestFails_ShouldReportRequestError() {
        when(invocationBuilder.get()).thenThrow(new ProcessingException("foo"));
        when(invocationBuilder.post(any(Entity.class))).thenThrow(new ProcessingException("foo"));

        assertModelsError("Could not fetch models from management server.");
        assertPredictionError("Could not get prediction from inference server.");
    }

    @Test
    void onPredictionRequested_WhenRequestCannotBeBuilt_ShouldReportRequestError() {
        when(target.request(MediaType.APPLICATION_JSON)).thenThrow(new IllegalStateException());

        assertPredictionError("Could not get prediction from inference server.");
    }

    @Test
    void onRequest_WhenServerRespondsWithErrorStatus_ShouldReportReasonAndCloseResponse() {
        when(response.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        assertModelsError("Could not fetch models from management server. Reason: Not Found");
        assertPredictionError("Could not get prediction from inference server. Reason: Not Found");
        verify(response, times(2)).close();
    }

    @Test
    void onRequest_WhenResponseCannotBeRead_ShouldReportRequestErrorAndCloseResponse() {
        when(response.readEntity(TorchServeRestClient.ModelsWrapper.class)).thenThrow(new ProcessingException("bar"));
        when(response.readEntity(any(GenericType.class))).thenThrow(new ProcessingException("bar"));

        assertModelsError("Could not fetch models from management server.");
        assertPredictionError("Could not get prediction from inference server.");
        verify(response, times(2)).close();
    }

    @Test
    void onRequest_WhenResponseHasInvalidFormat_ShouldReportFormatErrorAndCloseResponse() {
        when(response.readEntity(TorchServeRestClient.ModelsWrapper.class)).thenThrow(new JsonSyntaxException("bar"));
        when(response.readEntity(any(GenericType.class))).thenThrow(new JsonSyntaxException("bar"));

        assertModelsError("Invalid management server response format for resource \"models\".");
        assertPredictionError("Invalid inference server response format for resource \"predictions\".");
        verify(response, times(2)).close();
    }

    private void assertModelsError(String expectedMessage) {
        final PredictionClientException exception = assertThrows(PredictionClientException.class, restClient::models);
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void assertPredictionError(String expectedMessage) {
        final PredictionClientException exception = assertThrows(PredictionClientException.class,
                () -> restClient.predict(new ByteArrayInputStream(new byte[0])));
        assertEquals(expectedMessage, exception.getMessage());
    }
}
