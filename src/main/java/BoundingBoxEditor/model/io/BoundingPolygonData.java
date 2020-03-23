package boundingboxeditor.model.io;

import boundingboxeditor.model.ObjectCategory;

import java.util.List;

/**
 * Represents the data-component of a bounding-polygon. Objects of this class are used to
 * store the "blueprint" of the ui-component of a {@link boundingboxeditor.ui.BoundingPolygonView}-object).
 * BoundingPolygonData-objects are part of the model-component of the app and can be nested by adding child BoundingShapeData-objects as "parts" of
 * a parent object.
 *
 * @see BoundingShapeData#setParts(List)
 */
public class BoundingPolygonData extends BoundingShapeData {
    private final List<Double> pointsInImage;

    public BoundingPolygonData(ObjectCategory category, List<Double> points, List<String> tags) {
        super(category, tags);
        this.pointsInImage = points;
    }

    public List<Double> getPointsInImage() {
        return pointsInImage;
    }

    @Override
    public <T> T accept(BoundingShapeDataVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
