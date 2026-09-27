/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An editable binary pixel mask covering a whole image, used to paint masks and to build them when importing.
 * {@link #toBitmap()} turns it into an immutable {@link MaskBitmap}. Operations outside the image are clipped.
 */
public final class MutableMask {
    private final int imageWidth;
    private final int imageHeight;
    // Row by row: bit (y * imageWidth + x).
    private final long[] bits;

    /**
     * Creates an empty mask.
     *
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     */
    public MutableMask(int imageWidth, int imageHeight) {
        if(imageWidth <= 0 || imageHeight <= 0) {
            throw new IllegalArgumentException("Invalid mask size " + imageWidth + "x" + imageHeight + ".");
        }

        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.bits = new long[(int) (((long) imageWidth * imageHeight + 63) >>> 6)];
    }

    /**
     * Creates an editable copy of a mask.
     *
     * @param mask the mask
     * @return the copy
     */
    public static MutableMask of(MaskBitmap mask) {
        final MutableMask result = new MutableMask(mask.getImageWidth(), mask.getImageHeight());

        for(int y = mask.getMinY(); y < mask.getMinY() + mask.getHeight(); ++y) {
            for(int x = mask.getMinX(); x < mask.getMinX() + mask.getWidth(); ++x) {
                if(mask.get(x, y)) {
                    result.set(x, y, true);
                }
            }
        }

        return result;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * Returns whether a pixel is set.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @return true if the pixel is set, false if it isn't or lies outside the image
     */
    public boolean get(int x, int y) {
        if(x < 0 || y < 0 || x >= imageWidth || y >= imageHeight) {
            return false;
        }

        final long index = (long) y * imageWidth + x;
        return (bits[(int) (index >>> 6)] & (1L << index)) != 0;
    }

    /**
     * Sets or clears a pixel.
     *
     * @param x     the x-coordinate
     * @param y     the y-coordinate
     * @param value true to set, false to clear
     */
    public void set(int x, int y, boolean value) {
        if(x < 0 || y < 0 || x >= imageWidth || y >= imageHeight) {
            return;
        }

        final long index = (long) y * imageWidth + x;

        if(value) {
            bits[(int) (index >>> 6)] |= 1L << index;
        } else {
            bits[(int) (index >>> 6)] &= ~(1L << index);
        }
    }

    /**
     * Sets or clears the pixels from fromX (inclusive) to toX (exclusive) in a row.
     *
     * @param y     the row
     * @param fromX the first pixel
     * @param toX   the pixel after the last one
     * @param value true to set, false to clear
     */
    public void setRange(int y, int fromX, int toX, boolean value) {
        if(y < 0 || y >= imageHeight) {
            return;
        }

        final int from = Math.max(0, fromX);
        final int to = Math.min(imageWidth, toX);

        if(from >= to) {
            return;
        }

        long start = (long) y * imageWidth + from;
        final long end = (long) y * imageWidth + to;

        while(start < end) {
            final int word = (int) (start >>> 6);
            final int firstBit = (int) (start & 63);
            final int nrBits = (int) Math.min(64 - firstBit, end - start);
            final long wordMask = (nrBits == 64 ? -1L : ((1L << nrBits) - 1)) << firstBit;

            if(value) {
                bits[word] |= wordMask;
            } else {
                bits[word] &= ~wordMask;
            }

            start += nrBits;
        }
    }

    /**
     * Sets or clears all pixels whose centers lie in a circle.
     *
     * @param centerX the x-coordinate of the center (in pixels, 0 is the left edge of the first pixel)
     * @param centerY the y-coordinate of the center
     * @param radius  the radius in pixels
     * @param value   true to set, false to clear
     * @return the pixel rectangle that may have changed as {minX, minY, maxX (exclusive), maxY (exclusive)}, or
     * null if nothing lies in the image
     */
    public int[] fillCircle(double centerX, double centerY, double radius, boolean value) {
        // At least one pixel, however small the brush.
        final double r = Math.max(radius, 0.5);
        final int fromY = Math.max(0, (int) Math.floor(centerY - r));
        final int toY = Math.min(imageHeight, (int) Math.ceil(centerY + r));

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for(int y = fromY; y < toY; ++y) {
            final double dy = y + 0.5 - centerY;
            final double squaredHalfWidth = r * r - dy * dy;

            if(squaredHalfWidth < 0) {
                continue;
            }

            final double halfWidth = Math.sqrt(squaredHalfWidth);
            // Pixels whose centers lie in [centerX - halfWidth, centerX + halfWidth].
            final int fromX = (int) Math.ceil(centerX - halfWidth - 0.5);
            final int toX = (int) Math.floor(centerX + halfWidth - 0.5) + 1;
            setRange(y, fromX, toX, value);
            minX = Math.min(minX, fromX);
            maxX = Math.max(maxX, toX);
        }

        if(minX >= maxX) {
            return null;
        }

        return clipRectangle(minX, fromY, maxX, toY);
    }

    /**
     * Sets or clears the pixels covered by a line drawn with a round brush.
     *
     * @param fromX  the x-coordinate of the start
     * @param fromY  the y-coordinate of the start
     * @param toX    the x-coordinate of the end
     * @param toY    the y-coordinate of the end
     * @param radius the radius of the brush in pixels
     * @param value  true to set, false to clear
     * @return the pixel rectangle that may have changed (see {@link #fillCircle}), or null
     */
    public int[] strokeLine(double fromX, double fromY, double toX, double toY, double radius, boolean value) {
        final double length = Math.hypot(toX - fromX, toY - fromY);
        // Circles at most a quarter of the brush size apart cover the line without gaps.
        final double step = Math.max(Math.max(radius, 0.5) / 2, 0.5);
        final int nrSteps = Math.max(1, (int) Math.ceil(length / step));
        int[] changed = null;

        for(int i = 0; i <= nrSteps; ++i) {
            final double t = (double) i / nrSteps;
            changed = union(changed, fillCircle(fromX + t * (toX - fromX), fromY + t * (toY - fromY), radius, value));
        }

        return changed;
    }

    /**
     * Sets or clears the pixels whose centers lie inside a polygon (even-odd rule).
     *
     * @param points the polygon's points as x1, y1, x2, y2, ... in pixels
     * @param value  true to set, false to clear
     * @return the pixel rectangle that may have changed (see {@link #fillCircle}), or null
     */
    public int[] fillPolygon(double[] points, boolean value) {
        final int nrPoints = points.length / 2;

        if(nrPoints < 3) {
            return null;
        }

        double minPolygonY = Double.POSITIVE_INFINITY;
        double maxPolygonY = Double.NEGATIVE_INFINITY;

        for(int i = 1; i < points.length; i += 2) {
            minPolygonY = Math.min(minPolygonY, points[i]);
            maxPolygonY = Math.max(maxPolygonY, points[i]);
        }

        final int fromY = Math.max(0, (int) Math.floor(minPolygonY));
        final int toY = Math.min(imageHeight, (int) Math.ceil(maxPolygonY) + 1);
        final List<Double> crossings = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for(int y = fromY; y < toY; ++y) {
            final double scanY = y + 0.5;
            crossings.clear();

            for(int i = 0; i < nrPoints; ++i) {
                final double x1 = points[2 * i];
                final double y1 = points[2 * i + 1];
                final double x2 = points[(2 * i + 2) % points.length];
                final double y2 = points[(2 * i + 3) % points.length];

                // Half-open, so that a vertex on the scan line is counted once.
                if((y1 <= scanY) != (y2 <= scanY)) {
                    crossings.add(x1 + (scanY - y1) * (x2 - x1) / (y2 - y1));
                }
            }

            crossings.sort(Double::compare);

            for(int i = 0; i + 1 < crossings.size(); i += 2) {
                final int spanFromX = (int) Math.ceil(crossings.get(i) - 0.5);
                final int spanToX = (int) Math.ceil(crossings.get(i + 1) - 0.5);

                if(spanFromX < spanToX) {
                    setRange(y, spanFromX, spanToX, value);
                    minX = Math.min(minX, spanFromX);
                    maxX = Math.max(maxX, spanToX);
                }
            }
        }

        if(minX >= maxX) {
            return null;
        }

        return clipRectangle(minX, fromY, maxX, toY);
    }

    /**
     * Returns the immutable mask with the currently set pixels.
     *
     * @return the mask
     */
    public MaskBitmap toBitmap() {
        int minX = imageWidth;
        int maxX = -1;
        int minY = -1;
        int maxY = -1;

        for(int y = 0; y < imageHeight; ++y) {
            final int first = firstSetInRow(y);

            if(first < 0) {
                continue;
            }

            if(minY < 0) {
                minY = y;
            }

            maxY = y;
            minX = Math.min(minX, first);
            maxX = Math.max(maxX, lastSetInRow(y));
        }

        if(minY < 0) {
            return MaskBitmap.empty(imageWidth, imageHeight);
        }

        final int width = maxX - minX + 1;
        final int height = maxY - minY + 1;
        final long[] packed = new long[(int) (((long) width * height + 63) >>> 6)];

        for(int y = minY; y <= maxY; ++y) {
            for(int x = minX; x <= maxX; ++x) {
                if(get(x, y)) {
                    final long index = (long) (y - minY) * width + (x - minX);
                    packed[(int) (index >>> 6)] |= 1L << index;
                }
            }
        }

        return new MaskBitmap(imageWidth, imageHeight, minX, minY, width, height, packed);
    }

    /**
     * Returns whether no pixel is set.
     *
     * @return true if the mask is empty
     */
    public boolean isEmpty() {
        return Arrays.stream(bits).allMatch(word -> word == 0);
    }

    /**
     * Returns the union of two changed-pixel rectangles (see {@link #fillCircle}); either may be null.
     *
     * @param first  the first rectangle
     * @param second the second rectangle
     * @return the union, or null if both are null
     */
    public static int[] union(int[] first, int[] second) {
        if(first == null) {
            return second;
        }

        if(second == null) {
            return first;
        }

        return new int[]{Math.min(first[0], second[0]), Math.min(first[1], second[1]),
                Math.max(first[2], second[2]), Math.max(first[3], second[3])};
    }

    private int firstSetInRow(int y) {
        final long start = (long) y * imageWidth;
        final long end = start + imageWidth;

        for(long index = start; index < end; ) {
            final int word = (int) (index >>> 6);
            final int bit = (int) (index & 63);
            final long remaining = bits[word] >>> bit;

            if(remaining != 0) {
                final long found = index + Long.numberOfTrailingZeros(remaining);
                return found < end ? (int) (found - start) : -1;
            }

            index += 64 - bit;
        }

        return -1;
    }

    private int lastSetInRow(int y) {
        final long start = (long) y * imageWidth;

        for(long index = start + imageWidth - 1; index >= start; ) {
            final int word = (int) (index >>> 6);
            final int bit = (int) (index & 63);
            final long masked = bits[word] & (bit == 63 ? -1L : (1L << (bit + 1)) - 1);

            if(masked != 0) {
                final long found = ((long) word << 6) + 63 - Long.numberOfLeadingZeros(masked);
                return found >= start ? (int) (found - start) : -1;
            }

            index -= bit + 1;
        }

        return -1;
    }

    private int[] clipRectangle(int minX, int minY, int maxX, int maxY) {
        final int[] clipped = {Math.max(0, minX), Math.max(0, minY), Math.min(imageWidth, maxX),
                Math.min(imageHeight, maxY)};
        return clipped[0] < clipped[2] && clipped[1] < clipped[3] ? clipped : null;
    }
}
