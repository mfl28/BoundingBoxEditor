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

import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Writes autosaves into an {@link AutosaveStore} on one background thread. Saves that are requested while an earlier
 * one is still waiting replace it, so that a slow disk never makes saves pile up: at most one save runs and one (the
 * newest) waits.
 */
final class AutosaveWriter {
    private static final long TIMEOUT_SECONDS = 5;

    private final AutosaveStore store;
    private final ExecutorService executor;
    private final Runnable onSaved;
    private final Consumer<IOException> onFailed;
    private final AtomicReference<PendingSave> pendingSave = new AtomicReference<>();

    /**
     * Creates a writer.
     *
     * @param store    the store to write into
     * @param executor the executor (with one thread) that writes
     * @param onSaved  called on the writer thread after each save
     * @param onFailed called on the writer thread when a save fails
     */
    AutosaveWriter(AutosaveStore store, ExecutorService executor, Runnable onSaved, Consumer<IOException> onFailed) {
        this.store = store;
        this.executor = executor;
        this.onSaved = onSaved;
        this.onFailed = onFailed;
    }

    /**
     * Creates a writer with its own daemon thread.
     */
    static AutosaveWriter create(AutosaveStore store, Runnable onSaved, Consumer<IOException> onFailed) {
        return new AutosaveWriter(store, Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "autosave");
            thread.setDaemon(true);
            return thread;
        }), onSaved, onFailed);
    }

    /**
     * Saves annotations in the background. If an earlier save hasn't started yet, only these annotations are saved.
     *
     * @param imageFolder the image folder
     * @param annotations the annotations (which must not change anymore)
     */
    void save(File imageFolder, ImageAnnotationData annotations) {
        if(pendingSave.getAndSet(new PendingSave(imageFolder, annotations)) == null) {
            executor.execute(this::writePendingSave);
        }
    }

    /**
     * Deletes the autosave of a folder after the saves that were requested before, and waits for it (so that it is
     * gone even if the app quits right afterwards).
     *
     * @param imageFolder the image folder
     */
    void delete(File imageFolder) {
        try {
            executor.submit(() -> store.delete(imageFolder)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch(ExecutionException | TimeoutException e) {
            store.delete(imageFolder);
        }
    }

    /**
     * Waits until the saves and deletions that were requested are done.
     */
    void awaitCompletion() {
        try {
            executor.submit(() -> { }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch(ExecutionException | TimeoutException e) {
            // Nothing to wait for anymore.
        }
    }

    private void writePendingSave() {
        final PendingSave save = pendingSave.getAndSet(null);

        if(save == null) {
            return;
        }

        try {
            store.save(save.imageFolder(), save.annotations());
            onSaved.run();
        } catch(IOException e) {
            onFailed.accept(e);
        }
    }

    private record PendingSave(File imageFolder, ImageAnnotationData annotations) {
    }
}
