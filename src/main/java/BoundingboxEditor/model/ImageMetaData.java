package BoundingboxEditor.model;

import javafx.scene.image.Image;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ImageMetaData {
    private String fileName;
    private String folderName;
    private double imageWidth;
    private double imageHeight;

    public ImageMetaData(String fileName, String folderName, double imageWidth, double imageHeight) {
        this.fileName = fileName;
        this.folderName = folderName;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public static ImageMetaData fromImage(Image image) {
        Path path = Paths.get(URI.create(image.getUrl()));
        return new ImageMetaData(path.getFileName().toString(),
                path.getParent().toString(), image.getWidth(), image.getHeight());
    }

    public double getImageWidth() {
        return imageWidth;
    }

    public double getImageHeight() {
        return imageHeight;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFolderName() {
        return folderName;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ImageMetaData) {
            ImageMetaData other = (ImageMetaData) obj;
            return Objects.equals(fileName, other.fileName) && Objects.equals(folderName, other.folderName) &&
                    imageWidth == other.imageWidth && imageHeight == other.imageHeight;
        }
        return false;
    }
}
