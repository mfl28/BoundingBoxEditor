package BoundingboxEditor.model;

import javafx.scene.image.Image;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Holds metadata information about an image.
 */
public class ImageMetaData {
    private String fileName;
    private String folderName;
    private double imageWidth;
    private double imageHeight;

    /**
     * Creates a new ImageMetaData object.
     *
     * @param fileName    the name of the image-file
     * @param folderName  the name of the folder containing the image-file
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     */
    public ImageMetaData(String fileName, String folderName, double imageWidth, double imageHeight) {
        this.fileName = fileName;
        this.folderName = folderName;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    /**
     * Constructs an ImageMetaData object from an {@link Image} object.
     *
     * @param image the image
     * @return an ImageMetaData object containing metadata about the provided image
     */
    public static ImageMetaData fromImage(Image image) {
        Path path = Paths.get(URI.create(image.getUrl()));
        return new ImageMetaData(path.getFileName().toString(),
                path.getParent().toString(), image.getWidth(), image.getHeight());
    }

    /**
     * Returns the width of the image.
     *
     * @return the width
     */
    public double getImageWidth() {
        return imageWidth;
    }

    /**
     * Returns the height of the image.
     *
     * @return the height
     */
    public double getImageHeight() {
        return imageHeight;
    }

    /**
     * Returns the filename of the image.
     *
     * @return the filename
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the name of the folder that contains the image.
     *
     * @return the folder-name
     */
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
