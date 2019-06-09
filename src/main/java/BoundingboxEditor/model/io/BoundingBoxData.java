package BoundingboxEditor.model.io;

import BoundingboxEditor.model.BoundingBoxCategory;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

import java.util.Collections;
import java.util.List;

/**
 * Represents the data-component of a bounding-box. Objects of this class are used to
 * store the "blueprint" of the ui-component of a bounding-box (a {@link BoundingboxEditor.ui.BoundingBoxView BoundingBoxView}-object).
 * BoundingBoxData-objects are part of the model-component of the app and can be nested by adding child BoundinBoxData-objects as "parts" of
 * a parent object.
 *
 * @see BoundingBoxData#setParts(List)
 */
public class BoundingBoxData {
    private final BoundingBoxCategory category;
    private final Bounds boundsInImage;
    private final List<String> tags;
    private List<BoundingBoxData> parts = Collections.emptyList();

    /**
     * Creates a new object to store the "blueprint" of a {@link BoundingboxEditor.ui.BoundingBoxView BoundingBoxView}.
     *
     * @param category        the category of the bounding-box
     * @param rectangleBounds the rectangular bounds of the bounding-box with respect to the image it belongs to
     *                        (considering the image's original measurements)
     * @param tags            the tags that are registered for the bounding-box
     */
    public BoundingBoxData(BoundingBoxCategory category, Bounds rectangleBounds, List<String> tags) {
        this.category = category;
        this.boundsInImage = rectangleBounds;
        this.tags = tags;
    }

    /**
     * Creates a new object to store the "blueprint" of a {@link BoundingboxEditor.ui.BoundingBoxView BoundingBoxView}.
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
    BoundingBoxData(BoundingBoxCategory category, double xMin, double yMin, double xMax, double yMax, List<String> tags) {
        this.category = category;
        this.boundsInImage = new BoundingBox(xMin, yMin, xMax - xMin, yMax - yMin);
        this.tags = tags;
    }

    /**
     * Returns the name of the category of the bounding-box.
     *
     * @return the category name
     */
    public String getCategoryName() {
        return category.getName();
    }

    /**
     * Returns the category of the bounding-box.
     *
     * @return the category
     */
    public BoundingBoxCategory getCategory() {
        return category;
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

    /**
     * Returns a list of the tags that are registered with the bounding-box.
     *
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Returns the {@link BoundingBoxData}-objects that are registered as nested parts of the bounding-box.
     *
     * @return the parts
     */
    public List<BoundingBoxData> getParts() {
        return parts;
    }

    /**
     * Registers {@link BoundingBoxData}-objects as nested parts of the bounding-box.
     *
     * @param parts the BoundingBoxData-objects to be registered
     */
    public void setParts(List<BoundingBoxData> parts) {
        this.parts = parts;
    }
}


