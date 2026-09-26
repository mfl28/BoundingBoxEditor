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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the LitServe client's requests on the wire against a local HTTP server standing in for LitServe.
 */
@Tag("unit")
class LitServeRestClientHttpTest {
    private static final String PREDICTIONS_JSON =
            "[{\"foo\": [1.0, 2.0, 3.0, 4.0], \"score\": 0.9}, {\"bar\": [5.0, 6.0, 7.0, 8.0], \"score\": 0.4}]";
    private final AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();
    private HttpServer server;
    private ExecutorService serverExecutor;
    private Client client;
    private BoundingBoxPredictorClientConfig clientConfig;

    @BeforeEach
    void setUp() throws IOException {
        // Bound explicitly to IPv4, like the client's address below (see TorchServeRestClientTest).
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.createContext("/predict", exchange -> respond(exchange, 200, PREDICTIONS_JSON));
        server.createContext("/health", exchange -> respond(exchange, 200, "ok"));
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(2000);
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, PREDICTIONS_JSON);
        });
        server.start();

        client = ClientBuilder.newBuilder()
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .register(MultiPartFeature.class)
                .register(GsonMessageBodyHandler.class)
                .build();

        clientConfig = new BoundingBoxPredictorClientConfig();
        clientConfig.setServiceType(BoundingBoxPredictorClient.ServiceType.LIT_SERVE);
        clientConfig.setInferenceUrl("http://127.0.0.1");
        clientConfig.setInferencePort(server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop(0);
        serverExecutor.shutdownNow();
    }

    @Test
    void onPredictionRequested_ShouldUploadImageAsFileAndParsePredictions() throws PredictionClientException {
        clientConfig.setApiKey("secret");

        final List<BoundingBoxPredictionEntry> predictions = BoundingBoxPredictorClient.create(client, clientConfig)
                .predict(new ByteArrayInputStream("image-bytes".getBytes(StandardCharsets.UTF_8)));

        assertEquals(List.of(
                new BoundingBoxPredictionEntry(Map.of("foo", List.of(1.0, 2.0, 3.0, 4.0)), 0.9),
                new BoundingBoxPredictionEntry(Map.of("bar", List.of(5.0, 6.0, 7.0, 8.0)), 0.4)), predictions);

        final RecordedRequest request = lastRequest.get();
        assertEquals("POST", request.method());
        assertEquals("/predict", request.path());
        assertEquals("secret", request.apiKey());
        assertTrue(request.contentType().startsWith("multipart/form-data"), request.contentType());
        // LitServe only hands a part to the LitAPI as a file if it has a file name.
        final String contentDisposition = request.body().lines()
                .filter(line -> line.startsWith("Content-Disposition: form-data;"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No multipart part in body: " + request.body()));
        assertTrue(contentDisposition.contains("name=\"data\""), contentDisposition);
        assertTrue(contentDisposition.contains("filename=\"image\""), contentDisposition);
        assertTrue(request.body().contains("image-bytes"), request.body());
    }

    @Test
    void onConnectionCheck_WhenServerHealthy_ShouldSucceed() throws PredictionClientException {
        BoundingBoxPredictorClient.create(client, clientConfig).checkConnection();

        final RecordedRequest request = lastRequest.get();
        assertEquals("GET", request.method());
        assertEquals("/health", request.path());
        assertNull(request.apiKey());
    }

    @Test
    void onPredictionRequested_WhenServerDoesNotRespondInTime_ShouldReportTimeout() {
        clientConfig.setPredictionPath("/slow");
        final BoundingBoxPredictorClient restClient = BoundingBoxPredictorClient.create(client, clientConfig);

        final PredictionClientException exception = assertThrows(PredictionClientException.class,
                () -> restClient.predict(new ByteArrayInputStream(new byte[0])));
        assertEquals("Inference server did not respond in time.", exception.getMessage());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        lastRequest.set(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("X-API-Key"),
                exchange.getRequestHeaders().getFirst("Content-Type"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1)));

        final byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, responseBytes.length);

        try(OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    private record RecordedRequest(String method, String path, String apiKey, String contentType, String body) {
    }
}
