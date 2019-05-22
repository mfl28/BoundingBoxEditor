package BoundingboxEditor.model.io;

import BoundingboxEditor.model.ImageMetaData;

import java.util.List;

public class ImageAnnotationDataElement {
    private final ImageMetaData imageMetaData;
    private List<BoundingBoxData> boundingBoxes;

    public ImageAnnotationDataElement(final ImageMetaData imageMetaData, final List<BoundingBoxData> boundingBoxes) {
        this.imageMetaData = imageMetaData;
        this.boundingBoxes = boundingBoxes;
    }

    public List<BoundingBoxData> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void setBoundingBoxes(List<BoundingBoxData> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

    public String getImageFileName() {
        return imageMetaData.getFileName();
    }

    public ImageMetaData getImageMetaData() {
        return imageMetaData;
    }

    double getImageWidth() {
        return imageMetaData.getImageWidth();
    }

    double getImageHeight() {
        return imageMetaData.getImageHeight();
    }

    String getContainingFolderName() {
        return imageMetaData.getFolderName();
    }
}
