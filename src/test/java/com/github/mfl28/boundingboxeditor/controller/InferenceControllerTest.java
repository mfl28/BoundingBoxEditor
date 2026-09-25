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
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.TorchServeRestClient;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ModelNameFetchResult;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

/**
 * Tests the inference server handling without the UI and without a server: the REST client is created by a mocked
 * {@link ClientBuilder}, and a mocked {@link DialogService} scripts the user's answers.
 */
@Tag("unit")
class InferenceControllerTest {
    private final Model model = new Model();
    private final DialogService dialogService = mock(DialogService.class);
    private final InferenceController.Operations operations = mock(InferenceController.Operations.class);
    private final ClientBuilder clientBuilder = mock(ClientBuilder.class, Answers.RETURNS_SELF);
    private final Client client = mock(Client.class);
    private MockedStatic<ClientBuilder> clientBuilderFactory;
    private InferenceController inferenceController;

    @BeforeEach
    void setUp() {
        clientBuilderFactory = mockStatic(ClientBuilder.class);
        clientBuilderFactory.when(ClientBuilder::newBuilder).thenReturn(clientBuilder);
        when(clientBuilder.build()).thenReturn(client);

        inferenceController = new InferenceController(model, dialogService, operations);
    }

    @AfterEach
    void tearDown() {
        clientBuilderFactory.close();
    }

    @Test
    void onMakeClientAvailable_ShouldCreateClientOnceWithTimeouts() {
        inferenceController.makeClientAvailable();
        inferenceController.makeClientAvailable();

        verify(clientBuilder).connectTimeout(10, TimeUnit.SECONDS);
        verify(clientBuilder).readTimeout(120, TimeUnit.SECONDS);
        verify(clientBuilder, times(1)).build();
    }

    @Test
    void onMakeClientUnavailable_ShouldCloseClientAndCreateNewOneWhenNeededAgain() {
        inferenceController.makeClientAvailable();
        inferenceController.makeClientUnavailable();
        inferenceController.makeClientUnavailable();

        verify(client, times(1)).close();

        inferenceController.makeClientAvailable();

        verify(clientBuilder, times(2)).build();
    }

    @Test
    void onInferenceSettingsApplied_ShouldCreateOrCloseClientWhenInferenceIsSwitchedOnOrOff() {
        inferenceController.onInferenceSettingsApplied(false, false);
        verify(clientBuilder, never()).build();

        inferenceController.onInferenceSettingsApplied(false, true);
        verify(clientBuilder, times(1)).build();

        inferenceController.onInferenceSettingsApplied(true, true);
        verify(clientBuilder, times(1)).build();
        verify(client, never()).close();

        inferenceController.onInferenceSettingsApplied(true, false);
        verify(client).close();
    }

    @Test
    void onFetchModelNames_ShouldCreateClientAndStartFetching() {
        inferenceController.fetchModelNames(new BoundingBoxPredictorClientConfig());

        verify(clientBuilder).build();
        verify(operations).startModelNameFetching(isA(TorchServeRestClient.class));
    }

    @Test
    void onPredictCurrentImage_WhenNoImagesLoaded_ShouldDoNothing() {
        inferenceController.predictCurrentImage();

        verifyNoInteractions(operations);
    }

    @Test
    void onPredictCurrentImage_WhenImagesLoaded_ShouldPredictCurrentImage() {
        final File image = new File("image.jpg");
        model.setImageFiles(List.of(image, new File("other.jpg")));

        inferenceController.predictCurrentImage();

        verify(operations).updateModelFromView();
        verify(operations).startPrediction(eq(image), isA(TorchServeRestClient.class));
    }

    @Test
    void onModelNamesFetched_WhenErrorsOccurred_ShouldReportErrors() {
        final ModelNameFetchResult result = new ModelNameFetchResult(0,
                List.of(new IOErrorInfoEntry("Torch serve", "Could not connect to management server.")), List.of());

        inferenceController.onModelNamesFetched(result, null);

        verify(dialogService).displayIOResultErrorInfoAlert(result, null);
        verify(operations, never()).showSelectedModel(any());
    }

    @Test
    void onModelNamesFetched_WhenNoModelsRegistered_ShouldReportError() {
        inferenceController.onModelNamesFetched(new ModelNameFetchResult(1, List.of(), List.of()), null);

        verify(dialogService).displayErrorAlert(eq("Model Fetching Error"), any(), any());
        verify(operations, never()).showSelectedModel(any());
    }

    @Test
    void onModelNamesFetched_WhenUserChoosesModel_ShouldShowChosenModel() {
        doReturn(Optional.of("bar-model")).when(dialogService)
                .displayChoiceDialogAndGetResult(any(), any(), any(), any(), any(), any());

        inferenceController.onModelNamesFetched(
                new ModelNameFetchResult(1, List.of(), List.of("foo-model", "bar-model")), null);

        verify(dialogService).displayChoiceDialogAndGetResult(eq("foo-model"), eq(List.of("foo-model", "bar-model")),
                eq("Model Choice"), any(), any(), any());
        verify(operations).showSelectedModel("bar-model");
    }

    @Test
    void onModelNamesFetched_WhenUserCancelsChoice_ShouldNotChangeSelectedModel() {
        doReturn(Optional.empty()).when(dialogService)
                .displayChoiceDialogAndGetResult(any(), any(), any(), any(), any(), any());

        inferenceController.onModelNamesFetched(new ModelNameFetchResult(1, List.of(), List.of("foo-model")), null);

        verify(operations, never()).showSelectedModel(any());
    }
}
