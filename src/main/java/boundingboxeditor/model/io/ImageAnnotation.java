package boundingboxeditor.model.io;

import boundingboxeditor.model.ImageMetaData;

import java.util.ArrayList;
import java.util.List;

/**
 * A class holding all data of an image-annotation, including metadata of the image and all {@link BoundingShapeData}-objects assigned to it.
 * There will be at most one ImageAnnotation-object for each loaded image.
 */
public class ImageAnnotation {
    private final ImageMetaData imageMetaData;
    private List<BoundingShapeData> boundingShapeData = new ArrayList<>();

    /**
     * Creates a new image-annotation
     *
     * @param imageMetaData the metadata of the annotated image
     */
    public ImageAnnotation(ImageMetaData imageMetaData) {
        this.imageMetaData = imageMetaData;
    }

    /**
     * Creates a new image-annotation
     *
     * @param imageMetaData the metadata of the annotated image
     */
    public ImageAnnotation(ImageMetaData imageMetaData, List<BoundingShapeData> boundingShapeData) {
        this.imageMetaData = imageMetaData;
        this.boundingShapeData = boundingShapeData;
    }


    /**
     * Returns the annotation's bounding-shape data.
     *
     * @return list of data of bounding-shapes
     */
    public List<BoundingShapeData> getBoundingShapeData() {
        return boundingShapeData;
    }

    /**
     * Sets the annotation's bounding-shape data.
     *
     * @param boundingShapeData the list of data of bounding-shapes to set
     */
    public void setBoundingShapeData(List<BoundingShapeData> boundingShapeData) {
        this.boundingShapeData = boundingShapeData;
    }

    /**
     * Returns metadata of the annotated image-file.
     *
     * @return the image metadata
     */
    public ImageMetaData getImageMetaData() {
        return imageMetaData;
    }

    /**
     * Returns the name of the annotated image-file.
     *
     * @return the filename
     */
    public String getImageFileName() {
        return imageMetaData.getFileName();
    }

    /**
     * Returns the width of the annotated image.
     *
     * @return the width of the image
     */
    double getImageWidth() {
        return imageMetaData.getImageWidth();
    }

    /**
     * Returns the height of the annotated image.
     *
     * @return the height of the image
     */
    double getImageHeight() {
        return imageMetaData.getImageHeight();
    }

    /**
     * Returns the depth (= number of channels) of the annotated image.
     *
     * @return the depth of the image
     */
    int getImageDepth() {
        return imageMetaData.getImageDepth();
    }

    /**
     * Returns the name of the annotated image's containing folder.
     *
     * @return the folder-name
     */
    String getContainingFolderName() {
        return imageMetaData.getFolderName();
    }
}
