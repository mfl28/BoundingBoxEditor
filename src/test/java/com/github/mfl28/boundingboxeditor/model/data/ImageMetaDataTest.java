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
package com.github.mfl28.boundingboxeditor.model.data;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


@Tag("unit")
class ImageMetaDataTest {
    @Test
    void checkEqualsContract() {
        EqualsVerifier.simple().forClass(ImageMetaData.class).verify();
    }

    @Test
    void onGetDimensionsString_WhenNoDetailsPresent_ShouldReturnCorrectRepresentation() {
        final ImageMetaData imageMetaData = new ImageMetaData("test");
        Assertions.assertEquals("[]", imageMetaData.getDimensionsString());
    }

    @Test
    void onGetOrientedDimensions_ShouldTakeOrientationIntoAccount() {
        for(int i = 1; i <= 8; ++i) {
            ImageMetaData imageMetaData = new ImageMetaData("test", "testDir", "./test/testDir", 100, 50, 3, i);

            if(i < 5) {
                Assertions.assertEquals(100, imageMetaData.getOrientedWidth());
                Assertions.assertEquals(50, imageMetaData.getOrientedHeight());
            } else {
                Assertions.assertEquals(100, imageMetaData.getOrientedHeight());
                Assertions.assertEquals(50, imageMetaData.getOrientedWidth());
            }
        }
    }
}
