package BoundingboxEditor.model.io;

import BoundingboxEditor.ui.BoundingBoxView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;

public class BoundingBoxData {
    private final String categoryName;
    private final double xMin;
    private final double yMin;
    private final double xMax;
    private final double yMax;
    private final ObservableList<String> tags = FXCollections.observableArrayList();

    // TODO: Optionally add attributes: pose (Front, back, right etc.), truncated(0 or 1), difficult(0 or 1)
    //       occluded (0 or 1)

    public BoundingBoxData(String categoryName, double xMin, double yMin, double xMax, double yMax) {
        this.categoryName = categoryName;
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public BoundingBoxData(String categoryName, Bounds rectangleBounds) {
        this.categoryName = categoryName;
        this.xMin = rectangleBounds.getMinX();
        this.yMin = rectangleBounds.getMinY();
        this.xMax = rectangleBounds.getMaxX();
        this.yMax = rectangleBounds.getMaxY();
    }

    public static BoundingBoxData fromSelectionRectangle(final BoundingBoxView boundingBoxView) {
        return new BoundingBoxData(boundingBoxView.getBoundingBoxCategory().getName(), boundingBoxView.getImageRelativeBounds());
    }

    public String getCategoryName() {
        return categoryName;
    }

    public double getXMin() {
        return xMin;
    }

    public double getYMin() {
        return yMin;
    }

    public double getXMax() {
        return xMax;
    }

    public double getYMax() {
        return yMax;
    }

    public ObservableList<String> getTags() {
        return tags;
    }
}


