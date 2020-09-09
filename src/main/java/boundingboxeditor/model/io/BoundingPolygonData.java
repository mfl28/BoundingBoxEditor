package boundingboxeditor.model.io;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.ui.BoundingPolygonView;
import boundingboxeditor.ui.BoundingShapeViewable;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
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
    @SerializedName("polygon")
    private final List<Double> relativePointsInImage;

    public BoundingPolygonData(ObjectCategory category, List<Double> points, List<String> tags) {
        super(category, tags);
        this.relativePointsInImage = points;
    }

    public static List<Double> absoluteToRelativePoints(List<Double> absolutePoints, double width, double height) {
        List<Double> relativePoints = new ArrayList<>(absolutePoints.size());

        for(int i = 0; i < absolutePoints.size(); i += 2) {
            relativePoints.add(absolutePoints.get(i) / width);
            relativePoints.add(absolutePoints.get(i + 1) / height);
        }

        return relativePoints;
    }

    public static List<Double> relativeToAbsolutePoints(List<Double> relativePoints, double width, double height) {
        List<Double> absolutePoints = new ArrayList<>(relativePoints.size());

        for(int i = 0; i < relativePoints.size(); i += 2) {
            absolutePoints.add(relativePoints.get(i) * width);
            absolutePoints.add(relativePoints.get(i + 1) * height);
        }

        return absolutePoints;
    }

    public List<Double> getRelativePointsInImage() {
        return relativePointsInImage;
    }

    public List<Double> getAbsolutePointsInImage(ImageMetaData imageMetaData) {
        return BoundingPolygonData.relativeToAbsolutePoints(relativePointsInImage,
                                                            imageMetaData.getImageWidth(),
                                                            imageMetaData.getImageHeight());
    }

    @Override
    public BoundingShapeViewable toBoundingShapeView(ImageMetaData metaData) {
        return BoundingPolygonView.fromData(this, metaData);
    }

    @Override
    public <T> T accept(BoundingShapeDataVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
