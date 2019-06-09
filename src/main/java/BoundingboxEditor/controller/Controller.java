package BoundingboxEditor.controller;

import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.model.Model;
import BoundingboxEditor.model.io.*;
import BoundingboxEditor.ui.BoundingBoxCategoryDeleteTableCell;
import BoundingboxEditor.ui.BoundingBoxView;
import BoundingboxEditor.ui.MainView;
import BoundingboxEditor.ui.StatusEvents.ImageAnnotationsImportingSuccessfulEvent;
import BoundingboxEditor.ui.StatusEvents.ImageAnnotationsSavingSuccessfulEvent;
import BoundingboxEditor.ui.StatusEvents.ImageFilesLoadingSuccessfulEvent;
import BoundingboxEditor.utils.ColorUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.ProgressDialog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The control-component of the application (as in MVC pattern). Responsible for interaction-handling
 * between the {@link Model} and the classes implementing the {@link BoundingboxEditor.ui.View} interface.
 * {@link BoundingboxEditor.ui.View} implementors can register a controller via an interface method but
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

    private static final String DIRECTORY_CHOOSER_TITLE = "Choose an image folder";
    private static final String IMPORT_ANNOTATIONS_FOLDER_CHOOSER_TITLE = "Choose a folder containing image annotation files";
    private static final String APPLICATION_ICON_PATH = "/icons/app_icon.png";
    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;
    private static final String SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE = "Save Error";
    private static final String NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT = "There are no image annotations to save.";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE = "Saving Annotations";
    private static final String SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER = "Saving in progress...";

    private final Stage stage;
    private final MainView view = new MainView();
    private final Model model = new Model();
    private final ListChangeListener<BoundingBoxView> boundingBoxCountPerCategoryListener = createBoundingBoxCountPerCategoryListener();
    private final ChangeListener<Number> imageLoadProgressListener = createImageLoadingProgressListener();
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

        view.connectToController(this);
        setModelListeners();
    }

    /**
     * Handles the event of the user requesting to open new image folder.
     */
    public void onRegisterOpenImageFolderAction() {
        // TODO: when folder is changed: ask if the current categories should be kept or deleted
        final DirectoryChooser imageFolderChooser = new DirectoryChooser();
        imageFolderChooser.setTitle(DIRECTORY_CHOOSER_TITLE);

        final File imageFolder = imageFolderChooser.showDialog(stage);

        if(imageFolder != null) {
            loadImageFilesFromDirectory(imageFolder);
        }
    }

    /**
     * Loads image-files from the provided directory into the model and updates
     * the view.
     *
     * @param imageFileDirectory the directory containing the image-files to be loaded
     */
    public void loadImageFilesFromDirectory(File imageFileDirectory) {
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

        view.getStatusPanel().setStatusEvent(new ImageFilesLoadingSuccessfulEvent(imageFiles.size(), imageFileDirectory));
    }

    /**
     * Handles the event of the user requesting to save the image annotations.
     */
    public void onRegisterSaveAnnotationsAction() {
        // At first (if needed) update the model's bounding-box data from the bounding-boxes currently in view.
        if(model.imageFilesLoaded()) {
            model.updateCurrentBoundingBoxData(view.getCurrentBoundingBoxData());
        }

        if(model.getImageFileNameToAnnotation().isEmpty()) {
            MainView.displayErrorAlert(SAVE_IMAGE_ANNOTATIONS_ERROR_DIALOG_TITLE,
                    NO_IMAGE_ANNOTATIONS_TO_SAVE_ERROR_DIALOG_CONTENT);
            return;
        }

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(SAVE_IMAGE_ANNOTATIONS_DIRECTORY_CHOOSER_TITLE);

        final File saveDirectory = directoryChooser.showDialog(stage);

        if(saveDirectory != null) {
            final Service<IOResult> saveService = createSaveAnnotationsService(saveDirectory);
            MainView.displayServiceProgressDialog(saveService, SAVING_ANNOTATIONS_PROGRESS_DIALOG_TITLE,
                    SAVING_ANNOTATIONS_PROGRESS_DIALOGUE_HEADER);
            saveService.start();
        }
    }

    /**
     * Handles the event of the user requesting to save the current image-annotations.
     */
    public void onRegisterImportAnnotationsAction() {
        // At first (if needed) update the model's bounding-box data from the bounding-boxes currently in view.
        if(model.imageFilesLoaded()) {
            model.updateCurrentBoundingBoxData(view.getCurrentBoundingBoxData());
        }

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(IMPORT_ANNOTATIONS_FOLDER_CHOOSER_TITLE);

        final File importDirectory = directoryChooser.showDialog(stage);

        if(importDirectory != null) {
            importAnnotationsFromDirectory(importDirectory);
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

        if(model.getBoundingBoxCategoryNames().contains(categoryName)) {
            MainView.displayErrorAlert(CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                    "The category \"" + categoryName + "\" already exists.");
            view.getBoundingBoxCategoryInputField().clear();
            return;
        }

        final Color categoryColor = view.getBoundingBoxCategoryColorPicker().getValue();
        model.getBoundingBoxCategories().add(new BoundingBoxCategory(categoryName, categoryColor));

        view.getBoundingBoxCategoryTableView().getSelectionModel().selectLast();
        view.getBoundingBoxCategoryTableView().scrollTo(view
                .getBoundingBoxCategoryTableView()
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
        Platform.exit();
    }

    /**
     * Handles the event of the user typing a keyboard short-cut.
     *
     * @param event the short-cut key-event
     */
    public void onRegisterSceneKeyPressed(KeyEvent event) {
        final KeyCode keyCode = event.getCode();

        if(event.isControlDown()) {
            // Handle Ctrl-* short-cuts
            switch(keyCode) {
                case F:
                    if(event.isAltDown()) {
                        view.getImageFileSearchField().requestFocus();
                    } else {
                        view.getCategorySearchField().requestFocus();
                    }
                    break;
                case N:
                    view.getBoundingBoxCategoryInputField().requestFocus();
                    break;
                case T:
                    view.getTagInputField().requestFocus();
                    break;
                case H:
                    if(event.isAltDown()) {
                        view.getBoundingBoxTreeView().setToggleIconStateForAllTreeItems(false);
                    } else {
                        view.getBoundingBoxTreeView().setToggleIconStateForSelectedBoundingBoxTreeItem(false);
                    }
                    break;
                case V:
                    if(event.isAltDown()) {
                        view.getBoundingBoxTreeView().setToggleIconStateForAllTreeItems(true);
                    } else {
                        view.getBoundingBoxTreeView().setToggleIconStateForSelectedBoundingBoxTreeItem(true);
                    }
                    break;
            }
        } else {
            // Handle normal shortcuts
            switch(keyCode) {
                case D:
                    if(model.imageFilesLoaded() && model.nextImageFileExists()) {
                        onRegisterNextButtonClickedAction();
                    }
                    break;
                case A:
                    if(model.imageFilesLoaded() && model.previousImageFileExists()) {
                        onRegisterPreviousButtonClickedAction();
                    }
                    break;
                case DELETE:
                    view.removeSelectedTreeItemAndChildren();
                    break;
            }
        }
    }

    /**
     * Handles the event of the user clicking the next(-image)-button.
     */
    public void onRegisterNextButtonClickedAction() {
        model.incrementFileIndex();
        // Keep the currently selected item in the image-gallery in view.
        view.getImageGallery().scrollTo(Math.max(0, model.getCurrentFileIndex() - 1));
    }

    /**
     * Handles the event of the user clicking the previous(-image)-button.
     */
    public void onRegisterPreviousButtonClickedAction() {
        model.decrementFileIndex();
        // Keep the currently selected item in the image-gallery in view.
        view.getImageGallery().scrollTo(Math.max(0, model.getCurrentFileIndex() - 1));
    }

    /**
     * Handles the event of the user committing a bounding-box category name edit. Names of categories are allowed
     * to be changed by the user as long as the uniqueness of category-names is not violated, otherwise an error dialog
     * will be displayed and the edit will be reverted.
     *
     * @param event the edit event
     * @see BoundingboxEditor.ui.BoundingBoxCategoryTableView BoundingBoxCategoryTableView
     */
    public void onSelectorCellEditEvent(TableColumn.CellEditEvent<BoundingBoxCategory, String> event) {
        if(event.getOldValue().equals(event.getNewValue())) {
            // Nothing to do if the new name is the same as the current one.
            return;
        }

        final BoundingBoxCategory boundingBoxCategory = event.getRowValue();

        if(model.getBoundingBoxCategoryNames().contains(event.getNewValue())) {
            MainView.displayErrorAlert(Controller.CATEGORY_INPUT_ERROR_DIALOG_TITLE,
                    "The category \"" + boundingBoxCategory.getName() + "\" already exists.");
            boundingBoxCategory.setName(event.getOldValue());
            event.getTableView().refresh();
        } else {
            model.getBoundingBoxCategoryNames().remove(boundingBoxCategory.getName());
            model.getBoundingBoxCategoryNames().add(event.getNewValue());
            boundingBoxCategory.setName(event.getNewValue());
        }
    }

    /**
     * Handles the event of the user releasing a mouse-click on the displayed image.
     * This construct a new bounding-box.
     *
     * @param event the mouse-event
     */
    public void onImageViewMouseReleasedEvent(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY) && view.getBoundingBoxCategoryTableView().isCategorySelected()) {
            final ImageMetaData imageMetaData = model.getImageFileNameToMetaData()
                    .computeIfAbsent(model.getCurrentImageFileName(), key -> ImageMetaData.fromImage(view.getCurrentImage()));

            view.getImagePane().constructAndAddNewBoundingBox(imageMetaData);
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

    private void setModelListeners() {

        view.getImageShower().getImageToolBar()
                .getIndexLabel()
                .textProperty()
                .bind(model.fileIndexProperty().add(1).asString()
                        .concat(" | ")
                        .concat(model.nrImageFilesProperty().asString()));

        view.getImageExplorerPanel().getImageFileListView().getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue.intValue() != -1) {
                model.fileIndexProperty().set(newValue.intValue());
            }
        });

        view.getFileImportAnnotationsItem().disableProperty().bind(model.nrImageFilesProperty().isEqualTo(0));

        view.getPreviousImageNavigationButton().disableProperty().bind(model.previousImageFileExistsProperty().not());
        view.getNextImageNavigationButton().disableProperty().bind(model.nextImageFileExistsProperty().not());

        view.getBoundingBoxCategoryTableView().getDeleteColumn().setCellFactory(column -> {
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
        return FXCollections.observableArrayList(Files.walk(path, MAX_DIRECTORY_DEPTH)
                .filter(p -> Arrays.stream(imageExtensions).anyMatch(p.toString()::endsWith))
                .map(p -> new File(p.toString()))
                .collect(Collectors.toList())
        );
    }

    private void updateViewImageFiles() {
        view.reset();

        view.getImagePane().removeAllCurrentBoundingBoxes();

        view.updateImageFromFile(model.getCurrentImageFile());
        stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR + model.getCurrentImageFilePath());

        view.getBoundingBoxCategoryTableView().setItems(model.getBoundingBoxCategories());
        view.getBoundingBoxCategoryTableView().getSelectionModel().selectFirst();
        view.getImageExplorerPanel().setImageFiles(model.getImageFilesAsObservableList());
        view.getImageExplorerPanel().getImageFileListView().getSelectionModel().selectFirst();
        view.getImageExplorerPanel().getImageFileListView().scrollTo(0);
        view.getCurrentBoundingBoxes().addListener(boundingBoxCountPerCategoryListener);
    }

    private Service<IOResult> createSaveAnnotationsService(File saveDirectory) {
        Service<IOResult> saveService = new Service<>() {
            @Override
            protected Task<IOResult> createTask() {
                return new Task<>() {
                    @Override
                    protected IOResult call() {
                        final ImageAnnotationSaver saver = new ImageAnnotationSaver(ImageAnnotationSaveStrategy.Type.PASCAL_VOC);

                        saver.progressProperty().addListener((observable, oldValue, newValue) -> updateProgress(newValue.doubleValue(), 1.0));

                        return saver.save(model.getImageFileNameToAnnotationMap(), Paths.get(saveDirectory.getPath()));
                    }
                };
            }
        };

        saveService.setOnSucceeded(successEvent -> {
            IOResult saveResult = saveService.getValue();

            if(saveResult.getNrSuccessfullyProcessedItems() != 0) {
                view.getStatusPanel().setStatusEvent(new ImageAnnotationsSavingSuccessfulEvent(saveService.getValue()));
            }

            if(!saveResult.getErrorTableEntries().isEmpty()) {
                MainView.displayIOResultErrorInfoAlert(saveResult);
            }

        });

        return saveService;
    }

    private void importAnnotationsFromDirectory(File directoryPath) {
        Service<IOResult> loadService = createLoadAnnotationsService(directoryPath);

        ProgressDialog progressDialog = new ProgressDialog(loadService);
        progressDialog.setTitle("Loading");
        progressDialog.setHeaderText("Loading annotations...");

        loadService.setOnSucceeded(event -> {
            IOResult loadResult = loadService.getValue();

            if(loadResult.getNrSuccessfullyProcessedItems() != 0) {
                view.getStatusPanel().setStatusEvent(new ImageAnnotationsImportingSuccessfulEvent(loadResult));
            }

            ImageAnnotation annotation = model.getCurrentImageAnnotation();

            if(annotation != null) {
                view.getBoundingBoxTreeView().reset();
                view.getCurrentBoundingBoxes().removeListener(boundingBoxCountPerCategoryListener);
                view.loadBoundingBoxViewsFromAnnotation(annotation);
                view.getCurrentBoundingBoxes().addListener(boundingBoxCountPerCategoryListener);
            }

            if(!loadResult.getErrorTableEntries().isEmpty()) {
                MainView.displayIOResultErrorInfoAlert(loadResult);
            }
        });

        loadService.start();
    }

    private Service<IOResult> createLoadAnnotationsService(File loadDirectory) {
        return new Service<>() {
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
        };
    }

    private ChangeListener<Number> createImageLoadingProgressListener() {
        return (observable, oldValue, newValue) -> {
            if(newValue.intValue() == 1) {
                ImageAnnotation annotation = model.getCurrentImageAnnotation();

                if(annotation != null) {
                    view.loadBoundingBoxViewsFromAnnotation(annotation);
                }

                view.getCurrentBoundingBoxes().addListener(boundingBoxCountPerCategoryListener);
            }
        };
    }

    private ChangeListener<Number> createSelectedFileIndexListener() {
        return (value, oldValue, newValue) -> {
            view.getImageExplorerPanel().getImageFileListView().getSelectionModel().select(newValue.intValue());
            // Remove the old images bounding-box-loading listener (that triggers when an image is fully loaded.)
            removeImageLoadingProgressListener();
            // Updating bounding-box data corresponding to the previous image only needs to be done, if
            // the old image was fully loaded.
            if(view.getCurrentImage().getProgress() == 1.0) {
                // update model bounding-box-data from previous image:
                model.updateBoundingBoxDataAtFileIndex(oldValue.intValue(), view.getBoundingBoxTreeView().extractCurrentBoundingBoxData());
                // remove old image's bounding boxes
                view.getCurrentBoundingBoxes().removeListener(boundingBoxCountPerCategoryListener);
                view.getImagePane().removeAllCurrentBoundingBoxes();
                // Prevents javafx-bug with uncleared items in tree-view when switching between images.
                view.getBoundingBoxTreeView().reset();
            }

            stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR + model.getCurrentImageFilePath());
            view.updateImageFromFile(model.getCurrentImageFile());
            addImageLoadingProgressListener();
        };
    }

    private void removeImageLoadingProgressListener() {
        view.getCurrentImage().progressProperty().removeListener(imageLoadProgressListener);
    }

    private void addImageLoadingProgressListener() {
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

}
