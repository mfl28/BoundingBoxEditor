package BoundingboxEditor.model.io;

import BoundingboxEditor.model.BoundingBoxCategory;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

import java.util.List;

public class BoundingBoxData {
    private final BoundingBoxCategory category;
    private final Bounds boundsInImage;
    private final List<String> tags;

    // TODO: Optionally add attributes: pose (Front, back, right etc.), truncated(0 or 1), difficult(0 or 1)
    //       occluded (0 or 1)

    public BoundingBoxData(BoundingBoxCategory category, double xMin, double yMin, double xMax, double yMax, List<String> tags) {
        this.category = category;
        this.boundsInImage = new BoundingBox(xMin, yMin, xMax - xMin, yMax - yMin);
        this.tags = tags;
    }

    public BoundingBoxData(BoundingBoxCategory category, Bounds rectangleBounds, List<String> tags) {
        this.category = category;
        this.boundsInImage = rectangleBounds;
        this.tags = tags;
    }

    public String getCategoryName() {
        return category.getName();
    }

    public BoundingBoxCategory getCategory() {
        return category;
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

    public List<String> getTags() {
        return tags;
    }
}


