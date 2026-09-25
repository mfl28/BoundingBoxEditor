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

import java.io.File;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Stores the user's preferences between application runs: whether the window is maximized and the last used image
 * and annotation folders.
 */
class PreferencesStore {
    private static final String IS_WINDOW_MAXIMIZED_PREFERENCE_NAME = "isMaximized";
    private static final String CURRENT_IMAGE_LOADING_DIRECTORY_PREFERENCE_NAME = "currentImageLoadingDirectory";
    private static final String CURRENT_ANNOTATION_LOADING_DIRECTORY_PREFERENCE_NAME =
            "currentAnnotationLoadingDirectory";
    private static final String CURRENT_ANNOTATION_SAVING_DIRECTORY_PREFERENCE_NAME =
            "currentAnnotationSavingDirectory";

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
