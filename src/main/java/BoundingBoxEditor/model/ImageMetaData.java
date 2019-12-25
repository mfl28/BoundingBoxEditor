package BoundingBoxEditor.model;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Holds metadata information about an image.
 */
public class ImageMetaData {
    private static final Logger log = Logger.getLogger(ImageMetaData.class.getName());
    private String fileName;
    private String folderName;
    private double imageWidth;
    private double imageHeight;
    private int imageDepth;

    /**
     * Creates a new ImageMetaData object.
     *
     * @param fileName    the name of the image-file
     * @param folderName  the name of the folder containing the image-file
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     * @param imageDepth  the depth (= number of channels) of the image
     */
    public ImageMetaData(String fileName, String folderName, double imageWidth, double imageHeight, int imageDepth) {
        this.fileName = fileName;
        this.folderName = folderName;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.imageDepth = imageDepth;
    }

    /**
     * Constructs an ImageMetaData object from an image-file without loading the whole image.
     *
     * @param imageFile the image-file
     * @return an ImageMetaData object containing metadata about the provided image-file
     */
    public static ImageMetaData fromFile(File imageFile) {
        ImageDimensions imageDimensions = readImageDimensionsFromFile(imageFile);
        return new ImageMetaData(imageFile.getName(), imageFile.getParent(),
                imageDimensions.getWidth(), imageDimensions.getHeight(), imageDimensions.getDepth());
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
     * Returns the depth (= number of channels) of the image.
     *
     * @return the depth
     */
    public int getImageDepth() {
        return imageDepth;
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
    public int hashCode() {
        return Objects.hash(fileName, folderName, imageWidth, imageHeight, imageDepth);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ImageMetaData) {
            ImageMetaData other = (ImageMetaData) obj;
            return Objects.equals(fileName, other.fileName) && Objects.equals(folderName, other.folderName) &&
                    imageWidth == other.imageWidth && imageHeight == other.imageHeight && imageDepth == other.imageDepth;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ImageMetaData[fileName=" + getFileName() + ", folderName=" + getFolderName()
                + ", image-width=" + getImageWidth() + ", image-height=" + getImageHeight() + ", image-depth=" + getImageDepth();
    }

    private static ImageDimensions readImageDimensionsFromFile(File imageFile) {
        // Source: https://stackoverflow.com/a/1560052
        try(ImageInputStream imageStream = ImageIO.createImageInputStream(imageFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if(readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageStream);
                    return new ImageDimensions(
                            reader.getWidth(0),
                            reader.getHeight(0),
                            reader.getRawImageType(0).getNumComponents()
                    );
                } finally {
                    reader.dispose();
                }
            }
        } catch(IOException e) {
            log.severe("Could not open image-file " + imageFile.getName());
            return ImageDimensions.zeroDimensions();
        }

        log.severe("Could not read image-size data from file " + imageFile.getName());
        return ImageDimensions.zeroDimensions();
    }

    private static class ImageDimensions {
        private static ImageDimensions zero = new ImageDimensions(0, 0, 0);
        private final double width;
        private final double height;
        private final int depth;

        ImageDimensions(double width, double height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        static ImageDimensions zeroDimensions() {
            return zero;
        }

        double getWidth() {
            return width;
        }

        double getHeight() {
            return height;
        }

        int getDepth() {
            return depth;
        }
    }
}
