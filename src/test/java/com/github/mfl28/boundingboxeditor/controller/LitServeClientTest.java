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
package com.github.mfl28.boundingboxeditor.controller;

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient.ServiceType;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.ui.settings.InferenceSettingsView;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.testfx.api.FxAssert.verifyThat;

/**
 * Tests connecting to a LitServe server and predicting with it through the UI, against a local HTTP server
 * standing in for LitServe.
 */
@Tag("ui")
class LitServeClientTest extends BoundingBoxEditorTestBase {
    private static final String API_KEY = "secret";
    private static final String PREDICTIONS_JSON =
            "[{\"foo\": [10.0, 10.0, 100.0, 100.0], \"score\": 0.9}, {\"bar\": [20.0, 20.0, 50.0, 50.0], \"score\": 0.4}]";
    private final AtomicReference<String> lastPredictionBody = new AtomicReference<>();
    private HttpServer fakeLitServe;

    @Start
    void start(Stage stage) throws IOException {
        fakeLitServe = HttpServer.create(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0), 0);
        fakeLitServe.createContext("/health", exchange -> respondIfAuthorized(exchange, "ok"));
        fakeLitServe.createContext("/predict", exchange -> {
            lastPredictionBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            respondIfAuthorized(exchange, PREDICTIONS_JSON);
        });
        fakeLitServe.start();

        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @AfterEach
    void stopFakeLitServe() {
        fakeLitServe.stop(0);
    }

    @Test
    void onLitServeSelected_ShouldCheckConnectionAndPredict(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        // Opened like the settings shortcut does; posted because the settings dialog blocks until it is closed.
        Platform.runLater(controller::onRegisterSettingsAction);
        WaitForAsyncUtils.waitForFxEvents();

        final Stage settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        timeOutLookUpInStageAndClickOn(robot, settingsStage, "Inference", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        final InferenceSettingsView inferenceSettingsView = mainView.getInferenceSettingsView();
        robot.clickOn(inferenceSettingsView.getInferenceEnabledControl());
        WaitForAsyncUtils.waitForFxEvents();

        // Torch serve is the default and shows its management server and model selection.
        verifyThat(inferenceSettingsView.getServiceTypeControl().getValue(), Matchers.equalTo(ServiceType.TORCH_SERVE),
                saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getManagementAddressField().getParent().isVisible(), Matchers.is(true),
                saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getPredictionPathField().isVisible(), Matchers.is(false),
                saveScreenshot(testinfo));

        robot.interact(() -> inferenceSettingsView.getServiceTypeControl().setValue(ServiceType.LIT_SERVE));
        WaitForAsyncUtils.waitForFxEvents();

        // LitServe has no model registry: its endpoint path, API key and connection check are shown instead.
        verifyThat(inferenceSettingsView.getManagementAddressField().getParent().isVisible(), Matchers.is(false),
                saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getSelectModelButton().getParent().isVisible(), Matchers.is(false),
                saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getPredictionPathField().isVisible(), Matchers.is(true),
                saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getPredictionPathField().getText(), Matchers.equalTo("/predict"),
                saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getApiKeyField().getParent().isVisible(), Matchers.is(true),
                saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getCheckConnectionButton().getParent().isVisible(), Matchers.is(true),
                saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getInferencePortField().getText(), Matchers.equalTo("8000"),
                saveScreenshot(testinfo));

        robot.interact(() -> {
            inferenceSettingsView.getInferenceAddressField().setText("http://127.0.0.1");
            inferenceSettingsView.getInferencePortField().setText(Integer.toString(fakeLitServe.getAddress().getPort()));
            inferenceSettingsView.getApiKeyField().setText("wrong");
        });
        WaitForAsyncUtils.waitForFxEvents();

        // A rejected API key is reported.
        robot.clickOn(inferenceSettingsView.getCheckConnectionButton());
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getServerConnectionCheckService(), testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Connection Check Error Report", testinfo);
        final String checkError = WaitForAsyncUtils.waitFor(WaitForAsyncUtils.asyncFx(
                () -> controller.getServerConnectionCheckService().getValue().getErrorTableEntries().getFirst()
                        .getErrorDescription()));
        verifyThat(checkError, Matchers.equalTo("Missing or invalid API key."), saveScreenshot(testinfo));
        timeOutClickOnButtonInDialogStage(robot, errorReportStage, ButtonType.OK, testinfo);
        timeOutAssertTopModalStageClosed(robot, "Connection Check Error Report", testinfo);

        robot.interact(() -> inferenceSettingsView.getApiKeyField().setText(API_KEY));
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(inferenceSettingsView.getCheckConnectionButton());
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getServerConnectionCheckService(), testinfo);

        final Stage connectionCheckStage = timeOutGetTopModalStage(robot, "Connection Check", testinfo);
        timeOutClickOnButtonInDialogStage(robot, connectionCheckStage, ButtonType.OK, testinfo);
        timeOutAssertTopModalStageClosed(robot, "Connection Check", testinfo);

        // No model needs to be selected for LitServe.
        robot.clickOn(((DialogPane) settingsStage.getScene().getRoot()).lookupButton(ButtonType.OK));
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertNoTopModelStage(robot, testinfo);

        final BoundingBoxPredictorClientConfig clientConfig = model.getBoundingBoxPredictorClientConfig();
        verifyThat(clientConfig.getServiceType(), Matchers.equalTo(ServiceType.LIT_SERVE), saveScreenshot(testinfo));
        verifyThat(clientConfig.getPredictionPath(), Matchers.equalTo("/predict"), saveScreenshot(testinfo));
        verifyThat(clientConfig.getApiKey(), Matchers.equalTo(API_KEY), saveScreenshot(testinfo));
        verifyThat(clientConfig.getInferencePort(), Matchers.equalTo(fakeLitServe.getAddress().getPort()),
                saveScreenshot(testinfo));

        robot.moveTo(mainView.getEditor().getEditorToolBar().getPredictButton()).clickOn();
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getBoundingBoxPredictorService(), testinfo);

        // The prediction below the minimum score (0.5) is dropped.
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully predicted 1 bounding box"), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get("foo"), Matchers.equalTo(1),
                saveScreenshot(testinfo));
        verifyThat(lastPredictionBody.get(), Matchers.containsString("filename=\"image\""), saveScreenshot(testinfo));
    }

    private static void respondIfAuthorized(HttpExchange exchange, String body) throws IOException {
        final boolean authorized = API_KEY.equals(exchange.getRequestHeaders().getFirst("X-API-Key"));
        final byte[] responseBytes = (authorized ? body : "{\"detail\":\"Invalid API key.\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(authorized ? 200 : 401, responseBytes.length);

        try(OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }
}
