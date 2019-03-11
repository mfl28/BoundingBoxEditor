package BoundingboxEditor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ImageAnnotationDataElement {
    private final ImageMetaData imageMetaData;
    private final List<BoundingBoxElement> boundingBoxes;

    public static ImageAnnotationDataElement fromSelectionRectangles(final Collection<SelectionRectangle> selectionRectangles){
        if(selectionRectangles.isEmpty()) {
            return null;
        }

        final List<BoundingBoxElement> boundingBoxElements = new ArrayList<>(selectionRectangles.size());

        for(SelectionRectangle item : selectionRectangles){
            boundingBoxElements.add(BoundingBoxElement.fromSelectionRectangle(item));
        }

        return new ImageAnnotationDataElement(selectionRectangles.iterator().next().getImageMetaData(), boundingBoxElements);

    }

    private ImageAnnotationDataElement(final ImageMetaData imageMetaData, final List<BoundingBoxElement> boundingBoxes) {
        this.imageMetaData = imageMetaData;
        this.boundingBoxes = boundingBoxes;
    }

    public Path getImagePath() {
        return Paths.get(imageMetaData.getFilePath().replace("file:/C:", ""));
    }

    public double getImageWidth() {
        return imageMetaData.getImageWidth();
    }

    public double getImageHeight() {
        return imageMetaData.getImageHeight();
    }

    public double getImageDepth() {
        return imageMetaData.getImageDepth();
    }

    public List<BoundingBoxElement> getBoundingBoxes() {
        return boundingBoxes;
    }

    public String getContainingFolderName() {
        return getImagePath().getParent().toString();
    }

    public String getImageFileName() {
        return getImagePath().getFileName().toString();
    }
}
