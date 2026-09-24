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
package com.github.mfl28.boundingboxeditor.model.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileChangeWatcherTest {
    private static final Set<String> WATCHED_FILES = Set.of("a.jpg", "b.jpg");

    @Test
    void onOverflowEvent_WhenWatchedFilesStillExist_ShouldNotReportChange(@TempDir Path tempDir) throws IOException {
        createWatchedFiles(tempDir);

        assertFalse(FileChangeWatcher.affectsWatchedFiles(
                List.of(event(StandardWatchEventKinds.OVERFLOW, null)), tempDir, WATCHED_FILES));
    }

    @Test
    void onOverflowEvent_WhenWatchedFileWasRemoved_ShouldReportChange(@TempDir Path tempDir) throws IOException {
        createWatchedFiles(tempDir);
        Files.delete(tempDir.resolve("b.jpg"));

        assertTrue(FileChangeWatcher.affectsWatchedFiles(
                List.of(event(StandardWatchEventKinds.OVERFLOW, null)), tempDir, WATCHED_FILES));
    }

    @Test
    void onFileEvents_ShouldOnlyReportChangesToWatchedFiles(@TempDir Path tempDir) {
        assertTrue(FileChangeWatcher.affectsWatchedFiles(
                List.of(event(StandardWatchEventKinds.ENTRY_MODIFY, Path.of("other.jpg")),
                        event(StandardWatchEventKinds.ENTRY_DELETE, Path.of("a.jpg"))),
                tempDir, WATCHED_FILES));

        assertFalse(FileChangeWatcher.affectsWatchedFiles(
                List.of(event(StandardWatchEventKinds.ENTRY_DELETE, Path.of("other.jpg"))),
                tempDir, WATCHED_FILES));
    }

    private static void createWatchedFiles(Path directory) throws IOException {
        for(String fileName : WATCHED_FILES) {
            Files.createFile(directory.resolve(fileName));
        }
    }

    private static <T> WatchEvent<T> event(WatchEvent.Kind<T> kind, T context) {
        return new WatchEvent<>() {
            @Override
            public Kind<T> kind() {
                return kind;
            }

            @Override
            public int count() {
                return 1;
            }

            @Override
            public T context() {
                return context;
            }
        };
    }
}
