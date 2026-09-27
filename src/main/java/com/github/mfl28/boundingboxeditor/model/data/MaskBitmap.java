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

import java.util.Arrays;

/**
 * An immutable binary pixel mask on the pixel grid of an image (the size of the image file, oriented as shown).
 * Only the smallest rectangle containing all set pixels is stored, one bit per pixel, row by row. Being immutable,
 * a mask can be shared, e.g. by the snapshots of the edit history. Masks are edited with {@link MutableMask}.
 */
public final class MaskBitmap {
    private final int imageWidth;
    private final int imageHeight;
    // The bounds of the set pixels (all 0 for an empty mask).
    private final int minX;
    private final int minY;
    private final int width;
    private final int height;
    // Bit (row * width + column) is pixel (minX + column, minY + row).
    private final long[] bits;
    private final int area;
    private int hashCode;

    MaskBitmap(int imageWidth, int imageHeight, int minX, int minY, int width, int height, long[] bits) {
        if(imageWidth <= 0 || imageHeight <= 0) {
            throw new IllegalArgumentException("Invalid mask size " + imageWidth + "x" + imageHeight + ".");
        }

        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
        this.bits = bits;

        int count = 0;

        for(long word : bits) {
            count += Long.bitCount(word);
        }

        this.area = count;
    }

    /**
     * Creates a mask without set pixels.
     *
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     * @return the mask
     */
    public static MaskBitmap empty(int imageWidth, int imageHeight) {
        return new MaskBitmap(imageWidth, imageHeight, 0, 0, 0, 0, new long[0]);
    }

    /**
     * Creates a mask from COCO run-length counts: alternating runs of unset and set pixels (starting with unset
     * ones), with the pixels in column-major order (all pixels of the first column from top to bottom, then the
     * second, ...).
     *
     * @param counts      the run lengths
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     * @return the mask
     * @throws IllegalArgumentException if the counts don't cover the image exactly or are negative
     */
    public static MaskBitmap fromColumnMajorRunLengths(long[] counts, int imageWidth, int imageHeight) {
        final long pixelCount = (long) imageWidth * imageHeight;
        long total = 0;

        for(long count : counts) {
            if(count < 0) {
                throw new IllegalArgumentException("Negative run length " + count + ".");
            }

            total += count;
        }

        if(total != pixelCount) {
            throw new IllegalArgumentException("The run lengths cover " + total + " pixels, but the mask ("
                    + imageWidth + "x" + imageHeight + ") has " + pixelCount + ".");
        }

        final MutableMask mask = new MutableMask(imageWidth, imageHeight);
        long position = 0;

        for(int i = 0; i < counts.length; ++i) {
            final long end = position + counts[i];

            if(i % 2 == 1) {
                // Runs can span several columns.
                while(position < end) {
                    final int x = (int) (position / imageHeight);
                    final int y = (int) (position % imageHeight);
                    final int runInColumn = (int) Math.min(end - position, (long) imageHeight - y);

                    for(int row = y; row < y + runInColumn; ++row) {
                        mask.set(x, row, true);
                    }

                    position += runInColumn;
                }
            }

            position = end;
        }

        return mask.toBitmap();
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * Returns the smallest x-coordinate of a set pixel.
     *
     * @return the coordinate, 0 for an empty mask
     */
    public int getMinX() {
        return minX;
    }

    /**
     * Returns the smallest y-coordinate of a set pixel.
     *
     * @return the coordinate, 0 for an empty mask
     */
    public int getMinY() {
        return minY;
    }

    /**
     * Returns the width of the smallest rectangle that contains all set pixels.
     *
     * @return the width, 0 for an empty mask
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the smallest rectangle that contains all set pixels.
     *
     * @return the height, 0 for an empty mask
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the number of set pixels.
     *
     * @return the number of pixels
     */
    public int getArea() {
        return area;
    }

    public boolean isEmpty() {
        return area == 0;
    }

    /**
     * Returns whether a pixel is set.
     *
     * @param x the x-coordinate in the image
     * @param y the y-coordinate in the image
     * @return true if the pixel is set, false if it isn't or lies outside the image
     */
    public boolean get(int x, int y) {
        final int column = x - minX;
        final int row = y - minY;

        if(column < 0 || row < 0 || column >= width || row >= height) {
            return false;
        }

        final long index = (long) row * width + column;
        return (bits[(int) (index >>> 6)] & (1L << index)) != 0;
    }

    /**
     * Returns the COCO run-length counts of the mask: alternating runs of unset and set pixels (starting with
     * unset ones, so the first count may be 0), with the pixels in column-major order.
     *
     * @return the run lengths
     */
    public long[] toColumnMajorRunLengths() {
        final RunLengthBuilder builder = new RunLengthBuilder();

        if(!isEmpty()) {
            final int maxY = minY + height;

            for(int x = minX; x < minX + width; ++x) {
                final long columnStart = (long) x * imageHeight;

                // The pixels outside the bounds' rows are unset, so a run of set pixels ends at the bounds' edges.
                if(minY > 0) {
                    builder.observe(columnStart + minY - 1, false);
                }

                for(int y = minY; y < maxY; ++y) {
                    builder.observe(columnStart + y, get(x, y));
                }

                if(maxY < imageHeight) {
                    builder.observe(columnStart + maxY, false);
                }
            }

            if(minX + width < imageWidth) {
                builder.observe((long) (minX + width) * imageHeight, false);
            }
        }

        return builder.finish((long) imageWidth * imageHeight);
    }

    /**
     * Returns the mask moved by whole pixels. Pixels moved outside the image are dropped.
     *
     * @param dx the distance in x-direction
     * @param dy the distance in y-direction
     * @return the moved mask
     */
    public MaskBitmap translated(int dx, int dy) {
        if((dx == 0 && dy == 0) || isEmpty()) {
            return this;
        }

        final MutableMask moved = new MutableMask(imageWidth, imageHeight);

        for(int y = minY; y < minY + height; ++y) {
            for(int x = minX; x < minX + width; ++x) {
                if(get(x, y)) {
                    moved.set(x + dx, y + dy, true);
                }
            }
        }

        return moved.toBitmap();
    }

    /**
     * Returns the mask scaled to another image size (nearest neighbour), e.g. for a mask that was copied from an
     * image of a different size.
     *
     * @param newImageWidth  the width of the other image
     * @param newImageHeight the height of the other image
     * @return the scaled mask, or this mask if the size is the same
     */
    public MaskBitmap resizedTo(int newImageWidth, int newImageHeight) {
        if(newImageWidth == imageWidth && newImageHeight == imageHeight) {
            return this;
        }

        final MutableMask resized = new MutableMask(newImageWidth, newImageHeight);

        if(!isEmpty()) {
            final double scaleX = (double) imageWidth / newImageWidth;
            final double scaleY = (double) imageHeight / newImageHeight;
            final int fromX = (int) Math.floor(minX / scaleX);
            final int toX = (int) Math.ceil((minX + width) / scaleX);
            final int fromY = (int) Math.floor(minY / scaleY);
            final int toY = (int) Math.ceil((minY + height) / scaleY);

            for(int y = Math.max(0, fromY); y < Math.min(newImageHeight, toY); ++y) {
                final int sourceY = (int) ((y + 0.5) * scaleY);

                for(int x = Math.max(0, fromX); x < Math.min(newImageWidth, toX); ++x) {
                    if(get((int) ((x + 0.5) * scaleX), sourceY)) {
                        resized.set(x, y, true);
                    }
                }
            }
        }

        return resized.toBitmap();
    }

    @Override
    public int hashCode() {
        int result = hashCode;

        if(result == 0) {
            result = 31 * (31 * (31 * (31 * Integer.hashCode(imageWidth) + imageHeight) + minX) + minY) + width;
            result = 31 * (31 * result + height) + Arrays.hashCode(bits);
            hashCode = result;
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(!(o instanceof MaskBitmap other)) {
            return false;
        }

        return imageWidth == other.imageWidth && imageHeight == other.imageHeight && minX == other.minX
                && minY == other.minY && width == other.width && height == other.height && area == other.area
                && hashCode() == other.hashCode() && Arrays.equals(bits, other.bits);
    }

    @Override
    public String toString() {
        return "MaskBitmap[" + imageWidth + "x" + imageHeight + ", bounds=(" + minX + ", " + minY + ", " + width
                + "x" + height + "), area=" + area + "]";
    }

    long[] getBits() {
        return bits;
    }

    /**
     * Collects run lengths from pixel values observed at increasing positions: a run changes where an observed
     * value differs from the previous one.
     */
    private static final class RunLengthBuilder {
        private long[] counts = new long[16];
        private int nrCounts = 0;
        private boolean currentValue = false;
        private long runStart = 0;

        void observe(long position, boolean value) {
            if(value != currentValue) {
                add(position - runStart);
                runStart = position;
                currentValue = value;
            }
        }

        long[] finish(long pixelCount) {
            add(pixelCount - runStart);
            return Arrays.copyOf(counts, nrCounts);
        }

        private void add(long count) {
            if(nrCounts == counts.length) {
                counts = Arrays.copyOf(counts, counts.length * 2);
            }

            counts[nrCounts++] = count;
        }
    }
}
