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
import com.github.mfl28.boundingboxeditor.model.io.FileChangeWatcher;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageMetaDataLoadingResult;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import javafx.scene.control.ButtonBar;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Handles opening and reloading image folders: listing the image files, asking what to do with existing annotation
 * data once the images were loaded and watching the folder for external changes. Loading the image metadata and
 * showing the loaded images is delegated to the {@link Operations}.
 */
class ImageFolderController {
    private static final String IMAGE_FOLDER_CHOOSER_TITLE = "Choose Image Folder";
    private static final String OPEN_FOLDER_ERROR_DIALOG_TITLE = "Image Folder Loading Error";
    private static final String OPEN_FOLDER_ERROR_DIALOG_HEADER = "The selected folder is not a valid image folder.";
    private static final String LOAD_IMAGE_FOLDER_ERROR_DIALOG_TITLE = "Image Folder Loading Error";
    private static final String LOAD_IMAGE_FOLDER_ERROR_DIALOG_CONTENT =
            "The chosen folder does not contain any valid image files.";
    private static final String IMAGE_IMPORT_ERROR_ALERT_TITLE = "Image Import Error";
    private static final String IMAGE_IMPORT_ERROR_ALERT_CONTENT =
            "The folder does not contain any valid image files.";
    private static final String OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE = "Open Image Folder";
    private static final String KEEP_EXISTING_CATEGORIES_DIALOG_TEXT = "Keep existing categories?";
    private static final String RELOAD_IMAGE_FOLDER_OPTION_DIALOG_TITLE = "Reload Image Folder";
    private static final String RELOAD_IMAGE_FOLDER_OPTION_DIALOG_CONTENT =
            "Reloading the image folder will remove any existing annotation data. " +
                    "Do you want to save the currently existing annotation data (Closing = No)?";
    private static final String OPEN_IMAGE_FOLDER_OPTION_DIALOG_CONTENT =
            "Opening a new image folder will remove any existing annotation data. " +
                    "Do you want to save the currently existing annotation data?";
    private static final String IMAGE_FILES_CHANGED_ERROR_TITLE = "Image Files Changed";
    private static final String IMAGE_FILES_CHANGED_ERROR_CONTENT =
            "Image files were changed externally. Will reload folder.";
    private static final String IMAGE_FILE_CHANGE_WATCHER_THREAD_NAME = "ImageFileChangeWatcher";
    private static final int MAX_DIRECTORY_DEPTH = 1;

    private final Model model;
    private final IoMetaData ioMetaData;
    private final DialogService dialogService;
    private final Stage stage;
    private final AnnotationIoController annotationIoController;
    private final Operations operations;
    private Thread directoryWatcher;

    /**
     * The operations the image folder handling triggers, which are implemented by the {@link Controller}.
     */
    interface Operations {
        /**
         * Updates the model with the bounding shapes currently shown in the view.
         */
        void updateModelFromView();

        /**
         * Starts loading the metadata of the provided image files. The result is expected to be passed to
         * {@link #onImageMetaDataLoaded}.
         *
         * @param folder     the image folder
         * @param imageFiles the image files
         * @param reload     whether the currently loaded folder is reloaded
         */
        void startImageMetaDataLoading(File folder, List<File> imageFiles, boolean reload);

        /**
         * Hides the progress of the image metadata loading.
         */
        void hideImageMetaDataLoadingProgress();

        /**
         * Replaces the current image files and annotations in the model and view with the loaded image files.
         *
         * @param result         the result of loading the image metadata
         * @param folder         the image folder
         * @param keepCategories whether the existing object categories are kept
         */
        void showLoadedImageFiles(ImageMetaDataLoadingResult result, File folder, boolean keepCategories);

        /**
         * Removes all image files and annotation data from the model and the view.
         */
        void clearWorkspace();
    }

    ImageFolderController(Model model, IoMetaData ioMetaData, DialogService dialogService, Stage stage,
                          AnnotationIoController annotationIoController, Operations operations) {
        this.model = model;
        this.ioMetaData = ioMetaData;
        this.dialogService = dialogService;
        this.stage = stage;
        this.annotationIoController = annotationIoController;
        this.operations = operations;
    }

    /**
     * Lets the user choose an image folder and opens it.
     */
    void openImageFolder() {
        final File imageFolder = dialogService.displayDirectoryChooserAndGetChoice(IMAGE_FOLDER_CHOOSER_TITLE, stage,
                ioMetaData.getDefaultImageLoadingDirectory());

        if(imageFolder != null) {
            openImageFolder(imageFolder);
        }
    }

    /**
     * Opens the provided image folder and remembers it as the default image loading directory.
     *
     * @param imageFolder the image folder
     */
    void openImageFolder(File imageFolder) {
        operations.updateModelFromView();
        loadImageFiles(imageFolder);
        ioMetaData.setDefaultImageLoadingDirectory(imageFolder);
    }

    /**
     * Reloads the currently opened image folder.
     */
    void reloadCurrentFolder() {
        operations.updateModelFromView();
        loadImageFiles(ioMetaData.getDefaultImageLoadingDirectory(), true);
    }

    /**
     * Starts loading the image files of the provided folder.
     *
     * @param imageFolder the image folder
     */
    void loadImageFiles(File imageFolder) {
        loadImageFiles(imageFolder, false);
    }

    /**
     * Handles the result of loading the image metadata: asks what to do with existing annotation data before
     * showing the loaded images and reports errors.
     *
     * @param result the result of loading the image metadata
     * @param folder the image folder
     * @param reload whether the currently loaded folder was reloaded
     */
    void onImageMetaDataLoaded(ImageMetaDataLoadingResult result, File folder, boolean reload) {
        if(result.getNrSuccessfullyProcessedItems() != 0 && !handleSuccessfullyProcessedItemsPresent(result, folder,
                reload)) {
            return;
        }

        if(!result.getErrorTableEntries().isEmpty()) {
            operations.hideImageMetaDataLoadingProgress();
            dialogService.displayIOResultErrorInfoAlert(result, stage);
        } else if(result.getNrSuccessfullyProcessedItems() == 0) {
            operations.hideImageMetaDataLoadingProgress();
            dialogService.displayErrorAlert(IMAGE_IMPORT_ERROR_ALERT_TITLE, IMAGE_IMPORT_ERROR_ALERT_CONTENT, stage);
        }

        if(reload && result.getNrSuccessfullyProcessedItems() == 0) {
            operations.hideImageMetaDataLoadingProgress();
            askToSaveExistingAnnotationDataAndClearWorkspace();
        }
    }

    /**
     * Stops watching the current image folder for external changes.
     */
    void stopWatching() {
        if(directoryWatcher != null && directoryWatcher.isAlive()) {
            directoryWatcher.interrupt();
        }
    }

    private static List<File> getImageFilesFromDirectory(File directory) throws IOException {
        Path path = Path.of(directory.getPath());

        try(Stream<Path> imageFiles = Files.walk(path, MAX_DIRECTORY_DEPTH)) {
            return imageFiles.map(file -> new File(file.toString()))
                    .filter(file -> file.isFile() && !file.isHidden())
                    .sorted(Comparator.comparing(File::getName))
                    .toList();
        }
    }

    private void loadImageFiles(File imageFolder, boolean reload) {
        List<File> imageFiles;

        try {
            imageFiles = getImageFilesFromDirectory(imageFolder);
        } catch(IOException e) {
            dialogService.displayErrorAlert(OPEN_FOLDER_ERROR_DIALOG_TITLE, OPEN_FOLDER_ERROR_DIALOG_HEADER, stage);

            if(reload) {
                askToSaveExistingAnnotationDataAndClearWorkspace();
            }

            return;
        }

        if(imageFiles.isEmpty()) {
            dialogService.displayErrorAlert(LOAD_IMAGE_FOLDER_ERROR_DIALOG_TITLE, LOAD_IMAGE_FOLDER_ERROR_DIALOG_CONTENT,
                    stage);

            if(reload) {
                askToSaveExistingAnnotationDataAndClearWorkspace();
            }

            return;
        }

        operations.startImageMetaDataLoading(imageFolder, imageFiles, reload);
    }

    private boolean handleSuccessfullyProcessedItemsPresent(ImageMetaDataLoadingResult result, File folder,
                                                            boolean reload) {
        operations.updateModelFromView();

        boolean keepExistingCategories = false;

        if(model.containsCategories()) {
            operations.hideImageMetaDataLoadingProgress();
            ButtonBar.ButtonData answer = reload ?
                    dialogService.displayYesNoDialogAndGetResult(OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                            KEEP_EXISTING_CATEGORIES_DIALOG_TEXT, stage) :
                    dialogService.displayYesNoCancelDialogAndGetResult(OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                            KEEP_EXISTING_CATEGORIES_DIALOG_TEXT, stage);

            keepExistingCategories = (answer == ButtonBar.ButtonData.YES);

            if(answer == ButtonBar.ButtonData.CANCEL_CLOSE && !reload) {
                return false;
            }
        }

        if(!model.isSaved()) {
            operations.hideImageMetaDataLoadingProgress();
            // First ask if user wants to save the existing annotations.
            ButtonBar.ButtonData answer = reload ?
                    dialogService.displayYesNoDialogAndGetResult(RELOAD_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                            RELOAD_IMAGE_FOLDER_OPTION_DIALOG_CONTENT, stage) :
                    dialogService.displayYesNoCancelDialogAndGetResult(OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                            OPEN_IMAGE_FOLDER_OPTION_DIALOG_CONTENT, stage);

            handleAnnotationSavingDecision(result, folder, reload, keepExistingCategories, answer);
        } else {
            showLoadedImageFiles(result, folder, keepExistingCategories);
        }

        return true;
    }

    private void handleAnnotationSavingDecision(ImageMetaDataLoadingResult result, File folder, boolean reload,
                                                boolean keepExistingCategories, ButtonBar.ButtonData answer) {
        if(answer == ButtonBar.ButtonData.YES) {
            if(reload) {
                annotationIoController.saveWithFormatChoiceAndRunInAnyCase(
                        () -> showLoadedImageFiles(result, folder, keepExistingCategories));
            } else {
                annotationIoController.saveWithFormatChoiceAndRunOnSaveSuccess(
                        () -> showLoadedImageFiles(result, folder, keepExistingCategories));
            }
        } else if(answer == ButtonBar.ButtonData.NO || reload) {
            showLoadedImageFiles(result, folder, keepExistingCategories);
        }
    }

    private void showLoadedImageFiles(ImageMetaDataLoadingResult result, File folder, boolean keepCategories) {
        stopWatching();
        operations.showLoadedImageFiles(result, folder, keepCategories);
        startWatching(folder);
    }

    /**
     * Creates the (not yet started) thread that watches the provided folder for changes of the loaded image files.
     *
     * @param folder         the image folder
     * @param onFilesChanged run on the JavaFX application thread when the image files changed
     * @return the thread
     */
    Thread createDirectoryWatcher(File folder, Runnable onFilesChanged) {
        return new Thread(new FileChangeWatcher(folder.toPath(), model.getImageFileNameSet(), onFilesChanged),
                IMAGE_FILE_CHANGE_WATCHER_THREAD_NAME);
    }

    private void startWatching(File folder) {
        directoryWatcher = createDirectoryWatcher(folder, () -> {
            dialogService.displayErrorAlert(IMAGE_FILES_CHANGED_ERROR_TITLE, IMAGE_FILES_CHANGED_ERROR_CONTENT, stage);
            reloadCurrentFolder();
        });
        directoryWatcher.start();
    }

    private void askToSaveExistingAnnotationDataAndClearWorkspace() {
        if(!model.isSaved()) {
            // First ask if user wants to save the existing annotations.
            ButtonBar.ButtonData answer =
                    dialogService.displayYesNoDialogAndGetResult(RELOAD_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                            RELOAD_IMAGE_FOLDER_OPTION_DIALOG_CONTENT, stage);

            if(answer == ButtonBar.ButtonData.YES) {
                annotationIoController.saveWithFormatChoiceAndRunInAnyCase(this::clearWorkspace);
                return;
            }
        }

        clearWorkspace();
    }

    private void clearWorkspace() {
        stopWatching();
        operations.clearWorkspace();
    }
}
