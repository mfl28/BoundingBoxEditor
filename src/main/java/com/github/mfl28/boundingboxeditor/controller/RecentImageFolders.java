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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;

/**
 * The recently opened image folders, most recent first, kept in the preferences.
 */
class RecentImageFolders {
    static final int MAXIMUM_COUNT = 8;

    private final PreferencesStore preferencesStore;
    private final ObservableList<File> folders = FXCollections.observableArrayList();
    private final ObservableList<File> unmodifiableFolders = FXCollections.unmodifiableObservableList(folders);

    /**
     * Creates the list with the folders stored in the preferences.
     *
     * @param preferencesStore the preferences store
     */
    RecentImageFolders(PreferencesStore preferencesStore) {
        this.preferencesStore = preferencesStore;
        folders.setAll(preferencesStore.loadRecentImageFolders().stream().limit(MAXIMUM_COUNT).toList());
    }

    /**
     * Returns the folders.
     *
     * @return the folders, most recent first (not modifiable)
     */
    ObservableList<File> getFolders() {
        return unmodifiableFolders;
    }

    /**
     * Moves the provided folder to the front, adding it if necessary.
     *
     * @param folder the opened folder
     */
    void add(File folder) {
        final File absoluteFolder = folder.getAbsoluteFile();
        folders.remove(absoluteFolder);
        folders.addFirst(absoluteFolder);

        if(folders.size() > MAXIMUM_COUNT) {
            folders.remove(MAXIMUM_COUNT, folders.size());
        }

        save();
    }

    /**
     * Removes the provided folder.
     *
     * @param folder the folder
     */
    void remove(File folder) {
        if(folders.remove(folder.getAbsoluteFile())) {
            save();
        }
    }

    /**
     * Removes all folders.
     */
    void clear() {
        folders.clear();
        save();
    }

    private void save() {
        preferencesStore.saveRecentImageFolders(folders);
    }
}
