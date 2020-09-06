package boundingboxeditor.utils;

import javafx.scene.paint.Color;

import java.util.Random;

/***
 * A class that contains color-related utility-functions.
 */
public class ColorUtils {
    private static final Random random = new Random();

    private ColorUtils() {
        throw new IllegalStateException("ColorUtils class");
    }

    /***
     * Creates random color.
     * @return the random color
     */
    public static Color createRandomColor() {
        return Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }

    /***
     * Converts a color to hexadecimal string.
     * @param color The color
     * @return the hexadecimal representation of the color
     *
     * Source: https://stackoverflow.com/a/56733608/13302206
     */
    public static String colorToHexString(Color color) {
        return "#" + (format(color.getRed()) + format(color.getGreen()) + format(color.getBlue()))
                .toUpperCase();
    }

    /**
     * Source: https://stackoverflow.com/a/56733608/13302206
     */
    private static String format(double value) {
        String hexString = Integer.toHexString((int) Math.round(value * 255));
        return hexString.length() == 1 ? "0" + hexString : hexString;
    }
}
