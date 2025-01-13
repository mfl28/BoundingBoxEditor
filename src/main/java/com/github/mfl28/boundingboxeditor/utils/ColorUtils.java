/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.utils;

import javafx.scene.paint.Color;

import java.util.Locale;
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
     * Source: <a href="https://stackoverflow.com/a/56733608/13302206">...</a>
     */
    public static String colorToHexString(Color color) {
        return "#" + (format(color.getRed()) + format(color.getGreen()) + format(color.getBlue()))
                .toUpperCase(Locale.ENGLISH);
    }

    /**
     * Source: <a href="https://stackoverflow.com/a/56733608/13302206">...</a>
     */
    private static String format(double value) {
        String hexString = Integer.toHexString((int) Math.round(value * 255));
        return hexString.length() == 1 ? "0" + hexString : hexString;
    }
}
