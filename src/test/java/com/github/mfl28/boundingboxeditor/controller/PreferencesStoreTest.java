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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
