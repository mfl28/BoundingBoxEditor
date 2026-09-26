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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class RecentImageFoldersTest {
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
    void onAdd_ShouldMoveTheFolderToTheFrontAndKeepItInThePreferences() throws IOException {
        final File folderA = createFolder("a");
        final File folderB = createFolder("b");
        final RecentImageFolders recentImageFolders = new RecentImageFolders(preferencesStore);

        recentImageFolders.add(folderA);
        recentImageFolders.add(folderB);
        recentImageFolders.add(folderA);

        assertEquals(List.of(folderA, folderB), recentImageFolders.getFolders());
        assertEquals(List.of(folderA, folderB), new RecentImageFolders(preferencesStore).getFolders());
    }

    @Test
    void onAdd_ShouldKeepOnlyTheMostRecentFolders() throws IOException {
        final RecentImageFolders recentImageFolders = new RecentImageFolders(preferencesStore);
        final List<File> folders = new ArrayList<>();

        for(int i = 0; i < RecentImageFolders.MAXIMUM_COUNT + 2; ++i) {
            final File folder = createFolder("folder_" + i);
            folders.addFirst(folder);
            recentImageFolders.add(folder);
        }

        assertEquals(folders.subList(0, RecentImageFolders.MAXIMUM_COUNT), recentImageFolders.getFolders());
    }

    @Test
    void onLoad_ShouldDropFoldersThatNoLongerExist() throws IOException {
        final File existingFolder = createFolder("existing");
        final File deletedFolder = createFolder("deleted");
        final RecentImageFolders recentImageFolders = new RecentImageFolders(preferencesStore);
        recentImageFolders.add(existingFolder);
        recentImageFolders.add(deletedFolder);

        Files.delete(deletedFolder.toPath());

        assertEquals(List.of(existingFolder), new RecentImageFolders(preferencesStore).getFolders());
    }

    @Test
    void onRemoveAndClear_ShouldUpdateThePreferences() throws IOException {
        final File folderA = createFolder("a");
        final File folderB = createFolder("b");
        final RecentImageFolders recentImageFolders = new RecentImageFolders(preferencesStore);
        recentImageFolders.add(folderA);
        recentImageFolders.add(folderB);

        recentImageFolders.remove(folderB);
        assertEquals(List.of(folderA), new RecentImageFolders(preferencesStore).getFolders());

        recentImageFolders.clear();
        assertTrue(new RecentImageFolders(preferencesStore).getFolders().isEmpty());
        assertNull(preferences.get("recentImageFolders", null));
    }

    private File createFolder(String name) throws IOException {
        return Files.createDirectory(tempDir.resolve(name)).toFile().getAbsoluteFile();
    }
}
