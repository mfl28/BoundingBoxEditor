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
    private final List<BoundingBoxElement> boundingBoxes;

    ImageAnnotationDataElement(final ImageMetaData imageMetaData, final List<BoundingBoxElement> boundingBoxes) {
        this.imageMetaData = imageMetaData;
        this.boundingBoxes = boundingBoxes;
    }

    public static ImageAnnotationDataElement fromBoundingBoxes(final Collection<BoundingBoxView> boundingBoxViews) {
        if(boundingBoxViews.isEmpty()) {
            return null;
        }

        final List<BoundingBoxElement> boundingBoxElements = new ArrayList<>(boundingBoxViews.size());

        for(BoundingBoxView item : boundingBoxViews) {
            boundingBoxElements.add(BoundingBoxElement.fromSelectionRectangle(item));
        }

        return new ImageAnnotationDataElement(boundingBoxViews.iterator().next().getImageMetaData(), boundingBoxElements);

    }

    Path getImagePath() {
        return Paths.get(imageMetaData.getFilePath().replace("file:/C:", ""));
    }

    double getImageWidth() {
        return imageMetaData.getImageWidth();
    }

    double getImageHeight() {
        return imageMetaData.getImageHeight();
    }

    List<BoundingBoxElement> getBoundingBoxes() {
        return boundingBoxes;
    }

    String getContainingFolderName() {
        return getImagePath().getParent().toString();
    }

    String getImageFileName() {
        return getImagePath().getFileName().toString();
    }
}
