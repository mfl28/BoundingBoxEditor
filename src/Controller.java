import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
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
    }

    @Override
    public void handle(Event event) {
        final Object source = event.getSource();

        if(source == view.getFileOpenFolderItem()){
            onRegisterOpenFolderAction();
        }
        else if(source == view.getFileSaveItem()){
            System.out.println("Save clicked!");
        }
        else if(source == view.getViewFitWindowItem()){
            System.out.println("Fit Window clicked!");
        }
        else if(source == view.getNextButton()){
            model.incrementFileIndex();
            view.getSelectionRectangle().setVisible(false);
        }
        else if(source == view.getPreviousButton()){
            model.decrementFileIndex();
            view.getSelectionRectangle().setVisible(false);
        }
    }

    public void onRegisterOpenFolderAction(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if(selectedDirectory != null){
            Path inputPath = Paths.get(selectedDirectory.getPath());
            model.setImageFileList(inputPath);

            view.getPreviousButton().disableProperty().bind(model.hasPreviousFileBinding().not());
            view.getNextButton().disableProperty().bind(model.hasNextFileBinding().not());

            view.setImageView(model.getCurrentImage());

            // To Fix: currently this adds another listener whenever a new folder is chosen.
            model.fileIndexProperty().addListener((value, oldValue, newValue) -> {
                // Maybe pass index newval to the getImage function.
                view.setImageView(model.getCurrentImage());
            });
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
        double eventX = clamp(event.getX(), view.getImageView().getBoundsInParent().getMinX(),
                view.getImageView().getBoundsInParent().getMaxX());
        double mousePressedX = view.getMousePressedX();
        double eventY = clamp(event.getY(), view.getImageView().getBoundsInParent().getMinY(),
                view.getImageView().getBoundsInParent().getMaxY());
        double mousePressedY = view.getMousePressedY();
        Rectangle rectangle = view.getSelectionRectangle();
        rectangle.setX(Math.min(eventX, mousePressedX));
        rectangle.setY(Math.min(eventY, mousePressedY));
        rectangle.setWidth(Math.abs(eventX - mousePressedX));
        rectangle.setHeight(Math.abs(eventY - mousePressedY));
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public View getView(){
        return view;
    }
}
