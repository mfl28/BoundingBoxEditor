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
package com.github.mfl28.boundingboxeditor.model.io;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The layout of PNG masks, as in Pascal VOC's segmentation data: a folder with
 * <ul>
 *     <li>{@value #OBJECT_FOLDER_NAME}/&lt;image name&gt;.png: per pixel the number of the object (1, 2, ...);</li>
 *     <li>{@value #CLASS_FOLDER_NAME}/&lt;image name&gt;.png: per pixel the number of the object's category;</li>
 *     <li>{@value #CLASSES_FILE_NAME}: the category names, one per line (line 1 is category 1).</li>
 * </ul>
 * 0 is the background, and 255 marks pixels to ignore ("void" in Pascal VOC). The PNGs are 8-bit indexed images
 * with the Pascal VOC color palette, so they can be viewed as they are.
 */
final class PNGMasks {
    static final String OBJECT_FOLDER_NAME = "SegmentationObject";
    static final String CLASS_FOLDER_NAME = "SegmentationClass";
    static final String CLASSES_FILE_NAME = "classes.txt";
    static final String PNG_EXTENSION = ".png";
    static final int BACKGROUND = 0;
    static final int IGNORED = 255;
    // Objects and categories are numbered 1 to 254.
    static final int MAXIMUM_INDEX = 254;
    private static final IndexColorModel PALETTE = createPalette();

    private PNGMasks() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Writes a mask as an indexed PNG.
     *
     * @param values per pixel (row by row) the value, 0 to 255
     * @param width  the width
     * @param height the height
     * @param file   the file
     * @throws IOException if the file can't be written
     */
    static void write(byte[] values, int width, int height, Path file) throws IOException {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, PALETTE);
        image.getRaster().setDataElements(0, 0, width, height, values);

        if(!ImageIO.write(image, "png", file.toFile())) {
            throw new IOException("No PNG writer is available.");
        }
    }

    /**
     * Reads the values of a single-channel PNG mask (indexed or grayscale).
     *
     * @param file the file
     * @return the mask
     * @throws IOException if the file can't be read, or isn't an indexed or grayscale image
     */
    static Values read(Path file) throws IOException {
        final BufferedImage image = ImageIO.read(file.toFile());

        if(image == null) {
            throw new IOException("Not a readable image.");
        }

        final WritableRaster raster = image.getRaster();

        if(raster.getNumBands() != 1) {
            throw new IOException("The mask is not an indexed or grayscale image (it has " + raster.getNumBands()
                                          + " color channels).");
        }

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] values = raster.getSamples(0, 0, width, height, 0, new int[width * height]);
        return new Values(width, height, values);
    }

    /**
     * Pascal VOC's color palette: index i gets a color from the bits of i, spread over the high bits of red, green
     * and blue.
     */
    private static IndexColorModel createPalette() {
        final byte[] red = new byte[256];
        final byte[] green = new byte[256];
        final byte[] blue = new byte[256];

        for(int i = 0; i < 256; ++i) {
            int r = 0;
            int g = 0;
            int b = 0;
            int value = i;

            for(int bit = 7; bit >= 0; --bit) {
                r |= (value & 1) << bit;
                g |= ((value >> 1) & 1) << bit;
                b |= ((value >> 2) & 1) << bit;
                value >>= 3;
            }

            red[i] = (byte) r;
            green[i] = (byte) g;
            blue[i] = (byte) b;
        }

        return new IndexColorModel(8, 256, red, green, blue);
    }

    /**
     * The values of a mask image.
     */
    static final class Values {
        private final int width;
        private final int height;
        // Per pixel (row by row) the value.
        private final int[] pixelValues;

        Values(int width, int height, int[] pixelValues) {
            this.width = width;
            this.height = height;
            this.pixelValues = pixelValues;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        int get(int x, int y) {
            return pixelValues[y * width + x];
        }
    }
}
