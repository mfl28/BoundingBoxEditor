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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.prefs.Preferences;

/**
 * Stores the user's preferences between application runs: whether the window is maximized, the last used image
 * and annotation folders, and the inference settings (except the API key, which is not stored in plain text).
 */
class PreferencesStore {
    private static final String IS_WINDOW_MAXIMIZED_PREFERENCE_NAME = "isMaximized";
    private static final String CURRENT_IMAGE_LOADING_DIRECTORY_PREFERENCE_NAME = "currentImageLoadingDirectory";
    private static final String CURRENT_ANNOTATION_LOADING_DIRECTORY_PREFERENCE_NAME =
            "currentAnnotationLoadingDirectory";
    private static final String CURRENT_ANNOTATION_SAVING_DIRECTORY_PREFERENCE_NAME =
            "currentAnnotationSavingDirectory";
    private static final String RECENT_IMAGE_FOLDERS_PREFERENCE_NAME = "recentImageFolders";
    private static final String RECENT_IMAGE_FOLDERS_SEPARATOR = "\n";
    private static final String INFERENCE_ENABLED = "inferenceEnabled";
    private static final String INFERENCE_SERVICE_TYPE = "inferenceServiceType";
    private static final String INFERENCE_URL = "inferenceUrl";
    private static final String INFERENCE_PORT = "inferencePort";
    private static final String INFERENCE_MANAGEMENT_URL = "inferenceManagementUrl";
    private static final String INFERENCE_MANAGEMENT_PORT = "inferenceManagementPort";
    private static final String INFERENCE_MODEL_NAME = "inferenceModelName";
    private static final String INFERENCE_PREDICTION_PATH = "inferencePredictionPath";
    private static final String INFERENCE_MINIMUM_SCORE = "inferenceMinimumScore";
    private static final String INFERENCE_MERGE_CATEGORIES = "inferenceMergeCategories";
    private static final String INFERENCE_RESIZE_IMAGES = "inferenceResizeImages";
    private static final String INFERENCE_RESIZE_WIDTH = "inferenceResizeWidth";
    private static final String INFERENCE_RESIZE_HEIGHT = "inferenceResizeHeight";
    private static final String INFERENCE_RESIZE_KEEP_RATIO = "inferenceResizeKeepRatio";

    private final Preferences preferences;

    /**
     * Creates a store using the provided preferences node.
     *
     * @param preferences the preferences node
     */
    PreferencesStore(Preferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Creates the store of the application's user preferences.
     *
     * @return the store
     */
    static PreferencesStore forApplication() {
        // The node of this package, which the preferences were always stored in.
        return new PreferencesStore(Preferences.userNodeForPackage(PreferencesStore.class));
    }

    /**
     * Returns whether the window was maximized.
     *
     * @return true if the window was maximized
     */
    boolean loadWindowMaximized() {
        return preferences.getBoolean(IS_WINDOW_MAXIMIZED_PREFERENCE_NAME, false);
    }

    /**
     * Sets the stored last used folders as the default folders, if they still exist.
     *
     * @param ioMetaData the object holding the default folders
     */
    void loadDirectories(IoMetaData ioMetaData) {
        loadDirectory(CURRENT_IMAGE_LOADING_DIRECTORY_PREFERENCE_NAME, ioMetaData::setDefaultImageLoadingDirectory);
        loadDirectory(CURRENT_ANNOTATION_LOADING_DIRECTORY_PREFERENCE_NAME,
                ioMetaData::setDefaultAnnotationLoadingDirectory);
        loadDirectory(CURRENT_ANNOTATION_SAVING_DIRECTORY_PREFERENCE_NAME,
                ioMetaData::setDefaultAnnotationSavingDirectory);
    }

    /**
     * Stores whether the window is maximized and the default folders (the ones that are set).
     *
     * @param windowMaximized whether the window is maximized
     * @param ioMetaData      the object holding the default folders
     */
    void save(boolean windowMaximized, IoMetaData ioMetaData) {
        preferences.putBoolean(IS_WINDOW_MAXIMIZED_PREFERENCE_NAME, windowMaximized);
        saveDirectory(CURRENT_IMAGE_LOADING_DIRECTORY_PREFERENCE_NAME, ioMetaData.getDefaultImageLoadingDirectory());
        saveDirectory(CURRENT_ANNOTATION_LOADING_DIRECTORY_PREFERENCE_NAME,
                ioMetaData.getDefaultAnnotationLoadingDirectory());
        saveDirectory(CURRENT_ANNOTATION_SAVING_DIRECTORY_PREFERENCE_NAME,
                ioMetaData.getDefaultAnnotationSavingDirectory());
    }

    /**
     * Sets the stored inference settings. Settings that were never stored keep their current values.
     *
     * @param clientConfig    the connection settings
     * @param predictorConfig the prediction settings
     */
    void loadInferenceSettings(BoundingBoxPredictorClientConfig clientConfig,
                               BoundingBoxPredictorConfig predictorConfig) {
        final String serviceTypeName = preferences.get(INFERENCE_SERVICE_TYPE, null);

        if(serviceTypeName != null) {
            try {
                clientConfig.setServiceType(BoundingBoxPredictorClient.ServiceType.valueOf(serviceTypeName));
            } catch(IllegalArgumentException _) {
                // A server type that no longer exists: the current one is kept.
            }
        }

        clientConfig.setInferenceUrl(preferences.get(INFERENCE_URL, clientConfig.getInferenceUrl()));
        clientConfig.setInferencePort(preferences.getInt(INFERENCE_PORT, clientConfig.getInferencePort()));
        clientConfig.setManagementUrl(preferences.get(INFERENCE_MANAGEMENT_URL, clientConfig.getManagementUrl()));
        clientConfig.setManagementPort(preferences.getInt(INFERENCE_MANAGEMENT_PORT, clientConfig.getManagementPort()));
        clientConfig.setInferenceModelName(preferences.get(INFERENCE_MODEL_NAME, clientConfig.getInferenceModelName()));
        clientConfig.setPredictionPath(preferences.get(INFERENCE_PREDICTION_PATH, clientConfig.getPredictionPath()));

        predictorConfig.setInferenceEnabled(preferences.getBoolean(INFERENCE_ENABLED,
                predictorConfig.isInferenceEnabled()));
        predictorConfig.setMinimumScore(preferences.getDouble(INFERENCE_MINIMUM_SCORE,
                predictorConfig.getMinimumScore()));
        predictorConfig.setMergeCategories(preferences.getBoolean(INFERENCE_MERGE_CATEGORIES,
                predictorConfig.isMergeCategories()));
        predictorConfig.setResizeImages(preferences.getBoolean(INFERENCE_RESIZE_IMAGES,
                predictorConfig.isResizeImages()));
        predictorConfig.setImageResizeWidth(preferences.getInt(INFERENCE_RESIZE_WIDTH,
                predictorConfig.getImageResizeWidth()));
        predictorConfig.setImageResizeHeight(preferences.getInt(INFERENCE_RESIZE_HEIGHT,
                predictorConfig.getImageResizeHeight()));
        predictorConfig.setImageResizeKeepRatio(preferences.getBoolean(INFERENCE_RESIZE_KEEP_RATIO,
                predictorConfig.getImageResizeKeepRatio()));
    }

    /**
     * Stores the inference settings, except the API key.
     *
     * @param clientConfig    the connection settings
     * @param predictorConfig the prediction settings
     */
    void saveInferenceSettings(BoundingBoxPredictorClientConfig clientConfig,
                               BoundingBoxPredictorConfig predictorConfig) {
        preferences.put(INFERENCE_SERVICE_TYPE, clientConfig.getServiceType().name());
        putOrRemove(INFERENCE_URL, clientConfig.getInferenceUrl());
        preferences.putInt(INFERENCE_PORT, clientConfig.getInferencePort());
        putOrRemove(INFERENCE_MANAGEMENT_URL, clientConfig.getManagementUrl());
        preferences.putInt(INFERENCE_MANAGEMENT_PORT, clientConfig.getManagementPort());
        putOrRemove(INFERENCE_MODEL_NAME, clientConfig.getInferenceModelName());
        putOrRemove(INFERENCE_PREDICTION_PATH, clientConfig.getPredictionPath());

        preferences.putBoolean(INFERENCE_ENABLED, predictorConfig.isInferenceEnabled());
        preferences.putDouble(INFERENCE_MINIMUM_SCORE, predictorConfig.getMinimumScore());
        preferences.putBoolean(INFERENCE_MERGE_CATEGORIES, predictorConfig.isMergeCategories());
        preferences.putBoolean(INFERENCE_RESIZE_IMAGES, predictorConfig.isResizeImages());
        preferences.putInt(INFERENCE_RESIZE_WIDTH, predictorConfig.getImageResizeWidth());
        preferences.putInt(INFERENCE_RESIZE_HEIGHT, predictorConfig.getImageResizeHeight());
        preferences.putBoolean(INFERENCE_RESIZE_KEEP_RATIO, predictorConfig.getImageResizeKeepRatio());
    }

    /**
     * Loads the recently opened image folders that still exist.
     *
     * @return the folders, most recent first
     */
    List<File> loadRecentImageFolders() {
        final String folders = preferences.get(RECENT_IMAGE_FOLDERS_PREFERENCE_NAME, null);

        if(folders == null || folders.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(folders.split(RECENT_IMAGE_FOLDERS_SEPARATOR))
                     .map(File::new)
                     .filter(File::isDirectory)
                     .toList();
    }

    /**
     * Saves the recently opened image folders.
     *
     * @param folders the folders, most recent first
     */
    void saveRecentImageFolders(List<File> folders) {
        putOrRemove(RECENT_IMAGE_FOLDERS_PREFERENCE_NAME, folders.isEmpty() ? null :
                folders.stream().map(File::getPath).collect(Collectors.joining(RECENT_IMAGE_FOLDERS_SEPARATOR)));
    }

    private void putOrRemove(String preferenceName, String value) {
        if(value == null) {
            preferences.remove(preferenceName);
        } else {
            preferences.put(preferenceName, value);
        }
    }

    private void loadDirectory(String preferenceName, Consumer<File> directorySetter) {
        final String directoryPath = preferences.get(preferenceName, null);

        if(directoryPath != null) {
            final File directory = new File(directoryPath);

            if(directory.exists() && directory.isDirectory()) {
                directorySetter.accept(directory);
            }
        }
    }

    private void saveDirectory(String preferenceName, File directory) {
        if(directory != null) {
            preferences.put(preferenceName, directory.toString());
        }
    }
}
