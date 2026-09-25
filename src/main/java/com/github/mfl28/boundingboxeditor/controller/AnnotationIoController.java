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
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import com.github.mfl28.boundingboxeditor.ui.MainView;
import javafx.scene.control.ButtonBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

/**
 * Handles the user interaction around importing and exporting annotations: asking what to do with existing
 * annotations, which format to save in and where to load from or save to. Running the actual import or export
 * is delegated to the {@link Operations}.
 */
class AnnotationIoController {
    private static final String IMPORT_ANNOTATION_DATA_OPTION_DIALOG_TITLE = "Import Annotation Data";
    private static final String IMPORT_ANNOTATION_DATA_OPTION_DIALOG_CONTENT =
            "Do you want to keep existing categories and annotation data?";
    private static final String ANNOTATIONS_SAVE_FORMAT_DIALOG_TITLE = "Save Annotations";
    private static final String ANNOTATION_IMPORT_SAVE_EXISTING_DIALOG_CONTENT = "All current annotations are about " +
            "to be removed. Do you want to save them first?";
    private static final String ANNOTATIONS_SAVE_FORMAT_DIALOG_HEADER = "Choose the format for the saved annotations.";
    private static final String ANNOTATIONS_SAVE_FORMAT_DIALOG_CONTENT = "Annotation format:";
    private static final String SAVE_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE = "Save Image Annotations to File";
    private static final String DEFAULT_JSON_EXPORT_FILENAME = "annotations.json";
    private static final String DEFAULT_CSV_EXPORT_FILENAME = "annotations.csv";
    private static final String SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE = "Save Image Annotations to Folder";
    private static final String LOAD_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE = "Load Image Annotations from File";
    private static final String LOAD_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE =
            "Load image annotations from a folder containing annotation files";

    private final Model model;
    private final IoMetaData ioMetaData;
    private final DialogService dialogService;
    private final Stage stage;
    private final Operations operations;

    /**
     * The operations the annotation I/O interaction triggers, which are implemented by the {@link Controller}.
     */
    interface Operations {
        /**
         * Updates the model with the bounding shapes currently shown in the view.
         */
        void updateModelFromView();

        /**
         * Removes all annotations (and categories) from the model and the view.
         */
        void clearAnnotationData();

        /**
         * Starts importing annotations.
         *
         * @param source the annotation file or folder
         * @param format the annotation format
         */
        void startImport(File source, ImageAnnotationLoadStrategy.Type format);

        /**
         * Starts exporting the annotations.
         *
         * @param destination      the annotation file or folder
         * @param format           the annotation format
         * @param chainedOperation run after the export succeeded, may be null
         */
        void startExport(File destination, ImageAnnotationSaveStrategy.Type format, Runnable chainedOperation);
    }

    AnnotationIoController(Model model, IoMetaData ioMetaData, DialogService dialogService, Stage stage,
                           Operations operations) {
        this.model = model;
        this.ioMetaData = ioMetaData;
        this.dialogService = dialogService;
        this.stage = stage;
        this.operations = operations;
    }

    /**
     * Lets the user choose where to save the annotations in the provided format and starts the export.
     *
     * @param saveFormat the annotation format
     */
    void exportAnnotations(ImageAnnotationSaveStrategy.Type saveFormat) {
        final File destination = chooseSavingDestination(saveFormat);

        if(destination != null) {
            operations.startExport(destination, saveFormat, null);
        }
    }

    /**
     * Lets the user choose which annotations to import in the provided format and starts the import.
     *
     * @param loadFormat the annotation format
     */
    void importAnnotations(ImageAnnotationLoadStrategy.Type loadFormat) {
        final File source = chooseLoadingSource(loadFormat);

        if(source != null) {
            importAnnotations(source, loadFormat);
        }
    }

    /**
     * Imports the annotations from the provided source. If annotation data exists, the user is asked whether to keep
     * it and, before removing unsaved annotations, whether to save them first.
     *
     * @param source     the annotation file or folder
     * @param loadFormat the annotation format
     */
    void importAnnotations(File source, ImageAnnotationLoadStrategy.Type loadFormat) {
        operations.updateModelFromView();

        if(model.containsCategories()) {
            ButtonBar.ButtonData keepExistingDataAnswer =
                    dialogService.displayYesNoCancelDialogAndGetResult(IMPORT_ANNOTATION_DATA_OPTION_DIALOG_TITLE,
                            IMPORT_ANNOTATION_DATA_OPTION_DIALOG_CONTENT, stage);

            if(keepExistingDataAnswer == ButtonBar.ButtonData.NO) {
                if(!model.isSaved()) {
                    ButtonBar.ButtonData saveAnswer =
                            dialogService.displayYesNoCancelDialogAndGetResult(ANNOTATIONS_SAVE_FORMAT_DIALOG_TITLE,
                                    ANNOTATION_IMPORT_SAVE_EXISTING_DIALOG_CONTENT,
                                    stage);
                    if(saveAnswer == ButtonBar.ButtonData.YES) {
                        saveWithFormatChoiceAndRunOnSaveSuccess(() -> {
                            operations.clearAnnotationData();
                            operations.startImport(source, loadFormat);
                        });

                        return;
                    } else if(saveAnswer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                        return;
                    }
                }

                operations.clearAnnotationData();
            } else if(keepExistingDataAnswer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return;
            }
        }

        operations.startImport(source, loadFormat);
    }

    /**
     * Lets the user choose a format and destination and saves the annotations. The provided operation is only
     * run if the annotations were saved successfully.
     *
     * @param onSaveSuccess run after the annotations were saved successfully
     */
    void saveWithFormatChoiceAndRunOnSaveSuccess(Runnable onSaveSuccess) {
        chooseSaveFormat().ifPresent(choice -> {
            final File destination = chooseSavingDestination(choice);

            if(destination != null) {
                operations.startExport(destination, choice, onSaveSuccess);
            }
        });
    }

    /**
     * Lets the user choose a format and destination and saves the annotations. The provided operation is run after
     * the annotations were saved successfully, or right away if the user cancels choosing a format or destination.
     *
     * @param operation run after saving or cancelling
     */
    void saveWithFormatChoiceAndRunInAnyCase(Runnable operation) {
        chooseSaveFormat().ifPresentOrElse(choice -> {
            final File destination = chooseSavingDestination(choice);

            if(destination != null) {
                operations.startExport(destination, choice, operation);
            } else {
                operation.run();
            }
        }, operation);
    }

    /**
     * Remembers the folder of the provided annotation destination as the default saving directory.
     *
     * @param destination the annotation file or folder the annotations were saved to
     */
    void rememberSavingDirectory(File destination) {
        if(destination.isDirectory()) {
            ioMetaData.setDefaultAnnotationSavingDirectory(destination);
        } else if(destination.isFile() && destination.getParentFile().isDirectory()) {
            ioMetaData.setDefaultAnnotationSavingDirectory(destination.getParentFile());
        }
    }

    /**
     * Remembers the folder of the provided annotation source as the default loading directory.
     *
     * @param source the annotation file or folder the annotations were loaded from
     */
    void rememberLoadingDirectory(File source) {
        if(source.isDirectory()) {
            ioMetaData.setDefaultAnnotationLoadingDirectory(source);
        } else if(source.isFile() && source.getParentFile().isDirectory()) {
            ioMetaData.setDefaultAnnotationLoadingDirectory(source.getParentFile());
        }
    }

    private Optional<ImageAnnotationSaveStrategy.Type> chooseSaveFormat() {
        return dialogService.displayChoiceDialogAndGetResult(ImageAnnotationSaveStrategy.Type.PASCAL_VOC,
                Arrays.asList(ImageAnnotationSaveStrategy.Type.values()),
                ANNOTATIONS_SAVE_FORMAT_DIALOG_TITLE,
                ANNOTATIONS_SAVE_FORMAT_DIALOG_HEADER,
                ANNOTATIONS_SAVE_FORMAT_DIALOG_CONTENT, stage);
    }

    private File chooseSavingDestination(ImageAnnotationSaveStrategy.Type saveFormat) {
        return switch(saveFormat) {
            case JSON -> dialogService.displayFileChooserAndGetChoice(SAVE_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE, stage,
                    ioMetaData.getDefaultAnnotationSavingDirectory(),
                    DEFAULT_JSON_EXPORT_FILENAME,
                    new FileChooser.ExtensionFilter("JSON files", "*.json", "*.JSON"),
                    MainView.FileChooserType.SAVE);
            case CSV -> dialogService.displayFileChooserAndGetChoice(SAVE_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE, stage,
                    ioMetaData.getDefaultAnnotationSavingDirectory(),
                    DEFAULT_CSV_EXPORT_FILENAME,
                    new FileChooser.ExtensionFilter("CSV files", "*.csv", "*.CSV"),
                    MainView.FileChooserType.SAVE);
            default -> dialogService.displayDirectoryChooserAndGetChoice(SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE,
                    stage, ioMetaData.getDefaultAnnotationSavingDirectory());
        };
    }

    private File chooseLoadingSource(ImageAnnotationLoadStrategy.Type loadFormat) {
        return switch(loadFormat) {
            case JSON -> dialogService.displayFileChooserAndGetChoice(LOAD_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE, stage,
                    ioMetaData.getDefaultAnnotationLoadingDirectory(),
                    DEFAULT_JSON_EXPORT_FILENAME,
                    new FileChooser.ExtensionFilter("JSON files", "*.json", "*.JSON"),
                    MainView.FileChooserType.OPEN);
            case CSV -> dialogService.displayFileChooserAndGetChoice(LOAD_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE, stage,
                    ioMetaData.getDefaultAnnotationLoadingDirectory(),
                    DEFAULT_CSV_EXPORT_FILENAME,
                    new FileChooser.ExtensionFilter("CSV files", "*.csv", "*.CSV"),
                    MainView.FileChooserType.OPEN);
            default -> dialogService.displayDirectoryChooserAndGetChoice(LOAD_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE,
                    stage, ioMetaData.getDefaultAnnotationLoadingDirectory());
        };
    }
}
