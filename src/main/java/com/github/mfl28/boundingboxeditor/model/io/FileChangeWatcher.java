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

import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;

/**
 * Used to check if (image) files of the currently loaded folder were modified/removed and initiate folder reload when necessary.
 */
public class FileChangeWatcher implements Runnable {
    private final Path directoryToWatch;
    private final Set<String> fileNamesToWatch;
    private final Runnable onFilesChangedHandler;

    /**
     * Creates a new file change watcher
     *
     * @param directoryToWatch      the path to the directory that should be watched
     * @param fileNamesToWatch      the set of names of files to watch
     * @param onFilesChangedHandler what should be done when files were modified/removed
     */
    public FileChangeWatcher(Path directoryToWatch, Set<String> fileNamesToWatch, Runnable onFilesChangedHandler) {
        this.directoryToWatch = directoryToWatch;
        this.fileNamesToWatch = fileNamesToWatch;
        this.onFilesChangedHandler = onFilesChangedHandler;
    }

    @Override
    public void run() {
        try(final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directoryToWatch.register(watchService, StandardWatchEventKinds.ENTRY_DELETE,
                                      StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey key;
            while((key = watchService.take()) != null) {
                if(affectsWatchedFiles(key.pollEvents(), directoryToWatch, fileNamesToWatch)) {
                    Platform.runLater(onFilesChangedHandler);
                } else {
                    key.reset();
                }
            }
        } catch(InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch(IOException e) {
            Platform.runLater(onFilesChangedHandler);
        }
    }

    static boolean affectsWatchedFiles(List<WatchEvent<?>> events, Path directory, Set<String> fileNamesToWatch) {
        for(WatchEvent<?> event : events) {
            if(StandardWatchEventKinds.OVERFLOW.equals(event.kind())) {
                // Events were lost and an overflow event carries no file name, so check whether any watched
                // file was removed. Lost modification events cannot be recovered.
                if(fileNamesToWatch.stream().anyMatch(fileName -> !Files.exists(directory.resolve(fileName)))) {
                    return true;
                }
            } else if(event.context() != null && fileNamesToWatch.contains(event.context().toString())) {
                return true;
            }
        }

        return false;
    }
}
