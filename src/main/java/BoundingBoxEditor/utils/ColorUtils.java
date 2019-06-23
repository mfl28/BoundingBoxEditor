package BoundingBoxEditor.utils;

import javafx.scene.paint.Color;

import java.util.Random;

/***
 * A class that contains color-related utility-functions.
 */
public class ColorUtils {
    private static final Random random = new Random();

    /***
     * Creates random color.
     * @return the random color
     */
    public static Color createRandomColor() {
        return Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }
}
