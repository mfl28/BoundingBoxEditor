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

import com.github.mfl28.boundingboxeditor.model.Model;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.GsonMessageBodyHandler;
import com.github.mfl28.boundingboxeditor.model.io.results.ModelNameFetchResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ServerConnectionCheckResult;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import javafx.stage.Window;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Handles the connection to the inference server: managing the REST client, fetching the available models and
 * letting the user choose one, and starting bounding box predictions. Running the requests is delegated to the
 * {@link Operations}.
 */
class InferenceController {
    private static final long CLIENT_CONNECT_TIMEOUT_SECONDS = 10;
    // Matches Torch serve's default response timeout, so that slow predictions are not cut off early.
    private static final long CLIENT_READ_TIMEOUT_SECONDS = 120;
    private static final String MODEL_FETCHING_ERROR_DIALOG_TITLE = "Model Fetching Error";
    private static final String MODEL_FETCHING_NO_MODELS_ERROR_DIALOG_CONTENT =
            "No models are registered with the management server.";
    private static final String MODEL_CHOICE_DIALOG_TITLE = "Model Choice";
    private static final String MODEL_CHOICE_DIALOG_HEADER = "Choose the model used for performing predictions.";
    private static final String MODEL_CHOICE_DIALOG_CONTENT = "Model:";
    private static final String CONNECTION_CHECK_DIALOG_TITLE = "Connection Check";
    private static final String CONNECTION_CHECK_SUCCESS_DIALOG_HEADER = "The inference server is available.";

    private final Model model;
    private final DialogService dialogService;
    private final Operations operations;
    private Client client;

    /**
     * The operations the inference handling triggers, which are implemented by the {@link Controller}.
     */
    interface Operations {
        /**
         * Updates the model with the bounding shapes currently shown in the view.
         */
        void updateModelFromView();

        /**
         * Starts fetching the names of the models available on the inference server. The result is expected to be
         * passed to {@link #onModelNamesFetched}.
         *
         * @param predictorClient the client to use
         */
        void startModelNameFetching(BoundingBoxPredictorClient predictorClient);

        /**
         * Starts predicting bounding boxes for the provided image.
         *
         * @param imageFile       the image file
         * @param predictorClient the client to use
         */
        void startPrediction(File imageFile, BoundingBoxPredictorClient predictorClient);

        /**
         * Starts checking that the inference server is available. The result is expected to be passed to
         * {@link #onConnectionChecked}.
         *
         * @param predictorClient the client to use
         */
        void startConnectionCheck(BoundingBoxPredictorClient predictorClient);

        /**
         * Shows the chosen model as the selected one in the inference settings.
         *
         * @param modelName the name of the chosen model
         */
        void showSelectedModel(String modelName);
    }

    InferenceController(Model model, DialogService dialogService, Operations operations) {
        this.model = model;
        this.dialogService = dialogService;
        this.operations = operations;
    }

    /**
     * Creates the REST client, unless it already exists.
     */
    void makeClientAvailable() {
        if(client == null) {
            client = ClientBuilder.newBuilder()
                    .connectTimeout(CLIENT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(CLIENT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .register(MultiPartFeature.class)
                    .register(GsonMessageBodyHandler.class)
                    .build();
        }
    }

    /**
     * Closes the REST client, if it exists.
     */
    void makeClientUnavailable() {
        if(client != null) {
            client.close();
            client = null;
        }
    }

    /**
     * Creates or closes the REST client when inference was switched on or off in the settings.
     *
     * @param wasEnabled whether inference was enabled before applying the settings
     * @param isEnabled  whether inference is enabled after applying the settings
     */
    void onInferenceSettingsApplied(boolean wasEnabled, boolean isEnabled) {
        if(!wasEnabled && isEnabled) {
            makeClientAvailable();
        } else if(wasEnabled && !isEnabled) {
            makeClientUnavailable();
        }
    }

    /**
     * Starts fetching the names of the models available on the inference server.
     *
     * @param clientConfig the connection settings to use
     */
    void fetchModelNames(BoundingBoxPredictorClientConfig clientConfig) {
        makeClientAvailable();
        operations.startModelNameFetching(BoundingBoxPredictorClient.create(client, clientConfig));
    }

    /**
     * Starts checking that the inference server is available.
     *
     * @param clientConfig the connection settings to use
     */
    void checkConnection(BoundingBoxPredictorClientConfig clientConfig) {
        makeClientAvailable();
        operations.startConnectionCheck(BoundingBoxPredictorClient.create(client, clientConfig));
    }

    /**
     * Tells the user whether the inference server is available.
     *
     * @param result the result of the connection check
     * @param owner  the owner window of the shown dialogs
     */
    void onConnectionChecked(ServerConnectionCheckResult result, Window owner) {
        if(!result.getErrorTableEntries().isEmpty()) {
            dialogService.displayIOResultErrorInfoAlert(result, owner);
            return;
        }

        dialogService.displayTextInfoDialog(CONNECTION_CHECK_DIALOG_TITLE, CONNECTION_CHECK_SUCCESS_DIALOG_HEADER,
                "Connected to the " + result.getServerName() + " server.", owner);
    }

    /**
     * Lets the user choose one of the fetched models, or reports why none can be chosen.
     *
     * @param result the result of fetching the model names
     * @param owner  the owner window of the shown dialogs
     */
    void onModelNamesFetched(ModelNameFetchResult result, Window owner) {
        if(!result.getErrorTableEntries().isEmpty()) {
            dialogService.displayIOResultErrorInfoAlert(result, owner);
            return;
        }

        final List<String> modelNames = result.getModelNames();

        if(modelNames.isEmpty()) {
            dialogService.displayErrorAlert(MODEL_FETCHING_ERROR_DIALOG_TITLE,
                    MODEL_FETCHING_NO_MODELS_ERROR_DIALOG_CONTENT, owner);
            return;
        }

        final Optional<String> modelChoice = dialogService.displayChoiceDialogAndGetResult(modelNames.getFirst(),
                modelNames, MODEL_CHOICE_DIALOG_TITLE, MODEL_CHOICE_DIALOG_HEADER, MODEL_CHOICE_DIALOG_CONTENT, owner);
        modelChoice.ifPresent(operations::showSelectedModel);
    }

    /**
     * Starts predicting bounding boxes for the currently shown image, if images are loaded.
     */
    void predictCurrentImage() {
        if(model.containsImageFiles()) {
            predict(model.getCurrentImageFile());
        }
    }

    /**
     * Starts predicting bounding boxes for the provided image.
     *
     * @param imageFile the image file
     */
    void predict(File imageFile) {
        operations.updateModelFromView();
        operations.startPrediction(imageFile,
                BoundingBoxPredictorClient.create(client, model.getBoundingBoxPredictorClientConfig()));
    }
}
