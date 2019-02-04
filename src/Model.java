import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Model {
    private ObservableList<File> imageFileList;
    private IntegerProperty fileIndex;
    private IntegerBinding imageFileListSize;
    private BooleanBinding hasNextFile;
    private BooleanBinding hasPreviousFile;
    private ObjectBinding<File> currentFile;
    private ObservableMap<String, List<Double>> boundingBoxData;

    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;

    public Model(){
        fileIndex = new SimpleIntegerProperty(0);
        boundingBoxData = FXCollections.observableMap(new LinkedHashMap<>());
    }

    public void setImageFileList(Path path) throws Exception{
        imageFileList = FXCollections.observableArrayList(
                Files.walk(path, MAX_DIRECTORY_DEPTH)
                        .filter(p -> Arrays.stream(imageExtensions).anyMatch(p.toString()::endsWith))
                        .map(p -> new File(p.toString()))
                        .collect(Collectors.toList())
        );

        fileIndex.set(0);
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

    public ObservableMap<String, List<Double>> getBoundingBoxData() {
        return boundingBoxData;
    }

    void writeBoundingBoxDataToFile(File file) throws IOException {
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(file))) {
            boundingBoxData.forEach((key, value) -> printWriter.write(key + ", " +
                        value.stream().map(d -> d.toString()).collect(Collectors.joining(", "))));
        }
    }

    private Image getImageFromFile(File file){
        return new Image(file.toURI().toString());
    }
}
