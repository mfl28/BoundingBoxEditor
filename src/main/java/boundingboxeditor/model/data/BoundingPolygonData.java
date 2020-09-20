package boundingboxeditor.model.data;

import boundingboxeditor.ui.BoundingPolygonView;
import boundingboxeditor.ui.BoundingShapeViewable;
import boundingboxeditor.utils.MathUtils;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public List<Double> getAbsolutePointsInImage(double imageWidth, double imageHeight) {
        return BoundingPolygonData.relativeToAbsolutePoints(relativePointsInImage,
                                                            imageWidth,
                                                            imageHeight);
    }

    @Override
    public BoundingShapeViewable toBoundingShapeView(double imageWidth, double imageHeight) {
        return BoundingPolygonView.fromData(this, imageWidth, imageHeight);
    }

    @Override
    public <T> T accept(BoundingShapeDataVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relativePointsInImage);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(!(o instanceof BoundingPolygonData)) {
            return false;
        }

        if(!super.equals(o)) {
            return false;
        }

        BoundingPolygonData that = (BoundingPolygonData) o;

        if(relativePointsInImage == that.relativePointsInImage) {
            return true;
        }

        if(relativePointsInImage.size() != that.relativePointsInImage.size()) {
            return false;
        }

        for(int i = 0; i != relativePointsInImage.size(); ++i) {
            if(!MathUtils.doubleAlmostEqual(relativePointsInImage.get(i), that.relativePointsInImage.get(i))) {
                return false;
            }
        }

        return true;
    }
}
