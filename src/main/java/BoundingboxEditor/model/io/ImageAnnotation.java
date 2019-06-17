package BoundingboxEditor.model.io;

import BoundingboxEditor.model.ImageMetaData;

import java.util.List;

/**
 * A class holding all data of an image-annotation, including metadata of the image and all {@link BoundingBoxData}-objects assigned to it.
 * There will be at most one ImageAnnotation-object for each loaded image.
 */
public class ImageAnnotation {
    private final ImageMetaData imageMetaData;
    private List<BoundingBoxData> boundingBoxData;

    /**
     * Creates a new image-annotation
     *
     * @param imageMetaData   the metadata of the annotated image
     * @param boundingBoxData the data of the bounding-boxes assigned to the image
     */
    public ImageAnnotation(ImageMetaData imageMetaData, List<BoundingBoxData> boundingBoxData) {
        this.imageMetaData = imageMetaData;
        this.boundingBoxData = boundingBoxData;
    }

    /**
     * Returns the annotation's bounding-box data.
     *
     * @return list of data of bounding-boxes
     */
    public List<BoundingBoxData> getBoundingBoxData() {
        return boundingBoxData;
    }

    /**
     * Sets the annotation's bounding-box data.
     *
     * @param boundingBoxData the list of data of bounding-boxes to set
     */
    public void setBoundingBoxData(List<BoundingBoxData> boundingBoxData) {
        this.boundingBoxData = boundingBoxData;
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
