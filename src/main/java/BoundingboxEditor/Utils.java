package BoundingboxEditor;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import java.util.Random;


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

    // source: https://stackoverflow.com/questions/44331780/javafx-color-parsing
    public static String rgbaFromColor(Color color) {
        return String.format("rgba(%d, %d, %d, %f)",
                (int) (255 * color.getRed()),
                (int) (255 * color.getGreen()),
                (int) (255 * color.getBlue()),
                color.getOpacity());
    }

    public static Color createRandomColor(Random random) {
        return Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }

    public static Button createIconButton(final String iconPath, double fitWidth, double fitHeight) {
        final Button button = new Button();
        final ImageView iconView = new ImageView(iconPath);

        iconView.setFitWidth(fitWidth);
        iconView.setFitHeight(fitHeight);
        iconView.setPreserveRatio(true);
        button.setGraphic(iconView);
        button.setFocusTraversable(false);

        return button;
    }

    public static Pane createHSpacer() {
        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
