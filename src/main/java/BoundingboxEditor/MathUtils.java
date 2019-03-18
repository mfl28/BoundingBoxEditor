package BoundingboxEditor;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

public class MathUtils {
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static Point2D clampWithinBounds(double x, double y, Bounds bounds) {
        return new Point2D(clamp(x,bounds.getMinX(), bounds.getMaxX()),
                clamp(y, bounds.getMinY(), bounds.getMaxY()));
    }

    public static Point2D clampWithinBounds(Point2D point, Bounds bounds) {
        return clampWithinBounds(point.getX(), point.getY(), bounds);
    }


}
