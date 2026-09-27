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

import com.github.mfl28.boundingboxeditor.model.data.MaskBitmap;
import com.github.mfl28.boundingboxeditor.model.data.MutableMask;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compares the encoding with strings produced by pycocotools (pycocotools.mask.encode) for the same masks, see
 * testannotations/coco/rle_references.txt: line 1 is {@link #shapesMask()}, line 2 {@link #patternMask()}.
 */
@Tag("unit")
class COCORunLengthEncodingTest {
    private static final String REFERENCES_FILE = "/testannotations/coco/rle_references.txt";

    @Test
    void onEncodingMasks_ShouldProduceTheStringsOfPycocotools() throws Exception {
        final List<String> references = readReferences();

        assertEquals(references.get(0), COCORunLengthEncoding.encode(shapesMask().toColumnMajorRunLengths()));
        assertEquals(references.get(1), COCORunLengthEncoding.encode(patternMask().toColumnMajorRunLengths()));
        assertEquals("T3", COCORunLengthEncoding.encode(MaskBitmap.empty(10, 10).toColumnMajorRunLengths()));
        assertEquals("0d0", COCORunLengthEncoding.encode(fullMask(5, 4).toColumnMajorRunLengths()));
    }

    @Test
    void onDecodingStringsOfPycocotools_ShouldProduceTheMasks() throws Exception {
        final List<String> references = readReferences();

        final MaskBitmap shapes = MaskBitmap.fromColumnMajorRunLengths(
                COCORunLengthEncoding.decode(references.get(0)), 640, 480);
        final MaskBitmap pattern = MaskBitmap.fromColumnMajorRunLengths(
                COCORunLengthEncoding.decode(references.get(1)), 37, 23);

        assertEquals(shapesMask(), shapes);
        // The areas pycocotools computed for these masks.
        assertEquals(30504, shapes.getArea());
        assertEquals(patternMask(), pattern);
        assertEquals(370, pattern.getArea());
        assertEquals(fullMask(5, 4), MaskBitmap.fromColumnMajorRunLengths(COCORunLengthEncoding.decode("0d0"), 5, 4));
    }

    @Test
    void onDecodingInvalidStrings_ShouldThrow() {
        // A group with the continuation bit at the end.
        assertThrows(IllegalArgumentException.class, () -> COCORunLengthEncoding.decode("P"));
        // A character below '0'.
        assertThrows(IllegalArgumentException.class, () -> COCORunLengthEncoding.decode("0 1"));
    }

    /**
     * A circle (center 200, 150, radius 60) and a rectangle (x 400 to 520, y 300 to 460) on a 640x480 image: runs
     * longer than one group of 5 bits.
     */
    private static MaskBitmap shapesMask() {
        final MutableMask mask = new MutableMask(640, 480);
        mask.fillCircle(200, 150, 60, true);

        for(int y = 300; y < 460; ++y) {
            mask.setRange(y, 400, 520, true);
        }

        return mask.toBitmap();
    }

    /**
     * Many short runs, whose differences to the counts two places before are negative.
     */
    private static MaskBitmap patternMask() {
        final MutableMask mask = new MutableMask(37, 23);

        for(int y = 0; y < 23; ++y) {
            for(int x = 0; x < 37; ++x) {
                mask.set(x, y, (x * x + 3 * y) % 7 < 3);
            }
        }

        return mask.toBitmap();
    }

    private static MaskBitmap fullMask(int width, int height) {
        final MutableMask mask = new MutableMask(width, height);

        for(int y = 0; y < height; ++y) {
            mask.setRange(y, 0, width, true);
        }

        return mask.toBitmap();
    }

    private static List<String> readReferences() throws IOException, URISyntaxException {
        return Files.readAllLines(Path.of(Objects.requireNonNull(
                COCORunLengthEncodingTest.class.getResource(REFERENCES_FILE)).toURI()));
    }
}
