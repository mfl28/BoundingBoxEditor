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
import com.github.mfl28.boundingboxeditor.model.data.IoMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import com.github.mfl28.boundingboxeditor.ui.MainView;
import javafx.scene.control.ButtonBar;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Tests the user interaction around importing and exporting annotations without the UI, using a mocked
 * {@link DialogService} to script the user's answers.
 */
@Tag("unit")
class AnnotationIoControllerTest {
    private static final String IMPORT_DIALOG_TITLE = "Import Annotation Data";
    private static final String SAVE_DIALOG_TITLE = "Save Annotations";
    private static final File SOURCE = new File("annotations");
    private static final File DESTINATION = new File("saved");

    private final Model model = new Model();
    private final IoMetaData ioMetaData = new IoMetaData();
    private final DialogService dialogService = mock(DialogService.class);
    private final AnnotationIoController.Operations operations = mock(AnnotationIoController.Operations.class);
    private AnnotationIoController annotationIoController;

    @BeforeEach
    void setUp() {
        annotationIoController = new AnnotationIoController(model, ioMetaData, dialogService, null, operations);
    }

    @Test
    void onImport_WhenNoCategoriesExist_ShouldImportWithoutAsking() {
        annotationIoController.importAnnotations(SOURCE, ImageAnnotationLoadStrategy.Type.PASCAL_VOC);

        final InOrder inOrder = inOrder(operations);
        inOrder.verify(operations).updateModelFromView();
        inOrder.verify(operations).startImport(SOURCE, ImageAnnotationLoadStrategy.Type.PASCAL_VOC);
        verifyNoInteractions(dialogService);
        verify(operations, never()).clearAnnotationData();
    }

    @Test
    void onImport_WhenUserCancels_ShouldNotImport() {
        givenExistingCategory();
        answerYesNoCancel(IMPORT_DIALOG_TITLE, ButtonBar.ButtonData.CANCEL_CLOSE);

        annotationIoController.importAnnotations(SOURCE, ImageAnnotationLoadStrategy.Type.YOLO);

        verify(operations, never()).startImport(any(), any());
        verify(operations, never()).clearAnnotationData();
    }

    @Test
    void onImport_WhenUserKeepsExistingData_ShouldImportWithoutClearing() {
        givenExistingCategory();
        answerYesNoCancel(IMPORT_DIALOG_TITLE, ButtonBar.ButtonData.YES);

        annotationIoController.importAnnotations(SOURCE, ImageAnnotationLoadStrategy.Type.JSON);

        verify(operations).startImport(SOURCE, ImageAnnotationLoadStrategy.Type.JSON);
        verify(operations, never()).clearAnnotationData();
    }

    @Test
    void onImport_WhenUserDiscardsSavedData_ShouldClearAndImportWithoutAskingToSave() {
        givenExistingCategory();
        answerYesNoCancel(IMPORT_DIALOG_TITLE, ButtonBar.ButtonData.NO);

        annotationIoController.importAnnotations(SOURCE, ImageAnnotationLoadStrategy.Type.CSV);

        final InOrder inOrder = inOrder(operations);
        inOrder.verify(operations).clearAnnotationData();
        inOrder.verify(operations).startImport(SOURCE, ImageAnnotationLoadStrategy.Type.CSV);
        verify(dialogService, never()).displayYesNoCancelDialogAndGetResult(eq(SAVE_DIALOG_TITLE), any(), any());
    }

    @Test
    void onImport_WhenUserDiscardsUnsavedDataAndCancelsSaving_ShouldNotImport() {
        givenExistingCategory();
        model.setSaved(false);
        answerYesNoCancel(IMPORT_DIALOG_TITLE, ButtonBar.ButtonData.NO);
        answerYesNoCancel(SAVE_DIALOG_TITLE, ButtonBar.ButtonData.CANCEL_CLOSE);

        annotationIoController.importAnnotations(SOURCE, ImageAnnotationLoadStrategy.Type.PASCAL_VOC);

        verify(operations, never()).startImport(any(), any());
        verify(operations, never()).clearAnnotationData();
    }

    @Test
    void onImport_WhenUserDiscardsUnsavedDataWithoutSaving_ShouldClearAndImport() {
        givenExistingCategory();
        model.setSaved(false);
        answerYesNoCancel(IMPORT_DIALOG_TITLE, ButtonBar.ButtonData.NO);
        answerYesNoCancel(SAVE_DIALOG_TITLE, ButtonBar.ButtonData.NO);

        annotationIoController.importAnnotations(SOURCE, ImageAnnotationLoadStrategy.Type.PASCAL_VOC);

        final InOrder inOrder = inOrder(operations);
        inOrder.verify(operations).clearAnnotationData();
        inOrder.verify(operations).startImport(SOURCE, ImageAnnotationLoadStrategy.Type.PASCAL_VOC);
        verify(operations, never()).startExport(any(), any(), any());
    }

    @Test
    void onImport_WhenUserSavesUnsavedDataFirst_ShouldImportOnlyAfterSaving() {
        givenExistingCategory();
        model.setSaved(false);
        answerYesNoCancel(IMPORT_DIALOG_TITLE, ButtonBar.ButtonData.NO);
        answerYesNoCancel(SAVE_DIALOG_TITLE, ButtonBar.ButtonData.YES);
        chooseSaveFormat(ImageAnnotationSaveStrategy.Type.PASCAL_VOC);
        when(dialogService.displayDirectoryChooserAndGetChoice(any(), any(), any())).thenReturn(DESTINATION);

        annotationIoController.importAnnotations(SOURCE, ImageAnnotationLoadStrategy.Type.YOLO);

        final ArgumentCaptor<Runnable> onSaveSuccess = ArgumentCaptor.forClass(Runnable.class);
        verify(operations).startExport(eq(DESTINATION), eq(ImageAnnotationSaveStrategy.Type.PASCAL_VOC),
                onSaveSuccess.capture());
        verify(operations, never()).clearAnnotationData();
        verify(operations, never()).startImport(any(), any());

        onSaveSuccess.getValue().run();

        final InOrder inOrder = inOrder(operations);
        inOrder.verify(operations).clearAnnotationData();
        inOrder.verify(operations).startImport(SOURCE, ImageAnnotationLoadStrategy.Type.YOLO);
    }

    @Test
    void onSaveWithFormatChoiceAndRunOnSaveSuccess_WhenFormatChoiceCancelled_ShouldDoNothing() {
        chooseNoSaveFormat();
        final Runnable onSaveSuccess = mock(Runnable.class);

        annotationIoController.saveWithFormatChoiceAndRunOnSaveSuccess(onSaveSuccess);

        verify(operations, never()).startExport(any(), any(), any());
        verifyNoInteractions(onSaveSuccess);
    }

    @Test
    void onSaveWithFormatChoiceAndRunInAnyCase_WhenFormatChoiceCancelled_ShouldRunOperation() {
        chooseNoSaveFormat();
        final Runnable operation = mock(Runnable.class);

        annotationIoController.saveWithFormatChoiceAndRunInAnyCase(operation);

        verify(operation).run();
        verify(operations, never()).startExport(any(), any(), any());
    }

    @Test
    void onSaveWithFormatChoiceAndRunInAnyCase_WhenDestinationChoiceCancelled_ShouldRunOperation() {
        chooseSaveFormat(ImageAnnotationSaveStrategy.Type.YOLO);
        final Runnable operation = mock(Runnable.class);

        annotationIoController.saveWithFormatChoiceAndRunInAnyCase(operation);

        verify(operation).run();
        verify(operations, never()).startExport(any(), any(), any());
    }

    @Test
    void onSaveWithFormatChoiceAndRunInAnyCase_WhenDestinationChosen_ShouldRunOperationAfterExport() {
        chooseSaveFormat(ImageAnnotationSaveStrategy.Type.JSON);
        when(dialogService.displayFileChooserAndGetChoice(any(), any(), any(), any(), any(), any()))
                .thenReturn(DESTINATION);
        final Runnable operation = mock(Runnable.class);

        annotationIoController.saveWithFormatChoiceAndRunInAnyCase(operation);

        verify(operations).startExport(DESTINATION, ImageAnnotationSaveStrategy.Type.JSON, operation);
        verifyNoInteractions(operation);
    }

    @Test
    void onExport_WhenFileFormat_ShouldUseSaveFileChooserInDefaultSavingDirectory(@TempDir Path tempDir) {
        ioMetaData.setDefaultAnnotationSavingDirectory(tempDir.toFile());
        when(dialogService.displayFileChooserAndGetChoice(any(), any(), any(), any(), any(), any()))
                .thenReturn(DESTINATION);

        annotationIoController.exportAnnotations(ImageAnnotationSaveStrategy.Type.CSV);

        verify(dialogService).displayFileChooserAndGetChoice(any(), isNull(), eq(tempDir.toFile()),
                eq("annotations.csv"), any(), eq(MainView.FileChooserType.SAVE));
        verify(operations).startExport(DESTINATION, ImageAnnotationSaveStrategy.Type.CSV, null);
    }

    @Test
    void onExport_WhenFolderFormatAndChoiceCancelled_ShouldNotExport() {
        annotationIoController.exportAnnotations(ImageAnnotationSaveStrategy.Type.PASCAL_VOC);

        verify(dialogService).displayDirectoryChooserAndGetChoice(any(), isNull(), isNull());
        verify(operations, never()).startExport(any(), any(), any());
    }

    @Test
    void onImportWithFormat_WhenFileFormat_ShouldUseOpenFileChooserInDefaultLoadingDirectory(@TempDir Path tempDir) {
        ioMetaData.setDefaultAnnotationLoadingDirectory(tempDir.toFile());
        when(dialogService.displayFileChooserAndGetChoice(any(), any(), any(), any(), any(), any()))
                .thenReturn(SOURCE);

        annotationIoController.importAnnotations(ImageAnnotationLoadStrategy.Type.JSON);

        verify(dialogService).displayFileChooserAndGetChoice(any(), isNull(), eq(tempDir.toFile()),
                eq("annotations.json"), any(), eq(MainView.FileChooserType.OPEN));
        verify(operations).startImport(SOURCE, ImageAnnotationLoadStrategy.Type.JSON);
    }

    @Test
    void onImportWithFormat_WhenChoiceCancelled_ShouldNotImport() {
        annotationIoController.importAnnotations(ImageAnnotationLoadStrategy.Type.YOLO);

        verify(dialogService).displayDirectoryChooserAndGetChoice(any(), isNull(), isNull());
        verifyNoInteractions(operations);
    }

    @Test
    void onRememberDirectories_ShouldStoreFolderOrParentFolderOfFile(@TempDir Path tempDir) throws IOException {
        final File file = Files.createFile(tempDir.resolve("annotations.json")).toFile();

        annotationIoController.rememberSavingDirectory(file);
        annotationIoController.rememberLoadingDirectory(tempDir.toFile());

        assertEquals(tempDir.toFile(), ioMetaData.getDefaultAnnotationSavingDirectory());
        assertEquals(tempDir.toFile(), ioMetaData.getDefaultAnnotationLoadingDirectory());
    }

    private void givenExistingCategory() {
        model.getObjectCategories().add(new ObjectCategory("category", Color.RED));
    }

    private void answerYesNoCancel(String dialogTitle, ButtonBar.ButtonData answer) {
        when(dialogService.displayYesNoCancelDialogAndGetResult(eq(dialogTitle), any(), any())).thenReturn(answer);
    }

    private void chooseSaveFormat(ImageAnnotationSaveStrategy.Type format) {
        doReturn(Optional.of(format)).when(dialogService)
                .displayChoiceDialogAndGetResult(any(), any(), any(), any(), any(), any());
    }

    private void chooseNoSaveFormat() {
        doReturn(Optional.empty()).when(dialogService)
                .displayChoiceDialogAndGetResult(any(), any(), any(), any(), any(), any());
    }
}
