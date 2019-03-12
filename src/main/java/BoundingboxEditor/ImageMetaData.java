package BoundingboxEditor;

import javafx.scene.image.Image;

public class ImageMetaData {
    private String filePath;
    private double imageWidth;
    private double imageHeight;
    private double imageDepth;

    private ImageMetaData(String filePath, double imageWidth, double imageHeight, double imageDepth) {
        this.filePath = filePath;
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
        this.imageDepth = imageDepth;
    }

    public static ImageMetaData fromImage(final Image image) {
        return new ImageMetaData(image.getUrl(), image.getWidth(), image.getHeight(), 0);
    }

    public double getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(double imageWidth) {
        this.imageWidth = imageWidth;
    }

    public double getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(double imageHeight) {
        this.imageHeight = imageHeight;
    }

    public double getImageDepth() {
        return imageDepth;
    }

    public void setImageDepth(double imageDepth) {
        this.imageDepth = imageDepth;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
