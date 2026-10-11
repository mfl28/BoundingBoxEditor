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
import com.github.mfl28.boundingboxeditor.model.ImageFileFilter;
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
import com.github.mfl28.boundingboxeditor.ui.statusevents.AutosaveFailedEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.BoundingBoxPredictionSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.BoundingShapeClipboardEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.ImageFilesLoadingSuccessfulEvent;
import com.github.mfl28.boundingboxeditor.ui.statusevents.StatusEvent;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import com.github.mfl28.boundingboxeditor.utils.ImageUtils;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

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
    private static final String PROGRAM_VERSION = "3.3.0";
    private static final String PROGRAM_LICENSE = "GPL-3.0";
    private static final String PROGRAM_IDENTIFIER = PROGRAM_NAME + " " + PROGRAM_VERSION;

    private static final String SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE = "Save Error";
    private static final String RECENT_IMAGE_FOLDER_NOT_FOUND_ERROR_DIALOG_TITLE = "Image Folder Not Found";
    private static final String NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT =
            "There are no image annotations to save.";
    private static final String EXIT_APPLICATION_OPTION_DIALOG_TITLE = "Exit Application";
    private static final String EXIT_APPLICATION_OPTION_DIALOG_CONTENT =
            "Do you want to save the existing annotation data?";
    private static final String SETTINGS_APPLICATION_ERROR_DIALOG_TITLE = "Settings Application Error";
    private static final String SETTINGS_APPLICATION_INVALID_FIELDS_ERROR_DIALOG_CONTENT =
            "Please provide valid values for the indicated fields.";
    private static final String SETTINGS_APPLICATION_NO_MODEL_SELECTED_ERROR_DIALOG_CONTENT =
            "Please select a model or disable inference.";
    private static final String IMAGE_LOADING_ERROR_DIALOG_TITLE = "Image Loading Error";

    private final Stage stage;
    private final HostServices hostServices;
    private final MainView view;
    private final DialogService dialogService;
    private final CategoryController categoryController;
    private final IoServices ioServices;
    private final AnnotationIoController annotationIoController;
    private final ImageFolderController imageFolderController;
    private final InferenceController inferenceController;
    private final KeyboardShortcutHandler keyboardShortcutHandler;
    private final Model model = new Model();
    private final ImageFileFilterController imageFileFilterController = new ImageFileFilterController(model);
    private boolean imageFilterUpdating = false;
    private final EditHistoryController editHistoryController =
            new EditHistoryController(new EditHistoryOperations());
    private final ShapeClipboardController shapeClipboardController =
            new ShapeClipboardController(new ShapeClipboardOperations());
    private boolean editCheckpointScheduled = false;
    private final ListChangeListener<BoundingShapeViewable> boundingShapeCountPerCategoryListener =
            createBoundingShapeCountPerCategoryListener();
    private final ChangeListener<Number> imageLoadProgressListener = createImageLoadingProgressListener();
    private final ChangeListener<Boolean> imageNavigationKeyPressedListener = createImageNavigationKeyPressedListener();
    private final IoMetaData ioMetaData = new IoMetaData();
    private final PreferencesStore preferencesStore = PreferencesStore.forApplication();
    private final RecentImageFolders recentImageFolders = new RecentImageFolders(preferencesStore);
    private final AutosaveController autosaveController;
    // Removes the handlers that this controller added to its window.
    private final List<Runnable> stageListenerRemovals = new ArrayList<>();
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
        this.categoryController = new CategoryController(model, dialogService, stage);
        this.ioServices = new IoServices(dialogService, stage, new ServiceResultHandlers());
        this.keyboardShortcutHandler = new KeyboardShortcutHandler(model, new KeyboardShortcutEditor(),
                KeyboardShortcutHandler.createViewActionShortcuts(view, this::onRegisterSettingsAction,
                        this::onRegisterUndoAction, this::onRegisterRedoAction, shapeClipboardController::copySelectedShape,
                        shapeClipboardController::paste));
        this.keyCombinationHandlers = keyboardShortcutHandler.getKeyCombinationHandlers();

        setupStage();
        loadPreferences();
        view.connectToController(this);
        setUpModelListeners();
        autosaveController = AutosaveController.create(model, dialogService, stage, recentImageFolders,
                new AutosaveOperations());
        // After the window was shown.
        Platform.runLater(autosaveController::offerToRestoreLastSession);
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
        preferencesStore.saveInferenceSettings(model.getBoundingBoxPredictorClientConfig(),
                model.getBoundingBoxPredictorConfig());

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

    /**
     * Handles the event of the user requesting to open a recently opened image folder.
     *
     * @param folder the folder
     */
    public void onRegisterOpenRecentImageFolderAction(File folder) {
        if(!folder.isDirectory()) {
            recentImageFolders.remove(folder);
            dialogService.displayErrorAlert(RECENT_IMAGE_FOLDER_NOT_FOUND_ERROR_DIALOG_TITLE,
                    String.format("The folder \"%s\" doesn't exist anymore. It was removed from the recent folders.",
                                  folder.getPath()), stage);
            return;
        }

        imageFolderController.openImageFolder(folder);
    }

    /**
     * Handles the event of the user requesting to clear the list of recently opened image folders.
     */
    public void onRegisterClearRecentImageFoldersAction() {
        recentImageFolders.clear();
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

        if(categoryController.addCategory(categoryName, view.getObjectCategoryColorPicker().getValue())) {
            view.getObjectCategoryTable().getSelectionModel().selectLast();
            view.getObjectCategoryTable().scrollTo(view.getObjectCategoryTable()
                    .getSelectionModel()
                    .getSelectedIndex()
            );
            view.getObjectCategoryColorPicker().setValue(ColorUtils.createRandomColor());
        }

        view.getObjectCategoryInputField().clear();
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

            // The user decided not to keep the annotations.
            autosaveController.discard();
        }

        autosaveController.stop();
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
        imageFileFilterController.nextItem().ifPresent(this::showImageFileItem);
    }

    /**
     * Handles the event of the user clicking the previous(-image)-button.
     */
    public void onRegisterPreviousImageFileRequested() {
        imageFileFilterController.previousItem().ifPresent(this::showImageFileItem);
    }

    /**
     * Handles the event of the user changing the image file filter (search text, annotation status or categories).
     * If the current image does not match the new filter, the first matching image is shown.
     *
     * @param filter the new filter
     */
    public void onRegisterImageFilterChanged(ImageFileFilter filter) {
        if(!model.containsImageFiles()) {
            return;
        }

        saveCurrentShapesIfImageLoaded();
        updateImageFilter(() -> imageFileFilterController.setFilter(filter))
                .ifPresent(this::showImageFileItem);
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
        final String oldName = event.getOldValue();

        if(!categoryController.renameCategory(event.getRowValue(), oldName, event.getNewValue())) {
            event.getTableView().refresh();
        } else if(oldName != null && !Objects.equals(oldName, event.getNewValue())
                && imageFileFilterController.getFilter().categoryNames().contains(oldName)) {
            onRegisterImageFilterChanged(view.getImageFileExplorer().getFilter());
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

            final EditorImagePaneView.DrawingMode drawingMode = imagePane.getCurrentBoundingShapeDrawingMode();

            if(view.getObjectCategoryTable().isCategorySelected() &&
                    (drawingMode == EditorImagePaneView.DrawingMode.BOX
                            || drawingMode == EditorImagePaneView.DrawingMode.FREEHAND
                            || drawingMode == EditorImagePaneView.DrawingMode.MASK)) {
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
        return ioServices.getAnnotationExportService();
    }

    ImageAnnotationImportService getAnnotationImportService() {
        return ioServices.getAnnotationImportService();
    }

    ImageMetaDataLoadingService getImageMetaDataLoadingService() {
        return ioServices.getImageMetaDataLoadingService();
    }

    BoundingBoxPredictorService getBoundingBoxPredictorService() {
        return ioServices.getBoundingBoxPredictorService();
    }

    EditHistoryController getEditHistoryController() {
        return editHistoryController;
    }

    ModelNameFetchService getModelNameFetchService() {
        return ioServices.getModelNameFetchService();
    }

    ServerConnectionCheckService getServerConnectionCheckService() {
        return ioServices.getServerConnectionCheckService();
    }

    Stage getStage() {
        return stage;
    }

    void initiateAnnotationExport(File destination,
                                  ImageAnnotationSaveStrategy.Type exportFormat,
                                  Runnable chainedOperation) {
        ioServices.startExport(destination, exportFormat, model.createImageAnnotationData(), chainedOperation);
    }

    void initiateAnnotationExport(File destination, ImageAnnotationSaveStrategy.Type exportFormat) {
        initiateAnnotationExport(destination, exportFormat, null);
    }

    private void onBoundingBoxPredictionSucceeded() {
        final BoundingBoxPredictionResult predictionResult = ioServices.getBoundingBoxPredictorService().getValue();

        if(predictionResult.getNrSuccessfullyProcessedItems() != 0) {
            model.updateFromImageAnnotationData(predictionResult.getImageAnnotationData(),
                    predictionResult.getOperationType());
            view.getStatusBar().setStatusEvent(new BoundingBoxPredictionSuccessfulEvent(predictionResult));
        }

        updateViewFileExplorerFileInfoElements();

        reloadCurrentAnnotationInView();

        if(!predictionResult.getErrorTableEntries().isEmpty()) {
            ioServices.getBoundingBoxPredictorService().getProgressViewer().hideProgress();
            dialogService.displayIOResultErrorInfoAlert(predictionResult, stage);
        }
    }

    private void reloadCurrentAnnotationInView() {
        final ImageAnnotation annotation = model.getCurrentImageAnnotation();

        // While the image is still loading, its load listener shows the shapes once its size is known.
        if(annotation != null && view.getEditorImagePane().isImageFullyLoaded()) {
            view.getObjectTree().reset();
            view.getCurrentBoundingShapes().removeListener(boundingShapeCountPerCategoryListener);
            view.loadBoundingShapeViewsFromAnnotation(annotation);
            view.getCurrentBoundingShapes().addListener(boundingShapeCountPerCategoryListener);
            view.getObjectCategoryTable().refresh();
            view.getObjectTree().refresh();
        }
    }

    private void startAnnotationImportService(File source, ImageAnnotationLoadStrategy.Type importFormat) {
        ioServices.startImport(source, importFormat, model.getImageFileNameSet(), model.getCategoryNameToCategoryMap());
    }

    private void onModelNameFetchingSucceeded() {
        ioServices.getModelNameFetchService().getProgressViewer().hideProgress();
        inferenceController.onModelNamesFetched(ioServices.getModelNameFetchService().getValue(), view.getSettingsWindow().orElse(stage));
    }

    private void onServerConnectionCheckSucceeded() {
        ioServices.getServerConnectionCheckService().getProgressViewer().hideProgress();
        inferenceController.onConnectionChecked(ioServices.getServerConnectionCheckService().getValue(),
                view.getSettingsWindow().orElse(stage));
    }

    private void onImageMetaDataLoadingSucceeded() {
        imageFolderController.onImageMetaDataLoaded(ioServices.getImageMetaDataLoadingService().getValue(),
                ioServices.getImageMetaDataLoadingService().getSource(), ioServices.getImageMetaDataLoadingService().isReload());
    }

    private void onAnnotationImportSucceeded() {
        final ImageAnnotationImportService service = ioServices.getAnnotationImportService();
        annotationIoController.onImportFinished(service.getValue(), service.getSource(), service.getProgressViewer());
    }


    private void onIoServiceFailed(IoService<?> service) {
        if(ioServices.getAnnotationImportService().equals(service) && autosaveController.isRestoring()) {
            autosaveController.onRestoreFailed();
        }
    }

    private void onAnnotationExportSucceeded() {
        final ImageAnnotationExportService service = ioServices.getAnnotationExportService();
        annotationIoController.onExportFinished(service.getValue(), service.getDestination(),
                service.getChainedOperation(), service.getProgressViewer());
    }

    private void updateViewFileExplorerFileInfoElements() {
        final Map<String, ImageAnnotation> fileNameToAnnotationMap = model.getImageFileNameToAnnotationMap();

        for(ImageFileListView.FileInfo fileInfo : imageFileFilterController.getAllItems()) {
            ImageAnnotation annotation = fileNameToAnnotationMap.get(fileInfo.getFileName());

            if(annotation != null && !annotation.getBoundingShapeData().isEmpty()) {
                fileInfo.setHasAssignedBoundingShapes(true);
            }
        }

        refreshImageFilter();
    }

    private void showImageFileItem(ImageFileListView.FileInfo item) {
        final ImageFileListView imageFileListView = view.getImageFileListView();
        imageFileListView.getSelectionModel().select(item);
        // Keep the currently selected item in the image-gallery in view.
        imageFileListView.scrollTo(item);
    }

    /**
     * Runs a change of the image file filter. While the shown list is re-filtered, the list view's selection may
     * change temporarily; those changes must not navigate, and the current image is re-selected afterwards.
     */
    private <T> T updateImageFilter(Supplier<T> update) {
        final T result;
        imageFilterUpdating = true;

        try {
            result = update.get();
        } finally {
            imageFilterUpdating = false;
        }

        imageFileFilterController.getCurrentItem().ifPresent(item -> {
            if(!Objects.equals(view.getImageFileListView().getSelectionModel().getSelectedItem(), item)) {
                view.getImageFileListView().getSelectionModel().select(item);
            }
        });

        return result;
    }

    private void refreshImageFilter() {
        updateImageFilter(() -> {
            imageFileFilterController.refresh();
            return null;
        });
    }

    private void saveCurrentShapesIfImageLoaded() {
        final Image currentImage = view.getCurrentImage();

        if(currentImage != null && currentImage.getProgress() == 1.0) {
            updateModelFromView();
        }
    }

    private void updateModelFromView() {
        if(model.containsImageFiles()) {
            model.updateCurrentBoundingShapeData(view.extractCurrentBoundingShapeData());
        }
    }

    private void setUpModelListeners() {
        view.setRecentImageFolders(recentImageFolders.getFolders());
        view.getUndoMenuItem().disableProperty().bind(editHistoryController.undoAvailableProperty().not());
        view.getRedoMenuItem().disableProperty().bind(editHistoryController.redoAvailableProperty().not());
        view.getCurrentBoundingShapes().addListener((ListChangeListener<BoundingShapeViewable>) change ->
                scheduleEditCheckpoint());

        view.getEditor().getEditorToolBar()
                .getIndexLabel()
                .textProperty()
                .bind(imageFileFilterController.currentPositionProperty().add(1).asString()
                        .concat(" | ")
                        .concat(imageFileFilterController.shownCountProperty().asString()));

        view.getImageFileExplorer().getImageFileListView().getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if(newValue != null && !imageFilterUpdating) {
                        model.fileIndexProperty().set(newValue.getFileIndex());
                    }
                });

        view.getImageFileExplorer().getImageFileFilterView().setCategories(model.getObjectCategories());
        view.getImageFileExplorer().getFilterMatchLabel().visibleProperty()
                .bind(imageFileFilterController.filterActiveProperty());
        view.getImageFileExplorer().getFilterMatchLabel().textProperty()
                .bind(Bindings.format("%d of %d images match", imageFileFilterController.matchCountProperty(),
                        model.nrImageFilesProperty()));

        view.getFileImportAnnotationsItem().disableProperty().bind(model.nrImageFilesProperty().isEqualTo(0));

        view.getPreviousImageNavigationButton().disableProperty()
                .bind(imageFileFilterController.hasPreviousProperty().not());
        view.getNextImageNavigationButton().disableProperty()
                .bind(imageFileFilterController.hasNextProperty().not());

        view.getObjectCategoryTable().getDeleteColumn().setCellFactory(column -> {
            final ObjectCategoryDeleteTableCell cell = new ObjectCategoryDeleteTableCell();

            cell.getDeleteButton().setOnAction(action -> categoryController.deleteCategory(cell.getItem()));

            return cell;
        });

        view.getStatusBar().savedStatusProperty().bind(model.savedProperty());

        view.getEditor().getEditorToolBar().getPredictButton()
                .visibleProperty().bind(model.getBoundingBoxPredictorConfig().inferenceEnabledProperty());
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
            imageFileFilterController.getItem(newValue.intValue())
                    .ifPresent(item -> view.getImageFileListView().getSelectionModel().select(item));
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

            // The old image's shapes are now stored in the model, so it can be hidden if it no longer matches.
            final int newFileIndex = newValue.intValue();
            updateImageFilter(() -> {
                imageFileFilterController.onCurrentImageChanged(newFileIndex);
                return null;
            });

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
        } catch(Exception _) {
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
            while(change.next()) {
                if(change.wasAdded()) {
                    change.getAddedSubList().forEach(item ->
                            categoryController.onShapeAdded(item.getViewData().getObjectCategory().getName()));
                }

                if(change.wasRemoved()) {
                    change.getRemoved().forEach(item ->
                            categoryController.onShapeRemoved(item.getViewData().getObjectCategory().getName()));
                }

                if(change.wasUpdated()) {
                    for(int i = change.getFrom(); i != change.getTo(); ++i) {
                        final BoundingShapeViewData changedShapeViewData = change.getList().get(i).getViewData();
                        categoryController.onShapeCategoryChanged(changedShapeViewData.getPreviousObjectCategoryName(),
                                changedShapeViewData.getObjectCategory().getName());
                    }
                }
            }
        };
    }

    private void loadPreferences() {
        stage.setMaximized(preferencesStore.loadWindowMaximized());
        preferencesStore.loadDirectories(ioMetaData);
        preferencesStore.loadInferenceSettings(model.getBoundingBoxPredictorClientConfig(),
                model.getBoundingBoxPredictorConfig());
        // Inference that was switched on in an earlier run needs its client right away.
        inferenceController.onInferenceSettingsApplied(false, model.getBoundingBoxPredictorConfig().isInferenceEnabled());
    }

    private void savePreferences() {
        preferencesStore.save(stage.isMaximized(), ioMetaData);
    }

    private void setupStage() {
        stage.setTitle(PROGRAM_IDENTIFIER);

        if(!stage.getIcons().contains(MainView.APPLICATION_ICON)) {
            stage.getIcons().add(MainView.APPLICATION_ICON);
        }

        final EventHandler<WindowEvent> closeRequestHandler = event -> {
            onRegisterExitAction();
            event.consume();
        };
        stage.setOnCloseRequest(closeRequestHandler);
        // Once the window is closed, nothing is saved anymore (also not into the next test's autosave folder), and
        // this controller lets go of the window (UI tests show each new controller in the same window).
        addStageEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
            autosaveController.stop();
            // After the event was handled, so that its handler isn't removed while it runs.
            Platform.runLater(() -> releaseStage(closeRequestHandler));
        });

        addStageEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!Objects.equals(event.getTarget(), view.getEditorImageView())) {
                view.getEditorImagePane().finalizeBoundingShapeDrawing();
            }
        });

        // Edits of the bounding shapes end with one of these events; checkpoints that find no change are ignored.
        addStageEventFilter(MouseEvent.MOUSE_RELEASED, event -> scheduleEditCheckpoint());
        addStageEventFilter(KeyEvent.KEY_RELEASED, event -> scheduleEditCheckpoint());
        addStageEventFilter(DragEvent.DRAG_DROPPED, event -> scheduleEditCheckpoint());
    }

    private <T extends Event> void addStageEventFilter(EventType<T> eventType, EventHandler<? super T> filter) {
        stage.addEventFilter(eventType, filter);
        stageListenerRemovals.add(() -> stage.removeEventFilter(eventType, filter));
    }

    private <T extends Event> void addStageEventHandler(EventType<T> eventType, EventHandler<? super T> handler) {
        stage.addEventHandler(eventType, handler);
        stageListenerRemovals.add(() -> stage.removeEventHandler(eventType, handler));
    }

    /**
     * Removes this controller's handlers from its window, so that the window doesn't keep it in memory.
     */
    private void releaseStage(EventHandler<WindowEvent> closeRequestHandler) {
        stageListenerRemovals.forEach(Runnable::run);
        stageListenerRemovals.clear();

        if(Objects.equals(stage.getOnCloseRequest(), closeRequestHandler)) {
            stage.setOnCloseRequest(null);
        }
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
                autosaveController.onEdited();
            });
        }
    }

    AutosaveController getAutosaveController() {
        return autosaveController;
    }

    RecentImageFolders getRecentImageFolders() {
        return recentImageFolders;
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
        public void showStatus(StatusEvent event) {
            view.getStatusBar().setStatusEvent(event);
        }

        @Override
        public boolean isRestoringAutosave() {
            return autosaveController.isRestoring();
        }

        @Override
        public void onAnnotationsImported(boolean anyImported) {
            if(autosaveController.isRestoring()) {
                autosaveController.onRestoreFinished(anyImported);
            }

            updateViewFileExplorerFileInfoElements();
            reloadCurrentAnnotationInView();
            restartEditHistory();
        }

        private void clearModelAndViewAnnotationData() {
            editHistoryController.clear();
            model.clearAnnotationData(false);
            view.reset();
            view.getEditorImagePane().removeAllCurrentBoundingShapes();
            // Reset all 'assigned bounding shape states' in image file explorer.
            imageFileFilterController.getAllItems().forEach(item -> item.setHasAssignedBoundingShapes(false));
            refreshImageFilter();
        }

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
        private void startImageMetaDataLoadingService(File source, List<File> imageFiles, boolean reload) {
            ioServices.getImageMetaDataLoadingService().reset();
            ioServices.getImageMetaDataLoadingService().setSource(source);
            ioServices.getImageMetaDataLoadingService().setImageFiles(imageFiles);
            ioServices.getImageMetaDataLoadingService().setReload(reload);
            ioServices.getImageMetaDataLoadingService().restart();
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
            imageFileExplorerView.resetFilter();

            ImageFileListView imageFileListView = view.getImageFileListView();
            imageFileListView.setItems(imageFileFilterController.setItems(
                    ImageFileExplorerView.createFileInfos(model.getImageMetaDataList()), model.getCurrentFileIndex()));
            imageFileListView.getSelectionModel().selectFirst();
            imageFileListView.scrollTo(0);
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
            imageFileFilterController.clear();
            view.getImageFileExplorer().resetFilter();

            view.getStatusBar().clear();
            view.setWorkspaceVisible(false);
        }

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
            ioServices.getImageMetaDataLoadingService().getProgressViewer().hideProgress();
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
            recentImageFolders.add(folder);

            view.getStatusBar().setStatusEvent(new ImageFilesLoadingSuccessfulEvent(result, folder));
            autosaveController.onFolderOpened(folder);
        }

        @Override
        public void clearWorkspace() {
            clearViewAndModel();
            autosaveController.onFolderClosed();
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
            ioServices.getModelNameFetchService().reset();
            ioServices.getModelNameFetchService().setClient(predictorClient);
            ioServices.getModelNameFetchService().restart();
        }

        @Override
        public void startPrediction(File imageFile, BoundingBoxPredictorClient predictorClient) {
            ioServices.getBoundingBoxPredictorService().reset();
            ioServices.getBoundingBoxPredictorService().setImageFile(imageFile);
            ioServices.getBoundingBoxPredictorService().setCategoryNameToCategoryMap(model.getCategoryNameToCategoryMap());
            ioServices.getBoundingBoxPredictorService().setImageMetaData(
                    model.getImageFileNameToMetaDataMap().get(imageFile.getName()));
            ioServices.getBoundingBoxPredictorService().setBoundingBoxPredictorConfig(model.getBoundingBoxPredictorConfig());
            ioServices.getBoundingBoxPredictorService().setPredictorClient(predictorClient);
            ioServices.getBoundingBoxPredictorService().restart();
        }

        @Override
        public void startConnectionCheck(BoundingBoxPredictorClient predictorClient) {
            ioServices.getServerConnectionCheckService().reset();
            ioServices.getServerConnectionCheckService().setClient(predictorClient);
            ioServices.getServerConnectionCheckService().restart();
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
     * The shapes as seen by the {@link ShapeClipboardController}: like undo and redo, it changes the shown image's
     * shapes by restoring snapshots.
     */
    private class ShapeClipboardOperations extends EditHistoryOperations
            implements ShapeClipboardController.Operations {
        @Override
        public Optional<BoundingShapeData> getSelectedShape() {
            return view.getObjectTree().getSelectedBoundingShapeData();
        }

        @Override
        public void onShapeCopied(BoundingShapeData shape) {
            view.getStatusBar().setStatusEvent(BoundingShapeClipboardEvent.copied(shape.getCategoryName()));
        }

        @Override
        public void onShapePasted(BoundingShapeData shape) {
            scheduleEditCheckpoint();
            view.getStatusBar().setStatusEvent(BoundingShapeClipboardEvent.pasted(shape.getCategoryName()));
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

        @Override
        public boolean hasNextImage() {
            return imageFileFilterController.hasNextProperty().get();
        }

        @Override
        public boolean hasPreviousImage() {
            return imageFileFilterController.hasPreviousProperty().get();
        }
    }

    private class AutosaveOperations implements AutosaveController.Operations {
        @Override
        public void updateModelFromView() {
            Controller.this.updateModelFromView();
        }

        @Override
        public boolean isDrawingInProgress() {
            return view.getEditorImagePane().isDrawingInProgress();
        }

        @Override
        public void restore(File autosaveFile) {
            startAnnotationImportService(autosaveFile, ImageAnnotationLoadStrategy.Type.JSON);
        }

        @Override
        public void openImageFolder(File folder) {
            loadImageFiles(folder);
        }

        @Override
        public void runOnUiThread(Runnable action) {
            Platform.runLater(action);
        }

        @Override
        public void showAutosaveTime(Instant time) {
            view.getStatusBar().setAutosaveTime(time);
        }

        @Override
        public void showAutosaveFailed(String reason) {
            view.getStatusBar().setStatusEvent(new AutosaveFailedEvent(reason));
        }

        @Override
        public AutosaveController.Timer createTimer(Runnable action) {
            return AutosaveController.createSaveTimer(action);
        }
    }

    private class ServiceResultHandlers implements IoServices.ResultHandlers {
        @Override
        public void onAnnotationsExported(WorkerStateEvent event) {
            onAnnotationExportSucceeded();
        }

        @Override
        public void onAnnotationsImported(WorkerStateEvent event) {
            onAnnotationImportSucceeded();
        }

        @Override
        public void onImageMetaDataLoaded(WorkerStateEvent event) {
            onImageMetaDataLoadingSucceeded();
        }

        @Override
        public void onBoundingBoxesPredicted(WorkerStateEvent event) {
            onBoundingBoxPredictionSucceeded();
        }

        @Override
        public void onModelNamesFetched(WorkerStateEvent event) {
            onModelNameFetchingSucceeded();
        }

        @Override
        public void onServerConnectionChecked(WorkerStateEvent event) {
            onServerConnectionCheckSucceeded();
        }

        @Override
        public void onFailed(IoService<?> service) {
            onIoServiceFailed(service);
        }
    }
}
