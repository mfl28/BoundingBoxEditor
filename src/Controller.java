import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller implements EventHandler {
    private final Stage stage;
    private final View view;
    private final Model model;

    private static final String PROGRAM_NAME_EXTENSION = " - Bounding Box Editor";

    Controller(final Stage stage) {
        this.stage = stage;
        this.view = new View(this);
        this.model = new Model();
        setModelListeners();
    }

    @Override
    public void handle(Event event) {
        final Object source = event.getSource();

        if (source == view.getFileOpenFolderItem()) {
            onRegisterOpenFolderAction();
        } else if (source == view.getFileSaveItem()) {
            onRegisterSaveAction();
        } else if (source == view.getViewFitWindowItem()) {
            System.out.println("Fit Window clicked!");
        } else if (source == view.getNextButton()) {
            onRegisterNextAction();
        } else if (source == view.getPreviousButton()) {
            onRegisterPreviousAction();
        } else if (source == view.getAddButton()){
            onRegisterAddBoundingBoxItemAction();
        }
    }

    public void onRegisterOpenFolderAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose an image folder");
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            Path inputPath = Paths.get(selectedDirectory.getPath());
            try {
                model.setImageFileList(inputPath);
            } catch (Exception e) {
                view.displayErrorAlert("Error while opening image folder",
                        "The selected folder could not be opened.",
                        "Please chose another folder.");
            }

            view.getPreviousButton().disableProperty().bind(model.hasPreviousFileBinding().not());
            view.getNextButton().disableProperty().bind(model.hasNextFileBinding().not());
            view.getNavBar().setVisible(true);

            view.setImageView(model.getCurrentImage());
            stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION);
            view.getBoundingBoxItemTableView().setItems(model.getBoundingBoxItems());
            view.getBoundingBoxItemTableView().getSelectionModel().selectFirst();
        }
    }

    private void onRegisterSaveAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save bounding box data");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV file", "*.csv"),
                new FileChooser.ExtensionFilter("TXT file", "*.txt")
        );
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            if (view.getSelectionRectangle().isVisible()) {
                saveCurrentBoundingBox();
            }

            try {
                model.writeBoundingBoxDataToFile(saveFile);
            } catch (IOException e) {
                // Message text should wrap around.
                view.displayErrorAlert("Error while saving bounding box data",
                        "Could not save bounding box data.",
                        e.getMessage());
            }
        }
    }

    private void onRegisterNextAction() {
        if (view.getSelectionRectangle().isVisible()) {
            saveCurrentBoundingBox();
        }
        model.incrementFileIndex();
    }

    private void onRegisterPreviousAction() {
        if (view.getSelectionRectangle().isVisible()) {
            saveCurrentBoundingBox();
        }
        model.decrementFileIndex();
    }

    private void saveCurrentBoundingBox() {
        String currentFilename = Utils.filenameFromUrl(model.getCurrentImageFilePath());
        model.getBoundingBoxData().put(currentFilename,
                view.getSelectionRectangle().getScaledLocalCoordinatesInSiblingImage(view.getImageView()));
    }


    public void onMousePressed(MouseEvent event) {
        double eventX = event.getX();
        double eventY = event.getY();
        Point2D parentCoordinates = view.getImageView().localToParent(eventX, eventY);
        view.setMousePressed(eventX, eventY);
        Rectangle rectangle = view.getSelectionRectangle();
        rectangle.setX(parentCoordinates.getX());
        rectangle.setY(parentCoordinates.getY());
        rectangle.setWidth(0);
        rectangle.setHeight(0);
        rectangle.setVisible(true);
    }

    public void onMouseDragged(MouseEvent event) {
        ImageView imageView = view.getImageView();
        Point2D eventXY = new Point2D(event.getX(), event.getY());
        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, imageView.getBoundsInLocal());
        double mousePressedX = view.getMousePressedX();
        double mousePressedY = view.getMousePressedY();
        Point2D parentCoordinates = imageView.localToParent(Math.min(clampedEventXY.getX(),
                mousePressedX), Math.min(clampedEventXY.getY(), mousePressedY));
        Rectangle rectangle = view.getSelectionRectangle();
        rectangle.setX(parentCoordinates.getX());
        rectangle.setY(parentCoordinates.getY());
        rectangle.setWidth(Math.abs(clampedEventXY.getX() - mousePressedX));
        rectangle.setHeight(Math.abs(clampedEventXY.getY() - mousePressedY));
    }

    private void setModelListeners() {
        model.fileIndexProperty().addListener((value, oldValue, newValue) -> {
            view.setImageView(model.getCurrentImage());
            stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION);
        });

    }

    public void onSelectionRectangleMouseEntered(MouseEvent event) {
        SelectionRectangle rectangle = view.getSelectionRectangle();
        rectangle.setCursor(Cursor.MOVE);
    }

    public void onSelectionRectangleMousePressed(MouseEvent event) {
        SelectionRectangle rectangle = view.getSelectionRectangle();
        rectangle.getDragAnchor().setFromMouseEvent(event);
        event.consume();
    }

    public void onSelectionRectangleMouseDragged(MouseEvent event) {
        SelectionRectangle rectangle = view.getSelectionRectangle();

        // add boolean "dragFromOutside" detector
        if (!rectangle.getBoundsInParent().contains(new Point2D(event.getX(), event.getY()))) {
            rectangle.getDragAnchor().setFromMouseEvent(event);
            return;
        }

        double newX = rectangle.getX() + event.getX() - rectangle.getDragAnchor().getX();
        double newY = rectangle.getY() + event.getY() - rectangle.getDragAnchor().getY();
        Bounds regionBounds = view.getImageView().getBoundsInParent();
        double newConfinedX = Utils.clamp(newX, regionBounds.getMinX(), regionBounds.getMaxX() - rectangle.getWidth());
        double newConfinedY = Utils.clamp(newY, regionBounds.getMinY(), regionBounds.getMaxY() - rectangle.getHeight());

        rectangle.setX(newConfinedX);
        rectangle.setY(newConfinedY);
        rectangle.getDragAnchor().setFromMouseEvent(event);
        event.consume();
    }

    public void onRegisterAddBoundingBoxItemAction(){
        String boundingBoxItemName = view.getNameInput().getText();
        Color boundingBoxItemColor = view.getBoundingBoxColorPicker().getValue();

        model.getBoundingBoxItems().add(new BoundingBoxItem(boundingBoxItemName, boundingBoxItemColor));
        view.getNameInput().clear();
        view.getBoundingBoxColorPicker().setValue(Color.WHITE);
    }

    public View getView() {
        return view;
    }
}
