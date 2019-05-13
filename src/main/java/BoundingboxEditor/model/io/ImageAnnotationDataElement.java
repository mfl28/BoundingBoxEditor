package BoundingboxEditor.model.io;

import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.ui.BoundingBoxView;

import java.nio.file.Path;
import java.nio.file.Paths;
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
            boundingBoxData.add(BoundingBoxData.fromSelectionRectangle(item));
        }

        return new ImageAnnotationDataElement(boundingBoxViews.iterator().next().getImageMetaData(), boundingBoxData);

    }

    public Path getImagePath() {
        return Paths.get(imageMetaData.getImageFilePath().replace("file:/C:", ""));
    }

    public double getImageWidth() {
        return imageMetaData.getImageWidth();
    }

    public double getImageHeight() {
        return imageMetaData.getImageHeight();
    }

    public List<BoundingBoxData> getBoundingBoxes() {
        return boundingBoxes;
    }

    public String getContainingFolderName() {
        return getImagePath().getParent().toString();
    }

    public String getImageFileName() {
        return getImagePath().getFileName().toString();
    }

    public ImageMetaData getImageMetaData() {
        return imageMetaData;
    }
}
