package boundingboxeditor.model.io;

import boundingboxeditor.model.ObjectCategory;

import java.util.List;

public class BoundingPolygonData extends BoundingShapeData {
    private final List<Double> pointsInImage;

    public BoundingPolygonData(ObjectCategory category, List<Double> points, List<String> tags) {
        super(category, tags);
        this.pointsInImage = points;
    }

    public List<Double> getPointsInImage() {
        return pointsInImage;
    }
}
