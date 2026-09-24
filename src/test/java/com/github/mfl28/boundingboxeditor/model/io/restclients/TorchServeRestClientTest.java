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
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class TorchServeRestClientTest {
    private ServerSocket unresponsiveServer;
    private Client client;
    private BoundingBoxPredictorClientConfig clientConfig;

    @BeforeEach
    void setUp() throws IOException {
        // Connections are completed by the socket's backlog but no request is ever answered.
        // Bound explicitly to the IPv4 address used below: InetAddress.getLoopbackAddress() returns ::1 when
        // java.net.preferIPv6Addresses=system is set (as on CI), and the client's connection would be refused.
        unresponsiveServer = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));

        client = ClientBuilder.newBuilder()
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .register(MultiPartFeature.class)
                .register(GsonMessageBodyHandler.class)
                .build();

        clientConfig = new BoundingBoxPredictorClientConfig();
        clientConfig.setManagementUrl("http://127.0.0.1");
        clientConfig.setManagementPort(unresponsiveServer.getLocalPort());
        clientConfig.setInferenceUrl("http://127.0.0.1");
        clientConfig.setInferencePort(unresponsiveServer.getLocalPort());
        clientConfig.setInferenceModelName("model");
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        unresponsiveServer.close();
    }

    @Test
    void onModelsRequested_WhenServerDoesNotRespond_ShouldReportTimeout() {
        final TorchServeRestClient restClient = new TorchServeRestClient(client, clientConfig);

        PredictionClientException exception = assertThrows(PredictionClientException.class, restClient::models);
        assertEquals("Management server did not respond in time.", exception.getMessage());
    }

    @Test
    void onPredictionRequested_WhenServerDoesNotRespond_ShouldReportTimeout() {
        final TorchServeRestClient restClient = new TorchServeRestClient(client, clientConfig);

        PredictionClientException exception = assertThrows(PredictionClientException.class,
                () -> restClient.predict(new ByteArrayInputStream(new byte[]{1, 2, 3})));
        assertEquals("Inference server did not respond in time.", exception.getMessage());
    }
}
