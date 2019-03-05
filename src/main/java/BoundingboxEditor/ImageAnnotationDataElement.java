package BoundingboxEditor;

import java.nio.file.Path;
import java.util.List;

public class ImageAnnotationDataElement {
    private final Path imagePath;
    private final double imageWidth;
    private final double imageHeight;
    private final double imageDepth;
    private final List<BoundingBoxElement> boundingBoxes;

    public ImageAnnotationDataElement(final Path imagePath, double imageWidth, double imageHeight, double imageDepth, final List<BoundingBoxElement> boundingBoxes) {
        this.imagePath = imagePath;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.imageDepth = imageDepth;
        this.boundingBoxes = boundingBoxes;
    }

    public Path getImagePath() {
        return imagePath;
    }

    public double getImageWidth() {
        return imageWidth;
    }

    public double getImageHeight() {
        return imageHeight;
    }

    public double getImageDepth() {
        return imageDepth;
    }

    public List<BoundingBoxElement> getBoundingBoxes() {
        return boundingBoxes;
    }

    public String getContainingFolderName(){
        return imagePath.getParent().toString();
    }

    public String getImageFileName(){
        return imagePath.getFileName().toString();
    }
}
