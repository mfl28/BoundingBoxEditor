import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;


public class Utils {
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static Point2D clampWithinBounds(Point2D point, Bounds bounds) {
        return new Point2D(clamp(point.getX(), bounds.getMinX(), bounds.getMaxX()),
                clamp(point.getY(), bounds.getMinY(), bounds.getMaxY()));
    }

    public static Point2D clampWithinBounds(MouseEvent event, Bounds bounds) {
        return new Point2D(clamp(event.getX(), bounds.getMinX(), bounds.getMaxX()),
                clamp(event.getY(), bounds.getMinY(), bounds.getMaxY()));
    }

    public static String filenameFromUrl(String url) {
        return url.substring(Math.max(url.lastIndexOf("/"), url.lastIndexOf("\\")) + 1,
                url.lastIndexOf("."));
    }
}
