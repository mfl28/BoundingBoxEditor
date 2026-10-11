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
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import javafx.animation.PauseTransition;
import javafx.scene.control.ButtonBar;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Saves the not yet exported annotations of the open folder a few seconds after the last change (into an
 * {@link AutosaveStore}), deletes the autosave once they are exported or discarded, and offers to restore it when the
 * folder is opened again, or at startup.
 */
class AutosaveController {
    static final String RESTORE_DIALOG_TITLE = "Restore Unsaved Annotations";
    private static final Duration SAVE_DELAY = Duration.seconds(5);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

    private final Model model;
    private final DialogService dialogService;
    private final Stage stage;
    private final RecentImageFolders recentImageFolders;
    private final AutosaveStore store;
    private final AutosaveWriter writer;
    private final Timer saveTimer;
    private final Operations operations;
    // The folder whose images are shown, which the autosave belongs to.
    private File currentFolder;
    // While it is open whether to restore the shown folder's autosave, and while it is restored, nothing is saved.
    private boolean paused = false;
    // After the window was closed.
    private boolean stopped = false;
    private boolean restoring = false;
    // Set when the user agreed at startup to restore the last folder's autosave, so that it isn't asked again.
    private File folderToRestoreWithoutAsking;

    /**
     * What the autosave needs from the rest of the app.
     */
    interface Operations {
        /**
         * Stores the shown image's shapes in the model.
         */
        void updateModelFromView();

        boolean isDrawingInProgress();

        /**
         * Starts importing the annotations of an autosave file (in the JSON format). The import calls
         * {@link #onRestoreFinished(boolean)} or {@link #onRestoreFailed()} when it is done.
         */
        void restore(File autosaveFile);

        /**
         * Opens an image folder (which then calls {@link #onFolderOpened(File)}).
         */
        void openImageFolder(File folder);

        /**
         * Runs an action on the JavaFX thread (the autosave thread reports through it).
         */
        void runOnUiThread(Runnable action);

        /**
         * Shows when the annotations were last saved automatically.
         *
         * @param time the time, or null if there is no autosave
         */
        void showAutosaveTime(Instant time);

        void showAutosaveFailed(String reason);

        /**
         * Creates the delay after a change ({@link AutosaveController#createSaveTimer} in the app).
         *
         * @param action what to do when the delay runs out
         */
        Timer createTimer(Runnable action);
    }

    /**
     * The delay after a change before the annotations are saved.
     */
    interface Timer {
        void restart();

        void stop();
    }

    /**
     * Creates the autosave for the user's application data folder, with its own writer thread.
     */
    static AutosaveController create(Model model, DialogService dialogService, Stage stage,
                                     RecentImageFolders recentImageFolders, Operations operations) {
        return new AutosaveController(model, dialogService, stage, recentImageFolders, AutosaveStore.createDefault(),
                AutosaveWriter.createExecutor(), operations);
    }

    /**
     * Creates the autosave.
     *
     * @param executor the executor (with one thread) that writes the autosaves
     */
    AutosaveController(Model model, DialogService dialogService, Stage stage, RecentImageFolders recentImageFolders,
                       AutosaveStore store, ExecutorService executor, Operations operations) {
        this.model = model;
        this.dialogService = dialogService;
        this.stage = stage;
        this.recentImageFolders = recentImageFolders;
        this.store = store;
        this.operations = operations;
        this.writer = new AutosaveWriter(store, executor, this::onSaved,
                exception -> onSaveFailed(exception.getMessage()));
        this.saveTimer = operations.createTimer(this::saveNow);

        // Exported (or discarded, when another folder is opened) annotations need no autosave anymore.
        model.savedProperty().addListener((observable, oldValue, newValue) -> {
            if(paused) {
                return;
            }

            if(Boolean.TRUE.equals(newValue)) {
                saveTimer.stop();
                delete(currentFolder);
            } else {
                schedule();
            }
        });
    }

    AutosaveStore getStore() {
        return store;
    }

    /**
     * Restarts the delay after a change.
     */
    void onEdited() {
        schedule();
    }

    /**
     * Saves the annotations of the shown folder now (as after the delay), unless they are exported.
     */
    void saveNow() {
        final File folder = currentFolder;

        if(folder == null || paused || stopped) {
            return;
        }

        if(operations.isDrawingInProgress()) {
            schedule();
            return;
        }

        operations.updateModelFromView();

        if(model.isSaved()) {
            delete(folder);
            return;
        }

        writer.save(folder, createSnapshot());
    }

    /**
     * Waits until the autosaves that were started are written (or deleted).
     */
    void awaitCompletion() {
        writer.awaitCompletion();
    }

    /**
     * Called when a folder was opened: asks whether to restore its autosave (unless the user already agreed at
     * startup), and restores or deletes it.
     */
    void onFolderOpened(File folder) {
        currentFolder = folder;
        final Optional<Instant> lastSaved = store.lastSaved(folder);
        final boolean restoreWithoutAsking = folder.equals(folderToRestoreWithoutAsking);
        folderToRestoreWithoutAsking = null;

        if(lastSaved.isPresent()) {
            paused = true;
            saveTimer.stop();

            final boolean restore = restoreWithoutAsking
                    || dialogService.displayYesNoDialogAndGetResult(RESTORE_DIALOG_TITLE,
                    String.format("The annotations of this folder from %s were not exported. They were saved "
                                          + "automatically. Restore them?",
                                  TIME_FORMATTER.format(lastSaved.get())), stage) == ButtonBar.ButtonData.YES;

            if(restore) {
                restoring = true;
                operations.restore(store.fileFor(folder).toFile());
            } else {
                paused = false;
                delete(folder);
            }
        }

        showAutosaveTime();
    }

    /**
     * Called when no folder is shown anymore.
     */
    void onFolderClosed() {
        currentFolder = null;
        showAutosaveTime();
    }

    boolean isRestoring() {
        return restoring;
    }

    /**
     * Called when the autosave was imported.
     *
     * @param anyRestored whether any annotations were restored
     */
    void onRestoreFinished(boolean anyRestored) {
        restoring = false;
        paused = false;

        // Restored annotations are still not exported.
        if(anyRestored) {
            model.setSaved(false);
        }
    }

    void onRestoreFailed() {
        restoring = false;
        paused = false;
    }

    /**
     * Called when the user quits without saving: the annotations are discarded, so is their autosave.
     */
    void discard() {
        delete(currentFolder);
    }

    /**
     * Stops saving, after the save that is running (once the window is closed or the app quits).
     */
    void stop() {
        stopped = true;
        saveTimer.stop();
        writer.awaitCompletion();
    }

    /**
     * At startup, offers to reopen the most recently opened folder if its annotations were not exported.
     */
    void offerToRestoreLastSession() {
        final List<File> folders = recentImageFolders.getFolders();

        if(folders.isEmpty() || currentFolder != null) {
            return;
        }

        final File folder = folders.getFirst();
        final Optional<Instant> lastSaved = store.lastSaved(folder);

        if(!folder.isDirectory() || lastSaved.isEmpty()) {
            return;
        }

        final ButtonBar.ButtonData answer = dialogService.displayYesNoDialogAndGetResult(
                RESTORE_DIALOG_TITLE,
                String.format("The annotations of the folder \"%s\" from %s were not exported. They were saved "
                                      + "automatically. Open the folder and restore them?",
                              folder.getPath(), TIME_FORMATTER.format(lastSaved.get())), stage);

        if(answer == ButtonBar.ButtonData.YES) {
            openFolderAndRestore(folder);
        }
    }

    /**
     * Opens an image folder and restores its autosave without asking.
     *
     * @param folder the image folder
     */
    void openFolderAndRestore(File folder) {
        folderToRestoreWithoutAsking = folder;
        operations.openImageFolder(folder);
    }

    // Called on the autosave thread.
    private void onSaved() {
        operations.runOnUiThread(this::showAutosaveTime);
    }

    // Called on the autosave thread.
    private void onSaveFailed(String reason) {
        operations.runOnUiThread(() -> operations.showAutosaveFailed(reason));
    }

    private void schedule() {
        if(currentFolder != null && !paused && !stopped) {
            saveTimer.restart();
        }
    }

    /**
     * Deletes the autosave of a folder, after the autosaves that were already started, and waits for it (so that it
     * is gone even if the app quits right afterwards).
     */
    private void delete(File folder) {
        if(folder == null) {
            return;
        }

        writer.delete(folder);
        showAutosaveTime();
    }

    private void showAutosaveTime() {
        operations.showAutosaveTime(currentFolder == null ? null : store.lastSaved(currentFolder).orElse(null));
    }

    /**
     * Copies the annotations (with their tags and parts), so that they can be saved off the JavaFX thread while the
     * user goes on editing.
     */
    private ImageAnnotationData createSnapshot() {
        final ImageAnnotationData data = model.createImageAnnotationData();
        final List<ImageAnnotation> annotations = data.imageAnnotations().stream()
                .map(annotation -> new ImageAnnotation(annotation.getImageMetaData(),
                        annotation.getBoundingShapeData().stream().map(ShapeClipboardController::copyOf).toList()))
                .toList();
        return new ImageAnnotationData(annotations, Map.copyOf(data.categoryNameToBoundingShapeCountMap()),
                Map.copyOf(data.categoryNameToCategoryMap()));
    }

    /**
     * Creates the delay after a change for the app: a JavaFX transition.
     *
     * @param action what to do when the delay runs out
     * @return the timer
     */
    static Timer createSaveTimer(Runnable action) {
        final PauseTransition transition = new PauseTransition(SAVE_DELAY);
        transition.setOnFinished(event -> action.run());

        return new Timer() {
            @Override
            public void restart() {
                transition.playFromStart();
            }

            @Override
            public void stop() {
                transition.stop();
            }
        };
    }
}
