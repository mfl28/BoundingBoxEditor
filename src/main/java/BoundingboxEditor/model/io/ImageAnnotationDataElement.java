package BoundingboxEditor.model.io;

import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.ui.BoundingBoxView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ImageAnnotationDataElement {
    private final ImageMetaData imageMetaData;
    private final List<BoundingBoxData> boundingBoxes;

    ImageAnnotationDataElement(final ImageMetaData imageMetaData, final List<BoundingBoxData> boundingBoxes) {
        this.imageMetaData = imageMetaData;
        this.boundingBoxes = boundingBoxes;
    }

    public static ImageAnnotationDataElement fromBoundingBoxes(final Collection<BoundingBoxView> boundingBoxViews) {
        if(boundingBoxViews.isEmpty()) {
            return null;
        }

        final List<BoundingBoxData> boundingBoxData = new ArrayList<>(boundingBoxViews.size());

        for(BoundingBoxView item : boundingBoxViews) {
            boundingBoxData.add(BoundingBoxData.fromBoundingBoxView(item));
        }

        return new ImageAnnotationDataElement(boundingBoxViews.iterator().next().getImageMetaData(), boundingBoxData);

    }

    public List<BoundingBoxData> getBoundingBoxes() {
        return boundingBoxes;
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
