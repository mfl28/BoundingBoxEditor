package BoundingBoxEditor.controller;

import BoundingBoxEditor.model.BoundingBoxCategory;
import BoundingBoxEditor.model.ImageMetaData;
import BoundingBoxEditor.model.Model;
import BoundingBoxEditor.model.io.*;
import BoundingBoxEditor.ui.*;
import BoundingBoxEditor.ui.StatusEvents.ImageAnnotationsImportingSuccessfulEvent;
import BoundingBoxEditor.ui.StatusEvents.ImageAnnotationsSavingSuccessfulEvent;
import BoundingBoxEditor.ui.StatusEvents.ImageFilesLoadingSuccessfulEvent;
import BoundingBoxEditor.utils.ColorUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The control-component of the application (as in MVC pattern). Responsible for interaction-handling
 * between the {@link Model} and the classes implementing the {@link BoundingBoxEditor.ui.View} interface.
 * {@link BoundingBoxEditor.ui.View} implementors can register a controller via an interface method but
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
    private static final String SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE = "Save image annotations";
    private static final String LOAD_IMAGE_FOLDER_ERROR_DIALOG_TITLE = "Error loading image folder";
    private static final String LOAD_IMAGE_FOLDER_ERROR_DIALOG_CONTENT = "The chosen folder does not contain any valid image files.";
    private static final String CATEGORY_INPUT_ERROR_DIALOG_TITLE = "Category Creation Error";
    private static final String INVALID_CATEGORY_NAME_ERROR_DIALOG_CONTENT = "Please provide a non-blank category name.";
    private static final String CATEGORY_DELETION_ERROR_DIALOG_TITLE = "Category Deletion Error";
    private static final String CATEGORY_DELETION_ERROR_DIALOG_CONTENT = "You cannot delete a category that has existing bounding-boxes assigned to it.";

    private static final String IMAGE_FOLDER_CHOOSER_TITLE = "Choose an image folder";
    private static final String IMPORT_ANNOTATIONS_FOLDER_CHOOSER_TITLE = "Choose a folder containing image annotation files";
    private static final String APPLICATION_ICON_PATH = "/icons/app_icon.png";
    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;
    private static final String SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE = "Save Error";
    private static final String NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT = "There are no image annotations to save.";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE = "Saving Annotations";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER = "Saving in progress...";
    private static final String ANNOTATION_IMPORT_ERROR_TITLE = "Annotation Import Error";
    private static final String ANNOTATION_IMPORT_ERROR_NO_VALID_FILES_CONTENT = "The folder does not contain any valid annotation files.";
    private static final String LOADING_ANNOTATIONS_DIALOG_TITLE = "Loading";
    private static final String LOADING_ANNOTATIONS_DIALOG_HEADER = "Loading annotations...";
    private static final String OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE = "Open image folder";
    private static final String OPEN_IMAGE_FOLDER_OPTION_DIALOG_CONTENT = "Opening a new image folder will remove any existing annotation data. " +
            "Do you want to save the currently existing annotation data?";
    private static final String IMPORT_ANNOTATION_DATA_OPTION_DIALOG_TITLE = "Import annotation data";
    private static final String IMPORT_ANNOTATION_DATA_OPTION_DIALOG_CONTENT = "Do you want to keep existing categories and annotation data?";
    private static final String EXIT_APPLICATION_OPTION_DIALOG_TITLE = "Exit Application";
    private static final String EXIT_APPLICATION_OPTION_DIALOG_CONTENT = "Do you want to save the existing annotation data?";

    private final Stage stage;
    private final MainView view = new MainView();
    private final Model model = new Model();

    private final ListChangeListener<BoundingBoxView> boundingBoxCountPerCategoryListener = createBoundingBoxCountPerCategoryListener();
    private final ChangeListener<Number> imageLoadProgressListener = createImageLoadingProgressListener();
    private final ChangeListener<Boolean> imageNavigationKeyPressedListener = createImageNavigationKeyPressedListener();
    private final BooleanProperty navigatePreviousKeyPressed = new SimpleBooleanProperty(false);
    private final BooleanProperty navigateNextKeyPressed = new SimpleBooleanProperty(false);
    private String lastLoadedImageUrl;
    private final ChangeListener<Number> selectedFileIndexListener = createSelectedFileIndexListener();

    /**
     * Creates a new controller object that is responsible for handling the application logic and
     * handles interaction between the view and model components.
     *
     * @param mainStage the stage that represents the top level container of all used ui-elements
     */
    public Controller(final Stage mainStage) {
        stage = mainStage;
        stage.setTitle(PROGRAM_NAME);
        stage.getIcons().add(new Image(getClass().getResource(APPLICATION_ICON_PATH).toExternalForm()));
        stage.setOnCloseRequest(event -> {
            onRegisterExitAction();
            event.consume();
        });

        loadPreferences();

        view.connectToController(this);
        setUpModelListeners();
    }

    /**
     * Handles the event of the user requesting to open a new image folder.
     */
    public void onRegisterOpenImageFolderAction() {
        // TODO: when folder is changed: ask if the current categories should be kept or deleted
        final File imageFolder = MainView.displayDirectoryChooserAndGetChoice(IMAGE_FOLDER_CHOOSER_TITLE, stage);

        if(imageFolder != null) {
            if(model.containsAnnotations() || view.containsBoundingBoxViews()) {
                ButtonBar.ButtonData answer = MainView.displayYesNoCancelDialogAndGetResult(OPEN_IMAGE_FOLDER_OPTION_DIALOG_TITLE,
                        OPEN_IMAGE_FOLDER_OPTION_DIALOG_CONTENT);

                if(answer == ButtonBar.ButtonData.YES) {
                    final File saveDirectory = MainView.displayDirectoryChooserAndGetChoice(SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE, stage);

                    if(saveDirectory != null) {
                        AnnotationSaverService annotationSaverService = new AnnotationSaverService(saveDirectory);
                        annotationSaverService.runOnSuccess(() -> loadImageFiles(imageFolder));
                        annotationSaverService.startAndShowProgressDialog();
                    }
                    return;
                } else if(answer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                    return;
                }
            }

            loadImageFiles(imageFolder);
        }
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

        model.fileIndexProperty().removeListener(selectedFileIndexListener);
        model.resetDataAndSetImageFiles(imageFiles);
        model.fileIndexProperty().addListener(selectedFileIndexListener);

        updateViewImageFiles();

        view.getStatusBar().setStatusEvent(new ImageFilesLoadingSuccessfulEvent(imageFiles.size(), imageFileDirectory));
    }

    /**
     * Handles the event of the user requesting to save the image annotations.
     */
    public void onRegisterSaveAnnotationsAction() {
        if(!model.containsAnnotations() && !view.containsBoundingBoxViews()) {
            MainView.displayErrorAlert(SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE,
                    NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT);
            return;
        }

        final File saveDirectory = MainView.displayDirectoryChooserAndGetChoice(SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE, stage);

        if(saveDirectory != null) {
            new AnnotationSaverService(saveDirectory).startAndShowProgressDialog();
        }
    }

    /**
     * Handles the event of the user requesting to save the current image-annotations.
     */
    public void onRegisterImportAnnotationsAction() {
        final File importDirectory = MainView.displayDirectoryChooserAndGetChoice(IMPORT_ANNOTATIONS_FOLDER_CHOOSER_TITLE, stage);

        if(importDirectory != null) {
            if(model.containsCategories()) {
                ButtonBar.ButtonData answer = MainView.displayYesNoCancelDialogAndGetResult(IMPORT_ANNOTATION_DATA_OPTION_DIALOG_TITLE,
                        IMPORT_ANNOTATION_DATA_OPTION_DIALOG_CONTENT);

                if(answer == ButtonBar.ButtonData.NO) {
                    model.clearAnnotationData();
                    view.reset();
                } else if(answer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                    return;
                }
            }

            new AnnotationLoaderService(importDirectory).startAndShowProgressDialog();
        }
    }

    /**
     * Handles the event of the user adding a new bounding-box category.
     */
    public void onRegisterAddBoundingBoxCategoryAction() {
        final String categoryName = view.getBoundingBoxCategoryInputField().getText();

        if(categoryName.isBlank()) {
            MainView.displayErrorAlert(CATEGORY_INPUT_ERROR_DIALOG_TITLE, INVALID_CATEGORY_NAME_ERROR_DIALOG_CONTENT);
            view.getBoundingBoxCategoryInputField().clear();
            return;
        }

        if(model.getCategoryToAssignedBoundingBoxesCountMap().containsKey(categoryName)) {
            MainView.displayErrorAlert(CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                    "The category \"" + categoryName + "\" already exists.");
            view.getBoundingBoxCategoryInputField().clear();
            return;
        }

        final Color categoryColor = view.getBoundingBoxCategoryColorPicker().getValue();
        model.getBoundingBoxCategories().add(new BoundingBoxCategory(categoryName, categoryColor));
        model.getCategoryToAssignedBoundingBoxesCountMap().put(categoryName, 0);

        view.getBoundingBoxCategoryTable().getSelectionModel().selectLast();
        view.getBoundingBoxCategoryTable().scrollTo(view
                .getBoundingBoxCategoryTable()
                .getSelectionModel()
                .getSelectedIndex()
        );

        view.getBoundingBoxCategoryInputField().clear();
        view.getBoundingBoxCategoryColorPicker().setValue(ColorUtils.createRandomColor());
    }

    /**
     * Handles the event of the user requesting to exit the application.
     */
    public void onRegisterExitAction() {
        if(model.containsAnnotations() || view.containsBoundingBoxViews()) {
            ButtonBar.ButtonData answer = MainView.displayYesNoCancelDialogAndGetResult(EXIT_APPLICATION_OPTION_DIALOG_TITLE,
                    EXIT_APPLICATION_OPTION_DIALOG_CONTENT);

            if(answer == ButtonBar.ButtonData.YES) {
                final File saveDirectory = MainView.displayDirectoryChooserAndGetChoice(SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE, stage);

                if(saveDirectory != null) {
                    AnnotationSaverService saverService = new AnnotationSaverService(saveDirectory);
                    saverService.runOnSuccess(() -> {
                        Preferences.userNodeForPackage(getClass()).putBoolean("isMaximized", stage.isMaximized());
                        Platform.exit();
                    });
                    saverService.startAndShowProgressDialog();
                }
                return;
            } else if(answer == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return;
            }
        }

        Preferences.userNodeForPackage(getClass()).putBoolean("isMaximized", stage.isMaximized());
        Platform.exit();
    }

    /**
     * Handles the event of the user pressing a defined keyboard short-cut.
     *
     * @param event the short-cut key-event
     */
    public void onRegisterSceneKeyPressed(KeyEvent event) {
        // While the user is drawing a bounding box, all key-events will be ignored.
        if(view.getBoundingBoxEditorImagePane().isBoundingBoxDrawingInProgress()) {
            event.consume();
            return;
        }

        if(event.isShortcutDown()) {
            view.getBoundingBoxEditorImagePane().setZoomableAndPannable(true);
        }

        if(KEY_COMBINATIONS.navigateNext.match(event)) {
            if(model.containsImageFiles() && model.hasNextImageFile()
                    && !navigatePreviousKeyPressed.get()) {
                navigateNextKeyPressed.set(true);
                onRegisterNextImageFileRequested();
            } else {
                navigateNextKeyPressed.set(false);
            }
        } else if(KEY_COMBINATIONS.navigatePrevious.match(event)) {
            if(model.containsImageFiles() && model.hasPreviousImageFile()
                    && !navigateNextKeyPressed.get()) {
                navigatePreviousKeyPressed.set(true);
                onRegisterPreviousImageFileRequested();
            } else {
                navigatePreviousKeyPressed.set(false);
            }
        } else if(KEY_COMBINATIONS.deleteSelectedBoundingBox.match(event)) {
            view.removeSelectedTreeItemAndChildren();
        } else if(KEY_COMBINATIONS.focusCategorySearchField.match(event)) {
            view.getCategorySearchField().requestFocus();
        } else if(KEY_COMBINATIONS.focusFileSearchField.match(event)) {
            view.getImageFileSearchField().requestFocus();
        } else if(KEY_COMBINATIONS.focusCategoryNameTextField.match(event)) {
            view.getBoundingBoxCategoryInputField().requestFocus();
        } else if(KEY_COMBINATIONS.focusTagTextField.match(event)) {
            view.getTagInputField().requestFocus();
        } else if(KEY_COMBINATIONS.hideSelectedBoundingBox.match(event)) {
            view.getBoundingBoxTree().setToggleIconStateForSelectedBoundingBoxTreeItem(false);
        } else if(KEY_COMBINATIONS.hideAllBoundingBoxes.match(event)) {
            view.getBoundingBoxTree().setToggleIconStateForAllTreeItems(false);
        } else if(KEY_COMBINATIONS.showSelectedBoundingBox.match(event)) {
            view.getBoundingBoxTree().setToggleIconStateForSelectedBoundingBoxTreeItem(true);
        } else if(KEY_COMBINATIONS.showAllBoundingBoxes.match(event)) {
            view.getBoundingBoxTree().setToggleIconStateForAllTreeItems(true);
        } else if(KEY_COMBINATIONS.resetSizeAndCenterImage.match(event)) {
            view.getBoundingBoxEditorImagePane().resetImageViewSize();
        }
    }

    /**
     * Handles the event of the user releasing a keyboard short-cut.
     *
     * @param event the short-cut key-event
     */
    public void onRegisterSceneKeyReleased(KeyEvent event) {
        if(event.getCode() == KeyCode.CONTROL || event.getCode() == KeyCode.META) {
            view.getBoundingBoxEditorImagePane().setZoomableAndPannable(false);
        }

        if(KEY_COMBINATIONS.navigatePrevious.match(event)) {
            navigatePreviousKeyPressed.set(false);
        } else if(KEY_COMBINATIONS.navigateNext.match(event)) {
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
     * Handles the event of the user committing a bounding-box category name edit. Names of categories are allowed
     * to be changed by the user as long as the uniqueness of category-names is not violated, otherwise an error dialog
     * will be displayed and the edit will be reverted.
     *
     * @param event the edit event
     * @see BoundingBoxEditor.ui.BoundingBoxCategoryTableView BoundingBoxCategoryTableView
     */
    public void onSelectorCellEditEvent(TableColumn.CellEditEvent<BoundingBoxCategory, String> event) {
        String newName = event.getNewValue();
        String oldName = event.getOldValue();

        if(oldName.equals(newName)) {
            // Nothing to do if the new name is the same as the current one.
            return;
        }

        final BoundingBoxCategory boundingBoxCategory = event.getRowValue();
        final Map<String, Integer> boundingBoxesPerCategoryNameMap = model.getCategoryToAssignedBoundingBoxesCountMap();

        if(boundingBoxesPerCategoryNameMap.containsKey(newName)) {
            MainView.displayErrorAlert(Controller.CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                    "The category \"" + newName + "\" already exists.");
            boundingBoxCategory.setName(oldName);
            event.getTableView().refresh();
        } else {
            int assignedBoundingBoxesCount = boundingBoxesPerCategoryNameMap.get(oldName);
            boundingBoxesPerCategoryNameMap.remove(oldName);

            boundingBoxesPerCategoryNameMap.put(newName, assignedBoundingBoxesCount);
            boundingBoxCategory.setName(newName);
        }
    }

    /**
     * Handles the event of the user releasing a mouse-click on the displayed image.
     * This construct a new bounding-box.
     *
     * @param event the mouse-event
     */
    public void onRegisterImageViewMouseReleasedEvent(MouseEvent event) {
        final BoundingBoxEditorImagePaneView imagePane = view.getBoundingBoxEditorImagePane();

        if(imagePane.isImageFullyLoaded() && event.getButton().equals(MouseButton.PRIMARY)) {
            if(event.isControlDown()) {
                view.getBoundingBoxEditorImageView().setCursor(Cursor.OPEN_HAND);
            } else if(view.getBoundingBoxCategoryTable().isCategorySelected() && imagePane.isBoundingBoxDrawingInProgress()) {
                final ImageMetaData imageMetaData = model.getImageFileNameToMetaDataMap()
                        .get(model.getCurrentImageFileName());

                imagePane.constructAndAddNewBoundingBox(imageMetaData);
                imagePane.setBoundingBoxDrawingInProgress(false);
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

    private void setUpModelListeners() {
        view.getBoundingBoxEditor().getBoundingBoxEditorToolBar()
                .getIndexLabel()
                .textProperty()
                .bind(model.fileIndexProperty().add(1).asString()
                        .concat(" | ")
                        .concat(model.nrImageFilesProperty().asString()));

        view.getImageFileExplorer().getImageFileListView().getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue.intValue() != -1) {
                model.fileIndexProperty().set(newValue.intValue());
            }
        });

        view.getFileImportAnnotationsItem().disableProperty().bind(model.nrImageFilesProperty().isEqualTo(0));

        view.getPreviousImageNavigationButton().disableProperty().bind(model.hasPreviousImageFileProperty().not());
        view.getNextImageNavigationButton().disableProperty().bind(model.hasNextImageFileProperty().not());

        view.getBoundingBoxCategoryTable().getDeleteColumn().setCellFactory(column -> {
            final BoundingBoxCategoryDeleteTableCell cell = new BoundingBoxCategoryDeleteTableCell();

            cell.getDeleteButton().setOnAction(action -> {
                final BoundingBoxCategory category = cell.getItem();

                int nrExistingBoundingBoxes = model.getCategoryToAssignedBoundingBoxesCountMap().getOrDefault(category.getName(), 0);

                // Only allow to delete a bounding-box category that has no bounding-boxes assigned to it.
                if(nrExistingBoundingBoxes != 0) {
                    MainView.displayErrorAlert(CATEGORY_DELETION_ERROR_DIALOG_TITLE,
                            CATEGORY_DELETION_ERROR_DIALOG_CONTENT
                                    + "\nCurrently there " + (nrExistingBoundingBoxes == 1 ? "is " : "are ") + nrExistingBoundingBoxes
                                    + " bounding-box" + (nrExistingBoundingBoxes == 1 ? " " : "es ") + "with the category \"" + category.getName() + "\".");
                } else {
                    cell.getTableView().getItems().remove(category);
                }
            });

            return cell;
        });
    }

    private List<File> getImageFilesFromDirectory(File directory) throws IOException {
        Path path = Paths.get(directory.getPath());

        try(Stream<Path> imageFiles = Files.walk(path, MAX_DIRECTORY_DEPTH)) {
            return imageFiles.filter(file -> Arrays.stream(imageExtensions).anyMatch(file.toString()::endsWith))
                    .map(file -> new File(file.toString()))
                    .collect(Collectors.toList());
        }
    }

    private void updateViewImageFiles() {
        view.reset();

        BoundingBoxEditorImagePaneView imagePane = view.getBoundingBoxEditorImagePane();
        imagePane.removeAllCurrentBoundingBoxes();
        view.getCurrentBoundingBoxes().removeListener(boundingBoxCountPerCategoryListener);
        imagePane.getImageLoadingProgressIndicator().setVisible(true);

        ImageMetaData metaData = model.getImageFileNameToMetaDataMap().computeIfAbsent(model.getCurrentImageFileName(),
                key -> ImageMetaData.fromFile(model.getCurrentImageFile()));
        view.updateImageFromFile(model.getCurrentImageFile(), metaData.getImageWidth(), metaData.getImageHeight());
        view.getCurrentImage().progressProperty().addListener(imageLoadProgressListener);

        stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR + model.getCurrentImageFilePath());

        BoundingBoxCategoryTableView boundingBoxCategoryTableView = view.getBoundingBoxCategoryTable();
        boundingBoxCategoryTableView.setItems(model.getBoundingBoxCategories());
        boundingBoxCategoryTableView.getSelectionModel().selectFirst();

        ImageFileExplorerView imageFileExplorerView = view.getImageFileExplorer();
        imageFileExplorerView.setImageFiles(model.getImageFilesAsObservableList());

        ImageFileListView imageFileListView = view.getImageFileListView();
        imageFileListView.getSelectionModel().selectFirst();
        imageFileListView.scrollTo(0);
    }

    private ChangeListener<Number> createImageLoadingProgressListener() {
        return (observable, oldValue, newValue) -> {
            if(newValue.intValue() == 1) {
                ImageAnnotation annotation = model.getCurrentImageAnnotation();
                // Hide the progress spinner.
                view.getBoundingBoxEditorImagePane().getImageLoadingProgressIndicator().setVisible(false);

                if(annotation != null) {
                    view.loadBoundingBoxViewsFromAnnotation(annotation);
                }

                view.getCurrentBoundingBoxes().addListener(boundingBoxCountPerCategoryListener);
            }
        };
    }

    private ChangeListener<Number> createSelectedFileIndexListener() {
        return (value, oldValue, newValue) -> {
            // Update selected item in image-file-list-view.
            view.getImageFileExplorer().getImageFileListView().getSelectionModel().select(newValue.intValue());
            // Show the progress spinner.
            view.getBoundingBoxEditorImagePane().getImageLoadingProgressIndicator().setVisible(true);

            final Image oldImage = view.getCurrentImage();

            if(oldImage != null && !oldImage.getUrl().equals(lastLoadedImageUrl)) {
                // Remove the old images bounding-box-loading listener (that triggers when an image is fully loaded.)
                oldImage.progressProperty().removeListener(imageLoadProgressListener);
                // Updating bounding-box data corresponding to the previous image only needs to be done, if
                // the old image was fully loaded.
                if(oldImage.getProgress() == 1.0) {
                    // update model bounding-box-data from previous image:
                    model.updateBoundingBoxDataAtFileIndex(oldValue.intValue(), view.getBoundingBoxTree().extractCurrentBoundingBoxData());
                    // remove old image's bounding boxes
                    view.getCurrentBoundingBoxes().removeListener(boundingBoxCountPerCategoryListener);
                    view.getBoundingBoxEditorImagePane().removeAllCurrentBoundingBoxes();
                    // Prevents javafx-bug with uncleared items in tree-view when switching between images.
                    view.getBoundingBoxTree().reset();
                } else {
                    oldImage.cancel();
                }

                // Clears the current image from the view.
                view.getBoundingBoxEditorImageView().setImage(null);
                lastLoadedImageUrl = oldImage.getUrl();
            }

            stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR + model.getCurrentImageFilePath());

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
                if(!newValue) {
                    // Load the image corresponding to the current file index into the view-component.
                    updateViewImageFromModel();
                    observable.removeListener(this);
                }
            }
        };
    }

    private void updateViewImageFromModel() {
        ImageMetaData metaData = model.getImageFileNameToMetaDataMap().computeIfAbsent(model.getCurrentImageFileName(),
                key -> ImageMetaData.fromFile(model.getCurrentImageFile()));
        view.updateImageFromFile(model.getCurrentImageFile(), metaData.getImageWidth(), metaData.getImageHeight());
        view.getCurrentImage().progressProperty().addListener(imageLoadProgressListener);
    }

    private ListChangeListener<BoundingBoxView> createBoundingBoxCountPerCategoryListener() {
        return change -> {
            while(change.next()) {
                if(change.wasAdded()) {
                    change.getAddedSubList().forEach(item ->
                            model.getCategoryToAssignedBoundingBoxesCountMap()
                                    .merge(item.getBoundingBoxCategory().getName(), 1, Integer::sum));
                }

                if(change.wasRemoved()) {
                    change.getRemoved().forEach(item ->
                            model.getCategoryToAssignedBoundingBoxesCountMap()
                                    .computeIfPresent(item.getBoundingBoxCategory().getName(),
                                            (key, value) -> --value));
                }
            }
        };
    }

    private void loadPreferences() {
        Preferences preferences = Preferences.userNodeForPackage(getClass());
        stage.setMaximized(preferences.getBoolean("isMaximized", false));
    }

    /**
     * Interface for chaining function calls after e.g. a service has completed successfully.
     *
     * @param <T> Runnable or Service
     */
    interface OnSuccessRunner<T> {
        void runOnSuccess(T runnable);
    }

    /**
     * An interface for starting a service and showing a progress dialog.
     */
    interface ProgressShower {
        void startAndShowProgressDialog();
    }

    /**
     * Class containing possible key-combinations.
     */
    public static class KEY_COMBINATIONS {
        public static final KeyCombination navigateNext = new KeyCodeCombination(KeyCode.D);
        public static final KeyCombination navigatePrevious = new KeyCodeCombination(KeyCode.A);
        public static final KeyCombination showAllBoundingBoxes = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
        public static final KeyCombination hideAllBoundingBoxes = new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
        public static final KeyCombination showSelectedBoundingBox = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination hideSelectedBoundingBox = new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN);

        public static final KeyCombination resetSizeAndCenterImage = new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination focusCategoryNameTextField = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination focusCategorySearchField = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination focusTagTextField = new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN);
        public static final KeyCombination focusFileSearchField = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
        public static final KeyCombination deleteSelectedBoundingBox = new KeyCodeCombination(KeyCode.DELETE);
    }

    class AnnotationSaverService extends Service<IOResult> implements OnSuccessRunner<Runnable>, ProgressShower {
        private File saveDirectory;

        AnnotationSaverService(File saveDirectory) {
            this.saveDirectory = saveDirectory;
            setOnSucceeded(successEvent -> defaultOnSucceededHandler());
        }

        @Override
        public void runOnSuccess(Runnable runnable) {
            setOnSucceeded(successEvent -> {
                defaultOnSucceededHandler();
                runnable.run();
            });
        }

        @Override
        public void startAndShowProgressDialog() {
            MainView.displayServiceProgressDialog(this, SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE,
                    SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER);
            start();
        }

        @Override
        public void start() {
            if(model.containsImageFiles()) {
                model.updateCurrentBoundingBoxData(view.extractCurrentBoundingBoxData());
            }

            super.start();
        }

        @Override
        protected Task<IOResult> createTask() {
            return new Task<>() {
                @Override
                protected IOResult call() {
                    final ImageAnnotationSaver saver = new ImageAnnotationSaver(ImageAnnotationSaveStrategy.Type.PASCAL_VOC);

                    saver.progressProperty().addListener((observable, oldValue, newValue) -> updateProgress(newValue.doubleValue(), 1.0));

                    return saver.save(model.getImageAnnotations(), Paths.get(saveDirectory.getPath()));
                }
            };
        }

        private void defaultOnSucceededHandler() {
            IOResult saveResult = getValue();

            if(saveResult.getNrSuccessfullyProcessedItems() != 0) {
                view.getStatusBar().setStatusEvent(new ImageAnnotationsSavingSuccessfulEvent(saveResult));
            }

            if(!saveResult.getErrorTableEntries().isEmpty()) {
                MainView.displayIOResultErrorInfoAlert(saveResult);
            }
        }
    }

    class AnnotationLoaderService extends Service<IOResult> implements OnSuccessRunner<Runnable>, ProgressShower {
        private File loadDirectory;

        AnnotationLoaderService(File loadDirectory) {
            this.loadDirectory = loadDirectory;

            setOnSucceeded(successEvent -> defaultOnSuccessHandler());
        }

        @Override
        public void runOnSuccess(Runnable runnable) {
            setOnSucceeded(successEvent -> {
                defaultOnSuccessHandler();
                runnable.run();
            });
        }

        @Override
        public void startAndShowProgressDialog() {
            MainView.displayServiceProgressDialog(this, LOADING_ANNOTATIONS_DIALOG_TITLE,
                    LOADING_ANNOTATIONS_DIALOG_HEADER);
            start();
        }

        @Override
        public void start() {
            if(model.containsImageFiles()) {
                model.updateCurrentBoundingBoxData(view.extractCurrentBoundingBoxData());
            }

            super.start();
        }

        @Override
        protected Task<IOResult> createTask() {
            return new Task<>() {
                @Override
                protected IOResult call() throws Exception {
                    ImageAnnotationLoader loader = new ImageAnnotationLoader(ImageAnnotationLoadStrategy.Type.PASCAL_VOC);
                    loader.progressProperty().addListener((observable, oldValue, newValue) -> updateProgress(newValue.doubleValue(), 1.0));
                    return loader.load(model, Paths.get(loadDirectory.getPath()));
                }
            };
        }

        private void defaultOnSuccessHandler() {
            IOResult loadResult = getValue();

            if(loadResult.getNrSuccessfullyProcessedItems() != 0) {
                view.getStatusBar().setStatusEvent(new ImageAnnotationsImportingSuccessfulEvent(loadResult));
            }

            ImageAnnotation annotation = model.getCurrentImageAnnotation();

            if(annotation != null) {
                view.getBoundingBoxTree().reset();
                view.getCurrentBoundingBoxes().removeListener(boundingBoxCountPerCategoryListener);
                view.loadBoundingBoxViewsFromAnnotation(annotation);
                view.getCurrentBoundingBoxes().addListener(boundingBoxCountPerCategoryListener);
                view.getBoundingBoxCategoryTable().refresh();
                view.getBoundingBoxTree().refresh();
            }

            if(!loadResult.getErrorTableEntries().isEmpty()) {
                MainView.displayIOResultErrorInfoAlert(loadResult);
            } else if(loadResult.getNrSuccessfullyProcessedItems() == 0) {
                MainView.displayErrorAlert(ANNOTATION_IMPORT_ERROR_TITLE, ANNOTATION_IMPORT_ERROR_NO_VALID_FILES_CONTENT);
            }
        }
    }
}
