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
import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class AutosaveControllerTest {
    private static final String IMAGE = "a.jpg";

    private final Model model = new Model();
    private final DialogService dialogService = mock(DialogService.class);
    private final RecentImageFolders recentImageFolders = mock(RecentImageFolders.class);
    private final AutosaveController.Operations operations = mock(AutosaveController.Operations.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ObjectCategory category = new ObjectCategory("boat", Color.RED);
    private FakeTimer timer;
    private AutosaveStore store;
    private AutosaveController autosave;

    @TempDir
    Path tempDir;
    private File folder;

    @BeforeEach
    void setUp() throws Exception {
        folder = Files.createDirectory(tempDir.resolve("images")).toFile();
        model.getImageFileNameToMetaDataMap().put(IMAGE,
                new ImageMetaData(IMAGE, "images", new File(folder, IMAGE).toURI().toString(), 100, 50, 3));
        model.setImageFiles(List.of(new File(folder, IMAGE)));
        when(recentImageFolders.getFolders()).thenReturn(FXCollections.observableArrayList());
        // The autosave thread reports through the UI thread, which these tests don't have.
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(operations).runOnUiThread(any());

        store = new AutosaveStore(tempDir.resolve("autosave"));
        autosave = new AutosaveController(model, dialogService, null, recentImageFolders, store, executor,
                action -> timer = new FakeTimer(action), operations);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void onEditing_ShouldSaveTheAnnotationsWhenTheDelayRunsOut() {
        autosave.onFolderOpened(folder);
        assertFalse(timer.running);

        annotate();
        assertTrue(timer.running);

        timer.fire();
        autosave.awaitCompletion();

        assertTrue(Files.isRegularFile(store.fileFor(folder)));
        verify(operations).updateModelFromView();
        verify(operations, atLeastOnce()).showAutosaveTime(notNull());
        verifyNoInteractions(dialogService);
    }

    @Test
    void onEditingAgain_ShouldRestartTheDelay() {
        autosave.onFolderOpened(folder);
        annotate();
        timer.running = false;

        autosave.onEdited();

        assertTrue(timer.running);
    }

    @Test
    void onSavingWhileAShapeIsDrawn_ShouldWaitForTheDrawingToFinish() {
        autosave.onFolderOpened(folder);
        annotate();
        when(operations.isDrawingInProgress()).thenReturn(true);

        timer.fire();
        autosave.awaitCompletion();

        assertFalse(Files.exists(store.fileFor(folder)));
        assertTrue(timer.running);
    }

    @Test
    void onExportingTheAnnotations_ShouldDeleteTheAutosave() {
        autosave.onFolderOpened(folder);
        annotate();
        timer.fire();
        autosave.awaitCompletion();
        clearInvocations(operations);

        model.setSaved(true);

        assertFalse(Files.exists(store.fileFor(folder)));
        assertFalse(timer.running);
        verify(operations).showAutosaveTime(null);
    }

    @Test
    void onSavingExportedAnnotations_ShouldDeleteTheAutosaveInstead() throws Exception {
        store.save(folder, annotationData());
        when(dialogService.displayYesNoDialogAndGetResult(any(), any(), any())).thenReturn(ButtonBar.ButtonData.YES);
        autosave.onFolderOpened(folder);
        autosave.onRestoreFinished(false);

        autosave.saveNow();

        assertFalse(Files.exists(store.fileFor(folder)));
    }

    @Test
    void onQuittingWithoutSaving_ShouldDeleteTheAutosave() {
        autosave.onFolderOpened(folder);
        annotate();
        timer.fire();
        autosave.awaitCompletion();

        autosave.discard();

        assertFalse(Files.exists(store.fileFor(folder)));
    }

    @Test
    void onOpeningAFolderWithAutosave_WhenRestoring_ShouldImportItAndKeepItUnsaved() throws Exception {
        store.save(folder, annotationData());
        when(dialogService.displayYesNoDialogAndGetResult(eq(AutosaveController.RESTORE_DIALOG_TITLE),
                                                          startsWith("The annotations of this folder from "),
                                                          any())).thenReturn(ButtonBar.ButtonData.YES);

        autosave.onFolderOpened(folder);

        verify(operations).restore(store.fileFor(folder).toFile());
        assertTrue(autosave.isRestoring());
        // Nothing is saved while it is restored.
        model.setSaved(false);
        assertFalse(timer.running);

        model.setSaved(true);
        autosave.onRestoreFinished(true);

        assertFalse(autosave.isRestoring());
        assertFalse(model.isSaved());
        assertTrue(timer.running);
        assertTrue(Files.isRegularFile(store.fileFor(folder)));
    }

    @Test
    void onOpeningAFolderWithAutosave_WhenNotRestoring_ShouldDeleteIt() throws Exception {
        store.save(folder, annotationData());
        when(dialogService.displayYesNoDialogAndGetResult(any(), any(), any())).thenReturn(ButtonBar.ButtonData.NO);

        autosave.onFolderOpened(folder);

        verify(operations, never()).restore(any());
        assertFalse(Files.exists(store.fileFor(folder)));
        assertFalse(autosave.isRestoring());
    }

    @Test
    void onFailingToRestore_ShouldSaveAgainAfterEdits() throws Exception {
        store.save(folder, annotationData());
        when(dialogService.displayYesNoDialogAndGetResult(any(), any(), any())).thenReturn(ButtonBar.ButtonData.YES);
        autosave.onFolderOpened(folder);

        autosave.onRestoreFailed();
        annotate();

        assertFalse(autosave.isRestoring());
        assertTrue(timer.running);
    }

    @Test
    void onStartingWithAnAutosaveOfTheLastFolder_WhenAccepting_ShouldOpenAndRestoreItWithoutAskingAgain()
            throws Exception {
        store.save(folder, annotationData());
        when(recentImageFolders.getFolders()).thenReturn(FXCollections.observableArrayList(folder));
        when(dialogService.displayYesNoDialogAndGetResult(eq(AutosaveController.RESTORE_DIALOG_TITLE),
                                                          contains("Open the folder and restore them?"), any()))
                .thenReturn(ButtonBar.ButtonData.YES);

        autosave.offerToRestoreLastSession();
        verify(operations).openImageFolder(folder);

        // Opening the folder restores it.
        autosave.onFolderOpened(folder);
        verify(operations).restore(store.fileFor(folder).toFile());
        verify(dialogService, times(1)).displayYesNoDialogAndGetResult(any(), any(), any());
    }

    @Test
    void onStartingWithAnAutosaveOfTheLastFolder_WhenDeclining_ShouldNotOpenIt() throws Exception {
        store.save(folder, annotationData());
        when(recentImageFolders.getFolders()).thenReturn(FXCollections.observableArrayList(folder));
        when(dialogService.displayYesNoDialogAndGetResult(any(), any(), any())).thenReturn(ButtonBar.ButtonData.NO);

        autosave.offerToRestoreLastSession();

        verify(operations, never()).openImageFolder(any());
        assertTrue(Files.isRegularFile(store.fileFor(folder)));
    }

    @Test
    void onStarting_WithoutAnAutosaveOfTheLastFolder_ShouldNotAsk() {
        when(recentImageFolders.getFolders()).thenReturn(FXCollections.observableArrayList(folder));

        autosave.offerToRestoreLastSession();

        verifyNoInteractions(dialogService);
    }

    @Test
    void onStopping_ShouldNotSaveAnymore() {
        autosave.onFolderOpened(folder);
        annotate();

        autosave.stop();
        autosave.onEdited();
        autosave.saveNow();
        autosave.awaitCompletion();

        assertFalse(timer.running);
        assertFalse(Files.exists(store.fileFor(folder)));
    }

    @Test
    void onClosingTheFolder_ShouldNotSaveAnymore() {
        autosave.onFolderOpened(folder);
        annotate();

        autosave.onFolderClosed();
        autosave.saveNow();
        autosave.awaitCompletion();

        assertFalse(Files.exists(store.fileFor(folder)));
        verify(operations, atLeastOnce()).showAutosaveTime(null);
    }

    @Test
    void onFailingToSave_ShouldShowTheError() throws Exception {
        // A file in place of the autosave folder makes saving fail.
        Files.createFile(store.directory());
        autosave.onFolderOpened(folder);
        annotate();

        timer.fire();
        autosave.awaitCompletion();

        verify(operations).showAutosaveFailed(notNull());
    }

    private void annotate() {
        model.updateCurrentBoundingShapeData(List.of(new BoundingBoxData(category, 0.1, 0.1, 0.5, 0.5, List.of())));
    }

    private ImageAnnotationData annotationData() {
        final ImageAnnotation annotation = new ImageAnnotation(model.getImageFileNameToMetaDataMap().get(IMAGE),
                List.of(new BoundingBoxData(category, 0.1, 0.1, 0.5, 0.5, List.of())));
        return new ImageAnnotationData(List.of(annotation), Map.of(category.getName(), 1),
                                       Map.of(category.getName(), category));
    }

    private static final class FakeTimer implements AutosaveController.Timer {
        private final Runnable action;
        private boolean running;

        FakeTimer(Runnable action) {
            this.action = action;
        }

        @Override
        public void restart() {
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        void fire() {
            running = false;
            action.run();
        }
    }
}
