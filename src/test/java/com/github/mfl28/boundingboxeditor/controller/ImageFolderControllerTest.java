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
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageMetaDataLoadingResult;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import javafx.scene.control.ButtonBar;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests opening and reloading image folders without the UI, using a mocked {@link DialogService} to script the
 * user's answers.
 */
@Tag("unit")
class ImageFolderControllerTest {
    private static final String FOLDER_LOADING_ERROR_TITLE = "Image Folder Loading Error";
    private static final String OPEN_FOLDER_TITLE = "Open Image Folder";
    private static final String RELOAD_FOLDER_TITLE = "Reload Image Folder";

    private final Model model = new Model();
    private final IoMetaData ioMetaData = new IoMetaData();
    private final DialogService dialogService = mock(DialogService.class);
    private final AnnotationIoController annotationIoController = mock(AnnotationIoController.class);
    private final ImageFolderController.Operations operations = mock(ImageFolderController.Operations.class);
    private ImageFolderController imageFolderController;

    @TempDir
    Path folder;

    @BeforeEach
    void setUp() {
        imageFolderController = new ImageFolderController(model, ioMetaData, dialogService, null,
                annotationIoController, operations);
    }

    @AfterEach
    void tearDown() {
        imageFolderController.stopWatching();
    }

    @Test
    void onOpenImageFolder_WhenFolderChosen_ShouldLoadItsFilesSortedAndRememberFolder() throws IOException {
        final File image2 = Files.createFile(folder.resolve("b.jpg")).toFile();
        final File image1 = Files.createFile(folder.resolve("a.jpg")).toFile();
        Files.createDirectory(folder.resolve("subfolder"));
        Files.createFile(folder.resolve("subfolder").resolve("c.jpg"));
        when(dialogService.displayDirectoryChooserAndGetChoice(any(), any(), any())).thenReturn(folder.toFile());

        imageFolderController.openImageFolder();

        verify(operations).updateModelFromView();
        verify(operations).startImageMetaDataLoading(folder.toFile(), List.of(image1, image2), false);
        assertEquals(folder.toFile(), ioMetaData.getDefaultImageLoadingDirectory());
    }

    @Test
    void onOpenImageFolder_WhenChoiceCancelled_ShouldDoNothing() {
        imageFolderController.openImageFolder();

        verifyNoInteractions(operations);
    }

    @Test
    void onLoadImageFiles_WhenFolderIsEmpty_ShouldReportErrorAndNotLoad() {
        imageFolderController.loadImageFiles(folder.toFile());

        verify(dialogService).displayErrorAlert(eq(FOLDER_LOADING_ERROR_TITLE), any(), any());
        verify(operations, never()).startImageMetaDataLoading(any(), any(), anyBoolean());
        verify(operations, never()).clearWorkspace();
    }

    @Test
    void onReloadCurrentFolder_WhenFolderIsEmptyAndDataUnsaved_ShouldAskToSaveAndClearWorkspace() {
        ioMetaData.setDefaultImageLoadingDirectory(folder.toFile());
        model.setSaved(false);
        when(dialogService.displayYesNoDialogAndGetResult(eq(RELOAD_FOLDER_TITLE), any(), any()))
                .thenReturn(ButtonBar.ButtonData.NO);

        imageFolderController.reloadCurrentFolder();

        verify(dialogService).displayErrorAlert(eq(FOLDER_LOADING_ERROR_TITLE), any(), any());
        verify(operations).clearWorkspace();
        verifyNoInteractions(annotationIoController);
    }

    @Test
    void onImagesLoaded_WhenNoExistingData_ShouldShowImagesWithoutAsking() {
        final ImageMetaDataLoadingResult result = createResult(2);

        imageFolderController.onImageMetaDataLoaded(result, folder.toFile(), false);

        verify(operations).showLoadedImageFiles(result, folder.toFile(), false);
        verify(dialogService, never()).displayYesNoCancelDialogAndGetResult(any(), any(), any());
        verify(dialogService, never()).displayYesNoDialogAndGetResult(any(), any(), any());
    }

    @Test
    void onImagesLoaded_WhenUserCancelsKeepingCategories_ShouldNotShowImages() {
        givenExistingCategory();
        when(dialogService.displayYesNoCancelDialogAndGetResult(eq(OPEN_FOLDER_TITLE), any(), any()))
                .thenReturn(ButtonBar.ButtonData.CANCEL_CLOSE);

        imageFolderController.onImageMetaDataLoaded(createResult(2), folder.toFile(), false);

        verify(operations, never()).showLoadedImageFiles(any(), any(), anyBoolean());
    }

    @Test
    void onImagesLoaded_WhenUserKeepsCategoriesOfSavedData_ShouldShowImagesKeepingCategories() {
        givenExistingCategory();
        when(dialogService.displayYesNoCancelDialogAndGetResult(eq(OPEN_FOLDER_TITLE), any(), any()))
                .thenReturn(ButtonBar.ButtonData.YES);
        final ImageMetaDataLoadingResult result = createResult(1);

        imageFolderController.onImageMetaDataLoaded(result, folder.toFile(), false);

        verify(operations).showLoadedImageFiles(result, folder.toFile(), true);
    }

    @Test
    void onImagesLoaded_WhenUserSavesUnsavedDataFirst_ShouldShowImagesOnlyAfterSaving() {
        model.setSaved(false);
        when(dialogService.displayYesNoCancelDialogAndGetResult(eq(OPEN_FOLDER_TITLE), any(), any()))
                .thenReturn(ButtonBar.ButtonData.YES);
        final ImageMetaDataLoadingResult result = createResult(1);

        imageFolderController.onImageMetaDataLoaded(result, folder.toFile(), false);

        final ArgumentCaptor<Runnable> onSaveSuccess = ArgumentCaptor.forClass(Runnable.class);
        verify(annotationIoController).saveWithFormatChoiceAndRunOnSaveSuccess(onSaveSuccess.capture());
        verify(operations, never()).showLoadedImageFiles(any(), any(), anyBoolean());

        onSaveSuccess.getValue().run();

        verify(operations).showLoadedImageFiles(result, folder.toFile(), false);
    }

    @Test
    void onImagesReloaded_WhenUserSavesUnsavedData_ShouldShowImagesAfterSavingOrCancelling() {
        model.setSaved(false);
        when(dialogService.displayYesNoDialogAndGetResult(eq(RELOAD_FOLDER_TITLE), any(), any()))
                .thenReturn(ButtonBar.ButtonData.YES);

        imageFolderController.onImageMetaDataLoaded(createResult(1), folder.toFile(), true);

        verify(annotationIoController).saveWithFormatChoiceAndRunInAnyCase(any());
        verify(annotationIoController, never()).saveWithFormatChoiceAndRunOnSaveSuccess(any());
    }

    @Test
    void onImagesLoaded_WhenErrorsOccurred_ShouldShowImagesAndReportErrors() {
        final ImageMetaDataLoadingResult result = new ImageMetaDataLoadingResult(1,
                List.of(new IOErrorInfoEntry("broken.jpg", "Not an image.")), List.of(), Map.of());

        imageFolderController.onImageMetaDataLoaded(result, folder.toFile(), false);

        verify(operations).showLoadedImageFiles(result, folder.toFile(), false);
        verify(dialogService).displayIOResultErrorInfoAlert(result, null);
    }

    @Test
    void onImagesReloaded_WhenNoValidImages_ShouldReportErrorAndClearWorkspace() {
        imageFolderController.onImageMetaDataLoaded(createResult(0), folder.toFile(), true);

        verify(dialogService).displayErrorAlert(eq("Image Import Error"), any(), any());
        verify(operations, never()).showLoadedImageFiles(any(), any(), anyBoolean());
        verify(operations).clearWorkspace();
    }

    private void givenExistingCategory() {
        model.getObjectCategories().add(new ObjectCategory("category", Color.RED));
    }

    private static ImageMetaDataLoadingResult createResult(int nrValidFiles) {
        return new ImageMetaDataLoadingResult(nrValidFiles, List.of(), List.of(), Map.of());
    }
}
