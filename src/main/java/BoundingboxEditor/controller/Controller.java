package BoundingboxEditor.controller;

import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.model.Model;
import BoundingboxEditor.model.io.*;
import BoundingboxEditor.ui.BoundingBoxView;
import BoundingboxEditor.ui.DeleteTableCell;
import BoundingboxEditor.ui.MainView;
import BoundingboxEditor.utils.ColorUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
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
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Responsible for event-handling between the Model class and the classes implementing the View interface.
 * View-implementors register this controller via an interface method. View-implementors are themselves
 * responsible for handling interactions between their contained UI-components.
 */
//TODO: should probably extend EventHandler
public class Controller {
    private static final String PROGRAM_NAME = "Bounding Box Editor";
    private static final String PROGRAM_NAME_EXTENSION_SEPARATOR = " - ";
    private static final String OPEN_FOLDER_ERROR_TITLE = "Error while opening image folder";
    private static final String OPEN_FOLDER_ERROR_HEADER = "The selected folder is not a valid image folder.";
    private static final String SAVE_BOUNDING_BOX_DATA_TITLE = "Save bounding box data";
    private static final String SAVE_BOUNDING_BOX_DATA_ERROR_TITLE = "Error while saving bounding box data";
    private static final String SAVE_BOUNDING_BOX_DATA_ERROR_HEADER = "The specified save-folder does not exist.";
    private static final String DIRECTORY_CHOOSER_TITLE = "Choose an image folder";
    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;
    private static final String IMPORT_ANNOTATIONS_FOLDER_CHOOSER_TITLE = "Choose a folder containing image annotation files";
    private static final String LOAD_IMAGE_FOLDER_ERROR_TITLE = "Error loading image folder";
    private static final String LOAD_IMAGE_FOLDER_ERROR_CONTENT = "The chosen folder does not contain any valid image files.";
    private static final String CATEGORY_INPUT_ERROR_TITLE = "Category Input Error";
    private static final String INVALID_CATEGORY_NAME_ERROR_CONTENT = "Please provide a category name.";
    private static final String APPLICATION_ICON_PATH = "/icons/app_icon.png";
    private static final String CATEGORY_DELETION_ERROR_TITLE = "Category Deletion Error";
    private static final String CATEGORY_DELETION_ERROR_CONTENT = "You cannot delete a category that has existing bounding-boxes assigned to it.";

    private final Stage stage;
    private final MainView view = new MainView();
    private final Model model = new Model();
    private final Random random = new Random();
    private final ChangeListener<Number> imageFullyLoadedListener = createImageFullyLoadedListener();
    private final ChangeListener<Number> fileIndexListener = createFileIndexListener();

    public Controller(final Stage mainStage) {
        stage = mainStage;
        stage.setTitle(PROGRAM_NAME);

        view.connectToController(this);
        setModelListeners();
        stage.getIcons().add(new Image(getClass().getResource(APPLICATION_ICON_PATH).toExternalForm()));
    }

    //TODO: when folder is changed: ask if the current categories should be kept or deleted

    public void onRegisterOpenFolderAction() {
        final DirectoryChooser imageFolderChooser = new DirectoryChooser();
        imageFolderChooser.setTitle(DIRECTORY_CHOOSER_TITLE);

        final File imageFolder = imageFolderChooser.showDialog(stage);

        if(imageFolder != null) {
            updateViewFromDirectory(imageFolder);
        }
    }

    public void onRegisterSaveAction() {
        // first update bounding-boxes in the model.
        if(!model.getImageFiles().isEmpty()) {
            // if there are currently loaded image-files, then update boundingboxes in model.
            model.updateCurrentBoundingBoxData(view.getBoundingBoxExplorer().extractCurrentBoundingBoxData());
        }

        if(model.getImageFileNameToAnnotation().isEmpty()) {
            MainView.displayErrorAlert("Save Error", "There are no bounding-box annotations to save.");
            return;
        }

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(SAVE_BOUNDING_BOX_DATA_TITLE);

        final File saveDirectory = directoryChooser.showDialog(stage);

        if(saveDirectory != null) {

            if(!saveDirectory.exists()) {
                MainView.displayErrorAlert(SAVE_BOUNDING_BOX_DATA_ERROR_TITLE, SAVE_BOUNDING_BOX_DATA_ERROR_HEADER);
                return;
            }

            Service<IOResult> saveService = new Service<>() {
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
            };

            ProgressDialog progressDialog = new ProgressDialog(saveService);
            progressDialog.setTitle("Saving");
            progressDialog.setHeaderText("Saving Annotations...");

            saveService.start();

            saveService.setOnSucceeded(successEvent -> {
                IOResult saveResult = saveService.getValue();

                view.getStatusPanel().setStatus("Successfully saved " + model.getImageFileNameToAnnotation().size()
                        + " image-annotation" + (model.getImageFileNameToAnnotation().size() != 1 ? "s" : "") + " in "
                        + String.format("%.3f", saveResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.");
            });
        }
    }

    public void onRegisterImportAnnotationsAction() {
        // first update bounding-boxes in the model.
        // FIXME: disallow importing when no images loaded.
        if(!model.getImageFiles().isEmpty()) {
            model.updateCurrentBoundingBoxData(view.getBoundingBoxExplorer().extractCurrentBoundingBoxData());
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(IMPORT_ANNOTATIONS_FOLDER_CHOOSER_TITLE);

        File importDirectory = directoryChooser.showDialog(stage);

        if(importDirectory != null) {
            importAnnotationsFromDirectoryPath(Paths.get(importDirectory.getPath()));
        }
    }


    public void onRegisterNextButtonClickedAction() {
        model.incrementFileIndex();
        view.getImageExplorerPanel().getImageGallery().scrollTo(Math.max(0, model.getCurrentFileIndex() - 1));
    }

    public void onRegisterPreviousButtonClickedAction() {
        model.decrementFileIndex();
        view.getImageExplorerPanel().getImageGallery().scrollTo(Math.max(0, model.getCurrentFileIndex() - 1));
    }

    public void onRegisterAddBoundingBoxCategoryAction() {
        final String categoryName = view.getBoundingBoxCategoryInputField().getText();

        if(categoryName.trim().isEmpty()) {
            MainView.displayErrorAlert(CATEGORY_INPUT_ERROR_TITLE, INVALID_CATEGORY_NAME_ERROR_CONTENT);
            return;
        }

        if(model.getBoundingBoxCategoryNames().contains(categoryName)) {
            MainView.displayErrorAlert(CATEGORY_INPUT_ERROR_TITLE, "The category \"" + categoryName + "\" already exists.");
            return;
        }

        final Color categoryColor = view.getBoundingBoxCategoryColorPicker().getValue();

        model.getBoundingBoxCategories().add(new BoundingBoxCategory(categoryName, categoryColor));
        view.getBoundingBoxCategoryInputField().clear();

        final var selectionModel = view.getBoundingBoxCategorySelectorView().getSelectionModel();

        // auto select the created category
        selectionModel.selectLast();
        view.getBoundingBoxCategoryColorPicker().setValue(ColorUtils.createRandomColor(random));
        // auto scroll to the created category (if it would otherwise be outside the viewport)
        view.getBoundingBoxCategorySelectorView().scrollTo(selectionModel.getSelectedIndex());
    }

    public void onRegisterSceneKeyPressed(KeyEvent event) {
        KeyCode keyCode = event.getCode();

        if(keyCode.equals(KeyCode.D)) {
            if(!model.getImageFiles().isEmpty() && model.isHasNextFile()) {
                onRegisterNextButtonClickedAction();
            }
        } else if(keyCode.equals(KeyCode.A)) {
            if(!model.getImageFiles().isEmpty() && model.isHasPreviousFile()) {
                onRegisterPreviousButtonClickedAction();
            }
        } else if(event.isControlDown() && keyCode.equals(KeyCode.F)) {
            view.getCategorySearchField().requestFocus();
        } else if(event.isControlDown() && keyCode.equals(KeyCode.B)) {
            view.getBoundingBoxCategoryInputField().requestFocus();
        }
    }

    public void onRegisterExitAction() {
        Platform.exit();
    }

    public void onSelectorCellEditEvent(TableColumn.CellEditEvent<BoundingBoxCategory, String> event) {
        BoundingBoxCategory boundingBoxCategory = event.getRowValue();

        if(model.getBoundingBoxCategoryNames().contains(event.getNewValue())) {
            MainView.displayErrorAlert("Category Input Error", "The category already exits.");
            // FIXME: Supposedly not reliable
            boundingBoxCategory.setName(event.getOldValue());
            event.getTableView().refresh();
        } else {
            model.getBoundingBoxCategoryNames().remove(boundingBoxCategory.getName());
            model.getBoundingBoxCategoryNames().add(event.getNewValue());
            boundingBoxCategory.setName(event.getNewValue());
        }
    }

    public void onImageViewMouseReleasedEvent(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY) && view.getBoundingBoxCategorySelectorView().getSelectionModel().getSelectedItem() != null) {
            ImageMetaData imageMetaData = model.getImageMetaDataMap().computeIfAbsent(model.getCurrentImageFileName(),
                    key -> ImageMetaData.fromImage(view.getCurrentImage()));

            BoundingBoxView newBoundingBox = new BoundingBoxView(view.getBoundingBoxCategorySelectorView().getSelectionModel().getSelectedItem(),
                    imageMetaData);

            newBoundingBox.setUpFromInitializer(view.getImagePaneView().getBoundingBoxInitializer());
            newBoundingBox.setVisible(true);
            newBoundingBox.confineTo(view.getImageView().boundsInParentProperty());

            model.getCategoriesWithExistingBoundingBoxes().add(newBoundingBox.getBoundingBoxCategory().getName());

            view.getImagePaneView().addToCurrentBoundingBoxes(newBoundingBox);
            view.getImagePaneView().getBoundingBoxInitializer().setVisible(false);
        }
    }

    public MainView getView() {
        return view;
    }

    public void updateViewFromDirectory(final File selectedDirectory) {
        List<File> imageFiles;

        try {
            imageFiles = getImageFilesFromDirectory(selectedDirectory);
        } catch(IOException e) {
            MainView.displayErrorAlert(OPEN_FOLDER_ERROR_TITLE, OPEN_FOLDER_ERROR_HEADER);
            return;
        }

        if(imageFiles.isEmpty()) {
            MainView.displayErrorAlert(LOAD_IMAGE_FOLDER_ERROR_TITLE, LOAD_IMAGE_FOLDER_ERROR_CONTENT);
            return;
        }

        model.fileIndexProperty().removeListener(fileIndexListener);
        model.updateFromFiles(imageFiles);
        model.fileIndexProperty().addListener(fileIndexListener);
        view.getStatusPanel().setStatus("Successfully loaded " + imageFiles.size() + " image-file" + (imageFiles.size() != 1 ? "s" : "")
                + " from folder " + selectedDirectory.getPath() + ".");
        setUpView();
    }

    private void setUpView() {
        view.reset();

        view.getImagePaneView().removeCurrentBoundingBoxes();

        view.updateImageFromFile(model.getCurrentImageFile());
        stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR + model.getCurrentImageFilePath());

        // Should be done just once
        view.getBoundingBoxCategorySelectorView().setItems(model.getBoundingBoxCategories());
        view.getBoundingBoxCategorySelectorView().getSelectionModel().selectFirst();
        // Should be done just once
        view.getImageExplorerPanel().setImageGalleryItems(model.getImageFilesAsObservableList());
        view.getImageExplorerPanel().getImageGallery().getSelectionModel().selectFirst();
    }

    private ChangeListener<Number> createImageFullyLoadedListener() {
        return (observable, oldValue, newValue) -> {
            if(newValue.intValue() == 1) {
                ImageAnnotationDataElement annotation = model.getCurrentImageAnnotation();

                if(annotation != null) {
                    view.loadBoundingBoxesFromAnnotation(annotation);
                }
            }
        };
    }

    private void addFullyLoadedImageListener() {
        view.getCurrentImage().progressProperty().addListener(imageFullyLoadedListener);
    }

    private void removeFullyLoadedImageListener() {
        view.getCurrentImage().progressProperty().removeListener(imageFullyLoadedListener);
    }

    private ChangeListener<Number> createFileIndexListener() {
        return (value, oldValue, newValue) -> {
            view.getImageExplorerPanel().getImageGallery().getSelectionModel().select(newValue.intValue());
            // Remove the old images bounding-box-loading listener (that triggers when an image is fully loaded.)
            removeFullyLoadedImageListener();
            // Updating bounding-box data corresponding to the previous image only needs to be done, if
            // the old image was fully loaded.
            if(view.getCurrentImage().getProgress() == 1.0) {
                // FIXME: when switching between images very fast, sometimes bounding-box-data gets lost.
                // update model bounding-box-data from previous image:
                model.updateBoundingBoxData(oldValue.intValue(), view.getBoundingBoxExplorer().extractCurrentBoundingBoxData());

                // remove old image's bounding boxes
                view.getImagePaneView().removeCurrentBoundingBoxes();

                // Prevents javafx-bug with uncleared items in tree-view when switching between images.
                view.getBoundingBoxExplorer().reset();
            }

            stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR + model.getCurrentImageFilePath());
            view.updateImageFromFile(model.getCurrentImageFile());
            addFullyLoadedImageListener();
        };
    }

    private void setModelListeners() {

        view.getImageShower().getNavigationBar()
                .getIndexLabel()
                .textProperty()
                .bind(model.fileIndexProperty().add(1).asString()
                        .concat(" | ")
                        .concat(model.imageFileListSizeProperty().asString()));

        view.getImageExplorerPanel().getImageGallery().getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue.intValue() != -1) {
                model.fileIndexProperty().set(newValue.intValue());
            }
        });

        view.getFileImportAnnotationsItem().disableProperty().bind(model.imageFileListSizeProperty().isEqualTo(0));

        view.getPreviousButton().disableProperty().bind(model.previousFileValidProperty().not());
        view.getNextButton().disableProperty().bind(model.nextFileValidProperty().not());

        view.getBoundingBoxCategorySelectorView().getDeleteColumn().setCellFactory(factory -> {
            final DeleteTableCell cell = new DeleteTableCell();

            cell.getDeleteButton().setOnAction(action -> {
                final BoundingBoxCategory category = cell.getItem();

                if(model.getCategoriesWithExistingBoundingBoxes().contains(category.getName())) {
                    MainView.displayErrorAlert(CATEGORY_DELETION_ERROR_TITLE, CATEGORY_DELETION_ERROR_CONTENT);
                } else {
                    cell.getTableView().getItems().remove(category);
                }
            });

            return cell;
        });
    }

    private void importAnnotationsFromDirectoryPath(Path directoryPath) {

        Service<IOResult> loadService = new Service<>() {
            @Override
            protected Task<IOResult> createTask() {
                return new Task<>() {
                    @Override
                    protected IOResult call() throws Exception {
                        ImageAnnotationLoader loader = new ImageAnnotationLoader(ImageAnnotationLoadStrategy.Type.PASCAL_VOC);
                        loader.progressProperty().addListener((observable, oldValue, newValue) -> updateProgress(newValue.doubleValue(), 1.0));
                        return loader.load(model, directoryPath);
                    }
                };
            }
        };

        loadService.setOnSucceeded(event -> {
            IOResult loadResult = loadService.getValue();

            view.getStatusPanel().setStatus("Successfully imported annotations from " + loadResult.getNrSuccessfullyProcessedAnnotations() + " files in " +
                    String.format("%.3f", loadResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.");

            ImageAnnotationDataElement annotation = model.getCurrentImageAnnotation();

            if(annotation != null) {
                view.getBoundingBoxExplorer().reset();
                view.loadBoundingBoxesFromAnnotation(annotation);
            }

            if(!loadResult.getErrorEntries().isEmpty()) {
                MainView.displayLoadResultInfoAlert(loadResult);
            }
        });

        ProgressDialog progressDialog = new ProgressDialog(loadService);
        progressDialog.setTitle("Loading");
        progressDialog.setHeaderText("Loading annotations...");

        loadService.start();
    }

    private List<File> getImageFilesFromDirectory(File directory) throws IOException {
        Path path = Paths.get(directory.getPath());
        return FXCollections.observableArrayList(Files.walk(path, MAX_DIRECTORY_DEPTH)
                .filter(p -> Arrays.stream(imageExtensions).anyMatch(p.toString()::endsWith))
                .map(p -> new File(p.toString()))
                .collect(Collectors.toList())
        );
    }

}
