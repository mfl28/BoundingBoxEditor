/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.IoMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.FileChangeWatcher;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageMetaDataLoadingResult;
import com.github.mfl28.boundingboxeditor.model.io.services.ImageAnnotationExportService;
import com.github.mfl28.boundingboxeditor.model.io.services.ImageAnnotationImportService;
import com.github.mfl28.boundingboxeditor.model.io.services.ImageMetaDataLoadingService;
import com.github.mfl28.boundingboxeditor.ui.*;
import com.github.mfl28.boundingboxeditor.ui.statusevents.ImageAnnotationsImportingSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.ImageAnnotationsSavingSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.ImageFilesLoadingSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The control-component of the application (as in MVC pattern). Responsible for interaction-handling
 * between the {@link Model} and the classes implementing the {@link com.github.mfl28.boundingboxeditor.ui.View} interface.
 * {@link com.github.mfl28.boundingboxeditor.ui.View} implementors can register a controller via an interface method but
 * are themselves responsible for handling interactions between their contained
 * UI-components (i.e. interactions that do not require data from the {@link Model}).
 *
 * @see Model
 * @see MainView
 */
public class Controller {
    private static final String PROGRAM_NAME = "Bounding Box Editor";
    private static final String PROGRAM_NAME_EXTENSION_SEPARATOR = " - ";
    private static final String OPEN_FOLDER_ERROR_DIALOG_TITLE = "Error while opening image folder";
    private static final String OPEN_FOLDER_ERROR_DIALOG_HEADER = "The selected folder is not a valid image folder.";
    private static final String SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE = "Save image annotations to a folder";
    private static final String SAVE_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE = "Save image annotations to a file";
    private static final String LOAD_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE =
            "Load image annotations from a folder containing annotation files";
    private static final String LOAD_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE = "Load image annotations from a file";
    private static final String LOAD_IMAGE_FOLDER_ERROR_DIALOG_TITLE = "Error loading image folder";
    private static final String LOAD_IMAGE_FOLDER_ERROR_DIALOG_CONTENT =
            "The chosen folder does not contain any valid image files.";
    private static final String CATEGORY_INPUT_ERROR_DIALOG_TITLE = "Category Creation Error";
    private static final String INVALID_CATEGORY_NAME_ERROR_DIALOG_CONTENT =
            "Please provide a non-blank category name.";
    private static final String CATEGORY_DELETION_ERROR_DIALOG_TITLE = "Category Deletion Error";
    private static final String CATEGORY_DELETION_ERROR_DIALOG_CONTENT =
            "You cannot delete a category that has existing bounding-boxes assigned to it.";

    private static final String IMAGE_FOLDER_CHOOSER_TITLE = "Choose an image folder";
    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;
    private static final String SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE = "Save Error";
    private static final String NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT =
            "There are no image annotations to save.";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE = "Saving Annotations";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER = "Saving in progress...";
    private static final String ANNOTATION_IMPORT_ERROR_TITLE = "Annotation Import Error";
    private static final String ANNOTATION_IMPORT_ERROR_NO_VALID_FILES_CONTENT =
            "The source does not contain any valid annotations.";
    private static final String LOADING_ANNOTATIONS_DIALOG_TITLE = "Loading";
    private static final String LOADING_ANNOTATIONS_DIALOG_HEADER = "Loading annotations...";
    private static final String OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE = "Open image folder";
    private static final String OPEN_IMAGE_FOLDER_OPTION_DIALOG_CONTENT =
            "Opening a new image folder will remove any existing annotation data. " +
                    "Do you want to save the currently existing annotation data?";
    private static final String RELOAD_IMAGE_FOLDER_OPTION_DIALOG_CONTENT =
            "Reloading the image folder will remove any existing annotation data. " +
                    "Do you want to save the currently existing annotation data (Closing = No)?";
    private static final String IMPORT_ANNOTATION_DATA_OPTION_DIALOG_TITLE = "Import annotation data";
    private static final String IMPORT_ANNOTATION_DATA_OPTION_DIALOG_CONTENT =
            "Do you want to keep existing categories and annotation data?";
    private static final String EXIT_APPLICATION_OPTION_DIALOG_TITLE = "Exit Application";
    private static final String EXIT_APPLICATION_OPTION_DIALOG_CONTENT =
            "Do you want to save the existing annotation data?";
    private static final String IS_WINDOW_MAXIMIZED_PREFERENCE_NAME = "isMaximized";
    private static final String CURRENT_IMAGE_LOADING_DIRECTORY_PREFERENCE_NAME = "currentImageLoadingDirectory";
    private static final String CURRENT_ANNOTATION_LOADING_DIRECTORY_PREFERENCE_NAME =
            "currentAnnotationLoadingDirectory";
    private static final String CURRENT_ANNOTATION_SAVING_DIRECTORY_PREFERENCE_NAME =
            "currentAnnotationSavingDirectory";
    private static final String RELOAD_IMAGE_FOLDER_OPTION_DIALOG_TITLE = "Reload image folder";
    private static final String ANNOTATIONS_SAVE_FORMAT_DIALOG_TITLE = "Save annotations";
    private static final String ANNOTATIONS_SAVE_FORMAT_DIALOG_HEADER = "Choose the format for the saved annotations.";
    private static final String ANNOTATIONS_SAVE_FORMAT_DIALOG_CONTENT = "Annotation format: ";
    private static final String KEEP_EXISTING_CATEGORIES_DIALOG_TEXT = "Keep existing categories?";
    private static final String DEFAULT_JSON_EXPORT_FILENAME = "annotations.json";
    private static final String ANNOTATION_IMPORT_SAVE_EXISTING_DIALOG_CONTENT = "All current annotations are about " +
            "to be removed. Do you want to save them first?";
    private static final String IMAGE_IMPORT_ERROR_ALERT_TITLE = "Image Import Error";
    private static final String IMAGE_IMPORT_ERROR_ALERT_CONTENT =
            "The folder does not contain any valid image files.";
    private static final String IMAGE_FILES_LOADING_DIALOG_TITLE = "Loading images";
    private static final String IMAGE_FILES_LOADING_DIALOG_HEADER = "Loading image meta-data";
    private static final String IMAGE_FILES_CHANGED_ERROR_TITLE = "Image files changed";
    private static final String IMAGE_FILES_CHANGED_ERROR_CONTENT =
            "Image files were changed externally, will reload folder.";
    private static final String IMAGE_FILE_CHANGE_WATCHER_THREAD_NAME = "ImageFileChangeWatcher";
    private final ImageAnnotationExportService annotationExportService = new ImageAnnotationExportService();
    private final ImageAnnotationImportService annotationImportService = new ImageAnnotationImportService();
    private final ImageMetaDataLoadingService imageMetaDataLoadingService = new ImageMetaDataLoadingService();
    private final Stage stage;
    private final MainView view = new MainView();
    private final Model model = new Model();
    private final ListChangeListener<BoundingShapeViewable> boundingShapeCountPerCategoryListener =
            createBoundingShapeCountPerCategoryListener();
    private final ChangeListener<Number> imageLoadProgressListener = createImageLoadingProgressListener();
    private final ChangeListener<Boolean> imageNavigationKeyPressedListener = createImageNavigationKeyPressedListener();
    private final BooleanProperty navigatePreviousKeyPressed = new SimpleBooleanProperty(false);
    private final BooleanProperty navigateNextKeyPressed = new SimpleBooleanProperty(false);
    private final IoMetaData ioMetaData = new IoMetaData();
    String lastLoadedImageUrl;
    private final ChangeListener<Number> selectedFileIndexListener = createSelectedFileIndexListener();
    Thread directoryWatcher;

    /**
     * Creates a new controller object that is responsible for handling the application logic and
     * handles interaction between the view and model components.
     *
     * @param mainStage the stage that represents the top level container of all used ui-elements
     */
    public Controller(final Stage mainStage) {
        stage = mainStage;
        stage.setTitle(PROGRAM_NAME);
        stage.getIcons().add(MainView.APPLICATION_ICON);
        stage.setOnCloseRequest(event -> {
            onRegisterExitAction();
            event.consume();
        });

        loadPreferences();

        view.connectToController(this);
        setUpModelListeners();
        setUpServices();
    }

    /**
     * Handles the event of the user requesting to open a new image folder.
     */
    public void onRegisterOpenImageFolderAction() {
        final File imageFolder = MainView.displayDirectoryChooserAndGetChoice(IMAGE_FOLDER_CHOOSER_TITLE, stage,
                                                                              ioMetaData
                                                                                      .getDefaultImageLoadingDirectory());

        if(imageFolder != null) {
            initiateImageFolderLoading(imageFolder);
        }
    }

    /**
     * Initiates the loading of image files from a provided folder.
     *
     * @param imageFolder the folder containing the image files to load
     */
    public void initiateImageFolderLoading(File imageFolder) {
        updateModelFromView();
        loadImageFiles(imageFolder);
        ioMetaData.setDefaultImageLoadingDirectory(imageFolder);
    }

    public void initiateCurrentFolderReloading() {
        updateModelFromView();
        forceLoadImageFiles(ioMetaData.getDefaultImageLoadingDirectory());
    }

    /**
     * Loads image-files from the provided directory into the model and updates
     * the view.
     *
     * @param imageFileDirectory the directory containing the image-files to be loaded
     */
    public void loadImageFiles(File imageFileDirectory) {
        List<File> imageFiles;

        try {
            imageFiles = getImageFilesFromDirectory(imageFileDirectory);
        } catch(IOException e) {
            MainView.displayErrorAlert(OPEN_FOLDER_ERROR_DIALOG_TITLE, OPEN_FOLDER_ERROR_DIALOG_HEADER);
            return;
        }

        if(imageFiles.isEmpty()) {
            MainView.displayErrorAlert(LOAD_IMAGE_FOLDER_ERROR_DIALOG_TITLE, LOAD_IMAGE_FOLDER_ERROR_DIALOG_CONTENT);
            return;
        }

        lastLoadedImageUrl = null;

        startImageMetaDataLoadingService(imageFileDirectory, imageFiles, false);
    }

    /**
     * Handles the event of the user requesting to save the image annotations.
     */
    public void onRegisterSaveAnnotationsAction(ImageAnnotationSaveStrategy.Type saveFormat) {
        updateModelFromView();

        if(!model.containsAnnotations() && !view.containsBoundingShapeViews()) {
            MainView.displayErrorAlert(SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE,
                                       NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT);
            return;
        }

        File destination = getAnnotationSavingDestination(saveFormat);

        if(destination != null) {
            initiateAnnotationExport(destination, saveFormat, null);
        }
    }

    /**
     * Handles the event of the user requesting to save the current image-annotations.
     */
    public void onRegisterImportAnnotationsAction(ImageAnnotationLoadStrategy.Type loadFormat) {
        final File source = getAnnotationLoadingSource(loadFormat);

        if(source != null) {
            initiateAnnotationImport(source, loadFormat);
        }
    }

    /**
     * Initiates the import of annotations.
     *
     * @param source the source of the annotations, either a folder or a single file
     */
    public void initiateAnnotationImport(File source, ImageAnnotationLoadStrategy.Type loadFormat) {
        updateModelFromView();

        if(model.containsCategories()) {
            ButtonBar.ButtonData keepExistingDataAnswer =
                    MainView.displayYesNoCancelDialogAndGetResult(IMPORT_ANNOTATION_DATA_OPTION_DIALOG_TITLE,
                                                                  IMPORT_ANNOTATION_DATA_OPTION_DIALOG_CONTENT);

            if(keepExistingDataAnswer == ButtonBar.ButtonData.NO) {
                if(!model.isSaved()) {
                    ButtonBar.ButtonData saveAnswer =
                            MainView.displayYesNoCancelDialogAndGetResult(ANNOTATIONS_SAVE_FORMAT_DIALOG_TITLE,
                                                                          ANNOTATION_IMPORT_SAVE_EXISTING_DIALOG_CONTENT);
                    if(saveAnswer == ButtonBar.ButtonData.YES) {
                        initiateAnnotationSavingWithFormatChoiceAndRunOnSaveSuccess(() -> {
                            clearModelAndViewAnnotationData();
                            startAnnotationImportService(source, loadFormat);
                        });

                        return;
                    } else if(saveAnswer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                        return;
                    }
                }

                clearModelAndViewAnnotationData();
            } else if(keepExistingDataAnswer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return;
            }
        }

        startAnnotationImportService(source, loadFormat);
    }

    /**
     * Handles the event of the user adding a new object category.
     */
    public void onRegisterAddObjectCategoryAction() {
        final String categoryName = view.getObjectCategoryInputField().getText();

        if(categoryName.isBlank()) {
            MainView.displayErrorAlert(CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                                       INVALID_CATEGORY_NAME_ERROR_DIALOG_CONTENT);
            view.getObjectCategoryInputField().clear();
            return;
        }

        if(model.getCategoryToAssignedBoundingShapesCountMap().containsKey(categoryName)) {
            MainView.displayErrorAlert(CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                                       "The category \"" + categoryName + "\" already exists.");
            view.getObjectCategoryInputField().clear();
            return;
        }

        final Color categoryColor = view.getObjectCategoryColorPicker().getValue();
        model.getObjectCategories().add(new ObjectCategory(categoryName, categoryColor));

        view.getObjectCategoryTable().getSelectionModel().selectLast();
        view.getObjectCategoryTable().scrollTo(view
                                                       .getObjectCategoryTable()
                                                       .getSelectionModel()
                                                       .getSelectedIndex()
        );

        view.getObjectCategoryInputField().clear();
        view.getObjectCategoryColorPicker().setValue(ColorUtils.createRandomColor());
    }

    /**
     * Handles the event of the user requesting to exit the application.
     */
    public void onRegisterExitAction() {
        updateModelFromView();

        if(!model.isSaved()) {
            ButtonBar.ButtonData answer =
                    MainView.displayYesNoCancelDialogAndGetResult(EXIT_APPLICATION_OPTION_DIALOG_TITLE,
                                                                  EXIT_APPLICATION_OPTION_DIALOG_CONTENT);

            if(answer == ButtonBar.ButtonData.YES) {
                initiateAnnotationSavingWithFormatChoiceAndRunOnSaveSuccess(() -> {
                    savePreferences();
                    interruptDirectoryWatcher();
                    Platform.exit();
                });

                return;
            } else if(answer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return;
            }
        }

        savePreferences();
        interruptDirectoryWatcher();
        Platform.exit();
    }

    /**
     * Handles the event of the user pressing a defined keyboard short-cut.
     *
     * @param event the short-cut key-event
     */
    public void onRegisterSceneKeyPressed(KeyEvent event) {
        // While the user is drawing a bounding box, all key-events will be ignored.
        if(view.getEditorImagePane().isBoundingBoxDrawingInProgress()) {
            event.consume();
            return;
        }

        if(event.isShortcutDown()) {
            view.getEditorImagePane().setZoomableAndPannable(true);
        }

        if(KeyCombinations.navigateNext.match(event)) {
            handleNavigateNextKeyPressed();
        } else if(KeyCombinations.navigatePrevious.match(event)) {
            handleNavigatePreviousKeyPressed();
        } else if(KeyCombinations.deleteSelectedBoundingShape.match(event)) {
            view.removeSelectedTreeItemAndChildren();
        } else if(KeyCombinations.removeEditingVerticesWhenBoundingPolygonSelected.match(event)) {
            view.removeEditingVerticesWhenPolygonViewSelected();
        } else if(KeyCombinations.focusCategorySearchField.match(event)) {
            view.getCategorySearchField().requestFocus();
        } else if(KeyCombinations.focusFileSearchField.match(event)) {
            view.getImageFileSearchField().requestFocus();
        } else if(KeyCombinations.focusCategoryNameTextField.match(event)) {
            view.getObjectCategoryInputField().requestFocus();
        } else if(KeyCombinations.focusTagTextField.match(event)) {
            view.getTagInputField().requestFocus();
        } else if(KeyCombinations.hideSelectedBoundingShape.match(event)) {
            view.getObjectTree().setToggleIconStateForSelectedObjectTreeItem(false);
        } else if(KeyCombinations.hideAllBoundingShapes.match(event)) {
            view.getObjectTree().setToggleIconStateForAllTreeItems(false);
        } else if(KeyCombinations.showSelectedBoundingShape.match(event)) {
            view.getObjectTree().setToggleIconStateForSelectedObjectTreeItem(true);
        } else if(KeyCombinations.showAllBoundingShapes.match(event)) {
            view.getObjectTree().setToggleIconStateForAllTreeItems(true);
        } else if(KeyCombinations.resetSizeAndCenterImage.match(event)) {
            view.getEditorImagePane().resetImageViewSize();
        } else if(KeyCombinations.selectRectangleDrawingMode.match(event)) {
            view.getEditor().getEditorToolBar().getRectangleModeButton().setSelected(true);
        } else if(KeyCombinations.selectPolygonDrawingMode.match(event)) {
            view.getEditor().getEditorToolBar().getPolygonModeButton().setSelected(true);
        } else if(KeyCombinations.changeSelectedBoundingShapeCategory.match(event)) {
            view.initiateCurrentSelectedBoundingBoxCategoryChange();
        }
    }

    /**
     * Handles the event of the user releasing a keyboard short-cut.
     *
     * @param event the short-cut key-event
     */
    public void onRegisterSceneKeyReleased(KeyEvent event) {
        if(event.getCode() == KeyCode.CONTROL || event.getCode() == KeyCode.META) {
            view.getEditorImagePane().setZoomableAndPannable(false);
        }

        if(KeyCombinations.navigatePrevious.match(event)) {
            navigatePreviousKeyPressed.set(false);
        } else if(KeyCombinations.navigateNext.match(event)) {
            navigateNextKeyPressed.set(false);
        }
    }

    /**
     * Handles the event of the user clicking the next(-image)-button.
     */
    public void onRegisterNextImageFileRequested() {
        model.incrementFileIndex();
        // Keep the currently selected item in the image-gallery in view.
        view.getImageFileListView().scrollTo(model.getCurrentFileIndex());
    }

    /**
     * Handles the event of the user clicking the previous(-image)-button.
     */
    public void onRegisterPreviousImageFileRequested() {
        model.decrementFileIndex();
        // Keep the currently selected item in the image-gallery in view.
        view.getImageFileListView().scrollTo(model.getCurrentFileIndex());
    }

    /**
     * Handles the event of the user committing a object category name edit. Names of categories are allowed
     * to be changed by the user as long as the uniqueness of category-names is not violated, otherwise an error dialog
     * will be displayed and the edit will be reverted.
     *
     * @param event the edit event
     * @see com.github.mfl28.boundingboxeditor.ui.ObjectCategoryTableView
     */
    public void onSelectorCellEditEvent(TableColumn.CellEditEvent<ObjectCategory, String> event) {
        String newName = event.getNewValue();
        String oldName = event.getOldValue();

        if(oldName.equals(newName)) {
            // Nothing to do if the new name is the same as the current one.
            return;
        }

        final ObjectCategory objectCategory = event.getRowValue();
        final Map<String, Integer> boundingShapesPerCategoryNameMap =
                model.getCategoryToAssignedBoundingShapesCountMap();

        if(newName.isBlank()) {
            MainView.displayErrorAlert(Controller.CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                                       INVALID_CATEGORY_NAME_ERROR_DIALOG_CONTENT);
            objectCategory.setName(oldName);
            event.getTableView().refresh();
        } else if(boundingShapesPerCategoryNameMap.containsKey(newName)) {
            MainView.displayErrorAlert(Controller.CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                                       "The category \"" + newName + "\" already exists.");
            objectCategory.setName(oldName);
            event.getTableView().refresh();
        } else {
            int assignedBoundingShapesCount = boundingShapesPerCategoryNameMap.get(oldName);
            boundingShapesPerCategoryNameMap.remove(oldName);

            boundingShapesPerCategoryNameMap.put(newName, assignedBoundingShapesCount);
            objectCategory.setName(newName);
        }
    }

    /**
     * Handles the event of the user releasing a mouse-click on the displayed image.
     *
     * @param event the mouse-event
     */
    public void onRegisterImageViewMouseReleasedEvent(MouseEvent event) {
        final EditorImagePaneView imagePane = view.getEditorImagePane();

        if(imagePane.isImageFullyLoaded() && event.getButton().equals(MouseButton.PRIMARY)) {
            if(event.isControlDown()) {
                view.getEditorImageView().setCursor(Cursor.OPEN_HAND);
            } else if(view.getObjectCategoryTable().isCategorySelected() &&
                    imagePane.isBoundingBoxDrawingInProgress()) {
                imagePane.constructAndAddNewBoundingBox();
                imagePane.setBoundingBoxDrawingInProgress(false);
            }
        }
    }

    /**
     * Handles the event of the user pressing the mouse on the displayed image.
     *
     * @param event the mouse-event
     */
    public void onRegisterImageViewMousePressedEvent(MouseEvent event) {
        EditorImagePaneView imagePaneView = view.getEditorImagePane();

        if(imagePaneView.isImageFullyLoaded()
                && !event.isControlDown()
                && imagePaneView.isCategorySelected()) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                if(imagePaneView.getDrawingMode() == EditorImagePaneView.DrawingMode.BOX) {
                    imagePaneView.initializeBoundingRectangle(event);
                } else if(imagePaneView.getDrawingMode() == EditorImagePaneView.DrawingMode.POLYGON) {
                    imagePaneView.initializeBoundingPolygon(event);
                }
            } else if(event.getButton().equals(MouseButton.SECONDARY)
                    && imagePaneView.getDrawingMode() == EditorImagePaneView.DrawingMode.POLYGON) {
                imagePaneView.setBoundingPolygonsEditingAndConstructing(false);
            }
        }
    }

    /**
     * Gets the main view to register it in a scene.
     *
     * @return the main view
     */
    public MainView getView() {
        return view;
    }

    /**
     * Returns the model component this controller operates on.
     *
     * @return the model
     */
    public Model getModel() {
        return model;
    }

    IoMetaData getIoMetaData() {
        return ioMetaData;
    }

    ImageAnnotationExportService getAnnotationExportService() {
        return annotationExportService;
    }

    ImageAnnotationImportService getAnnotationImportService() {
        return annotationImportService;
    }

    ImageMetaDataLoadingService getImageMetaDataLoadingService() {
        return imageMetaDataLoadingService;
    }

    Stage getStage() {
        return stage;
    }

    void initiateAnnotationExport(File destination,
                                  ImageAnnotationSaveStrategy.Type exportFormat,
                                  Runnable chainedOperation) {
        annotationExportService.setDestination(destination);
        annotationExportService.setExportFormat(exportFormat);
        annotationExportService.setAnnotationData(model.createImageAnnotationData());
        annotationExportService.setChainedOperation(chainedOperation);
        annotationExportService.reset();
        MainView.displayServiceProgressDialog(annotationExportService, SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE,
                                              SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER);
        annotationExportService.restart();
    }

    void initiateAnnotationExport(File destination, ImageAnnotationSaveStrategy.Type exportFormat) {
        initiateAnnotationExport(destination, exportFormat, null);
    }

    private void startAnnotationImportService(File source, ImageAnnotationLoadStrategy.Type importFormat) {
        annotationImportService.reset();
        annotationImportService.setSource(source);
        annotationImportService.setImportFormat(importFormat);
        annotationImportService.setImportableFileNames(model.getImageFileNameSet());
        annotationImportService.setCategoryNameToCategoryMap(model.getCategoryNameToCategoryMap());

        MainView.displayServiceProgressDialog(annotationImportService, LOADING_ANNOTATIONS_DIALOG_TITLE,
                                              LOADING_ANNOTATIONS_DIALOG_HEADER);

        annotationImportService.restart();
    }

    private void startImageMetaDataLoadingService(File source, List<File> imageFiles, boolean reload) {
        imageMetaDataLoadingService.reset();
        imageMetaDataLoadingService.setSource(source);
        imageMetaDataLoadingService.setImageFiles(imageFiles);
        imageMetaDataLoadingService.setReload(reload);
        MainView.displayServiceProgressDialog(imageMetaDataLoadingService, IMAGE_FILES_LOADING_DIALOG_TITLE,
                                              IMAGE_FILES_LOADING_DIALOG_HEADER);

        imageMetaDataLoadingService.restart();
    }

    private void setUpServices() {
        annotationExportService.setOnSucceeded(this::onAnnotationExportSucceeded);
        annotationExportService.setOnFailed(this::onIoServiceFailed);

        annotationImportService.setOnSucceeded(this::onAnnotationImportSucceeded);
        annotationImportService.setOnFailed(this::onIoServiceFailed);

        imageMetaDataLoadingService.setOnSucceeded(this::onImageMetaDataLoadingSucceeded);
        imageMetaDataLoadingService.setOnFailed(this::onIoServiceFailed);
    }

    private void onImageMetaDataLoadingSucceeded(WorkerStateEvent workerStateEvent) {
        ImageMetaDataLoadingResult ioResult = imageMetaDataLoadingService.getValue();

        if(ioResult.getNrSuccessfullyProcessedItems() != 0 && !handleSuccessfullyProcessedItemsPresent()) {
            return;
        }

        if(!ioResult.getErrorTableEntries().isEmpty()) {
            MainView.displayIOResultErrorInfoAlert(ioResult);
        } else if(ioResult.getNrSuccessfullyProcessedItems() == 0) {
            MainView.displayErrorAlert(IMAGE_IMPORT_ERROR_ALERT_TITLE, IMAGE_IMPORT_ERROR_ALERT_CONTENT);
        }

        if(imageMetaDataLoadingService.isReload() && ioResult.getNrSuccessfullyProcessedItems() == 0) {
            Controller.this.askToSaveExistingAnnotationDataAndClearModelAndView();
        }
    }

    private boolean handleSuccessfullyProcessedItemsPresent() {
        updateModelFromView();

        boolean keepExistingCategories = false;

        if(model.containsCategories()) {
            ButtonBar.ButtonData answer = imageMetaDataLoadingService.isReload() ?
                    MainView.displayYesNoDialogAndGetResult(OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                                                            KEEP_EXISTING_CATEGORIES_DIALOG_TEXT) :
                    MainView.displayYesNoCancelDialogAndGetResult(OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                                                                  KEEP_EXISTING_CATEGORIES_DIALOG_TEXT);

            keepExistingCategories = (answer == ButtonBar.ButtonData.YES);

            if(answer == ButtonBar.ButtonData.CANCEL_CLOSE && !imageMetaDataLoadingService.isReload()) {
                return false;
            }
        }

        if(!model.isSaved()) {
            // First ask if user wants to save the existing annotations.
            ButtonBar.ButtonData answer = imageMetaDataLoadingService.isReload() ?
                    MainView.displayYesNoDialogAndGetResult(RELOAD_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                                                            RELOAD_IMAGE_FOLDER_OPTION_DIALOG_CONTENT) :
                    MainView.displayYesNoCancelDialogAndGetResult(OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                                                                  OPEN_IMAGE_FOLDER_OPTION_DIALOG_CONTENT);

            if(answer == ButtonBar.ButtonData.YES) {
                boolean finalKeepExistingCategories = keepExistingCategories;

                if(imageMetaDataLoadingService.isReload()) {
                    initiateAnnotationSavingWithFormatChoiceAndRunInAnyCase(() -> onValidFilesPresentHandler(
                            finalKeepExistingCategories));
                } else {
                    initiateAnnotationSavingWithFormatChoiceAndRunOnSaveSuccess(
                            () -> onValidFilesPresentHandler(
                                    finalKeepExistingCategories));
                }
            } else if(answer == ButtonBar.ButtonData.NO || imageMetaDataLoadingService.isReload()) {
                onValidFilesPresentHandler(keepExistingCategories);
            }
        } else {
            onValidFilesPresentHandler(keepExistingCategories);
        }

        return true;
    }

    private void onValidFilesPresentHandler(boolean keepCategories) {
        ImageMetaDataLoadingResult result = imageMetaDataLoadingService.getValue();

        interruptDirectoryWatcher();

        model.clearAnnotationData(keepCategories);
        model.getImageFileNameToMetaDataMap().clear();
        model.getImageFileNameToMetaDataMap().putAll(result.getFileNameToMetaDataMap());

        model.fileIndexProperty().removeListener(selectedFileIndexListener);
        model.setImageFiles(result.getValidFiles());
        model.fileIndexProperty().addListener(selectedFileIndexListener);

        updateViewImageFiles();

        view.getStatusBar()
            .setStatusEvent(new ImageFilesLoadingSuccessfulEvent(result, imageMetaDataLoadingService.getSource()));

        directoryWatcher = new Thread(new FileChangeWatcher(imageMetaDataLoadingService.getSource().toPath(),
                                                            model.getImageFileNameSet(), () -> {
            MainView.displayErrorAlert(IMAGE_FILES_CHANGED_ERROR_TITLE, IMAGE_FILES_CHANGED_ERROR_CONTENT);
            Controller.this.initiateCurrentFolderReloading();
        }), IMAGE_FILE_CHANGE_WATCHER_THREAD_NAME);
        directoryWatcher.start();
    }

    private void onAnnotationImportSucceeded(WorkerStateEvent workerStateEvent) {
        ImageAnnotationImportResult loadResult = annotationImportService.getValue();

        if(loadResult.getNrSuccessfullyProcessedItems() != 0) {
            model.updateFromImageAnnotationData(loadResult.getImageAnnotationData());
            view.getStatusBar().setStatusEvent(new ImageAnnotationsImportingSuccessfulEvent(loadResult));
        }

        updateViewFileExplorerFileInfoElements();

        ImageAnnotation annotation = model.getCurrentImageAnnotation();

        if(annotation != null) {
            view.getObjectTree().reset();
            view.getCurrentBoundingShapes().removeListener(boundingShapeCountPerCategoryListener);
            view.loadBoundingShapeViewsFromAnnotation(annotation);
            view.getCurrentBoundingShapes().addListener(boundingShapeCountPerCategoryListener);
            view.getObjectCategoryTable().refresh();
            view.getObjectTree().refresh();
        }

        if(!loadResult.getErrorTableEntries().isEmpty()) {
            MainView.displayIOResultErrorInfoAlert(loadResult);
        } else if(loadResult.getNrSuccessfullyProcessedItems() == 0) {
            MainView.displayErrorAlert(ANNOTATION_IMPORT_ERROR_TITLE,
                                       ANNOTATION_IMPORT_ERROR_NO_VALID_FILES_CONTENT);
            return;
        }

        setCurrentAnnotationLoadingDirectory(annotationImportService.getSource());
    }


    private void onIoServiceFailed(WorkerStateEvent event) {
        final Throwable exception = event.getSource().getException();

        if(exception != null) {
            MainView.displayExceptionDialog(exception);
        }
    }

    private void onAnnotationExportSucceeded(WorkerStateEvent event) {
        IOResult saveResult = annotationExportService.getValue();

        if(saveResult.getNrSuccessfullyProcessedItems() != 0) {
            view.getStatusBar().setStatusEvent(new ImageAnnotationsSavingSuccessfulEvent(saveResult));
        }

        if(!saveResult.getErrorTableEntries().isEmpty()) {
            MainView.displayIOResultErrorInfoAlert(saveResult);
        } else {
            model.setSaved(true);
        }

        setCurrentAnnotationSavingDirectory(annotationExportService.getDestination());

        if(annotationExportService.getChainedOperation() != null) {
            annotationExportService.getChainedOperation().run();
        }
    }

    private void updateViewFileExplorerFileInfoElements() {
        final Map<String, ImageAnnotation> fileNameToAnnotationMap = model.getImageFileNameToAnnotationMap();

        for(ImageFileListView.FileInfo fileInfo : view.getImageFileListView().getItems()) {
            ImageAnnotation annotation = fileNameToAnnotationMap.get(fileInfo.getFile().getName());

            if(annotation != null && !annotation.getBoundingShapeData().isEmpty()) {
                fileInfo.setHasAssignedBoundingShapes(true);
            }
        }
    }

    private void clearModelAndViewAnnotationData() {
        model.clearAnnotationData(false);
        view.reset();
        view.getEditorImagePane().removeAllCurrentBoundingShapes();
        // Reset all 'assigned bounding shape states' in image file explorer.
        view.getImageFileListView().getItems().forEach(item -> item.setHasAssignedBoundingShapes(false));
    }

    private void updateModelFromView() {
        if(model.containsImageFiles()) {
            model.updateCurrentBoundingShapeData(view.extractCurrentBoundingShapeData());
        }
    }

    private void forceLoadImageFiles(File imageFileDirectory) {
        List<File> imageFiles;

        try {
            imageFiles = getImageFilesFromDirectory(imageFileDirectory);
        } catch(IOException e) {
            MainView.displayErrorAlert(OPEN_FOLDER_ERROR_DIALOG_TITLE, OPEN_FOLDER_ERROR_DIALOG_HEADER);
            askToSaveExistingAnnotationDataAndClearModelAndView();
            return;
        }

        if(imageFiles.isEmpty()) {
            MainView.displayErrorAlert(LOAD_IMAGE_FOLDER_ERROR_DIALOG_TITLE, LOAD_IMAGE_FOLDER_ERROR_DIALOG_CONTENT);
            askToSaveExistingAnnotationDataAndClearModelAndView();
            return;
        }

        lastLoadedImageUrl = null;

        startImageMetaDataLoadingService(imageFileDirectory, imageFiles, true);
    }

    private void askToSaveExistingAnnotationDataAndClearModelAndView() {
        if(!model.isSaved()) {
            // First ask if user wants to save the existing annotations.
            ButtonBar.ButtonData answer =
                    MainView.displayYesNoDialogAndGetResult(RELOAD_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                                                            RELOAD_IMAGE_FOLDER_OPTION_DIALOG_CONTENT);

            if(answer == ButtonBar.ButtonData.YES) {
                initiateAnnotationSavingWithFormatChoiceAndRunInAnyCase(this::clearViewAndModel);
                return;
            }
        }

        clearViewAndModel();
    }

    private void initiateAnnotationSavingWithFormatChoiceAndRunOnSaveSuccess(Runnable runnable) {
        // Ask for annotation save format.
        Optional<ImageAnnotationSaveStrategy.Type> formatChoice =
                MainView.displayChoiceDialogAndGetResult(ImageAnnotationSaveStrategy.Type.PASCAL_VOC,
                                                         Arrays.asList(ImageAnnotationSaveStrategy.Type.values()),
                                                         ANNOTATIONS_SAVE_FORMAT_DIALOG_TITLE,
                                                         ANNOTATIONS_SAVE_FORMAT_DIALOG_HEADER,
                                                         ANNOTATIONS_SAVE_FORMAT_DIALOG_CONTENT);

        formatChoice.ifPresent(choice -> {
            // Ask for annotation save directory.
            final File destination = getAnnotationSavingDestination(choice);

            if(destination != null) {
                // Save annotations.
                initiateAnnotationExport(destination, choice, runnable);
            }
        });
    }

    private void initiateAnnotationSavingWithFormatChoiceAndRunInAnyCase(Runnable runnable) {
        // Ask for annotation save format.
        Optional<ImageAnnotationSaveStrategy.Type> formatChoice =
                MainView.displayChoiceDialogAndGetResult(ImageAnnotationSaveStrategy.Type.PASCAL_VOC,
                                                         Arrays.asList(ImageAnnotationSaveStrategy.Type.values()),
                                                         ANNOTATIONS_SAVE_FORMAT_DIALOG_TITLE,
                                                         ANNOTATIONS_SAVE_FORMAT_DIALOG_HEADER,
                                                         ANNOTATIONS_SAVE_FORMAT_DIALOG_CONTENT);

        formatChoice.ifPresentOrElse(choice -> {
            // Ask for annotation save directory.
            final File destination = getAnnotationSavingDestination(choice);

            if(destination != null) {
                // Save annotations.
                initiateAnnotationExport(destination, choice, runnable);
            } else {
                runnable.run();
            }
        }, runnable);
    }

    private File getAnnotationSavingDestination(ImageAnnotationSaveStrategy.Type saveFormat) {
        File destination;

        if(saveFormat.equals(ImageAnnotationSaveStrategy.Type.JSON)) {
            destination = MainView.displayFileChooserAndGetChoice(SAVE_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE, stage,
                                                                  ioMetaData.getDefaultAnnotationSavingDirectory(),
                                                                  DEFAULT_JSON_EXPORT_FILENAME,
                                                                  new FileChooser.ExtensionFilter("JSON files",
                                                                                                  "*.json",
                                                                                                  "*.JSON"),
                                                                  MainView.FileChooserType.SAVE);
        } else {
            destination =
                    MainView.displayDirectoryChooserAndGetChoice(SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE, stage,
                                                                 ioMetaData.getDefaultAnnotationSavingDirectory());
        }

        return destination;
    }

    private File getAnnotationLoadingSource(ImageAnnotationLoadStrategy.Type loadFormat) {
        File source;

        if(loadFormat.equals(ImageAnnotationLoadStrategy.Type.JSON)) {
            source = MainView.displayFileChooserAndGetChoice(LOAD_IMAGE_ANNOTATIONS_FILE_CHOOSER_TITLE, stage,
                                                             ioMetaData.getDefaultAnnotationLoadingDirectory(),
                                                             DEFAULT_JSON_EXPORT_FILENAME,
                                                             new FileChooser.ExtensionFilter("JSON files", "*.json",
                                                                                             "*.JSON"),
                                                             MainView.FileChooserType.OPEN);
        } else {
            source = MainView.displayDirectoryChooserAndGetChoice(LOAD_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE, stage,
                                                                  ioMetaData.getDefaultAnnotationLoadingDirectory());
        }

        return source;
    }

    private void interruptDirectoryWatcher() {
        if(directoryWatcher != null && directoryWatcher.isAlive()) {
            directoryWatcher.interrupt();
        }
    }

    private void handleNavigateNextKeyPressed() {
        if(model.containsImageFiles() && model.hasNextImageFile()
                && !navigatePreviousKeyPressed.get()) {
            navigateNextKeyPressed.set(true);
            onRegisterNextImageFileRequested();
        } else {
            navigateNextKeyPressed.set(false);
        }
    }

    private void handleNavigatePreviousKeyPressed() {
        if(model.containsImageFiles() && model.hasPreviousImageFile()
                && !navigateNextKeyPressed.get()) {
            navigatePreviousKeyPressed.set(true);
            onRegisterPreviousImageFileRequested();
        } else {
            navigatePreviousKeyPressed.set(false);
        }
    }

    private void setUpModelListeners() {
        view.getEditor().getEditorToolBar()
            .getIndexLabel()
            .textProperty()
            .bind(model.fileIndexProperty().add(1).asString()
                       .concat(" | ")
                       .concat(model.nrImageFilesProperty().asString()));

        view.getImageFileExplorer().getImageFileListView().getSelectionModel().selectedIndexProperty()
            .addListener((observable, oldValue, newValue) -> {
                if(newValue.intValue() != -1) {
                    model.fileIndexProperty().set(newValue.intValue());
                }
            });

        view.getFileImportAnnotationsItem().disableProperty().bind(model.nrImageFilesProperty().isEqualTo(0));

        view.getPreviousImageNavigationButton().disableProperty().bind(model.hasPreviousImageFileProperty().not());
        view.getNextImageNavigationButton().disableProperty().bind(model.hasNextImageFileProperty().not());

        view.getObjectCategoryTable().getDeleteColumn().setCellFactory(column -> {
            final ObjectCategoryDeleteTableCell cell = new ObjectCategoryDeleteTableCell();

            cell.getDeleteButton().setOnAction(action -> {
                final ObjectCategory category = cell.getItem();

                int nrExistingBoundingShapes =
                        model.getCategoryToAssignedBoundingShapesCountMap().getOrDefault(category.getName(), 0);

                // Only allow to delete a bounding-box category that has no bounding-boxes assigned to it.
                if(nrExistingBoundingShapes != 0) {
                    MainView.displayErrorAlert(CATEGORY_DELETION_ERROR_DIALOG_TITLE,
                                               CATEGORY_DELETION_ERROR_DIALOG_CONTENT
                                                       + "\nCurrently there " +
                                                       (nrExistingBoundingShapes == 1 ? "is " : "are ") +
                                                       nrExistingBoundingShapes
                                                       + " annotated object" +
                                                       (nrExistingBoundingShapes == 1 ? " " : "s ") +
                                                       "with the category \"" + category.getName() + "\".");
                } else {
                    cell.getTableView().getItems().remove(category);
                }
            });

            return cell;
        });

        view.getStatusBar().savedStatusProperty().bind(model.savedProperty());
    }

    private List<File> getImageFilesFromDirectory(File directory) throws IOException {
        Path path = Paths.get(directory.getPath());

        try(Stream<Path> imageFiles = Files.walk(path, MAX_DIRECTORY_DEPTH)) {
            return imageFiles.filter(file -> Arrays.stream(imageExtensions).anyMatch(file.toString()::endsWith))
                             .map(file -> new File(file.toString()))
                             .filter(File::isFile)
                             .sorted(Comparator.comparing(File::getName))
                             .collect(Collectors.toList());
        }
    }

    private void updateViewImageFiles() {
        view.reset();

        EditorImagePaneView imagePane = view.getEditorImagePane();
        imagePane.removeAllCurrentBoundingShapes();
        view.getCurrentBoundingShapes().removeListener(boundingShapeCountPerCategoryListener);
        imagePane.getImageLoadingProgressIndicator().setVisible(true);

        updateViewImageFromModel();

        updateStageTitle();

        ObjectCategoryTableView objectCategoryTableView = view.getObjectCategoryTable();
        objectCategoryTableView.setItems(model.getObjectCategories());
        objectCategoryTableView.getSelectionModel().selectFirst();

        ImageFileExplorerView imageFileExplorerView = view.getImageFileExplorer();
        imageFileExplorerView.setImageFiles(model.getImageFiles());

        ImageFileListView imageFileListView = view.getImageFileListView();
        imageFileListView.getSelectionModel().selectFirst();
        imageFileListView.scrollTo(0);
    }

    private void updateStageTitle() {
        ImageMetaData currentImageMetaData = model.getCurrentImageMetaData();
        stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR
                               + model.getCurrentImageFilePath() + " " + currentImageMetaData.getDimensionsString());
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ChangeListener<Number> createImageLoadingProgressListener() {
        return (observable, oldValue, newValue) -> {
            if(newValue.intValue() == 1) {
                ImageAnnotation annotation = model.getCurrentImageAnnotation();
                // Hide the progress spinner.
                view.getEditorImagePane().getImageLoadingProgressIndicator().setVisible(false);

                if(annotation != null) {
                    view.loadBoundingShapeViewsFromAnnotation(annotation);
                }

                view.getCurrentBoundingShapes().addListener(boundingShapeCountPerCategoryListener);
            }
        };
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ChangeListener<Number> createSelectedFileIndexListener() {
        return (value, oldValue, newValue) -> {
            // Update selected item in image-file-list-view.
            view.getImageFileExplorer().getImageFileListView().getSelectionModel().select(newValue.intValue());
            // Show the progress spinner.
            view.getEditorImagePane().getImageLoadingProgressIndicator().setVisible(true);

            final Image oldImage = view.getCurrentImage();

            if(oldImage != null && !oldImage.getUrl().equals(lastLoadedImageUrl)) {
                // Remove the old images bounding-box-loading listener (that triggers when an image is fully loaded.)
                oldImage.progressProperty().removeListener(imageLoadProgressListener);
                // Updating bounding-box data corresponding to the previous image only needs to be done, if
                // the old image was fully loaded.
                if(oldImage.getProgress() == 1.0) {
                    // update model bounding-box-data from previous image:
                    model.updateBoundingShapeDataAtFileIndex(oldValue.intValue(),
                                                             view.extractCurrentBoundingShapeData());
                    // remove old image's bounding boxes
                    view.getCurrentBoundingShapes().removeListener(boundingShapeCountPerCategoryListener);
                    view.getEditorImagePane().removeAllCurrentBoundingShapes();
                    // Prevents javafx-bug with uncleared items in tree-view when switching between images.
                    view.getObjectTree().reset();
                } else {
                    oldImage.cancel();
                }

                // Clears the current image from the view.
                view.getEditorImageView().setImage(null);
                lastLoadedImageUrl = oldImage.getUrl();
            }

            updateStageTitle();

            if(navigateNextKeyPressed.get() ^ navigatePreviousKeyPressed.get()) {
                // If a navigation key is pressed, image loading is skipped (but the image file index is still updated).
                // Once the navigation key is released the image corresponding to the file index at the point of the release
                // will be loaded.
                if(navigatePreviousKeyPressed.get()) {
                    navigatePreviousKeyPressed.removeListener(imageNavigationKeyPressedListener);
                    navigatePreviousKeyPressed.addListener(imageNavigationKeyPressedListener);
                } else {
                    navigateNextKeyPressed.removeListener(imageNavigationKeyPressedListener);
                    navigateNextKeyPressed.addListener(imageNavigationKeyPressedListener);
                }

            } else {
                // Load the image corresponding to the current file index into the view-component.
                updateViewImageFromModel();
            }
        };
    }

    private ChangeListener<Boolean> createImageNavigationKeyPressedListener() {
        return new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(!Boolean.TRUE.equals(newValue)) {
                    // Load the image corresponding to the current file index into the view-component.
                    updateViewImageFromModel();
                    observable.removeListener(this);
                }
            }
        };
    }

    private void updateViewImageFromModel() {
        ImageMetaData metaData;

        try {
            metaData = model.getCurrentImageMetaData();
        } catch(Exception e) {
            view.getEditorImagePane().getImageLoadingProgressIndicator().setVisible(false);
            MainView.displayErrorAlert("Image loading error",
                                       "Could not read meta-data from image file \"" + model.getCurrentImageFileName() +
                                               "\".");
            return;
        }

        view.updateImageFromFile(model.getCurrentImageFile(), metaData.getImageWidth(), metaData.getImageHeight());
        view.getCurrentImage().progressProperty().addListener(imageLoadProgressListener);
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ListChangeListener<BoundingShapeViewable> createBoundingShapeCountPerCategoryListener() {
        return change -> {
            Map<String, Integer> categoryToShapesCountMap = model.getCategoryToAssignedBoundingShapesCountMap();

            while(change.next()) {
                if(change.wasAdded()) {
                    change.getAddedSubList().forEach(item ->
                                                             categoryToShapesCountMap
                                                                     .merge(item.getViewData().getObjectCategory()
                                                                                .getName(),
                                                                            1, Integer::sum));
                }

                if(change.wasRemoved()) {
                    change.getRemoved().forEach(item ->
                                                        categoryToShapesCountMap.computeIfPresent(
                                                                item.getViewData().getObjectCategory().getName(),
                                                                (key, value) -> --value));
                }

                if(change.wasUpdated()) {
                    for(int i = change.getFrom(); i != change.getTo(); ++i) {
                        BoundingShapeViewData changedShapeViewData = change.getList().get(i).getViewData();

                        categoryToShapesCountMap.computeIfPresent(changedShapeViewData.getPreviousObjectCategoryName(),
                                                                  (key, value) -> --value);

                        categoryToShapesCountMap.merge(changedShapeViewData.getObjectCategory().getName(),
                                                       1, Integer::sum);
                    }
                }
            }
        };
    }

    private void loadPreferences() {
        Preferences preferences = Preferences.userNodeForPackage(getClass());
        stage.setMaximized(preferences.getBoolean(IS_WINDOW_MAXIMIZED_PREFERENCE_NAME, false));

        String imageLoadingDirectoryPathPreference =
                preferences.get(CURRENT_IMAGE_LOADING_DIRECTORY_PREFERENCE_NAME, null);

        if(imageLoadingDirectoryPathPreference != null) {
            File imageLoadingDirectoryPreference = new File(imageLoadingDirectoryPathPreference);

            if(imageLoadingDirectoryPreference.exists() && imageLoadingDirectoryPreference.isDirectory()) {
                ioMetaData.setDefaultImageLoadingDirectory(imageLoadingDirectoryPreference);
            }
        }

        String annotationLoadingDirectoryPathPreference =
                preferences.get(CURRENT_ANNOTATION_LOADING_DIRECTORY_PREFERENCE_NAME, null);

        if(annotationLoadingDirectoryPathPreference != null) {
            File annotationLoadingDirectoryPreference = new File(annotationLoadingDirectoryPathPreference);

            if(annotationLoadingDirectoryPreference.exists() && annotationLoadingDirectoryPreference.isDirectory()) {
                ioMetaData.setDefaultAnnotationLoadingDirectory(annotationLoadingDirectoryPreference);
            }
        }

        String annotationSavingDirectoryPathPreference =
                preferences.get(CURRENT_ANNOTATION_SAVING_DIRECTORY_PREFERENCE_NAME, null);

        if(annotationSavingDirectoryPathPreference != null) {
            File annotationSavingDirectoryPreference = new File(annotationSavingDirectoryPathPreference);

            if(annotationSavingDirectoryPreference.exists() && annotationSavingDirectoryPreference.isDirectory()) {
                ioMetaData.setDefaultAnnotationSavingDirectory(annotationSavingDirectoryPreference);
            }
        }
    }

    private void savePreferences() {
        Preferences preferences = Preferences.userNodeForPackage(getClass());

        preferences.putBoolean(IS_WINDOW_MAXIMIZED_PREFERENCE_NAME, stage.isMaximized());

        if(ioMetaData.getDefaultImageLoadingDirectory() != null) {
            preferences.put(CURRENT_IMAGE_LOADING_DIRECTORY_PREFERENCE_NAME,
                            ioMetaData.getDefaultImageLoadingDirectory().toString());
        }

        if(ioMetaData.getDefaultAnnotationLoadingDirectory() != null) {
            preferences.put(CURRENT_ANNOTATION_LOADING_DIRECTORY_PREFERENCE_NAME,
                            ioMetaData.getDefaultAnnotationLoadingDirectory().toString());
        }

        if(ioMetaData.getDefaultAnnotationSavingDirectory() != null) {
            preferences.put(CURRENT_ANNOTATION_SAVING_DIRECTORY_PREFERENCE_NAME,
                            ioMetaData.getDefaultAnnotationSavingDirectory().toString());
        }
    }

    private void clearViewAndModel() {
        interruptDirectoryWatcher();

        model.fileIndexProperty().removeListener(selectedFileIndexListener);
        model.clear();

        view.reset();

        EditorImagePaneView imagePane = view.getEditorImagePane();
        imagePane.removeAllCurrentBoundingShapes();
        view.getCurrentBoundingShapes().removeListener(boundingShapeCountPerCategoryListener);

        stage.setTitle(PROGRAM_NAME);

        ObjectCategoryTableView objectCategoryTableView = view.getObjectCategoryTable();
        objectCategoryTableView.getItems().clear();
        objectCategoryTableView.getSelectionModel().clearSelection();

        ImageFileListView imageFileListView = view.getImageFileListView();
        imageFileListView.setItems(null);
        imageFileListView.getSelectionModel().clearSelection();

        view.getStatusBar().clear();
        view.setWorkspaceVisible(false);
    }

    private void setCurrentAnnotationSavingDirectory(File destination) {
        if(destination.isDirectory()) {
            ioMetaData.setDefaultAnnotationSavingDirectory(destination);
        } else if(destination.isFile() && destination.getParentFile().isDirectory()) {
            ioMetaData.setDefaultAnnotationSavingDirectory(destination.getParentFile());
        }
    }

    private void setCurrentAnnotationLoadingDirectory(File source) {
        if(source.isDirectory()) {
            ioMetaData.setDefaultAnnotationLoadingDirectory(source);
        } else if(source.isFile() && source.getParentFile().isDirectory()) {
            ioMetaData.setDefaultAnnotationLoadingDirectory(source.getParentFile());
        }
    }

    /**
     * Class containing possible key-combinations.
     */
    public static class KeyCombinations {
        public static final KeyCombination navigateNext = new KeyCodeCombination(KeyCode.D);
        public static final KeyCombination navigatePrevious = new KeyCodeCombination(KeyCode.A);
        public static final KeyCombination showAllBoundingShapes =
                new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
        public static final KeyCombination hideAllBoundingShapes =
                new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
        public static final KeyCombination showSelectedBoundingShape =
                new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination hideSelectedBoundingShape =
                new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN);

        public static final KeyCombination resetSizeAndCenterImage =
                new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination focusCategoryNameTextField =
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination focusCategorySearchField =
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination focusTagTextField =
                new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination focusFileSearchField =
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
        public static final KeyCombination deleteSelectedBoundingShape = new KeyCodeCombination(KeyCode.DELETE);
        public static final KeyCombination selectRectangleDrawingMode =
                new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination selectPolygonDrawingMode =
                new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination removeEditingVerticesWhenBoundingPolygonSelected =
                new KeyCodeCombination(KeyCode.DELETE, KeyCombination.SHIFT_DOWN);
        public static final KeyCombination addVerticesToPolygon =
                KeyCombination.keyCombination("Shift + Middle-Click inside Polygon");
        public static final KeyCombination changeSelectedBoundingShapeCategory =
                new KeyCodeCombination(KeyCode.C, KeyCombination.SHIFT_DOWN);

        private KeyCombinations() {
            throw new IllegalStateException("Key Combination Class");
        }
    }

}