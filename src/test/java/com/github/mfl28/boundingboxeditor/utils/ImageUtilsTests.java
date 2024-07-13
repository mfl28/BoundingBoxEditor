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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Tag("unit")
class ImageUtilsTests {
    @Test
    void onCreateImageUtils_ShouldThrowException() throws Exception {
        Constructor<ImageUtils> imageUtilsConstructor = ImageUtils.class.getDeclaredConstructor();
        imageUtilsConstructor.setAccessible(true);

        try {
            imageUtilsConstructor.newInstance();
            Assertions.fail("Expected an InvocationTargetException to be thrown");
        } catch(InvocationTargetException e) {
            MatcherAssert.assertThat(e.getCause(), Matchers.instanceOf(IllegalStateException.class));
            Assertions.assertEquals("ImageUtils class", e.getCause().getMessage());
        }
    }

    @Test
    void onGetExifTransform_ShouldCreateCorrectTransform() {
        int width = 400;
        int height = 300;

        double[] matrix = new double[6];

        AffineTransform transform = ImageUtils.getExifTransformation(1, width, height);
        transform.getMatrix(matrix);
        Assertions.assertArrayEquals(new double[]{1.0, 0.0, 0.0, 1.0, 0.0, 0.0}, matrix, 1e-4);

        transform = ImageUtils.getExifTransformation(2, width, height);
        transform.getMatrix(matrix);
        Assertions.assertArrayEquals(new double[]{-1.0, 0.0, 0.0, 1.0, 400.0, 0.0}, matrix, 1e-4);

        transform = ImageUtils.getExifTransformation(3, width, height);
        transform.getMatrix(matrix);
        Assertions.assertArrayEquals(new double[]{-1.0, 0.0, 0.0, -1.0, 400.0, 300.0}, matrix, 1e-4);

        transform = ImageUtils.getExifTransformation(4, width, height);
        transform.getMatrix(matrix);
        Assertions.assertArrayEquals(new double[]{1.0, 0.0, 0.0, -1.0, 0.0, 300.0}, matrix, 1e-4);

        transform = ImageUtils.getExifTransformation(5, width, height);
        transform.getMatrix(matrix);
        Assertions.assertArrayEquals(new double[]{0.0, 1.0, 1.0, 0.0, 0.0, 0.0}, matrix, 1e-4);

        transform = ImageUtils.getExifTransformation(6, width, height);
        transform.getMatrix(matrix);
        Assertions.assertArrayEquals(new double[]{0.0, 1.0, -1.0, 0.0, 300.0, 0.0}, matrix, 1e-4);

        transform = ImageUtils.getExifTransformation(7, width, height);
        transform.getMatrix(matrix);
        Assertions.assertArrayEquals(new double[]{0.0, -1.0, -1.0, 0.0, 300.0, 400.0}, matrix, 1e-4);

        transform = ImageUtils.getExifTransformation(8, width, height);
        transform.getMatrix(matrix);
        Assertions.assertArrayEquals(new double[]{0.0, -1.0, 1.0, 0.0, 0.0, 400.0}, matrix, 1e-4);
    }

    @Test
    void onReorientImage_ShouldReturnCorrectlyOrientedImage() {
        BufferedImage bufferedImage = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        Image image = SwingFXUtils.toFXImage(bufferedImage, null);

        Assertions.assertEquals(image, ImageUtils.reorientImage(image, 1));

        Image transformedImage = ImageUtils.reorientImage(image, 5);

        Assertions.assertEquals(50, transformedImage.getWidth());
        Assertions.assertEquals(100, transformedImage.getHeight());
    }

}
