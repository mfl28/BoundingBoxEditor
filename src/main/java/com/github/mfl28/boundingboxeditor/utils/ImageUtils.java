/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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


import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class ImageUtils {
    private ImageUtils() {
        throw new IllegalStateException("ImageUtils class");
    }

    /**
     * Reorients an image based on an EXIF orientation code.
     * @param image The image to reorient.
     * @param orientation The EXIF orientation code.
     *                    1 = 0 degrees (no adjustment required)
     *                    2 = 0 degrees, mirrored
     *                    3 = 180 degrees
     *                    4 = 180 degrees, mirrored
     *                    5 = 90 degrees
     *                    6 = 90 degrees, mirrored
     *                    7 = 270 degrees
     *                    8 = 270 degrees, mirrored
     * @return The reoriented image.
     */
    public static Image reorientImage(Image image, int orientation) {
        if(orientation == 1) {
            return image;
        }

        final BufferedImage srcImage = SwingFXUtils.fromFXImage(image, null);

        final AffineTransform transform = getExifTransformation(orientation, srcImage.getWidth(), srcImage.getHeight());
        final AffineTransformOp affineTransformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        final BufferedImage destinationImage = affineTransformOp.filter(srcImage, null);

        return SwingFXUtils.toFXImage(destinationImage, null);
    }

    /**
     * Builds an affine transform based on an exif orientation
     * code and image dimensions.
     * @param orientation The EXIF orientation code.
     * @param width The image width.
     * @param height The image height.
     * @return The affine transformation.
     */
    public static AffineTransform getExifTransformation(int orientation, int width, int height) {
        final AffineTransform transform = new AffineTransform();

        switch(orientation) {
            case 2 -> {
                transform.scale(-1.0, 1.0);
                transform.translate(-width, 0);
            }
            case 3 -> {
                transform.translate(width, height);
                transform.rotate(Math.PI);
            }
            case 4 -> {
                transform.scale(1.0, -1.0);
                transform.translate(0, -height);
            }
            case 5 -> {
                transform.rotate(-Math.PI / 2);
                transform.scale(-1.0, 1.0);
            }
            case 6 -> {
                transform.translate(height, 0);
                transform.rotate(Math.PI / 2);
            }
            case 7 -> {
                transform.scale(-1.0, 1.0);
                transform.translate(-height, 0);
                transform.translate(0, width);
                transform.rotate(3 * Math.PI / 2);
            }
            case 8 -> {
                transform.translate(0, width);
                transform.rotate(3 * Math.PI / 2);
            }
            default -> {
                // Identity.
            }
        }

        return transform;
    }
}
