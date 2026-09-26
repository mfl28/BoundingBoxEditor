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

import com.github.mfl28.boundingboxeditor.model.data.IoMetaData;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the preferences handling on a throwaway preferences node, so the user's real preferences stay untouched.
 */
@Tag("unit")
class PreferencesStoreTest {
    private final Preferences preferences =
            Preferences.userRoot().node("boundingboxeditor-test-" + UUID.randomUUID());
    private final PreferencesStore preferencesStore = new PreferencesStore(preferences);

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() throws BackingStoreException {
        preferences.removeNode();
    }

    @Test
    void onLoad_WhenNothingStored_ShouldUseDefaults() {
        final IoMetaData ioMetaData = new IoMetaData();

        assertFalse(preferencesStore.loadWindowMaximized());

        preferencesStore.loadDirectories(ioMetaData);

        assertNull(ioMetaData.getDefaultImageLoadingDirectory());
        assertNull(ioMetaData.getDefaultAnnotationLoadingDirectory());
        assertNull(ioMetaData.getDefaultAnnotationSavingDirectory());
    }

    @Test
    void onSaveAndLoad_ShouldRestoreWindowStateAndDirectories() throws IOException {
        final IoMetaData savedIoMetaData = new IoMetaData();
        savedIoMetaData.setDefaultImageLoadingDirectory(Files.createDirectory(tempDir.resolve("images")).toFile());
        savedIoMetaData.setDefaultAnnotationLoadingDirectory(Files.createDirectory(tempDir.resolve("in")).toFile());
        savedIoMetaData.setDefaultAnnotationSavingDirectory(Files.createDirectory(tempDir.resolve("out")).toFile());

        preferencesStore.save(true, savedIoMetaData);

        final IoMetaData loadedIoMetaData = new IoMetaData();
        preferencesStore.loadDirectories(loadedIoMetaData);

        assertTrue(preferencesStore.loadWindowMaximized());
        assertEquals(savedIoMetaData.getDefaultImageLoadingDirectory(),
                loadedIoMetaData.getDefaultImageLoadingDirectory());
        assertEquals(savedIoMetaData.getDefaultAnnotationLoadingDirectory(),
                loadedIoMetaData.getDefaultAnnotationLoadingDirectory());
        assertEquals(savedIoMetaData.getDefaultAnnotationSavingDirectory(),
                loadedIoMetaData.getDefaultAnnotationSavingDirectory());
    }

    @Test
    void onSave_ShouldUseTheExistingPreferenceKeys() throws IOException {
        final File imageDirectory = Files.createDirectory(tempDir.resolve("images")).toFile();
        final IoMetaData ioMetaData = new IoMetaData();
        ioMetaData.setDefaultImageLoadingDirectory(imageDirectory);

        preferencesStore.save(true, ioMetaData);

        // Changing the keys would lose the preferences stored by earlier versions.
        assertTrue(preferences.getBoolean("isMaximized", false));
        assertEquals(imageDirectory.toString(), preferences.get("currentImageLoadingDirectory", null));
    }

    @Test
    void onSave_WhenDirectoryNotSet_ShouldKeepPreviouslyStoredDirectory() throws IOException {
        final File annotationDirectory = Files.createDirectory(tempDir.resolve("in")).toFile();
        preferences.put("currentAnnotationLoadingDirectory", annotationDirectory.toString());

        preferencesStore.save(false, new IoMetaData());

        assertFalse(preferences.getBoolean("isMaximized", true));
        assertEquals(annotationDirectory.toString(), preferences.get("currentAnnotationLoadingDirectory", null));
    }

    @Test
    void onLoad_WhenStoredDirectoryIsMissingOrAFile_ShouldIgnoreIt() throws IOException {
        preferences.put("currentImageLoadingDirectory", tempDir.resolve("deleted").toString());
        preferences.put("currentAnnotationLoadingDirectory",
                Files.createFile(tempDir.resolve("file.txt")).toString());

        final IoMetaData ioMetaData = new IoMetaData();
        preferencesStore.loadDirectories(ioMetaData);

        assertNull(ioMetaData.getDefaultImageLoadingDirectory());
        assertNull(ioMetaData.getDefaultAnnotationLoadingDirectory());
    }

    @Test
    void onForApplication_ShouldUseTheControllerPackageNode() {
        // Earlier versions stored the preferences in the node of the Controller's package.
        assertEquals(Preferences.userNodeForPackage(Controller.class).absolutePath(),
                Preferences.userNodeForPackage(PreferencesStore.class).absolutePath());
    }

    @Test
    void onLoadInferenceSettings_WhenNothingStored_ShouldKeepDefaults() {
        final BoundingBoxPredictorClientConfig clientConfig = new BoundingBoxPredictorClientConfig();
        final BoundingBoxPredictorConfig predictorConfig = new BoundingBoxPredictorConfig();

        preferencesStore.loadInferenceSettings(clientConfig, predictorConfig);

        assertEquals(BoundingBoxPredictorClient.ServiceType.TORCH_SERVE, clientConfig.getServiceType());
        assertEquals("http://localhost", clientConfig.getInferenceUrl());
        assertEquals(8080, clientConfig.getInferencePort());
        assertEquals(8081, clientConfig.getManagementPort());
        assertNull(clientConfig.getInferenceModelName());
        assertEquals(BoundingBoxPredictorClientConfig.DEFAULT_PREDICTION_PATH, clientConfig.getPredictionPath());
        assertFalse(predictorConfig.isInferenceEnabled());
        assertEquals(0.5, predictorConfig.getMinimumScore());
        assertTrue(predictorConfig.isMergeCategories());
        assertTrue(predictorConfig.isResizeImages());
        assertEquals(600, predictorConfig.getImageResizeWidth());
    }

    @Test
    void onSaveAndLoadInferenceSettings_ShouldRestoreAllButTheApiKey() {
        final BoundingBoxPredictorClientConfig savedClientConfig = new BoundingBoxPredictorClientConfig();
        savedClientConfig.setServiceType(BoundingBoxPredictorClient.ServiceType.LIT_SERVE);
        savedClientConfig.setInferenceUrl("http://gpu-server");
        savedClientConfig.setInferencePort(8000);
        savedClientConfig.setManagementUrl("http://gpu-server");
        savedClientConfig.setManagementPort(9001);
        savedClientConfig.setInferenceModelName("detector");
        savedClientConfig.setPredictionPath("/detect");
        savedClientConfig.setApiKey("secret");

        final BoundingBoxPredictorConfig savedPredictorConfig = new BoundingBoxPredictorConfig();
        savedPredictorConfig.setInferenceEnabled(true);
        savedPredictorConfig.setMinimumScore(0.75);
        savedPredictorConfig.setMergeCategories(false);
        savedPredictorConfig.setResizeImages(false);
        savedPredictorConfig.setImageResizeWidth(800);
        savedPredictorConfig.setImageResizeHeight(400);
        savedPredictorConfig.setImageResizeKeepRatio(false);

        preferencesStore.saveInferenceSettings(savedClientConfig, savedPredictorConfig);

        final BoundingBoxPredictorClientConfig clientConfig = new BoundingBoxPredictorClientConfig();
        final BoundingBoxPredictorConfig predictorConfig = new BoundingBoxPredictorConfig();
        preferencesStore.loadInferenceSettings(clientConfig, predictorConfig);

        assertEquals(BoundingBoxPredictorClient.ServiceType.LIT_SERVE, clientConfig.getServiceType());
        assertEquals("http://gpu-server", clientConfig.getInferenceUrl());
        assertEquals(8000, clientConfig.getInferencePort());
        assertEquals("http://gpu-server", clientConfig.getManagementUrl());
        assertEquals(9001, clientConfig.getManagementPort());
        assertEquals("detector", clientConfig.getInferenceModelName());
        assertEquals("/detect", clientConfig.getPredictionPath());
        assertNull(clientConfig.getApiKey());
        assertTrue(predictorConfig.isInferenceEnabled());
        assertEquals(0.75, predictorConfig.getMinimumScore());
        assertFalse(predictorConfig.isMergeCategories());
        assertFalse(predictorConfig.isResizeImages());
        assertEquals(800, predictorConfig.getImageResizeWidth());
        assertEquals(400, predictorConfig.getImageResizeHeight());
        assertFalse(predictorConfig.getImageResizeKeepRatio());

        assertTrue(Arrays.stream(assertDoesNotThrow(preferences::keys))
                                   .noneMatch(key -> key.toLowerCase(Locale.ROOT).contains("key")),
                "The API key must not be stored.");
    }

    @Test
    void onSaveInferenceSettings_WhenModelNameCleared_ShouldRemoveStoredName() {
        final BoundingBoxPredictorClientConfig clientConfig = new BoundingBoxPredictorClientConfig();
        clientConfig.setInferenceModelName("detector");
        preferencesStore.saveInferenceSettings(clientConfig, new BoundingBoxPredictorConfig());

        clientConfig.setInferenceModelName(null);
        preferencesStore.saveInferenceSettings(clientConfig, new BoundingBoxPredictorConfig());

        final BoundingBoxPredictorClientConfig loadedClientConfig = new BoundingBoxPredictorClientConfig();
        preferencesStore.loadInferenceSettings(loadedClientConfig, new BoundingBoxPredictorConfig());
        assertNull(loadedClientConfig.getInferenceModelName());
    }

    @Test
    void onLoadInferenceSettings_WhenStoredServiceTypeIsUnknown_ShouldKeepTheCurrentOne() {
        preferences.put("inferenceServiceType", "TRITON");

        final BoundingBoxPredictorClientConfig clientConfig = new BoundingBoxPredictorClientConfig();
        preferencesStore.loadInferenceSettings(clientConfig, new BoundingBoxPredictorConfig());

        assertEquals(BoundingBoxPredictorClient.ServiceType.TORCH_SERVE, clientConfig.getServiceType());
    }
}
