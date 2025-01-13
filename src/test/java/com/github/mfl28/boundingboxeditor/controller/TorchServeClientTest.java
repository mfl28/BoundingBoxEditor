/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictionEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.ModelEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.TorchServeRestClient;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.google.gson.JsonSyntaxException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.ComboBoxMatchers;
import org.testfx.matcher.control.TextInputControlMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TorchServeClientTest extends BoundingBoxEditorTestBase {
    private static final String INFERENCE_SERVER = "http://foo123";
    private static final String INFERENCE_PORT = "8080";
    private static final String MANAGEMENT_SERVER = "http://bar456";
    private static final String MANAGEMENT_PORT = "8081";

    @Mock
    ClientBuilder mockClientBuilder;

    @Mock
    Client mockClient;

    @Mock
    WebTarget mockManagementTarget;

    @Mock
    WebTarget mockModelsTarget;

    @Mock
    Invocation.Builder mockModelInvocationBuilder;

    @Mock
    Response mockModelResponse;

    @Mock
    WebTarget mockInferenceTarget;

    @Mock
    WebTarget mockPredictionsTarget;

    @Mock
    WebTarget mockPredictionModelTarget;

    @Mock
    Invocation.Builder mockInferenceInvocationBuilder;

    @Mock
    Response mockPredictionResponse;

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_EXIF_IMAGE_FOLDER_PATH).getFile()));
    }

    @Test
    void onModelNamesRequested_ShouldHandleCorrectly(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        // Setup
        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-settings-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        final Stage settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.equalTo(true));

        timeOutLookUpInStageAndClickOn(robot, settingsStage, "Inference", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getInferenceSettingsView().getInferenceEnabledControl());
        WaitForAsyncUtils.waitForFxEvents();

        setUpAndVerifyManagementServerSettings(robot, testinfo);

        verifyManagementConnectionErrorHandling(robot, testinfo);
        verifyManagementProcessingErrorHandling(robot, testinfo);
        verifyManagementNonOkResponseErrorHandling(robot, testinfo);
        verifyManagementEntityReadingErrorHandling(robot, testinfo);
        verifyManagementJsonSyntaxErrorHandling(robot, testinfo);
        verifyManagementNoModelsRegisteredWithServerErrorHandling(robot, testinfo);
        verifyManagementCorrectResponseHandling(robot, testinfo);
    }

    @Test
    void onPredictionRequested_ShouldHandleCorrectly(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        // Setup
        enterNewCategory(robot, "Foo", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getObjectCategories().size(), Matchers.equalTo(1));

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-settings-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Stage settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        timeOutLookUpInStageAndClickOn(robot, settingsStage, "Inference", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getInferenceSettingsView().getInferenceEnabledControl());
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getBoundingBoxPredictorConfig().isMergeCategories(), Matchers.is(true));

        setUpAndVerifyInferenceServerSettings(robot, testinfo);
        setUpAndVerifyManagementServerSettings(robot, testinfo);

        controller.makeClientUnavailable();

        try(MockedStatic<ClientBuilder> testBuilder = Mockito.mockStatic(ClientBuilder.class)) {
            testBuilder.when(ClientBuilder::newBuilder).thenReturn(mockClientBuilder);
            when(mockClientBuilder.register(Mockito.any())).thenReturn(mockClientBuilder);
            when(mockClientBuilder.build()).thenReturn(mockClient);

            // Mocked management server setup
            when(mockClient.target(MANAGEMENT_SERVER + ":" + MANAGEMENT_PORT)).thenReturn(mockManagementTarget);
            when(mockManagementTarget.path("models")).thenReturn(mockModelsTarget);
            when(mockModelsTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockModelInvocationBuilder);
            when(mockModelInvocationBuilder.get()).thenReturn(mockModelResponse);
            final TorchServeRestClient.ModelsWrapper modelsWrapper =
                    new TorchServeRestClient.ModelsWrapper(List.of(new ModelEntry("foo-model", "foo-model-url")));
            when(mockModelResponse.getStatusInfo()).thenReturn(Response.Status.OK);
            when(mockModelResponse.readEntity(TorchServeRestClient.ModelsWrapper.class)).thenReturn(modelsWrapper);

            // Mocked inference server setup
            when(mockClient.target(INFERENCE_SERVER + ":" + INFERENCE_PORT)).thenReturn(mockInferenceTarget);
            when(mockInferenceTarget.path("predictions")).thenReturn(mockPredictionsTarget);
            when(mockPredictionsTarget.path("foo-model")).thenReturn(mockPredictionModelTarget);
            when(mockPredictionModelTarget.request(MediaType.APPLICATION_JSON))
                    .thenReturn(mockInferenceInvocationBuilder);

            controller.makeClientAvailable();
        }

        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getInferenceSettingsView().getSelectModelButton());
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getModelNameFetchService(), testinfo);

        final Stage modelSelectionDialogStage = timeOutGetTopModalStage(robot, "Model Choice", testinfo);
        timeOutClickOnButtonInDialogStage(robot, modelSelectionDialogStage, ButtonType.OK, testinfo);

        timeOutAssertTopModalStageClosed(robot, "Model Choice", testinfo);

        settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.is(true));
        verifyThat(settingsStage.getScene().getRoot(), Matchers.instanceOf(DialogPane.class));

        final DialogPane settingsPane = (DialogPane) settingsStage.getScene().getRoot();

        robot.clickOn(settingsPane.lookupButton(ButtonType.OK));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertNoTopModelStage(robot, testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.is(false), saveScreenshot(testinfo));

        verifyThat(mainView.getEditor().getEditorToolBar().getPredictButton().isVisible(), Matchers.is(true),
                saveScreenshot(testinfo));

        verifyInferenceConnectionErrorHandling(robot, testinfo);
        verifyInferenceProcessingErrorHandling(robot, testinfo);
        verifyInferenceNonOkResponseErrorHandling(robot, testinfo);
        verifyInferenceEntityReadingErrorHandling(robot, testinfo);
        verifyInferenceJsonSyntaxErrorHandling(robot, testinfo);
        verifyInferenceNoBoundingBoxPredictionsReceivedHandling(robot, testinfo);

        List<BoundingBoxPredictionEntry> testPredictions = new ArrayList<>();
        testPredictions.add(new BoundingBoxPredictionEntry(
                Map.of("foo", List.of(100.0, 100.0, 200.0, 200.0)),
                0.7));
        testPredictions.add(new BoundingBoxPredictionEntry(
                Map.of("foo", List.of(250.0, 250.0, 300.0, 275.0)),
                0.9));
        testPredictions.add(new BoundingBoxPredictionEntry(
                Map.of("bar", List.of(50.0, 100.0, 150.0, 175.0)),
                0.3));
        testPredictions.add(new BoundingBoxPredictionEntry(
                Map.of("bar", List.of(75.0, 150.0, 225.0, 275.0)),
                0.8));

        verifyInferenceBoundingBoxPredictionsReceivedHandling(robot, testinfo, testPredictions, 2, 1);

        Platform.runLater(() -> mainView.getImageFileListView().getSelectionModel().select(5));

        WaitForAsyncUtils.waitForFxEvents();
        waitUntilCurrentImageIsLoaded(testinfo);
        verifyInferenceBoundingBoxPredictionsReceivedHandling(robot, testinfo, testPredictions, 4, 2);

        Platform.runLater(() -> mainView.getImageFileListView().getSelectionModel().select(6));

        WaitForAsyncUtils.waitForFxEvents();
        waitUntilCurrentImageIsLoaded(testinfo);

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-settings-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        timeOutLookUpInStageAndClickOn(robot, settingsStage, "Inference", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getInferenceSettingsView().getResizeImagesControl());
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(((DialogPane) settingsStage.getScene().getRoot()).lookupButton(ButtonType.OK));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertNoTopModelStage(robot, testinfo);

        testPredictions = new ArrayList<>();
        testPredictions.add(new BoundingBoxPredictionEntry(
                Map.of("foo", List.of(10.0, 10.0, 20.0, 20.0)),
                0.7));
        testPredictions.add(new BoundingBoxPredictionEntry(
                Map.of("foo", List.of(25.0, 25.0, 30.0, 27.0)),
                0.9));
        testPredictions.add(new BoundingBoxPredictionEntry(
                Map.of("bar", List.of(5.0, 10.0, 15.0, 17.0)),
                0.3));
        testPredictions.add(new BoundingBoxPredictionEntry(
                Map.of("bar", List.of(7.0, 15.0, 22.0, 27.0)),
                0.8));

        verifyInferenceBoundingBoxPredictionsReceivedHandling(robot, testinfo, testPredictions, 6, 3);

        Platform.runLater(() -> mainView.getImageFileListView().getSelectionModel().select(1));

        WaitForAsyncUtils.waitForFxEvents();
        waitUntilCurrentImageIsLoaded(testinfo);

        verifyInferenceBoundingBoxPredictionsReceivedHandling(robot, testinfo, testPredictions, 8, 4);
    }


    private void verifyInferenceConnectionErrorHandling(FxRobot robot, TestInfo testinfo) {
        when(mockInferenceInvocationBuilder.post(Mockito.any()))
                .thenThrow(new ProcessingException(new ConnectException()));

        robot.moveTo(mainView.getEditor().getEditorToolBar().getPredictButton()).clickOn();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getBoundingBoxPredictorService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Bounding Box Prediction Error Report",
                "There were errors while performing the prediction",
                "Torch serve",
                "Could not connect to inference server.");

        reset(mockInferenceInvocationBuilder);
    }

    private void verifyInferenceProcessingErrorHandling(FxRobot robot, TestInfo testinfo) {
        when(mockInferenceInvocationBuilder.post(Mockito.any())).thenThrow(new ProcessingException("foo"));

        robot.moveTo(mainView.getEditor().getEditorToolBar().getPredictButton()).clickOn();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getBoundingBoxPredictorService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Bounding Box Prediction Error Report",
                "There were errors while performing the prediction",
                "Torch serve",
                "Could not get prediction from inference server.");

        reset(mockInferenceInvocationBuilder);
    }

    private void verifyInferenceNonOkResponseErrorHandling(FxRobot robot, TestInfo testinfo) {
        when(mockInferenceInvocationBuilder.post(Mockito.any())).thenReturn(mockPredictionResponse);
        when(mockPredictionResponse.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        robot.moveTo(mainView.getEditor().getEditorToolBar().getPredictButton()).clickOn();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getBoundingBoxPredictorService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Bounding Box Prediction Error Report",
                "There were errors while performing the prediction",
                "Torch serve",
                "Could not get prediction from inference server. Reason: Not Found");

        reset(mockInferenceInvocationBuilder);
        reset(mockPredictionResponse);
    }

    private void verifyInferenceEntityReadingErrorHandling(FxRobot robot, TestInfo testinfo) {
        when(mockInferenceInvocationBuilder.post(Mockito.any())).thenReturn(mockPredictionResponse);
        when(mockPredictionResponse.getStatusInfo()).thenReturn(Response.Status.OK);
        when(mockPredictionResponse.readEntity(new GenericType<List<BoundingBoxPredictionEntry>>() {
        }))
                .thenThrow(new ProcessingException("bar"));

        robot.moveTo(mainView.getEditor().getEditorToolBar().getPredictButton()).clickOn();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getBoundingBoxPredictorService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Bounding Box Prediction Error Report",
                "There were errors while performing the prediction",
                "Torch serve",
                "Could not get prediction from inference server.");

        reset(mockInferenceInvocationBuilder);
        reset(mockPredictionResponse);
    }

    private void verifyInferenceJsonSyntaxErrorHandling(FxRobot robot, TestInfo testinfo) {
        when(mockInferenceInvocationBuilder.post(Mockito.any())).thenReturn(mockPredictionResponse);
        when(mockPredictionResponse.getStatusInfo()).thenReturn(Response.Status.OK);
        when(mockPredictionResponse.readEntity(new GenericType<List<BoundingBoxPredictionEntry>>() {
        }))
                .thenThrow(new JsonSyntaxException("bar"));

        robot.moveTo(mainView.getEditor().getEditorToolBar().getPredictButton()).clickOn();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getBoundingBoxPredictorService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Bounding Box Prediction Error Report",
                "There were errors while performing the prediction",
                "Torch serve",
                "Invalid inference server response format for resource \"predictions\".");

        reset(mockInferenceInvocationBuilder);
        reset(mockPredictionResponse);
    }


    private void verifyInferenceNoBoundingBoxPredictionsReceivedHandling(FxRobot robot, TestInfo testinfo) {
        when(mockInferenceInvocationBuilder.post(Mockito.any())).thenReturn(mockPredictionResponse);
        when(mockPredictionResponse.getStatusInfo()).thenReturn(Response.Status.OK);
        when(mockPredictionResponse.readEntity(new GenericType<List<BoundingBoxPredictionEntry>>() {
        }))
                .thenReturn(new ArrayList<>());

        robot.moveTo(mainView.getEditor().getEditorToolBar().getPredictButton()).clickOn();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getBoundingBoxPredictorService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully predicted 0 bounding boxes for 1 image in"),
                saveScreenshot(testinfo));

        verifyThat(model.getObjectCategories().size(), Matchers.equalTo(1));
        verifyThat(mainView.getObjectCategoryTable().getItems().size(), Matchers.equalTo(1));

        reset(mockInferenceInvocationBuilder);
        reset(mockPredictionResponse);
    }

    private void verifyInferenceBoundingBoxPredictionsReceivedHandling(FxRobot robot, TestInfo testinfo, List<BoundingBoxPredictionEntry> testPredictions, int expectedFooCount, int expectedBarCount) {
        when(mockInferenceInvocationBuilder.post(Mockito.any())).thenReturn(mockPredictionResponse);
        when(mockPredictionResponse.getStatusInfo()).thenReturn(Response.Status.OK);

        when(mockPredictionResponse.readEntity(new GenericType<List<BoundingBoxPredictionEntry>>() {
        }))
                .thenReturn(testPredictions);
        WaitForAsyncUtils.waitForFxEvents();

        robot.moveTo(mainView.getEditor().getEditorToolBar().getPredictButton()).clickOn();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getBoundingBoxPredictorService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully predicted 3 bounding boxes for 1 image in"),
                saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(3));
        verifyThat(mainView.getObjectCategoryTable().getItems().size(), Matchers.equalTo(2));
        verifyThat(model.getCategoryNameToCategoryMap().size(), Matchers.equalTo(2));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get("Foo"), Matchers.equalTo(expectedFooCount));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get("bar"), Matchers.equalTo(expectedBarCount));

        reset(mockInferenceInvocationBuilder);
        reset(mockPredictionResponse);
    }

    private void verifyManagementConnectionErrorHandling(FxRobot robot, TestInfo testinfo) {
        controller.makeClientUnavailable();

        try(MockedStatic<ClientBuilder> testBuilder = Mockito.mockStatic(ClientBuilder.class)) {
            testBuilder.when(ClientBuilder::newBuilder).thenReturn(mockClientBuilder);
            when(mockClientBuilder.register(Mockito.any())).thenReturn(mockClientBuilder);
            when(mockClientBuilder.build()).thenReturn(mockClient);

            when(mockClient.target(MANAGEMENT_SERVER + ":" + MANAGEMENT_PORT)).thenReturn(mockManagementTarget);
            when(mockManagementTarget.path("models")).thenReturn(mockModelsTarget);
            when(mockModelsTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockModelInvocationBuilder);
            when(mockModelInvocationBuilder.get()).thenThrow(new ProcessingException(new ConnectException()));
            controller.makeClientAvailable();
        }

        robot.clickOn(mainView.getInferenceSettingsView().getSelectModelButton());
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getModelNameFetchService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Model Fetching Error Report",
                "There were errors while fetching model names from the server",
                "Torch serve",
                "Could not connect to management server.");

        WaitForAsyncUtils.waitForFxEvents();
        resetAllMocks();
    }


    private void verifyManagementProcessingErrorHandling(FxRobot robot, TestInfo testinfo) {
        controller.makeClientUnavailable();

        try(MockedStatic<ClientBuilder> testBuilder = Mockito.mockStatic(ClientBuilder.class)) {
            testBuilder.when(ClientBuilder::newBuilder).thenReturn(mockClientBuilder);
            when(mockClientBuilder.register(Mockito.any())).thenReturn(mockClientBuilder);
            when(mockClientBuilder.build()).thenReturn(mockClient);

            when(mockClient.target(MANAGEMENT_SERVER + ":" + MANAGEMENT_PORT)).thenReturn(mockManagementTarget);
            when(mockManagementTarget.path("models")).thenReturn(mockModelsTarget);
            when(mockModelsTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockModelInvocationBuilder);
            when(mockModelInvocationBuilder.get()).thenThrow(new ProcessingException("foo"));
            controller.makeClientAvailable();
        }

        robot.clickOn(mainView.getInferenceSettingsView().getSelectModelButton());
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getModelNameFetchService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Model Fetching Error Report",
                "There were errors while fetching model names from the server",
                "Torch serve",
                "Could not fetch models from management server.");

        WaitForAsyncUtils.waitForFxEvents();
        resetAllMocks();
    }

    private void verifyManagementNonOkResponseErrorHandling(FxRobot robot, TestInfo testinfo) {
        controller.makeClientUnavailable();

        try(MockedStatic<ClientBuilder> testBuilder = Mockito.mockStatic(ClientBuilder.class)) {
            testBuilder.when(ClientBuilder::newBuilder).thenReturn(mockClientBuilder);
            when(mockClientBuilder.register(Mockito.any())).thenReturn(mockClientBuilder);
            when(mockClientBuilder.build()).thenReturn(mockClient);

            when(mockClient.target(MANAGEMENT_SERVER + ":" + MANAGEMENT_PORT)).thenReturn(mockManagementTarget);
            when(mockManagementTarget.path("models")).thenReturn(mockModelsTarget);
            when(mockModelsTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockModelInvocationBuilder);
            when(mockModelInvocationBuilder.get()).thenReturn(mockModelResponse);
            when(mockModelResponse.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);
            controller.makeClientAvailable();
        }

        robot.clickOn(mainView.getInferenceSettingsView().getSelectModelButton());
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getModelNameFetchService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Model Fetching Error Report",
                "There were errors while fetching model names from the server",
                "Torch serve",
                "Could not fetch models from management server. Reason: Not Found");

        WaitForAsyncUtils.waitForFxEvents();
        resetAllMocks();
    }

    private void verifyManagementJsonSyntaxErrorHandling(FxRobot robot, TestInfo testinfo) {
        controller.makeClientUnavailable();

        try(MockedStatic<ClientBuilder> testBuilder = Mockito.mockStatic(ClientBuilder.class)) {
            testBuilder.when(ClientBuilder::newBuilder).thenReturn(mockClientBuilder);
            when(mockClientBuilder.register(Mockito.any())).thenReturn(mockClientBuilder);
            when(mockClientBuilder.build()).thenReturn(mockClient);

            when(mockClient.target(MANAGEMENT_SERVER + ":" + MANAGEMENT_PORT)).thenReturn(mockManagementTarget);
            when(mockManagementTarget.path("models")).thenReturn(mockModelsTarget);
            when(mockModelsTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockModelInvocationBuilder);
            when(mockModelInvocationBuilder.get()).thenReturn(mockModelResponse);
            when(mockModelResponse.getStatusInfo()).thenReturn(Response.Status.OK);
            when(mockModelResponse.readEntity(TorchServeRestClient.ModelsWrapper.class))
                    .thenThrow(new JsonSyntaxException("foo"));
            controller.makeClientAvailable();
        }

        robot.clickOn(mainView.getInferenceSettingsView().getSelectModelButton());
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getModelNameFetchService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Model Fetching Error Report",
                "There were errors while fetching model names from the server",
                "Torch serve",
                "Invalid management server response format for resource \"models\".");

        WaitForAsyncUtils.waitForFxEvents();
        resetAllMocks();
    }

    private void verifyManagementEntityReadingErrorHandling(FxRobot robot, TestInfo testinfo) {
        controller.makeClientUnavailable();

        try(MockedStatic<ClientBuilder> testBuilder = Mockito.mockStatic(ClientBuilder.class)) {
            testBuilder.when(ClientBuilder::newBuilder).thenReturn(mockClientBuilder);
            when(mockClientBuilder.register(Mockito.any())).thenReturn(mockClientBuilder);
            when(mockClientBuilder.build()).thenReturn(mockClient);

            when(mockClient.target(MANAGEMENT_SERVER + ":" + MANAGEMENT_PORT)).thenReturn(mockManagementTarget);
            when(mockManagementTarget.path("models")).thenReturn(mockModelsTarget);
            when(mockModelsTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockModelInvocationBuilder);
            when(mockModelInvocationBuilder.get()).thenReturn(mockModelResponse);
            when(mockModelResponse.getStatusInfo()).thenReturn(Response.Status.OK);
            when(mockModelResponse.readEntity(TorchServeRestClient.ModelsWrapper.class))
                    .thenThrow(new ProcessingException("bar"));
            controller.makeClientAvailable();
        }

        robot.clickOn(mainView.getInferenceSettingsView().getSelectModelButton());
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getModelNameFetchService(), testinfo);

        verifyErrorReportStage(robot, testinfo, "Model Fetching Error Report",
                "There were errors while fetching model names from the server",
                "Torch serve",
                "Could not fetch models from management server.");

        WaitForAsyncUtils.waitForFxEvents();
        resetAllMocks();
    }

    private void verifyManagementNoModelsRegisteredWithServerErrorHandling(FxRobot robot, TestInfo testinfo) {
        controller.makeClientUnavailable();

        try(MockedStatic<ClientBuilder> testBuilder = Mockito.mockStatic(ClientBuilder.class)) {
            testBuilder.when(ClientBuilder::newBuilder).thenReturn(mockClientBuilder);
            when(mockClientBuilder.register(Mockito.any())).thenReturn(mockClientBuilder);
            when(mockClientBuilder.build()).thenReturn(mockClient);

            when(mockClient.target(MANAGEMENT_SERVER + ":" + MANAGEMENT_PORT)).thenReturn(mockManagementTarget);
            when(mockManagementTarget.path("models")).thenReturn(mockModelsTarget);
            when(mockModelsTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockModelInvocationBuilder);
            when(mockModelInvocationBuilder.get()).thenReturn(mockModelResponse);
            when(mockModelResponse.getStatusInfo()).thenReturn(Response.Status.OK);
            when(mockModelResponse.readEntity(TorchServeRestClient.ModelsWrapper.class))
                    .thenReturn(new TorchServeRestClient.ModelsWrapper());
            controller.makeClientAvailable();
        }

        robot.clickOn(mainView.getInferenceSettingsView().getSelectModelButton());
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getModelNameFetchService(), testinfo);

        final Stage errorDialogStage = timeOutAssertDialogOpenedAndGetStage(robot, "Model Fetching Error", "No " +
                "models are registered with the " +
                "management server.", testinfo);

        timeOutClickOnButtonInDialogStage(robot, errorDialogStage, ButtonType.OK, testinfo);

        WaitForAsyncUtils.waitForFxEvents();
        resetAllMocks();
    }

    private void verifyManagementCorrectResponseHandling(FxRobot robot, TestInfo testinfo) {
        controller.makeClientUnavailable();

        try(MockedStatic<ClientBuilder> testBuilder = Mockito.mockStatic(ClientBuilder.class)) {
            testBuilder.when(ClientBuilder::newBuilder).thenReturn(mockClientBuilder);
            when(mockClientBuilder.register(Mockito.any())).thenReturn(mockClientBuilder);
            when(mockClientBuilder.build()).thenReturn(mockClient);

            when(mockClient.target(MANAGEMENT_SERVER + ":" + MANAGEMENT_PORT)).thenReturn(mockManagementTarget);
            when(mockManagementTarget.path("models")).thenReturn(mockModelsTarget);
            when(mockModelsTarget.request(MediaType.APPLICATION_JSON)).thenReturn(mockModelInvocationBuilder);
            when(mockModelInvocationBuilder.get()).thenReturn(mockModelResponse);
            final TorchServeRestClient.ModelsWrapper modelsWrapper =
                    new TorchServeRestClient.ModelsWrapper(List.of(new ModelEntry("foo-model", "foo-model-url"),
                            new ModelEntry("bar-model", "bar-model-url"),
                            new ModelEntry("   ", "blank-url")));
            when(mockModelResponse.getStatusInfo()).thenReturn(Response.Status.OK);
            when(mockModelResponse.readEntity(TorchServeRestClient.ModelsWrapper.class)).thenReturn(modelsWrapper);
            controller.makeClientAvailable();
        }

        robot.clickOn(mainView.getInferenceSettingsView().getSelectModelButton());
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getModelNameFetchService(), testinfo);

        final Stage modelSelectionDialogStage = timeOutGetTopModalStage(robot, "Model Choice", testinfo);
        final DialogPane modelSelectionDialog = (DialogPane) modelSelectionDialogStage.getScene().getRoot();

        verifyThat(modelSelectionDialog.getHeaderText(), Matchers.equalTo("Choose the model used for performing " +
                "predictions."));
        verifyThat(modelSelectionDialog.getContentText(), Matchers.equalTo("Model:"));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> robot.from(modelSelectionDialog)
                                .lookup(".combo-box").tryQuery()
                                .isPresent()),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected combo-box not found within " +
                                TIMEOUT_DURATION_IN_SEC +
                                " sec."));
        WaitForAsyncUtils.waitForFxEvents();

        final ComboBox<ObjectCategory> comboBox =
                robot.from(modelSelectionDialog).lookup(".combo-box").queryComboBox();

        verifyThat(comboBox, ComboBoxMatchers.containsExactlyItemsInOrder("foo-model", "bar-model"),
                saveScreenshot(testinfo));
        verifyThat(comboBox, ComboBoxMatchers.hasSelectedItem("foo-model"), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, modelSelectionDialogStage, ButtonType.OK, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Model Choice", testinfo);

        verifyThat(mainView.getInferenceSettingsView().getSelectedModelLabel().getText(),
                Matchers.equalTo("foo-model"));

        resetAllMocks();
    }

    private void verifyErrorReportStage(FxRobot robot, TestInfo testinfo, String stageTitle, String headerText,
                                        String errorInfoEntrySource, String errorInfoEntryDescription) {
        final Stage errorReportStage = timeOutGetTopModalStage(robot, stageTitle, testinfo);
        verifyThat(errorReportStage.isShowing(), Matchers.is(true));

        verifyThat(((DialogPane) errorReportStage.getScene().getRoot()).getHeaderText(),
                Matchers.equalTo(headerText));

        final List<IOErrorInfoEntry> errorInfoEntries =
                timeOutGetErrorInfoEntriesFromStage(errorReportStage, testinfo);
        verifyThat(errorInfoEntries, Matchers.hasSize(1));
        verifyThat(errorInfoEntries.get(0).getSourceName(), Matchers.equalTo(errorInfoEntrySource));
        verifyThat(errorInfoEntries.get(0).getErrorDescription(),
                Matchers.equalTo(errorInfoEntryDescription));

        timeOutClickOnButtonInDialogStage(robot, errorReportStage, ButtonType.OK, testinfo);
    }

    private void setUpAndVerifyManagementServerSettings(FxRobot robot, TestInfo testinfo) {
        mainView.getInferenceSettingsView().getManagementAddressField().clear();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getInferenceSettingsView().getManagementAddressField())
                .write(MANAGEMENT_SERVER);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getInferenceSettingsView().getManagementAddressField(),
                TextInputControlMatchers.hasText(MANAGEMENT_SERVER), saveScreenshot(testinfo));

        mainView.getInferenceSettingsView().getManagementPortField().clear();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getInferenceSettingsView().getManagementPortField())
                .write(MANAGEMENT_PORT);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getInferenceSettingsView().getManagementPortField(),
                TextInputControlMatchers.hasText(MANAGEMENT_PORT), saveScreenshot(testinfo));
    }

    private void setUpAndVerifyInferenceServerSettings(FxRobot robot, TestInfo testinfo) {
        mainView.getInferenceSettingsView().getInferenceAddressField().clear();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getInferenceSettingsView().getInferenceAddressField())
                .write(INFERENCE_SERVER);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getInferenceSettingsView().getInferenceAddressField(),
                TextInputControlMatchers.hasText(INFERENCE_SERVER), saveScreenshot(testinfo));

        mainView.getInferenceSettingsView().getInferencePortField().clear();
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getInferenceSettingsView().getInferencePortField())
                .write(INFERENCE_PORT);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getInferenceSettingsView().getInferencePortField(),
                TextInputControlMatchers.hasText(INFERENCE_PORT), saveScreenshot(testinfo));
    }

    private void resetAllMocks() {
        reset(mockClientBuilder, mockClient, mockManagementTarget,
                mockModelsTarget, mockModelInvocationBuilder, mockModelResponse);
    }
}
