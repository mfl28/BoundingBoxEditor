import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Model {
    private ObservableList<File> imageFileList;
    private IntegerProperty fileIndex;
    private IntegerBinding imageFileListSize;
    private BooleanBinding hasNextFile;
    private BooleanBinding hasPreviousFile;
    private ObjectBinding<File> currentFile;

    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;

    public Model(){
    }

    public void setImageFileList(Path path){
        // try/catch and exception handling should be done in Controller class
        try{
            imageFileList = FXCollections.observableArrayList(
                    Files.walk(path, MAX_DIRECTORY_DEPTH)
                            .filter(p -> Arrays.stream(imageExtensions).anyMatch(p.toString()::endsWith))
                            .map(p -> new File(p.toString()))
                            .collect(Collectors.toList())
            );
        }
        catch(SecurityException exception){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error" + exception.toString());
            alert.setHeaderText("Cannot open folder.");
            alert.setContentText("Please chose another folder!");
            alert.showAndWait();
        }
        catch(IOException | UncheckedIOException exception){
            System.err.println(exception.getClass());
        }

        fileIndex = new SimpleIntegerProperty(0);
        imageFileListSize = Bindings.size(imageFileList);
        hasNextFile = fileIndex.lessThan(imageFileListSize.subtract(1));
        hasPreviousFile = fileIndex.greaterThan(0);
        currentFile = Bindings.valueAt(imageFileList, fileIndex);
    }

    public Image getCurrentImage(){
        return getImageFromFile(currentFile.get());
    }

    public IntegerProperty fileIndexProperty(){
        return fileIndex;
    }

    public void incrementFileIndex(){
        fileIndex.set(fileIndex.get() + 1);
    }

    public void decrementFileIndex(){
        fileIndex.set(fileIndex.get() - 1);
    }

    public IntegerBinding getFileListSizeBinding(){
        return imageFileListSize;
    }

    public BooleanBinding hasNextFileBinding(){
        return hasNextFile;
    }

    public BooleanBinding hasPreviousFileBinding(){
        return hasPreviousFile;
    }

    private Image getImageFromFile(File file){
        return new Image(file.toURI().toString());
    }
}
