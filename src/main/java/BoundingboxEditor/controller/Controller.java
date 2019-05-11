package BoundingboxEditor.controller;

import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.Model;
import BoundingboxEditor.model.io.*;
import BoundingboxEditor.ui.MainView;
import BoundingboxEditor.utils.ColorUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.xml.parsers.ParserConfigurationException;
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
    private static final String SAVE_BOUNDING_BOX_DATA_ERROR_HEADER = "Could not save bounding box data into the specified file.";
    private static final String DIRECTORY_CHOOSER_TITLE = "Choose an image folder";
    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;
    private static final String IMPORT_ANNOTATIONS_FOLDER_CHOOSER_TITLE = "Choose a folder containing image annotation files";
    private static final String LOAD_IMAGE_FOLDER_ERROR_TITLE = "Error loading image folder";
    private static final String LOAD_IMAGE_FOLDER_ERROR_CONTENT = "The chosen folder does not contain any valid image files.";
    private static final String CATEGORY_INPUT_ERROR_TITLE = "Category Input Error";
    private static final String INVALID_CATEGORY_NAME_ERROR_CONTENT = "Please provide a category name.";
    private static final String APPLICATION_ICON_PATH = "/icons/app_icon.png";

    private final Stage stage;
    private final MainView view = new MainView();
    private final Model model = new Model();
    private final Random random = new Random();

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
        if(view.getImagePaneView().getBoundingBoxDataBase() == null || view.getImagePaneView().getBoundingBoxDataBase().isEmpty()) {
            MainView.displayErrorAlert("Save Error", "There are no bounding-box annotations to save.");
            return;
        }

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(SAVE_BOUNDING_BOX_DATA_TITLE);

        final File saveDirectory = directoryChooser.showDialog(stage);

        if(saveDirectory != null) {
            Service<Void> saveService = new Service<>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            final List<ImageAnnotationDataElement> imageAnnotations = createImageAnnotations();
                            final ImageAnnotationSaver saver = new ImageAnnotationSaver(ImageAnnotationSaveStrategy.SaveStrategy.PASCAL_VOC);
                            saver.save(imageAnnotations, Paths.get(saveDirectory.getPath()));
                            return null;
                        }
                    };
                }
            };

            try {
                saveService.start();
            } catch(Exception e) {
                // Message text should wrap around.
                MainView.displayErrorAlert(SAVE_BOUNDING_BOX_DATA_ERROR_TITLE, SAVE_BOUNDING_BOX_DATA_ERROR_HEADER);
            }

            saveService.setOnSucceeded(event1 -> view.getBottomLabel().setText("Saved Successfully"));
        }
    }

    public void onRegisterImportAnnotationsAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(IMPORT_ANNOTATIONS_FOLDER_CHOOSER_TITLE);

        File importDirectory = directoryChooser.showDialog(stage);

        if(importDirectory != null) {
            try {
                importAnnotationsFromFolder(importDirectory);
            } catch(ParserConfigurationException exception) {
                MainView.displayErrorAlert("XML-Parser Error", "Could not set up XML-Parser");
            } catch(IOException exception) {
                MainView.displayErrorAlert("Error reading image annotation files", "Could not read image-annotation files");
            }
        }
    }


    public void onRegisterNextButtonClickedAction() {
        // cancel image loading when clicking next
        // button while the image has not been loaded completely
        if(!view.getImagePaneView().getProgressIndicator().isVisible()) {
            view.getBoundingBoxTreeViewRoot().getChildren().clear();
            //view.getCurrentBoundingBoxes().clear();
            model.incrementFileIndex();
        }

    }

    public void onRegisterPreviousButtonClickedAction() {
        // cancel image loading when clicking previous
        // button while the image has not been loaded completely
        if(!view.getImagePaneView().getProgressIndicator().isVisible()) {
            view.getBoundingBoxTreeViewRoot().getChildren().clear();
            //view.getCurrentBoundingBoxes().clear();
            model.decrementFileIndex();
        }
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

        final var selectionModel = view.getBoundingBoxCategoryTableView().getSelectionModel();

        // auto select the created category
        selectionModel.selectLast();
        view.getBoundingBoxCategoryColorPicker().setValue(ColorUtils.createRandomColor(random));
        // auto scroll to the created category (if it would otherwise be outside the viewport)
        view.getBoundingBoxCategoryTableView().scrollTo(selectionModel.getSelectedIndex());
    }

    public void onRegisterSceneKeyPressed(KeyEvent event) {
        KeyCode keyCode = event.getCode();

        if(keyCode.equals(KeyCode.D)) {
            if(model.getImageFiles() != null && model.isHasNextFile()) {
                onRegisterNextButtonClickedAction();
            }
        } else if(keyCode.equals(KeyCode.A)) {
            if(model.getImageFiles() != null && model.isHasPreviousFile()) {
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

        model.updateFromFiles(imageFiles);
        setUpView();
    }

    private void setUpView() {
        view.reset();

        view.getImagePaneView().resetBoundingBoxDatabase(model.getImageFileListSize());
        view.getImagePaneView().getBoundingBoxDataBase().indexProperty().bind(model.fileIndexProperty());

        view.updateBoundingBoxDatabaseListener();

        view.setImageView(model.getCurrentImage());
        stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR + model.getCurrentImageFilePath());

        // Should be done just once
        view.getBoundingBoxCategoryTableView().setItems(model.getBoundingBoxCategories());
        view.getBoundingBoxCategoryTableView().getSelectionModel().selectFirst();
        // Should be done just once
        view.getImageExplorerPanel().setImageGalleryItems(model.getImageFiles());
        view.getImageExplorerPanel().getImageGallery().getSelectionModel().selectFirst();
    }

    private void setModelListeners() {
        model.fileIndexProperty().addListener((value, oldValue, newValue) -> {
            view.setImageView(model.getCurrentImage());
            stage.setTitle(PROGRAM_NAME + PROGRAM_NAME_EXTENSION_SEPARATOR + model.getCurrentImageFilePath());
            view.updateBoundingBoxesInWorkspace();
            view.getBottomLabel().setText(model.getCurrentImageFilePath());
            view.getImageExplorerPanel().getImageGallery().getSelectionModel().select(newValue.intValue());
        });

        view.getImageShower().getNavigationBar()
                .getIndexLabel()
                .textProperty()
                .bind(model.fileIndexProperty().add(1).asString()
                        .concat(" | ")
                        .concat(model.imageFileListSizeProperty().asString()));

        view.getImageExplorerPanel().getImageGallery().getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue.intValue() != -1) {
                model.fileIndexProperty().set(newValue.intValue());
            }
        }));

        view.getPreviousButton().disableProperty().bind(model.previousFileValidProperty().not());
        view.getNextButton().disableProperty().bind(model.nextFileValidProperty().not());
    }

    private void importAnnotationsFromFolder(File annotationFolder) throws ParserConfigurationException, IOException {
        ImageAnnotationLoader loader = new ImageAnnotationLoader(ImageAnnotationLoadStrategy.LoadStrategy.PASCAL_VOC);
        List<ImageAnnotationDataElement> imageAnnotations = loader.load(model.getImageFileSet(), Paths.get(annotationFolder.getPath()));

        view.getBottomLabel().setText("Successfully imported annotations for " + imageAnnotations.size() + " files.");

        view.updateWorkspaceFromImageAnnotations(imageAnnotations);
    }


    private List<ImageAnnotationDataElement> createImageAnnotations() {
        return view.getImagePaneView().getBoundingBoxDataBase().stream()
                .filter(boundingBoxes -> !boundingBoxes.isEmpty())
                .map(ImageAnnotationDataElement::fromBoundingBoxes)
                .collect(Collectors.toList());
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
