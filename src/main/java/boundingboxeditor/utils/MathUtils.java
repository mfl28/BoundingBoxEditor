package boundingboxeditor.utils;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/***
 * A class that comprises mathematical utility-functions.
 */
public class MathUtils {
    private static final double DOUBLE_EQUAL_THRESHOLD = 1e-8;

    /***
     * Clamps a point 'point' inside the rectangle denoted by 'bounds'.
     * @param point the point to be clamped
     * @param bounds the bounds describing the rectangle
     * @return the clamped point
     */
    public static Point2D clampWithinBounds(Point2D point, Bounds bounds) {
        return clampWithinBounds(point.getX(), point.getY(), bounds);
    }

    /***
     * Clamps a point ('x', 'y') inside the rectangle denoted by 'bounds'.
     * @param x x-coordinate of the point
     * @param y y-coordinate of the point
     * @param bounds the bounds describing the rectangle
     * @return the clamped point
     */
    public static Point2D clampWithinBounds(double x, double y, Bounds bounds) {
        return new Point2D(clamp(x, bounds.getMinX(), bounds.getMaxX()),
                clamp(y, bounds.getMinY(), bounds.getMaxY()));
    }

    /***
     * Clamps a double value 'val' between the bounds 'min' and 'max'.
     * @param val the value to be clamped
     * @param min the lower bound
     * @param max the upper bound
     * @return clamped value
     */
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Compares two double values for equality using a predefined threshold for the precision.
     *
     * @param a the first value
     * @param b the second value
     * @return true if the two values are considered equal according to the
     * predefined precision, false otherwise
     */
    public static boolean doubleAlmostEqual(double a, double b) {
        return Math.abs(a - b) < DOUBLE_EQUAL_THRESHOLD;
    }

    private MathUtils() {
        throw new IllegalStateException("MathUtils class");
    }
}
