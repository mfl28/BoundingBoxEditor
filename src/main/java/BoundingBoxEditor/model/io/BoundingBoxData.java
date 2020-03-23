package boundingboxeditor.model.io;

import boundingboxeditor.model.ObjectCategory;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

import java.util.List;

/**
 * Represents the data-component of a bounding-box. Objects of this class are used to
 * store the "blueprint" of the ui-component of a bounding-box (a {@link boundingboxeditor.ui.BoundingBoxView BoundingBoxView}-object).
 * BoundingBoxData-objects are part of the model-component of the app and can be nested by adding child BoundingShapeData-objects as "parts" of
 * a parent object.
 *
 * @see BoundingShapeData#setParts(List)
 */
public class BoundingBoxData extends BoundingShapeData {
    private final Bounds boundsInImage;

    /**
     * Creates a new object to store the "blueprint" of a {@link boundingboxeditor.ui.BoundingBoxView BoundingBoxView}.
     *
     * @param category        the category of the bounding-box
     * @param rectangleBounds the rectangular bounds of the bounding-box with respect to the image it belongs to
     *                        (considering the image's original measurements)
     * @param tags            the tags that are registered for the bounding-box
     */
    public BoundingBoxData(ObjectCategory category, Bounds rectangleBounds, List<String> tags) {
        super(category, tags);
        this.boundsInImage = rectangleBounds;
    }

    /**
     * Creates a new object to store the "blueprint" of a {@link boundingboxeditor.ui.BoundingBoxView BoundingBoxView}.
     * All coordinate arguments are considered to be with respect to the image the bounding-box belongs to
     * (considering the image's original measurements).
     *
     * @param category the category of the bounding-box
     * @param xMin     the x-coordinate of the upper left corner of the bounding-box
     * @param yMin     the y-coordinate of the upper left corner of the bounding-box
     * @param xMax     the x-coordinate of the lower right corner of the bounding-box
     * @param yMax     the y-coordinate of the lower right corner of the bounding-box
     * @param tags     the tags that are registered for the bounding-box
     */
    BoundingBoxData(ObjectCategory category, double xMin, double yMin, double xMax, double yMax, List<String> tags) {
        super(category, tags);
        this.boundsInImage = new BoundingBox(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    /**
     * Returns the x-coordinate of the upper left corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the x-coordinate
     */
    public double getXMin() {
        return boundsInImage.getMinX();
    }

    /**
     * Returns the y-coordinate of the upper left corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the y-coordinate
     */
    public double getYMin() {
        return boundsInImage.getMinY();
    }

    /**
     * Returns the x-coordinate of the lower right corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the x-coordinate
     */
    public double getXMax() {
        return boundsInImage.getMaxX();
    }

    /**
     * Returns the y-coordinate of the lower right corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the y-coordinate
     */
    public double getYMax() {
        return boundsInImage.getMaxY();
    }

    /**
     * Returns the bounds of the bounding-box with respect to the image
     * it belongs to (considering the image's original measurements).
     *
     * @return the bounds in the coordinate system defined by the originally loaded image.
     */
    public Bounds getBoundsInImage() {
        return boundsInImage;
    }

    @Override
    public <T> T accept(BoundingShapeDataVisitor<T> visitor) {
        return visitor.visit(this);
    }
}


