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

import com.github.mfl28.boundingboxeditor.controller.utils.KeyCombinationEventHandler;
import com.github.mfl28.boundingboxeditor.model.Model;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.IoMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.model.io.results.*;
import com.github.mfl28.boundingboxeditor.model.io.services.*;
import com.github.mfl28.boundingboxeditor.ui.*;
import com.github.mfl28.boundingboxeditor.ui.settings.InferenceSettingsView;
import com.github.mfl28.boundingboxeditor.ui.statusevents.BoundingBoxPredictionSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.ImageAnnotationsImportingSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.ImageAnnotationsSavingSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.ImageFilesLoadingSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import com.github.mfl28.boundingboxeditor.utils.ImageUtils;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

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
    private static final String GITHUB_WIKI_URL = "https://github.com/mfl28/BoundingBoxEditor/wiki";
    private static final String PROGRAM_VERSION = "2.8.0";
    private static final String PROGRAM_LICENSE = "GPL-3.0";
    private static final String PROGRAM_IDENTIFIER = PROGRAM_NAME + " " + PROGRAM_VERSION;
    private static final String CATEGORY_INPUT_ERROR_DIALOG_TITLE = "Category Creation Error";
    private static final String INVALID_CATEGORY_NAME_ERROR_DIALOG_CONTENT =
            "Please provide a non-blank category name.";
    private static final String CATEGORY_DELETION_ERROR_DIALOG_TITLE = "Category Deletion Error";
    private static final String CATEGORY_DELETION_ERROR_DIALOG_CONTENT =
            "You cannot delete a category that has existing bounding-boxes assigned to it.";

    private static final String SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE = "Save Error";
    private static final String NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT =
            "There are no image annotations to save.";
    private static final String ANNOTATION_IMPORT_ERROR_TITLE = "Annotation Import Error";
    private static final String ANNOTATION_IMPORT_ERROR_NO_VALID_FILES_CONTENT =
            "The source does not contain any valid annotations.";
    private static final String EXIT_APPLICATION_OPTION_DIALOG_TITLE = "Exit Application";
    private static final String EXIT_APPLICATION_OPTION_DIALOG_CONTENT =
            "Do you want to save the existing annotation data?";
    private static final String SETTINGS_APPLICATION_ERROR_DIALOG_TITLE = "Settings Application Error";
    private static final String SETTINGS_APPLICATION_INVALID_FIELDS_ERROR_DIALOG_CONTENT =
            "Please provide valid values for the indicated fields.";
    private static final String SETTINGS_APPLICATION_NO_MODEL_SELECTED_ERROR_DIALOG_CONTENT =
            "Please select a model or disable inference.";
    private static final String IMAGE_LOADING_ERROR_DIALOG_TITLE = "Image Loading Error";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE = "Saving Annotations";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER = "Saving in progress...";
    private static final String LOADING_ANNOTATIONS_PROGRESS_DIALOG_TITLE = "Loading";
    private static final String LOADING_ANNOTATIONS_PROGRESS_DIALOG_HEADER = "Loading annotations...";
    private static final String IMAGE_FILES_LOADING_PROGRESS_DIALOG_TITLE = "Loading Images";
    private static final String IMAGE_FILES_LOADING_PROGRESS_DIALOG_HEADER = "Loading image meta-data";
    private static final String BOUNDING_BOX_PREDICTION_PROGRESS_DIALOG_TITLE = "Predicting";
    private static final String BOUNDING_BOX_PREDICTION_PROGRESS_DIALOG_HEADER = "Predicting bounding boxes";
    private static final String FETCHING_MODELS_PROGRESS_DIALOG_TITLE = "Fetching Models";
    private static final String FETCHING_MODELS_PROGRESS_DIALOG_HEADER = "Fetching model names from server";
    private static final String CONNECTION_CHECK_PROGRESS_DIALOG_TITLE = "Checking Connection";
    private static final String CONNECTION_CHECK_PROGRESS_DIALOG_HEADER = "Checking the inference server";

    private final ImageAnnotationExportService annotationExportService = new ImageAnnotationExportService();
    private final ImageAnnotationImportService annotationImportService = new ImageAnnotationImportService();
    private final ImageMetaDataLoadingService imageMetaDataLoadingService = new ImageMetaDataLoadingService();
    private final BoundingBoxPredictorService boundingBoxPredictorService = new BoundingBoxPredictorService();
    private final ModelNameFetchService modelNameFetchService = new ModelNameFetchService();
    private final ServerConnectionCheckService serverConnectionCheckService = new ServerConnectionCheckService();
    private final Stage stage;
    private final HostServices hostServices;
    private final MainView view;
    private final DialogService dialogService;
    private final AnnotationIoController annotationIoController;
    private final ImageFolderController imageFolderController;
    private final InferenceController inferenceController;
    private final KeyboardShortcutHandler keyboardShortcutHandler;
    private final Model model = new Model();
    private final EditHistoryController editHistoryController =
            new EditHistoryController(new EditHistoryOperations());
    private boolean editCheckpointScheduled = false;
    private final ListChangeListener<BoundingShapeViewable> boundingShapeCountPerCategoryListener =
            createBoundingShapeCountPerCategoryListener();
    private final ChangeListener<Number> imageLoadProgressListener = createImageLoadingProgressListener();
    private final ChangeListener<Boolean> imageNavigationKeyPressedListener = createImageNavigationKeyPressedListener();
    private final IoMetaData ioMetaData = new IoMetaData();
    private final PreferencesStore preferencesStore = PreferencesStore.forApplication();
    final List<KeyCombinationEventHandler> keyCombinationHandlers;
    String lastLoadedImageUrl;
    private final ChangeListener<Number> selectedFileIndexListener = createSelectedFileIndexListener();

    /**
     * Creates a new controller object that is responsible for handling the application logic and
     * handles interaction between the view and model components.
     *
     * @param mainStage the stage that represents the top level container of all used ui-elements
     */
    public Controller(final Stage mainStage, final MainView view, final HostServices hostServices) {
        this(mainStage, view, hostServices, new JavaFxDialogService());
    }

    /**
     * Creates a new controller object that is responsible for handling the application logic and
     * handles communication between the model- and view-components.
     *
     * @param mainStage     the stage that represents the top level container of all used ui-elements
     * @param view          the main view object
     * @param hostServices  the host services of the application
     * @param dialogService the service used to show dialogs to the user
     */
    public Controller(final Stage mainStage, final MainView view, final HostServices hostServices,
                      final DialogService dialogService) {
        stage = mainStage;
        this.view = view;
        this.hostServices = hostServices;
        this.dialogService = dialogService;
        this.annotationIoController = new AnnotationIoController(model, ioMetaData, dialogService, stage,
                new AnnotationIoOperations());
        this.imageFolderController = new ImageFolderController(model, ioMetaData, dialogService, stage,
                annotationIoController, new ImageFolderOperations());
        this.inferenceController = new InferenceController(model, dialogService, new InferenceOperations());
        this.keyboardShortcutHandler = new KeyboardShortcutHandler(model, new KeyboardShortcutEditor(),
                KeyboardShortcutHandler.createViewActionShortcuts(view, this::onRegisterSettingsAction,
                        this::onRegisterUndoAction, this::onRegisterRedoAction));
        this.keyCombinationHandlers = keyboardShortcutHandler.getKeyCombinationHandlers();

        setupStage();
        loadPreferences();
        view.connectToController(this);
        setUpModelListeners();
        setUpServices();
    }

    public void onRegisterSettingsAction() {
        view.getInferenceSettingsView()
                .setDisplayedSettingsFromPredictorClientConfig(model.getBoundingBoxPredictorClientConfig());
        view.getInferenceSettingsView()
                .setDisplayedSettingsFromPredictorConfig(model.getBoundingBoxPredictorConfig());
        view.getUiSettingsView()
                .setDisplayedSettingsFromUISettingsConfig(view.getUiSettingsConfig());
        view.getEditorSettingsView()
                .setDisplayedSettingsFromEditorSettingsConfig(view.getEditorSettingsConfig());

        view.displaySettingsDialog(this, stage);
    }

    public void onRegisterSettingsApplyAction(ActionEvent event, ButtonType buttonType) {
        final InferenceSettingsView inferenceSettingsView = view.getInferenceSettingsView();

        if(buttonType.equals(ButtonType.OK) || buttonType.equals(ButtonType.APPLY)) {
            if(!inferenceSettingsView.validateSettings()) {
                dialogService.displayErrorAlert(SETTINGS_APPLICATION_ERROR_DIALOG_TITLE,
                        SETTINGS_APPLICATION_INVALID_FIELDS_ERROR_DIALOG_CONTENT,
                        view.getSettingsWindow().orElse(stage));
                event.consume();
                return;
            }

            if(inferenceSettingsView.isModelSelectionMissing()) {
                dialogService.displayErrorAlert(SETTINGS_APPLICATION_ERROR_DIALOG_TITLE,
                        SETTINGS_APPLICATION_NO_MODEL_SELECTED_ERROR_DIALOG_CONTENT,
                        view.getSettingsWindow().orElse(stage));
                event.consume();
                return;
            }
        }

        final boolean inferenceWasEnabled = model.getBoundingBoxPredictorConfig().isInferenceEnabled();

        view.getInferenceSettingsView()
                .applyDisplayedSettingsToPredictorClientConfig(model.getBoundingBoxPredictorClientConfig());
        view.getInferenceSettingsView()
                .applyDisplayedSettingsToPredictorConfig(model.getBoundingBoxPredictorConfig());
        view.getUiSettingsView()
                .applyDisplayedSettingsToUISettingsConfig(view.getUiSettingsConfig());
        view.getEditorSettingsView()
                .applyDisplayedSettingsToEditorSettingsConfig(view.getEditorSettingsConfig());

        inferenceController.onInferenceSettingsApplied(inferenceWasEnabled,
                model.getBoundingBoxPredictorConfig().isInferenceEnabled());

        if(buttonType.equals(ButtonType.APPLY)) {
            event.consume();
        }
    }

    /**
     * Handles the event of the user requesting to open a new image folder.
     */
    public void onRegisterOpenImageFolderAction() {
        imageFolderController.openImageFolder();
    }

    public void onRegisterPerformCurrentImageBoundingBoxPredictionAction() {
        inferenceController.predictCurrentImage();
    }

    /**
     * Initiates the loading of image files from a provided folder.
     *
     * @param imageFolder the folder containing the image files to load
     */
    public void initiateImageFolderLoading(File imageFolder) {
        imageFolderController.openImageFolder(imageFolder);
    }

    public void initiateCurrentFolderReloading() {
        imageFolderController.reloadCurrentFolder();
    }

    /**
     * Loads image-files from the provided directory into the model and updates
     * the view.
     *
     * @param imageFileDirectory the directory containing the image-files to be loaded
     */
    public void loadImageFiles(File imageFileDirectory) {
        imageFolderController.loadImageFiles(imageFileDirectory);
    }

    /**
     * Handles the event of the user requesting to save the image annotations.
     */
    public void onRegisterSaveAnnotationsAction(ImageAnnotationSaveStrategy.Type saveFormat) {
        updateModelFromView();

        if(!model.containsAnnotations() && !view.containsBoundingShapeViews()) {
            dialogService.displayErrorAlert(SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE,
                    NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT, stage);
            return;
        }

        annotationIoController.exportAnnotations(saveFormat);
    }

    /**
     * Handles the event of the user requesting to save the current image-annotations.
     */
    public void onRegisterImportAnnotationsAction(ImageAnnotationLoadStrategy.Type loadFormat) {
        annotationIoController.importAnnotations(loadFormat);
    }

    /**
     * Handles the user's request to undo the last edit of the bounding shapes. While a shape is being drawn, the last
     * drawing step (e.g. a polygon vertex) is undone instead.
     */
    public void onRegisterUndoAction() {
        editHistoryController.undo();
    }

    /**
     * Handles the user's request to redo the last undone edit of the bounding shapes.
     */
    public void onRegisterRedoAction() {
        editHistoryController.redo();
    }

    /**
     * Handles the end of a user interaction that may have changed the bounding shapes, so the change can be undone.
     */
    public void onRegisterBoundingShapeEditFinished() {
        scheduleEditCheckpoint();
    }

    public void onRegisterModelNameFetchingAction() {
        final BoundingBoxPredictorClientConfig clientConfig = new BoundingBoxPredictorClientConfig();
        view.getInferenceSettingsView().applyDisplayedSettingsToPredictorClientConfig(clientConfig);
        inferenceController.fetchModelNames(clientConfig);
    }

    /**
     * Handles the user's request to check the connection to the inference server with the displayed settings.
     */
    public void onRegisterServerConnectionCheckAction() {
        final BoundingBoxPredictorClientConfig clientConfig = new BoundingBoxPredictorClientConfig();
        view.getInferenceSettingsView().applyDisplayedSettingsToPredictorClientConfig(clientConfig);
        inferenceController.checkConnection(clientConfig);
    }

    /**
     * Initiates the import of annotations.
     *
     * @param source the source of the annotations, either a folder or a single file
     */
    public void initiateAnnotationImport(File source, ImageAnnotationLoadStrategy.Type loadFormat) {
        annotationIoController.importAnnotations(source, loadFormat);
    }

    public void initiateBoundingBoxPrediction(File imageFile) {
        inferenceController.predict(imageFile);
    }

    /**
     * Handles the event of the user adding a new object category.
     */
    public void onRegisterAddObjectCategoryAction() {
        final String categoryName = view.getObjectCategoryInputField().getText();

        if(categoryName == null || categoryName.isBlank()) {
            dialogService.displayErrorAlert(CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                    INVALID_CATEGORY_NAME_ERROR_DIALOG_CONTENT, stage);
            view.getObjectCategoryInputField().clear();
            view.getEditorImagePane().requestFocus();
            return;
        }

        if(model.getCategoryToAssignedBoundingShapesCountMap().containsKey(categoryName)) {
            dialogService.displayErrorAlert(CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                    "The category \"" + categoryName + "\" already exists.", stage);
            view.getObjectCategoryInputField().clear();
            view.getEditorImagePane().requestFocus();
            return;
        }

        final Color categoryColor = view.getObjectCategoryColorPicker().getValue();
        model.getObjectCategories().add(new ObjectCategory(categoryName, categoryColor));

        view.getObjectCategoryTable().getSelectionModel().selectLast();
        view.getObjectCategoryTable().scrollTo(view.getObjectCategoryTable()
                .getSelectionModel()
                .getSelectedIndex()
        );

        view.getObjectCategoryInputField().clear();
        view.getObjectCategoryColorPicker().setValue(ColorUtils.createRandomColor());
        view.getEditorImagePane().requestFocus();
    }

    /**
     * Handles the event of the user requesting to exit the application.
     */
    public void onRegisterExitAction() {
        view.getEditorImagePane().finalizeBoundingShapeDrawing();

        updateModelFromView();

        if(!model.isSaved()) {
            ButtonBar.ButtonData answer =
                    dialogService.displayYesNoCancelDialogAndGetResult(EXIT_APPLICATION_OPTION_DIALOG_TITLE,
                            EXIT_APPLICATION_OPTION_DIALOG_CONTENT, stage);

            if(answer == ButtonBar.ButtonData.YES) {
                annotationIoController.saveWithFormatChoiceAndRunOnSaveSuccess(() -> {
                    savePreferences();
                    imageFolderController.stopWatching();
                    Platform.exit();
                });

                return;
            } else if(answer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return;
            }
        }

        savePreferences();
        imageFolderController.stopWatching();
        makeClientUnavailable();
        Platform.exit();
    }

    /**
     * Handles the event of the user pressing a defined keyboard short-cut.
     *
     * @param event the short-cut key-event
     */
    public void onRegisterSceneKeyPressed(KeyEvent event) {
        keyboardShortcutHandler.onKeyPressed(event);
    }

    /**
     * Handles the event of the user releasing a keyboard short-cut.
     *
     * @param event the short-cut key-event
     */
    public void onRegisterSceneKeyReleased(KeyEvent event) {
        keyboardShortcutHandler.onKeyReleased(event);
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
     * Handles the event of the user committing an object category name edit. Names of categories are allowed
     * to be changed by the user as long as the uniqueness of category-names is not violated, otherwise an error dialog
     * will be displayed and the edit will be reverted.
     *
     * @param event the edit event
     * @see com.github.mfl28.boundingboxeditor.ui.ObjectCategoryTableView
     */
    public void onSelectorCellEditEvent(TableColumn.CellEditEvent<ObjectCategory, String> event) {
        String newName = event.getNewValue();
        String oldName = event.getOldValue();

        if(Objects.equals(oldName, newName)) {
            // Nothing to do if the new name is the same as the current one.
            return;
        }

        final ObjectCategory objectCategory = event.getRowValue();
        final Map<String, Integer> boundingShapesPerCategoryNameMap =
                model.getCategoryToAssignedBoundingShapesCountMap();

        if(newName == null || newName.isBlank()) {
            dialogService.displayErrorAlert(Controller.CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                    INVALID_CATEGORY_NAME_ERROR_DIALOG_CONTENT, stage);
            objectCategory.setName(oldName);
            event.getTableView().refresh();
        } else if(boundingShapesPerCategoryNameMap.containsKey(newName)) {
            dialogService.displayErrorAlert(Controller.CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                    "The category \"" + newName + "\" already exists.", stage);
            objectCategory.setName(oldName);
            event.getTableView().refresh();
        } else {
            if(oldName != null) {
                int assignedBoundingShapesCount = boundingShapesPerCategoryNameMap.get(oldName);
                boundingShapesPerCategoryNameMap.remove(oldName);
                boundingShapesPerCategoryNameMap.put(newName, assignedBoundingShapesCount);
            } else {
                boundingShapesPerCategoryNameMap.put(newName, 0);
            }

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
            if(event.isShortcutDown()) {
                view.getEditorImageView().setCursor(Cursor.OPEN_HAND);
            }

            if(view.getObjectCategoryTable().isCategorySelected() &&
                    (Objects.equals(imagePane.getCurrentBoundingShapeDrawingMode(), EditorImagePaneView.DrawingMode.BOX) ||
                Objects.equals(imagePane.getCurrentBoundingShapeDrawingMode(), EditorImagePaneView.DrawingMode.FREEHAND))) {
                imagePane.finalizeBoundingShapeDrawing();
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
                && !event.isShortcutDown()
                && imagePaneView.isCategorySelected()) {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                if(!imagePaneView.isDrawingInProgress()) {
                    imagePaneView.initializeBoundingShapeDrawing(event);
                } else {
                    imagePaneView.updateBoundingShapeDrawing(event);
                }
            } else if(event.getButton().equals(MouseButton.SECONDARY)
                    && Objects.equals(imagePaneView.getCurrentBoundingShapeDrawingMode(),
                        EditorImagePaneView.DrawingMode.POLYGON)) {
                imagePaneView.finalizeBoundingShapeDrawing();
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

    public void onRegisterSettingsCancelCloseAction() {
        view.getInferenceSettingsView()
                .setDisplayedSettingsFromPredictorClientConfig(model.getBoundingBoxPredictorClientConfig());
        view.getInferenceSettingsView()
                .setDisplayedSettingsFromPredictorConfig(model.getBoundingBoxPredictorConfig());
        view.getInferenceSettingsView().setAllFieldsValid();
    }

    public void onRegisterDocumentationAction() {
        hostServices.showDocument(GITHUB_WIKI_URL);
    }

    public void onRegisterAboutAction() {
        dialogService.displayTextInfoDialog(
                "About " + PROGRAM_NAME,
                PROGRAM_NAME,
                "Version: " + PROGRAM_VERSION +
                        "\nLicense: " + PROGRAM_LICENSE,
                stage);
    }

    void makeClientAvailable() {
        inferenceController.makeClientAvailable();
    }

    void makeClientUnavailable() {
        inferenceController.makeClientUnavailable();
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

    BoundingBoxPredictorService getBoundingBoxPredictorService() {
        return boundingBoxPredictorService;
    }

    EditHistoryController getEditHistoryController() {
        return editHistoryController;
    }

    ModelNameFetchService getModelNameFetchService() {
        return modelNameFetchService;
    }

    ServerConnectionCheckService getServerConnectionCheckService() {
        return serverConnectionCheckService;
    }

    Stage getStage() {
        return stage;
    }

    void initiateAnnotationExport(File destination,
                                  ImageAnnotationSaveStrategy.Type exportFormat,
                                  Runnable chainedOperation) {
        annotationExportService.reset();
        annotationExportService.setDestination(destination);
        annotationExportService.setExportFormat(exportFormat);
        annotationExportService.setAnnotationData(model.createImageAnnotationData());
        annotationExportService.setChainedOperation(chainedOperation);
        annotationExportService.restart();
    }

    void initiateAnnotationExport(File destination, ImageAnnotationSaveStrategy.Type exportFormat) {
        initiateAnnotationExport(destination, exportFormat, null);
    }

    private void onBoundingBoxPredictionSucceeded(WorkerStateEvent event) {
        final BoundingBoxPredictionResult predictionResult = boundingBoxPredictorService.getValue();

        if(predictionResult.getNrSuccessfullyProcessedItems() != 0) {
            model.updateFromImageAnnotationData(predictionResult.getImageAnnotationData(),
                    predictionResult.getOperationType());
            view.getStatusBar().setStatusEvent(new BoundingBoxPredictionSuccessfulEvent(predictionResult));
        }

        updateViewFileExplorerFileInfoElements();

        reloadCurrentAnnotationInView();

        if(!predictionResult.getErrorTableEntries().isEmpty()) {
            boundingBoxPredictorService.getProgressViewer().hideProgress();
            dialogService.displayIOResultErrorInfoAlert(predictionResult, stage);
        }
    }

    private void reloadCurrentAnnotationInView() {
        final ImageAnnotation annotation = model.getCurrentImageAnnotation();

        if(annotation != null) {
            view.getObjectTree().reset();
            view.getCurrentBoundingShapes().removeListener(boundingShapeCountPerCategoryListener);
            view.loadBoundingShapeViewsFromAnnotation(annotation);
            view.getCurrentBoundingShapes().addListener(boundingShapeCountPerCategoryListener);
            view.getObjectCategoryTable().refresh();
            view.getObjectTree().refresh();
        }
    }

    private void startAnnotationImportService(File source, ImageAnnotationLoadStrategy.Type importFormat) {
        annotationImportService.reset();
        annotationImportService.setSource(source);
        annotationImportService.setImportFormat(importFormat);
        annotationImportService.setImportableFileNames(model.getImageFileNameSet());
        annotationImportService.setCategoryNameToCategoryMap(model.getCategoryNameToCategoryMap());
        annotationImportService.restart();
    }

    private void startImageMetaDataLoadingService(File source, List<File> imageFiles, boolean reload) {
        imageMetaDataLoadingService.reset();
        imageMetaDataLoadingService.setSource(source);
        imageMetaDataLoadingService.setImageFiles(imageFiles);
        imageMetaDataLoadingService.setReload(reload);
        imageMetaDataLoadingService.restart();
    }

    private void setUpServices() {
        setUpService(annotationExportService, SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE,
                SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER, this::onAnnotationExportSucceeded, false);
        setUpService(annotationImportService, LOADING_ANNOTATIONS_PROGRESS_DIALOG_TITLE,
                LOADING_ANNOTATIONS_PROGRESS_DIALOG_HEADER, this::onAnnotationImportSucceeded, false);
        setUpService(imageMetaDataLoadingService, IMAGE_FILES_LOADING_PROGRESS_DIALOG_TITLE,
                IMAGE_FILES_LOADING_PROGRESS_DIALOG_HEADER, this::onImageMetaDataLoadingSucceeded, false);
        setUpService(boundingBoxPredictorService, BOUNDING_BOX_PREDICTION_PROGRESS_DIALOG_TITLE,
                BOUNDING_BOX_PREDICTION_PROGRESS_DIALOG_HEADER, this::onBoundingBoxPredictionSucceeded, true);
        setUpService(modelNameFetchService, FETCHING_MODELS_PROGRESS_DIALOG_TITLE,
                FETCHING_MODELS_PROGRESS_DIALOG_HEADER, this::onModelNameFetchingSucceeded, true);
        setUpService(serverConnectionCheckService, CONNECTION_CHECK_PROGRESS_DIALOG_TITLE,
                CONNECTION_CHECK_PROGRESS_DIALOG_HEADER, this::onServerConnectionCheckSucceeded, true);
    }

    /**
     * Gives the service its progress dialog and its result handlers. Each service gets a single dialog that is reused
     * for every run: ControlsFX progress dialogs never detach from their worker, so creating one per run would leave
     * stale dialogs that reappear on every later run.
     */
    private void setUpService(IoService<?> service, String progressDialogTitle, String progressDialogHeader,
                              EventHandler<WorkerStateEvent> onSucceeded, boolean cancellable) {
        final ServiceProgressDialog progressDialog =
                dialogService.createServiceProgressDialog(service, progressDialogTitle, progressDialogHeader);
        progressDialog.setOwnerParentWindow(stage);

        if(cancellable) {
            progressDialog.enableCancellation();
        }

        service.setProgressViewer(progressDialog);
        service.setOnSucceeded(onSucceeded);
        service.setOnFailed(this::onIoServiceFailed);
    }

    private void onModelNameFetchingSucceeded(WorkerStateEvent event) {
        modelNameFetchService.getProgressViewer().hideProgress();
        inferenceController.onModelNamesFetched(modelNameFetchService.getValue(), view.getSettingsWindow().orElse(stage));
    }

    private void onServerConnectionCheckSucceeded(WorkerStateEvent event) {
        serverConnectionCheckService.getProgressViewer().hideProgress();
        inferenceController.onConnectionChecked(serverConnectionCheckService.getValue(),
                view.getSettingsWindow().orElse(stage));
    }

    private void onImageMetaDataLoadingSucceeded(WorkerStateEvent workerStateEvent) {
        imageFolderController.onImageMetaDataLoaded(imageMetaDataLoadingService.getValue(),
                imageMetaDataLoadingService.getSource(), imageMetaDataLoadingService.isReload());
    }

    private void onAnnotationImportSucceeded(WorkerStateEvent workerStateEvent) {
        ImageAnnotationImportResult importResult = annotationImportService.getValue();

        if(importResult.getNrSuccessfullyProcessedItems() != 0) {
            model.updateFromImageAnnotationData(importResult.getImageAnnotationData(), importResult.getOperationType());
            view.getStatusBar().setStatusEvent(new ImageAnnotationsImportingSuccessfulEvent(importResult));
        }

        updateViewFileExplorerFileInfoElements();

        reloadCurrentAnnotationInView();
        restartEditHistory();

        if(!importResult.getErrorTableEntries().isEmpty()) {
            annotationImportService.getProgressViewer().hideProgress();
            dialogService.displayIOResultErrorInfoAlert(importResult, stage);
        } else if(importResult.getNrSuccessfullyProcessedItems() == 0) {
            annotationImportService.getProgressViewer().hideProgress();
            dialogService.displayErrorAlert(ANNOTATION_IMPORT_ERROR_TITLE,
                    ANNOTATION_IMPORT_ERROR_NO_VALID_FILES_CONTENT, stage);
            return;
        }

        annotationIoController.rememberLoadingDirectory(annotationImportService.getSource());
    }


    private void onIoServiceFailed(WorkerStateEvent event) {
        final Throwable exception = event.getSource().getException();

        if(exception != null) {
            dialogService.displayExceptionDialog(exception, stage);
        }
    }

    private void onAnnotationExportSucceeded(WorkerStateEvent event) {
        IOResult saveResult = annotationExportService.getValue();

        if(saveResult.getNrSuccessfullyProcessedItems() != 0) {
            view.getStatusBar().setStatusEvent(new ImageAnnotationsSavingSuccessfulEvent(saveResult));
        }

        if(!saveResult.getErrorTableEntries().isEmpty()) {
            annotationExportService.getProgressViewer().hideProgress();
            dialogService.displayIOResultErrorInfoAlert(saveResult, stage);
        } else {
            model.setSaved(true);
        }

        annotationIoController.rememberSavingDirectory(annotationExportService.getDestination());

        if(annotationExportService.getChainedOperation() != null) {
            annotationExportService.getChainedOperation().run();
        }
    }

    private void updateViewFileExplorerFileInfoElements() {
        final Map<String, ImageAnnotation> fileNameToAnnotationMap = model.getImageFileNameToAnnotationMap();

        for(ImageFileListView.FileInfo fileInfo : view.getImageFileListView().getItems()) {
            ImageAnnotation annotation = fileNameToAnnotationMap.get(fileInfo.getFileName());

            if(annotation != null && !annotation.getBoundingShapeData().isEmpty()) {
                fileInfo.setHasAssignedBoundingShapes(true);
            }
        }
    }

    private void clearModelAndViewAnnotationData() {
        editHistoryController.clear();
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

    private void setUpModelListeners() {
        view.getUndoMenuItem().disableProperty().bind(editHistoryController.undoAvailableProperty().not());
        view.getRedoMenuItem().disableProperty().bind(editHistoryController.redoAvailableProperty().not());
        view.getCurrentBoundingShapes().addListener((ListChangeListener<BoundingShapeViewable>) change ->
                scheduleEditCheckpoint());

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
                    dialogService.displayErrorAlert(CATEGORY_DELETION_ERROR_DIALOG_TITLE,
                            CATEGORY_DELETION_ERROR_DIALOG_CONTENT
                                    + "\nCurrently there " +
                                    (nrExistingBoundingShapes == 1 ? "is " : "are ") +
                                    nrExistingBoundingShapes
                                    + " object" +
                                    (nrExistingBoundingShapes == 1 ? " " : "s ") +
                                    "with the category \"" + category.getName() + "\".",
                            stage);
                } else {
                    cell.getTableView().getItems().remove(category);
                }
            });

            return cell;
        });

        view.getStatusBar().savedStatusProperty().bind(model.savedProperty());

        view.getEditor().getEditorToolBar().getPredictButton()
                .visibleProperty().bind(model.getBoundingBoxPredictorConfig().inferenceEnabledProperty());
    }

    private void updateViewImageFiles() {
        view.reset();

        EditorImagePaneView imagePane = view.getEditorImagePane();
        imagePane.removeAllCurrentBoundingShapes();
        view.getCurrentBoundingShapes().removeListener(boundingShapeCountPerCategoryListener);
        imagePane.getImageLoadingProgressIndicator().setVisible(true);
        view.getEditor().getEditorToolBar().getPredictButton().setDisable(true);

        updateViewImageFromModel();

        updateStageTitle();

        ObjectCategoryTableView objectCategoryTableView = view.getObjectCategoryTable();
        objectCategoryTableView.setItems(model.getObjectCategories());
        objectCategoryTableView.getSelectionModel().selectFirst();

        ImageFileExplorerView imageFileExplorerView = view.getImageFileExplorer();
        imageFileExplorerView.setImageMetaData(model.getImageMetaDataList());

        ImageFileListView imageFileListView = view.getImageFileListView();
        imageFileListView.getSelectionModel().selectFirst();
        imageFileListView.scrollTo(0);
    }

    private void updateStageTitle() {
        ImageMetaData currentImageMetaData = model.getCurrentImageMetaData();
        stage.setTitle(PROGRAM_IDENTIFIER + PROGRAM_NAME_EXTENSION_SEPARATOR
                + model.getCurrentImageFilePath() + " " + currentImageMetaData.getDimensionsString());
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ChangeListener<Number> createImageLoadingProgressListener() {
        return (observable, oldValue, newValue) -> {
            if(newValue.intValue() == 1) {
                ImageAnnotation annotation = model.getCurrentImageAnnotation();
                ImageMetaData imageMetaData = model.getCurrentImageMetaData();

                if(imageMetaData.getOrientation() != 1) {
                    view.getCurrentImage().progressProperty().removeListener(imageLoadProgressListener);
                    view.getEditorImagePane().updateImage(ImageUtils.reorientImage(view.getCurrentImage(), imageMetaData.getOrientation()),
                            imageMetaData.getFileUrl());
                }

                // Hide the progress spinner.
                view.getEditorImagePane().getImageLoadingProgressIndicator().setVisible(false);
                view.getEditor().getEditorToolBar().getPredictButton().setDisable(false);

                if(annotation != null) {
                    view.loadBoundingShapeViewsFromAnnotation(annotation);
                }

                view.getCurrentBoundingShapes().addListener(boundingShapeCountPerCategoryListener);
                editHistoryController.onImageShown(model.getCurrentImageFile());
            }
        };
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ChangeListener<Number> createSelectedFileIndexListener() {
        return (value, oldValue, newValue) -> {
            // Records pending edits of the old image while its shapes are still shown.
            editHistoryController.onImageHidden();
            // Update selected item in image-file-list-view.
            view.getImageFileExplorer().getImageFileListView().getSelectionModel().select(newValue.intValue());
            // Show the progress spinner.
            view.getEditorImagePane().getImageLoadingProgressIndicator().setVisible(true);
            view.getEditor().getEditorToolBar().getPredictButton().setDisable(true);

            final Image oldImage = view.getCurrentImage();
            String oldImageUrl = model.getImageFiles().get(oldValue.intValue()).getName();

            if(oldImage != null && !oldImageUrl.equals(lastLoadedImageUrl)) {
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
                lastLoadedImageUrl = oldImageUrl;
            }

            updateStageTitle();

            final ReadOnlyBooleanProperty navigateNextKeyPressed = keyboardShortcutHandler.navigateNextKeyPressedProperty();
            final ReadOnlyBooleanProperty navigatePreviousKeyPressed =
                    keyboardShortcutHandler.navigatePreviousKeyPressedProperty();

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
            dialogService.displayErrorAlert(IMAGE_LOADING_ERROR_DIALOG_TITLE,
                    "Could not read meta-data from image file \"" + model.getCurrentImageFileName() +
                            "\".", stage);
            return;
        }

        view.updateImageFromMetaData(metaData);
        view.getCurrentImage().progressProperty().addListener(imageLoadProgressListener);
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ListChangeListener<BoundingShapeViewable> createBoundingShapeCountPerCategoryListener() {
        return change -> {
            Map<String, Integer> categoryToShapesCountMap = model.getCategoryToAssignedBoundingShapesCountMap();

            while(change.next()) {
                if(change.wasAdded()) {
                    change.getAddedSubList().forEach(item -> categoryToShapesCountMap
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
        stage.setMaximized(preferencesStore.loadWindowMaximized());
        preferencesStore.loadDirectories(ioMetaData);
    }

    private void savePreferences() {
        preferencesStore.save(stage.isMaximized(), ioMetaData);
    }

    private void clearViewAndModel() {
        editHistoryController.clear();
        model.fileIndexProperty().removeListener(selectedFileIndexListener);
        model.clear();

        view.reset();

        EditorImagePaneView imagePane = view.getEditorImagePane();
        imagePane.removeAllCurrentBoundingShapes();
        view.getCurrentBoundingShapes().removeListener(boundingShapeCountPerCategoryListener);

        stage.setTitle(PROGRAM_IDENTIFIER);

        ObjectCategoryTableView objectCategoryTableView = view.getObjectCategoryTable();
        objectCategoryTableView.getItems().clear();
        objectCategoryTableView.getSelectionModel().clearSelection();

        ImageFileListView imageFileListView = view.getImageFileListView();
        imageFileListView.setItems(null);
        imageFileListView.getSelectionModel().clearSelection();

        view.getStatusBar().clear();
        view.setWorkspaceVisible(false);
    }

    private void setupStage() {
        stage.setTitle(PROGRAM_IDENTIFIER);
        stage.getIcons().add(MainView.APPLICATION_ICON);
        stage.setOnCloseRequest(event -> {
            onRegisterExitAction();
            event.consume();
        });

        stage.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getTarget() != view.getEditorImageView()) {
                view.getEditorImagePane().finalizeBoundingShapeDrawing();
            }
        });

        // Edits of the bounding shapes end with one of these events; checkpoints that find no change are ignored.
        stage.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> scheduleEditCheckpoint());
        stage.addEventFilter(KeyEvent.KEY_RELEASED, event -> scheduleEditCheckpoint());
        stage.addEventFilter(DragEvent.DRAG_DROPPED, event -> scheduleEditCheckpoint());
    }

    /**
     * Records the bounding shapes in the edit history once the current event has been handled completely.
     */
    private void scheduleEditCheckpoint() {
        if(!editCheckpointScheduled) {
            editCheckpointScheduled = true;
            Platform.runLater(() -> {
                editCheckpointScheduled = false;
                editHistoryController.checkpoint();
            });
        }
    }

    /**
     * Discards all edit histories and starts a new one for the shown image, e.g. after annotations were imported.
     */
    private void restartEditHistory() {
        editHistoryController.clear();

        if(model.containsImageFiles() && view.getEditorImagePane().isImageFullyLoaded()) {
            editHistoryController.onImageShown(model.getCurrentImageFile());
        }
    }

    /**
     * Runs the annotation imports and exports initiated by the {@link AnnotationIoController}.
     */
    private class AnnotationIoOperations implements AnnotationIoController.Operations {
        @Override
        public void updateModelFromView() {
            Controller.this.updateModelFromView();
        }

        @Override
        public void clearAnnotationData() {
            clearModelAndViewAnnotationData();
        }

        @Override
        public void startImport(File source, ImageAnnotationLoadStrategy.Type format) {
            startAnnotationImportService(source, format);
        }

        @Override
        public void startExport(File destination, ImageAnnotationSaveStrategy.Type format, Runnable chainedOperation) {
            initiateAnnotationExport(destination, format, chainedOperation);
        }
    }

    /**
     * Loads and shows the image folders opened by the {@link ImageFolderController}.
     */
    private class ImageFolderOperations implements ImageFolderController.Operations {
        @Override
        public void updateModelFromView() {
            Controller.this.updateModelFromView();
        }

        @Override
        public void startImageMetaDataLoading(File folder, List<File> imageFiles, boolean reload) {
            lastLoadedImageUrl = null;
            startImageMetaDataLoadingService(folder, imageFiles, reload);
        }

        @Override
        public void hideImageMetaDataLoadingProgress() {
            imageMetaDataLoadingService.getProgressViewer().hideProgress();
        }

        @Override
        public void showLoadedImageFiles(ImageMetaDataLoadingResult result, File folder, boolean keepCategories) {
            editHistoryController.clear();
            model.clearAnnotationData(keepCategories);
            model.getImageFileNameToMetaDataMap().clear();
            model.getImageFileNameToMetaDataMap().putAll(result.getFileNameToMetaDataMap());

            model.fileIndexProperty().removeListener(selectedFileIndexListener);
            model.setImageFiles(result.getValidFiles());
            model.fileIndexProperty().addListener(selectedFileIndexListener);

            updateViewImageFiles();

            view.getStatusBar().setStatusEvent(new ImageFilesLoadingSuccessfulEvent(result, folder));
        }

        @Override
        public void clearWorkspace() {
            clearViewAndModel();
        }
    }

    /**
     * Runs the model fetching, connection checks and predictions initiated by the {@link InferenceController}.
     */
    private class InferenceOperations implements InferenceController.Operations {
        @Override
        public void updateModelFromView() {
            Controller.this.updateModelFromView();
        }

        @Override
        public void startModelNameFetching(BoundingBoxPredictorClient predictorClient) {
            modelNameFetchService.reset();
            modelNameFetchService.setClient(predictorClient);
            modelNameFetchService.restart();
        }

        @Override
        public void startPrediction(File imageFile, BoundingBoxPredictorClient predictorClient) {
            boundingBoxPredictorService.reset();
            boundingBoxPredictorService.setImageFile(imageFile);
            boundingBoxPredictorService.setCategoryNameToCategoryMap(model.getCategoryNameToCategoryMap());
            boundingBoxPredictorService.setImageMetaData(
                    model.getImageFileNameToMetaDataMap().get(imageFile.getName()));
            boundingBoxPredictorService.setBoundingBoxPredictorConfig(model.getBoundingBoxPredictorConfig());
            boundingBoxPredictorService.setPredictorClient(predictorClient);
            boundingBoxPredictorService.restart();
        }

        @Override
        public void startConnectionCheck(BoundingBoxPredictorClient predictorClient) {
            serverConnectionCheckService.reset();
            serverConnectionCheckService.setClient(predictorClient);
            serverConnectionCheckService.restart();
        }

        @Override
        public void showSelectedModel(String modelName) {
            view.getInferenceSettingsView().getSelectedModelLabel().setText(modelName);
        }
    }

    /**
     * The shown bounding shapes as seen by the {@link EditHistoryController}.
     */
    private class EditHistoryOperations implements EditHistoryController.Operations {
        @Override
        public boolean isEditingPossible() {
            return model.containsImageFiles() && view.getEditorImagePane().isImageFullyLoaded()
                    && !view.getEditorImagePane().isDrawingInProgress();
        }

        @Override
        public boolean isDrawingInProgress() {
            return view.getEditorImagePane().isDrawingInProgress();
        }

        @Override
        public void undoDrawingStep() {
            view.undoBoundingShapeDrawingStep();
        }

        @Override
        public List<BoundingShapeData> extractShapes() {
            return view.extractCurrentBoundingShapeData();
        }

        @Override
        public void restoreShapes(List<BoundingShapeData> shapes) {
            // A category may have been deleted after its last shape was, and an undo brings the shape back.
            shapes.stream()
                  .flatMap(BoundingShapeData::flatten)
                  .map(BoundingShapeData::getCategory)
                  .distinct()
                  .filter(category -> !model.getCategoryNameToCategoryMap().containsKey(category.getName()))
                  .forEach(category -> model.getObjectCategories().add(category));

            view.getEditorImagePane().removeAllCurrentBoundingShapes();
            view.getObjectTree().reset();
            view.loadBoundingShapeViewsFromAnnotation(
                    new ImageAnnotation(model.getCurrentImageMetaData(), new ArrayList<>(shapes)));
            view.getObjectCategoryTable().refresh();
            view.getObjectTree().refresh();
        }

        @Override
        public void selectShape(List<Integer> path) {
            view.getObjectTree().selectBoundingShapeTreeItem(path);
        }
    }

    /**
     * The image editor as seen by the {@link KeyboardShortcutHandler}.
     */
    private class KeyboardShortcutEditor implements KeyboardShortcutHandler.Editor {
        @Override
        public boolean isDrawingInProgress() {
            return view.getEditorImagePane().isDrawingInProgress();
        }

        @Override
        public void setZoomableAndPannable(boolean zoomableAndPannable) {
            view.getEditorImagePane().setZoomableAndPannable(zoomableAndPannable);
        }

        @Override
        public void showNextImage() {
            onRegisterNextImageFileRequested();
        }

        @Override
        public void showPreviousImage() {
            onRegisterPreviousImageFileRequested();
        }
    }
}
