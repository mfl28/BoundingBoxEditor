package BoundingboxEditor.controller;

import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.model.Model;
import BoundingboxEditor.model.io.ImageAnnotationDataElement;
import BoundingboxEditor.model.io.ImageAnnotationsSaveStrategy;
import BoundingboxEditor.model.io.ImageAnnotationsSaver;
import BoundingboxEditor.model.io.PVOCLoader;
import BoundingboxEditor.ui.BoundingBoxView;
import BoundingboxEditor.ui.DragAnchor;
import BoundingboxEditor.ui.MainView;
import BoundingboxEditor.utils.ColorUtils;
import BoundingboxEditor.utils.MathUtils;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

    private final Stage stage;
    private final MainView view = new MainView();
    private final Model model = new Model();
    private final Random random = new Random();

    public Controller(final Stage mainStage) {
        stage = mainStage;
        stage.setTitle(PROGRAM_NAME);

        view.connectToController(this);
        setModelListeners();
        stage.getIcons().add(new Image(getClass().getResource("/icons/drawing_crop120.png").toExternalForm()));
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
                            final ImageAnnotationsSaver saver = new ImageAnnotationsSaver(ImageAnnotationsSaveStrategy.SaveStrategy.PASCAL_VOC);
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
                MainView.displayErrorAlert("XML Parser Error", "Could not set up XML-Parser");
            } catch(IOException exception) {
                MainView.displayErrorAlert("Error reading image annotation files", "Could not read image-annotation files");
            }
        }
    }


    public void onRegisterNextButtonClickedAction(ActionEvent event) {
        // cancel image loading when clicking next
        // button while the image has not been loaded completely
        if(!view.getImagePaneView().getProgressIndicator().isVisible()) {
            view.getBoundingBoxTreeViewRoot().getChildren().clear();
            //view.getCurrentBoundingBoxes().clear();
            model.incrementFileIndex();
        }

    }

    public void onRegisterPreviousButtonClickedAction(ActionEvent event) {
        // cancel image loading when clicking previous
        // button while the image has not been loaded completely
        if(!view.getImagePaneView().getProgressIndicator().isVisible()) {
            view.getBoundingBoxTreeViewRoot().getChildren().clear();
            //view.getCurrentBoundingBoxes().clear();
            model.decrementFileIndex();
        }
    }

    public void onMousePressed(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY) &&
                !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            final Point2D parentCoordinates = view.getImageView().localToParent(event.getX(), event.getY());
            view.getImagePaneDragAnchor().setFromMouseEvent(event);

            final BoundingBoxView rectangle = view.getInitializerBoundingBox();
            rectangle.setXYWH(parentCoordinates.getX(), parentCoordinates.getY(), 0, 0);
            rectangle.setVisible(true);
        }
    }

    public void onMouseDragged(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY) &&
                !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            final ImageView imageView = view.getImageView();
            final Point2D clampedEventXY = MathUtils.clampWithinBounds(event.getX(), event.getY(), imageView.getBoundsInLocal());
            final DragAnchor mousePressed = view.getImagePaneDragAnchor();
            final Point2D parentCoordinates = imageView.localToParent(Math.min(clampedEventXY.getX(),
                    mousePressed.getX()), Math.min(clampedEventXY.getY(), mousePressed.getY()));

            view.getInitializerBoundingBox().setXYWH(parentCoordinates.getX(),
                    parentCoordinates.getY(),
                    Math.abs(clampedEventXY.getX() - mousePressed.getX()),
                    Math.abs(clampedEventXY.getY() - mousePressed.getY()));
        }
    }

    public void onMouseReleased(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY) && !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            BoundingBoxView rectangle = view.getInitializerBoundingBox();

            BoundingBoxCategory selectedBoundingBox = view.getBoundingBoxItemTableView().getSelectionModel().getSelectedItem();
            ImageMetaData imageMetaData = ImageMetaData.fromImage(view.getCurrentImage());
            BoundingBoxView newRectangle = new BoundingBoxView(selectedBoundingBox, imageMetaData);
            newRectangle.setXYWH(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
            newRectangle.setVisible(true);
            newRectangle.setStroke(rectangle.getStroke());
            newRectangle.confineTo(view.getImageView().boundsInParentProperty());

            view.getImagePaneView().getBoundingBoxDataBase().addToCurrentBoundingBoxes(newRectangle);
            rectangle.setVisible(false);
        }
    }

    public void onRegisterAddBoundingBoxItemAction(ActionEvent event) {
        final String boundingBoxItemName = view.getCategoryInputField().getText();

        if(boundingBoxItemName.trim().isEmpty()) {
            MainView.displayErrorAlert("Category Input Error", "Please provide a category name.");
            return;
        }

        if(model.getBoundingBoxCategoryNames().contains(boundingBoxItemName)) {
            MainView.displayErrorAlert("Category Input Error", "The category \"" + boundingBoxItemName + "\" already exists.");
            return;
        }

        final Color boundingBoxItemColor = view.getBoundingBoxColorPicker().getValue();

        model.getBoundingBoxCategories().add(new BoundingBoxCategory(boundingBoxItemName, boundingBoxItemColor));
        view.getCategoryInputField().clear();

        final var selectionModel = view.getBoundingBoxItemTableView().getSelectionModel();

        // auto select the created category
        selectionModel.selectLast();
        view.getBoundingBoxColorPicker().setValue(ColorUtils.createRandomColor(random));
        // auto scroll to the created category (if it would otherwise be outside the viewport)
        view.getBoundingBoxItemTableView().scrollTo(selectionModel.getSelectedIndex());
    }

    public void onRegisterSceneKeyPressed(KeyEvent event) {
        KeyCode keyCode = event.getCode();

        if(keyCode.equals(KeyCode.D)) {
            if(model.getImageFiles() != null && model.isHasNextFile()) {
                onRegisterNextButtonClickedAction(new ActionEvent());
            }
        } else if(keyCode.equals(KeyCode.A)) {
            if(model.getImageFiles() != null && model.isHasPreviousFile()) {
                onRegisterPreviousButtonClickedAction(new ActionEvent());
            }
        } else if(event.isControlDown() && keyCode.equals(KeyCode.F)) {
            view.getCategorySearchField().requestFocus();
        } else if(event.isControlDown() && keyCode.equals(KeyCode.B)) {
            view.getCategoryInputField().requestFocus();
        }
    }

    public void onRegisterExitAction() {
        Platform.exit();
    }

    public void onSelectorCellEditEvent(TableColumn.CellEditEvent<BoundingBoxCategory, String> event) {
        // FIXME: This is not working, implement custom tablecell
        BoundingBoxCategory boundingBoxCategory = event.getRowValue();

        if(model.getBoundingBoxCategoryNames().contains(event.getNewValue())) {
            MainView.displayErrorAlert("Category Input Error", "The category already exits.");
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

    private void loadSelectionRectangleList() {
        Image newImage = view.getCurrentImage();
        newImage.progressProperty().removeListener(this::fullProgressListener);
        newImage.progressProperty().addListener(this::fullProgressListener);
    }

    private void fullProgressListener(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        if(newValue.doubleValue() == 1.0) {
            // reset visibility status to visible
            view.getImagePaneView().getCurrentBoundingBoxes().forEach(item -> item.setVisible(true));
            view.getImagePaneView().removeAllBoundingBoxesFromView();
            view.getProjectSidePanel().getBoundingBoxExplorer().getRoot().getChildren().clear();
// Set the loaded list as the current working list
            final ObservableList<BoundingBoxView> loadedBoundingBoxViews = view.getCurrentBoundingBoxes();


            if(!loadedBoundingBoxViews.isEmpty()) {
                view.getImagePaneView().addBoundingBoxesToView(loadedBoundingBoxViews);
                // Add the loaded rectangles to the scenegraph and the explorer tree
                view.getProjectSidePanel().getBoundingBoxExplorer().addTreeItemsFromSelectionRectangles(loadedBoundingBoxViews);
            }

            view.updateBoundingBoxDatabaseListener();

            // Setup the listeners for add/remove functionality

        }
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
            MainView.displayErrorAlert("Error: Empty image folder", null);
            return;
        }

        ///-------------------------------------------------------------------------------------------

        model.updateFromFiles(imageFiles);

        view.reset();

        // TODO: selection rectangles in model? -> hashmap<Integer, selectionrectangle>

        view.getImagePaneView().resetSelectionRectangleDatabase(model.getImageFileListSize());
        view.getImagePaneView().getBoundingBoxDataBase().indexProperty().bind(model.fileIndexProperty());

        view.updateBoundingBoxDatabaseListener();

        view.setImageView(model.getCurrentImage());
        stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION_SEPARATOR + PROGRAM_NAME);

        // Should be done just once
        view.getBoundingBoxItemTableView().setItems(model.getBoundingBoxCategories());
        view.getBoundingBoxItemTableView().getSelectionModel().selectFirst();
        // Should be done just once
        view.getImageExplorerPanel().setImageGalleryItems(model.getImageFiles());
        view.getImageExplorerPanel().getImageGallery().getSelectionModel().selectFirst();
    }

    private void setModelListeners() {
        model.fileIndexProperty().addListener((value, oldValue, newValue) -> {
            // FIXME: Load new folder -> add rectangles to a picture -> load another folder -> exception thrown
            view.setImageView(model.getCurrentImage());
            stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION_SEPARATOR + PROGRAM_NAME);
            loadSelectionRectangleList();
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
        PVOCLoader loader = new PVOCLoader();
        List<ImageAnnotationDataElement> imageAnnotations = loader.load(Paths.get(annotationFolder.getPath()));

        System.out.println("Imported " + imageAnnotations.size() + " annotations");

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
