package BoundingboxEditor.model.io;

import BoundingboxEditor.ui.BoundingBoxView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

public class BoundingBoxData {
    private final String categoryName;
    private final Bounds boundsInImage;
    private final ObservableList<String> tags = FXCollections.observableArrayList();

    // TODO: Optionally add attributes: pose (Front, back, right etc.), truncated(0 or 1), difficult(0 or 1)
    //       occluded (0 or 1)

    public BoundingBoxData(String categoryName, double xMin, double yMin, double xMax, double yMax) {
        this.categoryName = categoryName;
        this.boundsInImage = new BoundingBox(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    public BoundingBoxData(String categoryName, Bounds rectangleBounds) {
        this.categoryName = categoryName;
        this.boundsInImage = rectangleBounds;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public double getXMin() {
        return boundsInImage.getMinX();
    }

    public double getYMin() {
        return boundsInImage.getMinY();
    }

    public double getXMax() {
        return boundsInImage.getMaxX();
    }

    public double getYMax() {
        return boundsInImage.getMaxY();
    }

    public Bounds getBoundsInImage() {
        return boundsInImage;
    }

    public ObservableList<String> getTags() {
        return tags;
    }

    static BoundingBoxData fromBoundingBoxView(final BoundingBoxView boundingBoxView) {
        return new BoundingBoxData(boundingBoxView.getBoundingBoxCategory().getName(), boundingBoxView.getImageRelativeBounds());
    }
}


