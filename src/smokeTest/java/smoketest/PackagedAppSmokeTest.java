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
package smoketest;

import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictionEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.GsonMessageBodyHandler;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import javafx.beans.property.SimpleDoubleProperty;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs, inside the packaged app's Java runtime, code paths that only break there: the tests run on the classpath,
 * so they can't notice missing module declarations. E.g. the merged module (Jersey) once lacked
 * "requires java.logging", and every prediction failed in the installed app only.
 * <p>
 * Checks the REST client (Jersey, multipart upload, Gson parsing) against a local fake LitServe server, and the JSON
 * (Gson) and CSV (Jackson) annotation imports, which read their data classes by reflection. Exits with 1 on failure.
 */
public final class PackagedAppSmokeTest {
    private static final String PREDICTIONS_JSON = "[{\"cat\": [10.0, 20.0, 30.0, 40.0], \"score\": 0.9}]";

    private PackagedAppSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        checkPrediction();
        checkAnnotationImport(ImageAnnotationLoadStrategy.Type.JSON, "annotations.json", """
                [{"image": {"fileName": "a.jpg"}, "objects": [{"bndbox": {"minX": 0.1, "minY": 0.1, "maxX": 0.4,
                "maxY": 0.4}, "category": {"name": "cat", "color": "#C31D8F"}, "tags": []}]}]
                """);
        checkAnnotationImport(ImageAnnotationLoadStrategy.Type.CSV, "annotations.csv", """
                filename,width,height,class,xmin,ymin,xmax,ymax
                a.jpg,100,100,cat,10,10,40,40
                """);
        System.out.println("Packaged app smoke test passed.");
    }

    private static void checkPrediction() throws Exception {
        final AtomicReference<String> request = new AtomicReference<>();

        try(ServerSocket server = new ServerSocket(0, 1, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}))) {
            final Thread serverThread = Thread.ofVirtual().start(() -> answerOnce(server, request));
            // As InferenceController creates the client.
            final Client client = ClientBuilder.newBuilder()
                                               .register(MultiPartFeature.class)
                                               .register(GsonMessageBodyHandler.class)
                                               .build();

            try {
                final BoundingBoxPredictorClientConfig config = new BoundingBoxPredictorClientConfig();
                config.setServiceType(BoundingBoxPredictorClient.ServiceType.LIT_SERVE);
                config.setInferenceUrl("http://127.0.0.1");
                config.setInferencePort(server.getLocalPort());

                final List<BoundingBoxPredictionEntry> predictions = BoundingBoxPredictorClient.create(client, config)
                        .predict(new ByteArrayInputStream(new byte[]{1, 2, 3}));

                check(predictions.equals(List.of(new BoundingBoxPredictionEntry(
                        Map.of("cat", List.of(10.0, 20.0, 30.0, 40.0)), 0.9))), "Unexpected predictions: " + predictions);
            } finally {
                client.close();
            }

            serverThread.join();
            check(request.get() != null && request.get().startsWith("POST /predict")
                          && request.get().contains("filename="), "Unexpected prediction request: " + request.get());
        }

        System.out.println("Prediction: ok");
    }

    private static void checkAnnotationImport(ImageAnnotationLoadStrategy.Type type, String fileName, String content)
            throws IOException {
        final Path directory = Files.createTempDirectory("smoketest");

        try {
            final Path file = Files.writeString(directory.resolve(fileName), content);
            final ImageAnnotationImportResult result = ImageAnnotationLoadStrategy.createStrategy(type)
                    .load(file, Set.of("a.jpg"), new HashMap<>(), new SimpleDoubleProperty(0));

            check(result.getErrorTableEntries().isEmpty() && result.getNrSuccessfullyProcessedItems() == 1,
                  type + " import failed: " + result.getErrorTableEntries());
        } finally {
            Files.deleteIfExists(directory.resolve(fileName));
            Files.deleteIfExists(directory);
        }

        System.out.println(type.name().charAt(0) + type.name().substring(1).toLowerCase(Locale.ROOT) + " import: ok");
    }

    /**
     * Answers one HTTP request with the prediction JSON and records the request (line, headers and body).
     */
    private static void answerOnce(ServerSocket server, AtomicReference<String> request) {
        try(Socket socket = server.accept()) {
            final InputStream input = socket.getInputStream();
            final String head = readHead(input);
            final String body = new String(readBody(input, head), StandardCharsets.ISO_8859_1);
            request.set(head + body);

            final byte[] response = PREDICTIONS_JSON.getBytes(StandardCharsets.UTF_8);
            final OutputStream output = socket.getOutputStream();
            output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + response.length
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            output.write(response);
            output.flush();
        } catch(IOException e) {
            request.set("Server error: " + e);
        }
    }

    private static String readHead(InputStream input) throws IOException {
        final ByteArrayOutputStream head = new ByteArrayOutputStream();
        int matched = 0;

        // Reads up to and including the empty line that ends the headers.
        while(matched != 4) {
            final int value = input.read();

            if(value == -1) {
                throw new IOException("Connection closed in the headers.");
            }

            head.write(value);
            matched = (value == (matched % 2 == 0 ? '\r' : '\n')) ? matched + 1 : (value == '\r' ? 1 : 0);
        }

        return head.toString(StandardCharsets.US_ASCII);
    }

    private static byte[] readBody(InputStream input, String head) throws IOException {
        final String lowerCaseHead = head.toLowerCase(Locale.ROOT);

        if(lowerCaseHead.contains("transfer-encoding: chunked")) {
            final ByteArrayOutputStream body = new ByteArrayOutputStream();
            while(true) {
                final int size = Integer.parseInt(readLine(input).strip().split(";")[0], 16);

                if(size == 0) {
                    readLine(input);
                    return body.toByteArray();
                }

                body.write(input.readNBytes(size));
                readLine(input);
            }
        }

        final int lengthStart = lowerCaseHead.indexOf("content-length:");

        if(lengthStart == -1) {
            return new byte[0];
        }

        final int lengthEnd = lowerCaseHead.indexOf("\r\n", lengthStart);
        return input.readNBytes(Integer.parseInt(head.substring(lengthStart + "content-length:".length(), lengthEnd)
                                                     .strip()));
    }

    private static String readLine(InputStream input) throws IOException {
        final ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;

        while((value = input.read()) != -1 && value != '\n') {
            line.write(value);
        }

        return line.toString(StandardCharsets.US_ASCII);
    }

    private static void check(boolean condition, String message) {
        if(!condition) {
            System.err.println("Packaged app smoke test failed: " + message);
            System.exit(1);
        }
    }
}
