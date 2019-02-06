import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jdk.jshell.execution.Util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Controller implements EventHandler {
    private final Stage stage;
    private final View view;
    private final Model model;

    private static final String PROGRAM_NAME_EXTENSION = " - Bounding Box Editor";

    Controller(final Stage stage){
        this.stage = stage;
        this.view = new View(this);
        this.model = new Model();
        setModelListeners();
    }

    @Override
    public void handle(Event event) {
        final Object source = event.getSource();

        if(source == view.getFileOpenFolderItem()){
            onRegisterOpenFolderAction();
        }
        else if(source == view.getFileSaveItem()){
            onRegisterSaveAction();
        }
        else if(source == view.getViewFitWindowItem()){
            System.out.println("Fit Window clicked!");
        }
        else if(source == view.getNextButton()){
            onRegisterNextAction();
            //model.getBoundingBoxData().put()
        }
        else if(source == view.getPreviousButton()){
            onRegisterPreviousAction();
        }
    }

    public void onRegisterOpenFolderAction(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose an image folder");
        File selectedDirectory = directoryChooser.showDialog(stage);

        if(selectedDirectory != null){
            Path inputPath = Paths.get(selectedDirectory.getPath());
            try {
                model.setImageFileList(inputPath);
            }
            catch(Exception e){
                view.displayErrorAlert("Error while opening image folder",
                        "The selected folder could not be opened.",
                        "Please chose another folder.");
            }


            view.getPreviousButton().disableProperty().bind(model.hasPreviousFileBinding().not());
            view.getNextButton().disableProperty().bind(model.hasNextFileBinding().not());

            view.setImageView(model.getCurrentImage());
            stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION);
        }
    }

    private void onRegisterSaveAction(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save bounding box data");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV file", "*.csv"),
                new FileChooser.ExtensionFilter("TXT file", "*.txt")
        );
        File saveFile = fileChooser.showSaveDialog(stage);

        if(saveFile != null){
            if(view.getSelectionRectangle().isVisible()){
                saveCurrentBoundingBox();
            }
            
            try{
                model.writeBoundingBoxDataToFile(saveFile);
            }
            catch(IOException e) {
                // Message text should wrap around.
                view.displayErrorAlert("Error while saving bounding box data",
                        "Could not save bounding box data.",
                        e.getMessage());
            }
        }
    }

    private void onRegisterNextAction(){
        if(view.getSelectionRectangle().isVisible()){
            saveCurrentBoundingBox();
        }
        model.incrementFileIndex();
    }

    private void onRegisterPreviousAction(){
        if(view.getSelectionRectangle().isVisible()){
            saveCurrentBoundingBox();
        }
        model.decrementFileIndex();
    }

    private void saveCurrentBoundingBox(){
        String currentFilename = Utils.filenameFromUrl(model.getCurrentImageFilePath());
        model.getBoundingBoxData().put(currentFilename,
                view.getSelectionRectangle().getScaledLocalCoordinatesInSiblingImage(view.getImageView()));
    }




    public void onMousePressed(MouseEvent event){
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

    public void onMouseDragged(MouseEvent event){
        ImageView imageView = view.getImageView();
        double eventX = Utils.clamp(event.getX(), 0, imageView.getFitWidth());
        double eventY = Utils.clamp(event.getY(), 0, imageView.getFitHeight());
        double mousePressedX = view.getMousePressedX();
        double mousePressedY = view.getMousePressedY();
        Point2D parentCoordinates = imageView.localToParent(Math.min(eventX, mousePressedX), Math.min(eventY, mousePressedY));
        Rectangle rectangle = view.getSelectionRectangle();
        rectangle.setX(parentCoordinates.getX());
        rectangle.setY(parentCoordinates.getY());
        rectangle.setWidth(Math.abs(eventX - mousePressedX));
        rectangle.setHeight(Math.abs(eventY - mousePressedY));
    }

    private void setModelListeners(){
        model.fileIndexProperty().addListener((value, oldValue, newValue) -> {
            view.setImageView(model.getCurrentImage());
            stage.setTitle(model.getCurrentImageFilePath() + PROGRAM_NAME_EXTENSION);
        });

    }

    public void onSelectionRectangleMouseEntered(MouseEvent event){
        SelectionRectangle rectangle = view.getSelectionRectangle();
        rectangle.setCursor(Cursor.MOVE);
    }

    public void onSelectionRectangleMousePressed(MouseEvent event){
        SelectionRectangle rectangle = view.getSelectionRectangle();
        rectangle.getDragAnchor().setFromMouseEvent(event);
        event.consume();
    }

    public void onSelectionRectangleMouseDragged(MouseEvent event){
        SelectionRectangle rectangle = view.getSelectionRectangle();

        // add boolean "dragFromOutside" detector
        if(!rectangle.getBoundsInParent().contains(new Point2D(event.getX(), event.getY()))){
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

    public View getView(){
        return view;
    }
}
