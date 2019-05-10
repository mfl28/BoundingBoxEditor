package BoundingboxEditor.model;

import javafx.scene.image.Image;

import java.util.Objects;

public class ImageMetaData {
    private String imageFilePath;
    private double imageWidth;
    private double imageHeight;

    public ImageMetaData(String imageFilePath, double imageWidth, double imageHeight) {
        this.imageFilePath = imageFilePath;
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

    public String getImageFilePath() {
        return imageFilePath;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ImageMetaData) {
            return Objects.equals(imageFilePath, ((ImageMetaData) obj).imageFilePath);
        }
        return false;
    }
}
