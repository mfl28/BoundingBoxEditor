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

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/***
 * A class that comprises mathematical utility-functions.
 */
public class MathUtils {
    public static final double DOUBLE_EQUAL_THRESHOLD = 1e-8;

    private MathUtils() {
        throw new IllegalStateException("MathUtils class");
    }

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
        return new Point2D(Math.clamp(x, bounds.getMinX(), bounds.getMaxX()),
                           Math.clamp(y, bounds.getMinY(), bounds.getMaxY()));
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

    /**
     * Checks whether a double value is an element of a given closed interval.
     *
     * @param x the value to check
     * @param a the lower bound of the interval
     * @param b the upper bound of the interval
     * @return true if x is within [a, b], false otherwise
     */
    public static boolean isWithin(double x, double a, double b) {
        return Double.compare(x, a) >= 0 && Double.compare(x, b) <= 0.0;
    }
}
