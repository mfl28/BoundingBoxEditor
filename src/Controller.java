import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
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
            System.out.println("Save clicked!");
        }
        else if(source == view.getViewFitWindowItem()){
            System.out.println("Fit Window clicked!");
        }
        else if(source == view.getNextButton()){
            model.incrementFileIndex();
        }
        else if(source == view.getPreviousButton()){
            model.decrementFileIndex();
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
        }
    }

    void onRegisterSaveAction(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save bounding box data");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV file", "*.csv"),
                new FileChooser.ExtensionFilter("TXT file", "*.txt")
        );
        File saveFile = fileChooser.showSaveDialog(stage);

        if(saveFile != null){
            try{
                model.writeBoundingBoxDataToFile(saveFile);
            }
            catch(IOException e) {
                view.displayErrorAlert("Error while saving bounding box data",
                        "Could not save bounding box data.",
                        "Choose a different save-file.");
            }
        }
    }

    public void onMousePressed(MouseEvent event){
        if(!view.getImageView().getBoundsInParent().contains(event.getX(), event.getY())){
            return;
        }

        double eventX = event.getX();
        double eventY = event.getY();
        view.setMousePressed(eventX, eventY);
        Rectangle rectangle = view.getSelectionRectangle();
        rectangle.setX(eventX);
        rectangle.setY(eventY);
        rectangle.setWidth(0);
        rectangle.setHeight(0);
        rectangle.setVisible(true);
    }

    public void onMouseDragged(MouseEvent event){
        double eventX = Utils.clamp(event.getX(), view.getImageView().getBoundsInParent().getMinX(),
                view.getImageView().getBoundsInParent().getMaxX());
        double mousePressedX = view.getMousePressedX();
        double eventY = Utils.clamp(event.getY(), view.getImageView().getBoundsInParent().getMinY(),
                view.getImageView().getBoundsInParent().getMaxY());
        double mousePressedY = view.getMousePressedY();
        Rectangle rectangle = view.getSelectionRectangle();
        rectangle.setX(Math.min(eventX, mousePressedX));
        rectangle.setY(Math.min(eventY, mousePressedY));
        rectangle.setWidth(Math.abs(eventX - mousePressedX));
        rectangle.setHeight(Math.abs(eventY - mousePressedY));
    }

    private void setModelListeners(){
        model.fileIndexProperty().addListener((value, oldValue, newValue) -> {
            view.setImageView(model.getCurrentImage());
        });
    }

    public View getView(){
        return view;
    }
}
