package BoundingboxEditor.controller;

import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.model.Model;
import BoundingboxEditor.model.io.ImageAnnotationDataElement;
import BoundingboxEditor.model.io.ImageAnnotationsSaveStrategy;
import BoundingboxEditor.model.io.ImageAnnotationsSaver;
import BoundingboxEditor.utils.ColorUtils;
import BoundingboxEditor.ui.DragAnchor;
import BoundingboxEditor.ui.MainView;
import BoundingboxEditor.ui.SelectionRectangle;
import BoundingboxEditor.utils.MathUtils;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    private final Stage stage;
    private final MainView view = new MainView();
    private final Model model = new Model();
    private final Random random = new Random();

    public Controller(final Stage mainStage) {
        stage = mainStage;
        stage.setTitle(PROGRAM_NAME);

        view.connectToController(this);
        setModelListeners();
    }

    public void onRegisterOpenFolderAction() {
        final DirectoryChooser imageFolderChooser = new DirectoryChooser();
        imageFolderChooser.setTitle(DIRECTORY_CHOOSER_TITLE);

        final File imageFolder = imageFolderChooser.showDialog(stage);

        if(imageFolder != null) {
            updateViewFromDirectory(imageFolder);
        }
    }

    public void onRegisterSaveAction() {
        if(view.getImageSelectionRectangles() == null || view.getImageSelectionRectangles().isEmpty()
                || view.getImageSelectionRectangles().stream().allMatch((item) -> item == null || item.isEmpty())) {
            view.displayErrorAlert("Save Error", null, "There are no bounding-box annotations to save.");
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
                view.displayErrorAlert(SAVE_BOUNDING_BOX_DATA_ERROR_TITLE, SAVE_BOUNDING_BOX_DATA_ERROR_HEADER,
                        e.getMessage());
            }

            saveService.setOnSucceeded(event1 -> {
                view.getBottomLabel().setText("Saved Successfully");
            });
        }
    }

    public void onRegisterNextAction(ActionEvent event) {
        // cancel image loading when clicking next
        // button while the image has not been loaded completely
        if(!view.getImagePaneView().getProgressIndicator().isVisible()) {
            view.getBoundingBoxTreeViewRoot().getChildren().clear();
            //view.getSelectionRectangleList().clear();
            model.incrementFileIndex();
        }

    }

    public void onRegisterPreviousAction(ActionEvent event) {
        // cancel image loading when clicking previous
        // button while the image has not been loaded completely
        if(!view.getImagePaneView().getProgressIndicator().isVisible()) {
            view.getBoundingBoxTreeViewRoot().getChildren().clear();
            //view.getSelectionRectangleList().clear();
            model.decrementFileIndex();
        }
    }

    public void onRegisterFitWindowAction() {
        // TO BE IMPLEMENTED
    }

    public void onMousePressed(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY) &&
                !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            final Point2D parentCoordinates = view.getImageView().localToParent(event.getX(), event.getY());
            view.getMousePressed().setFromMouseEvent(event);

            final SelectionRectangle rectangle = view.getSelectionRectangle();
            rectangle.setXYWH(parentCoordinates.getX(), parentCoordinates.getY(), 0, 0);
            rectangle.setVisible(true);
        }
    }

    public void onMouseDragged(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY) &&
                !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            final ImageView imageView = view.getImageView();
            final Point2D clampedEventXY = MathUtils.clampWithinBounds(event.getX(), event.getY(), imageView.getBoundsInLocal());
            final DragAnchor mousePressed = view.getMousePressed();
            final Point2D parentCoordinates = imageView.localToParent(Math.min(clampedEventXY.getX(),
                    mousePressed.getX()), Math.min(clampedEventXY.getY(), mousePressed.getY()));

            view.getSelectionRectangle().setXYWH(parentCoordinates.getX(),
                    parentCoordinates.getY(),
                    Math.abs(clampedEventXY.getX() - mousePressed.getX()),
                    Math.abs(clampedEventXY.getY() - mousePressed.getY()));
        }
    }

    public void onMouseReleased(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY) &&
                !view.getBoundingBoxItemTableView().getSelectionModel().isEmpty()) {
            SelectionRectangle rectangle = view.getSelectionRectangle();
            BoundingBoxCategory selectedBoundingBox = view.getBoundingBoxItemTableView().getSelectionModel().getSelectedItem();
            ImageMetaData imageMetaData = ImageMetaData.fromImage(view.getCurrentImage());
            SelectionRectangle newRectangle = new SelectionRectangle(selectedBoundingBox, imageMetaData);
            newRectangle.setXYWH(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
            newRectangle.setVisible(true);
            newRectangle.setStroke(rectangle.getStroke());
            newRectangle.confineTo(view.getImageView().boundsInParentProperty());

            view.getSelectionRectangleList().add(newRectangle);
            rectangle.setVisible(false);
        }
    }

    public void onRegisterAddBoundingBoxItemAction(ActionEvent event) {
        final String boundingBoxItemName = view.getCategoryInputField().getText();

        if(boundingBoxItemName.isEmpty()) {
            view.displayErrorAlert("Category Input Error", null, "Please provide a category name.");
            return;
        }

        if(model.getBoundingBoxCategoryNames().contains(boundingBoxItemName)) {
            view.displayErrorAlert("Category Input Error", null, "The category \"" + boundingBoxItemName + "\" already exists.");
            return;
        }

        final Color boundingBoxItemColor = view.getBoundingBoxColorPicker().getValue();

        model.getBoundingBoxCategories().add(new BoundingBoxCategory(boundingBoxItemName, boundingBoxItemColor));
        model.getBoundingBoxCategoryNames().add(boundingBoxItemName);
        view.getCategoryInputField().clear();

        final var selectionModel = view.getBoundingBoxItemTableView().getSelectionModel();

        // auto select the created category
        selectionModel.selectLast();
        view.getBoundingBoxColorPicker().setValue(ColorUtils.createRandomColor(random));
        // auto scroll to the created category (if it would otherwise be outside the viewport)
        view.getBoundingBoxItemTableView().scrollTo(selectionModel.getSelectedIndex());
    }

    public void handleSceneKeyPress(KeyEvent event) {
        KeyCode keyCode = event.getCode();

        if(keyCode.equals(KeyCode.D)) {
            if(model.getImageFileList() != null && model.isHasNextFile()) {
                onRegisterNextAction(new ActionEvent());
            }
        } else if(keyCode.equals(KeyCode.A)) {
            if(model.getImageFileList() != null && model.isHasPreviousFile()) {
                onRegisterPreviousAction(new ActionEvent());
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

    public MainView getView() {
        return view;
    }

    public void loadSelectionRectangleList() {
        Image newImage = view.getCurrentImage();
        newImage.progressProperty().removeListener(this::fullProgressListener);
        newImage.progressProperty().addListener(this::fullProgressListener);
    }

    public void fullProgressListener(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        if(newValue.doubleValue() == 1.0) {
            // reset visibility status to visible
            view.getSelectionRectangleList().forEach(item -> item.setVisible(true));
            view.getImagePaneView().removeSelectionRectanglesFromChildren(view.getSelectionRectangleList());
            view.getProjectSidePanel().getExplorerView().getRoot().getChildren().clear();
            final ObservableList<SelectionRectangle> loadedSelectionRectangles = view.getImageSelectionRectangles().get(model.fileIndexProperty().get());
            if(loadedSelectionRectangles == null) {
                // Create a new empty list of rectangles
                final ObservableList<SelectionRectangle> newRectangles = FXCollections.observableArrayList();
                view.getImageSelectionRectangles().set(model.fileIndexProperty().get(), newRectangles);

                // Set the newly created list as the current working list
                view.setSelectionRectangleList(newRectangles);

                // Setup the listeners for add/remove functionality
                view.setSelectionRectangleListListener();
            } else {
                // Set the loaded list as the current working list
                view.getImagePaneView().addSelectionRectanglesAsChildren(loadedSelectionRectangles);
                view.setSelectionRectangleList(loadedSelectionRectangles);
                // Add the loaded rectangles to the scenegraph and the explorer tree
                view.getProjectSidePanel().getExplorerView().addTreeItemsFromSelectionRectangles(loadedSelectionRectangles);
            }
        }
    }

    public void updateViewFromDirectory(final File selectedDirectory) {
        // clear current selection rectangles when new folder is loaded
        view.setSelectionRectangleListListener();
        view.getSelectionRectangleList().clear();
        final Path inputPath = Paths.get(selectedDirectory.getPath());

        try {
            model.setImageFileListFromPath(inputPath);
        } catch(Exception e) {
            view.displayErrorAlert(OPEN_FOLDER_ERROR_TITLE, OPEN_FOLDER_ERROR_HEADER, e.getMessage());
            return;
        }

        view.getPreviousButton().disableProperty().bind(model.hasPreviousFileBinding().not());
        view.getNextButton().disableProperty().bind(model.hasNextFileBinding().not());
        view.getNavigationBar().setVisible(true);
        view.getNavigationBar().setManaged(true);
        view.getProjectSidePanel().setVisible(true);
        view.getProjectSidePanel().setManaged(true);
        view.getImageExplorerPanel().setVisible(true);
        view.getWorkspace().setVisible(true);

        view.getImagePaneView().resetSelectionRectangleDatabase(model.fileListSizeProperty().get());

        view.getImageSelectionRectangles().set(model.fileIndexProperty().get(), FXCollections.observableArrayList());
        view.setSelectionRectangleList(view.getImageSelectionRectangles().get(model.fileIndexProperty().get()));
        view.setSelectionRectangleListListener();

        view.setImageView(model.getCurrentImage());
        stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION_SEPARATOR + PROGRAM_NAME);
        view.getBoundingBoxItemTableView().setItems(model.getBoundingBoxCategories());
        view.getBoundingBoxItemTableView().getSelectionModel().selectFirst();

        view.getImageExplorerPanel().setImageGalleryItems(model.getImageFileList());
        view.getImageExplorerPanel().getImageGallery().getSelectionModel().selectFirst();
    }

    private void setModelListeners() {
        model.fileIndexProperty().addListener((value, oldValue, newValue) -> {
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
                        .concat(model.fileListSizeProperty().asString()));

        // Synchronizes name hashset with bounding box category list when items are deleted.
        // TODO: This should also work when changing names in existing categories,
        model.getBoundingBoxCategories().addListener((ListChangeListener<BoundingBoxCategory>) c -> {
            Set<String> boundingBoxCategoryNames = model.getBoundingBoxCategoryNames();
            while(c.next()) {
                c.getRemoved().forEach(boundingBoxCategory ->
                        boundingBoxCategoryNames.remove(boundingBoxCategory.getName()));
            }
        });

        view.getImageExplorerPanel().getImageGallery().getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue.intValue() != -1) {
                model.fileIndexProperty().set(newValue.intValue());
            }

        }));

    }

    private List<ImageAnnotationDataElement> createImageAnnotations() {
        final List<ImageAnnotationDataElement> imageAnnotations = new ArrayList<>();

        view.getImageSelectionRectangles().forEach(imageSelectionRectangles -> {
            if(imageSelectionRectangles != null && !imageSelectionRectangles.isEmpty()) {
                imageAnnotations.add(ImageAnnotationDataElement.fromSelectionRectangles(imageSelectionRectangles));
            }
        });

        return imageAnnotations;
    }

}
