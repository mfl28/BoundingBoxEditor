package BoundingboxEditor.utils;

import javafx.scene.paint.Color;

import java.util.Random;

public class ColorUtils {
    public static Color createRandomColor(Random random) {
        return Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }
}
