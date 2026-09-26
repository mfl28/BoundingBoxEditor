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
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests the LitServe client's requests and error messages with a mocked JAX-RS {@link Client}.
 */
@Tag("unit")
class LitServeRestClientTest {
    private final Client client = mock(Client.class);
    private final WebTarget target = mock(WebTarget.class);
    private final Invocation.Builder invocationBuilder = mock(Invocation.Builder.class);
    private final Response response = mock(Response.class);
    private final BoundingBoxPredictorClientConfig clientConfig = new BoundingBoxPredictorClientConfig();
    private BoundingBoxPredictorClient restClient;

    @BeforeEach
    void setUp() {
        clientConfig.setServiceType(BoundingBoxPredictorClient.ServiceType.LIT_SERVE);
        clientConfig.setInferencePort(8000);
        restClient = BoundingBoxPredictorClient.create(client, clientConfig);

        when(client.target(anyString())).thenReturn(target);
        when(target.path(anyString())).thenReturn(target);
        when(target.request(anyString())).thenReturn(invocationBuilder);
        when(invocationBuilder.header(anyString(), any())).thenReturn(invocationBuilder);
        when(invocationBuilder.get()).thenReturn(response);
        when(invocationBuilder.post(any(Entity.class))).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.getStatus()).thenReturn(200);
    }

    @Test
    void onCreate_ShouldCreateClientForServiceType() {
        assertInstanceOf(LitServeRestClient.class, restClient);
        assertEquals("LitServe", restClient.getName());

        clientConfig.setServiceType(BoundingBoxPredictorClient.ServiceType.TORCH_SERVE);
        assertInstanceOf(TorchServeRestClient.class, BoundingBoxPredictorClient.create(client, clientConfig));
    }

    @Test
    void onPredictionRequested_ShouldPostToPredictionPathAndReturnPredictions() throws PredictionClientException {
        final List<BoundingBoxPredictionEntry> predictions =
                List.of(new BoundingBoxPredictionEntry(Map.of("foo", List.of(1.0, 2.0, 3.0, 4.0)), 0.9));
        doReturn(predictions).when(response).readEntity(any(GenericType.class));
        clientConfig.setPredictionPath("/detect");

        assertEquals(predictions, restClient.predict(new ByteArrayInputStream(new byte[0])));
        verify(client).target("http://localhost:8000");
        verify(target).path("/detect");
        verify(target).request(MediaType.APPLICATION_JSON);
        verify(invocationBuilder, never()).header(anyString(), any());
        verify(response).close();
    }

    @Test
    void onRequest_WhenApiKeySet_ShouldSendApiKeyHeader() throws PredictionClientException {
        doReturn(List.of()).when(response).readEntity(any(GenericType.class));
        clientConfig.setApiKey("secret");

        restClient.predict(new ByteArrayInputStream(new byte[0]));
        restClient.checkConnection();

        verify(invocationBuilder, times(2)).header("X-API-Key", "secret");
    }

    @Test
    void onRequest_WhenApiKeyBlank_ShouldNotSendApiKeyHeader() throws PredictionClientException {
        clientConfig.setApiKey("  ");

        restClient.checkConnection();

        verify(invocationBuilder, never()).header(anyString(), any());
    }

    @Test
    void onConnectionCheck_WhenServerHealthy_ShouldSucceedAndCloseResponse() throws PredictionClientException {
        restClient.checkConnection();

        verify(target).path("health");
        verify(response).close();
    }

    @Test
    void onConnectionCheck_WhenServerNotReady_ShouldReportNotReady() {
        when(response.getStatus()).thenReturn(503);
        when(response.getStatusInfo()).thenReturn(Response.Status.SERVICE_UNAVAILABLE);

        assertConnectionCheckError("LitServe server is not ready yet.");
        verify(response).close();
    }

    @Test
    void onRequest_WhenApiKeyRejected_ShouldReportInvalidApiKey() {
        when(response.getStatus()).thenReturn(401);
        when(response.getStatusInfo()).thenReturn(Response.Status.UNAUTHORIZED);

        assertConnectionCheckError("Missing or invalid API key.");
        assertPredictionError("Missing or invalid API key.");

        when(response.getStatus()).thenReturn(403);
        when(response.getStatusInfo()).thenReturn(Response.Status.FORBIDDEN);

        assertConnectionCheckError("Missing or invalid API key.");
        verify(response, times(3)).close();
    }

    @Test
    void onRequest_WhenServerRespondsWithOtherErrorStatus_ShouldReportReason() {
        when(response.getStatus()).thenReturn(404);
        when(response.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        assertConnectionCheckError("Could not check the inference server's health. Reason: Not Found");
        assertPredictionError("Could not get prediction from inference server. Reason: Not Found");
    }

    @Test
    void onRequest_WhenServerIsNotReachable_ShouldReportConnectionError() {
        when(invocationBuilder.get()).thenThrow(new ProcessingException(new ConnectException()));
        when(invocationBuilder.post(any(Entity.class))).thenThrow(new ProcessingException(new ConnectException()));

        assertConnectionCheckError("Could not connect to inference server.");
        assertPredictionError("Could not connect to inference server.");
    }

    @Test
    void onRequest_WhenServerDoesNotRespondInTime_ShouldReportTimeout() {
        when(invocationBuilder.get()).thenThrow(new ProcessingException(new SocketTimeoutException()));
        when(invocationBuilder.post(any(Entity.class)))
                .thenThrow(new ProcessingException(new SocketTimeoutException()));

        assertConnectionCheckError("Inference server did not respond in time.");
        assertPredictionError("Inference server did not respond in time.");
    }

    @Test
    void onRequest_WhenAddressIsInvalid_ShouldReportInvalidAddress() {
        when(client.target(anyString())).thenThrow(new IllegalArgumentException());

        assertConnectionCheckError("Invalid inference address or port.");
        assertPredictionError("Invalid inference address or port.");
    }

    @Test
    void onRequest_WhenRequestCannotBeBuilt_ShouldReportRequestError() {
        when(target.request(anyString())).thenThrow(new IllegalStateException());

        assertConnectionCheckError("Could not check the inference server's health.");
        assertPredictionError("Could not get prediction from inference server.");
    }

    @Test
    void onPredictionRequested_WhenResponseHasInvalidFormat_ShouldReportFormatError() {
        when(response.readEntity(any(GenericType.class))).thenThrow(new JsonSyntaxException("bar"));

        assertPredictionError("Invalid inference server response format for the prediction endpoint.");
        verify(response).close();
    }

    @Test
    void onModelsRequested_ShouldReportThatThereIsNoModelList() {
        final PredictionClientException exception = assertThrows(PredictionClientException.class,
                restClient::models);
        assertEquals("LitServe does not provide a model list.", exception.getMessage());
        verifyNoInteractions(client);
    }

    private void assertConnectionCheckError(String expectedMessage) {
        final PredictionClientException exception = assertThrows(PredictionClientException.class,
                restClient::checkConnection);
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void assertPredictionError(String expectedMessage) {
        final PredictionClientException exception = assertThrows(PredictionClientException.class,
                () -> restClient.predict(new ByteArrayInputStream(new byte[0])));
        assertEquals(expectedMessage, exception.getMessage());
    }
}
