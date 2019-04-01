package BoundingboxEditor.model;

import javafx.scene.image.Image;

import java.util.Objects;

public class ImageMetaData {
    private String filePath;
    private double imageWidth;
    private double imageHeight;

    public ImageMetaData(String filePath, double imageWidth, double imageHeight) {
        this.filePath = filePath;
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
    }

    public static ImageMetaData fromImage(final Image image) {
        return new ImageMetaData(image.getUrl(), image.getWidth(), image.getHeight());
    }

    public double getImageWidth() {
        return imageWidth;
    }

    public double getImageHeight() {
        return imageHeight;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ImageMetaData) {
            return Objects.equals(filePath, ((ImageMetaData) obj).filePath);
        }
        return false;
    }
}
