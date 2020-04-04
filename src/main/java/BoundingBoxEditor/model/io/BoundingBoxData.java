package boundingboxeditor.model.io;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.ui.BoundingBoxView;
import boundingboxeditor.ui.BoundingShapeViewable;
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
    private final Bounds relativeBoundsInImage;

    /**
     * Creates a new object to store the "blueprint" of a {@link boundingboxeditor.ui.BoundingBoxView BoundingBoxView}.
     *
     * @param category        the category of the bounding-box
     * @param relativeBoundsInImage the rectangular bounds of the bounding-box in relative coordinates
     * @param tags            the tags that are registered for the bounding-box
     */
    public BoundingBoxData(ObjectCategory category, Bounds relativeBoundsInImage, List<String> tags) {
        super(category, tags);
        this.relativeBoundsInImage = relativeBoundsInImage;
    }

    /**
     * Creates a new object to store the "blueprint" of a {@link boundingboxeditor.ui.BoundingBoxView BoundingBoxView}.
     * All coordinate arguments are considered to be with respect to the image the bounding-box belongs to
     * (considering the image's original measurements).
     *
     * @param category the category of the bounding-box
     * @param xMinRelative     the x-coordinate of the upper left corner of the bounding-box
     * @param yMinRelative     the y-coordinate of the upper left corner of the bounding-box
     * @param xMaxRelative     the x-coordinate of the lower right corner of the bounding-box
     * @param yMaxRelative     the y-coordinate of the lower right corner of the bounding-box
     * @param tags     the tags that are registered for the bounding-box
     */
    BoundingBoxData(ObjectCategory category, double xMinRelative, double yMinRelative,
                    double xMaxRelative, double yMaxRelative, List<String> tags) {
        super(category, tags);
        this.relativeBoundsInImage = new BoundingBox(xMinRelative, yMinRelative,
                xMaxRelative - xMinRelative, yMaxRelative - yMinRelative);
    }

    /**
     * Returns the x-coordinate of the upper left corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the x-coordinate
     */
    public double getXMinRelative() {
        return relativeBoundsInImage.getMinX();
    }

    /**
     * Returns the y-coordinate of the upper left corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the y-coordinate
     */
    public double getYMinRelative() {
        return relativeBoundsInImage.getMinY();
    }

    /**
     * Returns the x-coordinate of the lower right corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the x-coordinate
     */
    public double getXMaxRelative() {
        return relativeBoundsInImage.getMaxX();
    }

    /**
     * Returns the y-coordinate of the lower right corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the y-coordinate
     */
    public double getYMaxRelative() {
        return relativeBoundsInImage.getMaxY();
    }

    /**
     * Returns the relative bounds of the bounding-box.
     *
     * @return the bounds in the coordinate system defined by the originally loaded image.
     */
    public Bounds getRelativeBoundsInImage() {
        return relativeBoundsInImage;
    }

    public Bounds getAbsoluteBoundsInImage(ImageMetaData metaData) {
            double xMin = getXMinRelative() * metaData.getImageWidth();
            double yMin = getYMinRelative() * metaData.getImageHeight();
            double xMax = getXMaxRelative() * metaData.getImageWidth();
            double yMax = getYMaxRelative() * metaData.getImageHeight();

            return new BoundingBox(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    @Override
    public BoundingShapeViewable toBoundingShapeView(ImageMetaData metaData) {
        return BoundingBoxView.fromData(this, metaData);
    }

    @Override
    public <T> T accept(BoundingShapeDataVisitor<T> visitor) {
        return visitor.visit(this);
    }
}


