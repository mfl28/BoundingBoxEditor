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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MaskBitmapTest {
    @Test
    void onCreatingEmptyMask_ShouldHaveNoPixelsAndZeroBounds() {
        final MaskBitmap mask = MaskBitmap.empty(20, 10);

        assertTrue(mask.isEmpty());
        assertEquals(0, mask.getArea());
        assertEquals(0, mask.getWidth());
        assertEquals(0, mask.getHeight());
        assertFalse(mask.get(0, 0));
        assertArrayEquals(new long[]{200}, mask.toColumnMajorRunLengths());
    }

    @Test
    void onConvertingMutableMask_ShouldKeepOnlyTheBoundsOfTheSetPixels() {
        final MutableMask mutableMask = new MutableMask(100, 50);
        mutableMask.set(10, 5, true);
        mutableMask.set(70, 40, true);
        mutableMask.set(200, 5, true);

        final MaskBitmap mask = mutableMask.toBitmap();

        assertEquals(2, mask.getArea());
        assertEquals(10, mask.getMinX());
        assertEquals(5, mask.getMinY());
        assertEquals(61, mask.getWidth());
        assertEquals(36, mask.getHeight());
        assertTrue(mask.get(10, 5));
        assertTrue(mask.get(70, 40));
        assertFalse(mask.get(11, 5));
        assertFalse(mask.get(-1, -1));
        assertEquals(mask, MutableMask.of(mask).toBitmap());
    }

    @Test
    void onSettingRangesAcrossWords_ShouldSetExactlyThosePixels() {
        final MutableMask mutableMask = new MutableMask(150, 3);
        mutableMask.setRange(1, 3, 140, true);
        mutableMask.setRange(1, 60, 70, false);

        final MaskBitmap mask = mutableMask.toBitmap();

        assertEquals(137 - 10, mask.getArea());
        assertFalse(mask.get(2, 1));
        assertTrue(mask.get(3, 1));
        assertTrue(mask.get(59, 1));
        assertFalse(mask.get(60, 1));
        assertFalse(mask.get(69, 1));
        assertTrue(mask.get(70, 1));
        assertTrue(mask.get(139, 1));
        assertFalse(mask.get(140, 1));
        assertFalse(mask.get(3, 0));
    }

    @Test
    void onFillingCircle_ShouldSetThePixelsWhoseCentersAreInside() {
        final MutableMask mutableMask = new MutableMask(40, 40);

        final int[] changed = mutableMask.fillCircle(20, 20, 5, true);
        final MaskBitmap mask = mutableMask.toBitmap();

        int expectedArea = 0;

        for(int y = 0; y < 40; ++y) {
            for(int x = 0; x < 40; ++x) {
                final boolean inside = Math.pow(x + 0.5 - 20, 2) + Math.pow(y + 0.5 - 20, 2) <= 25;
                assertEquals(inside, mask.get(x, y), "pixel " + x + ", " + y);
                expectedArea += inside ? 1 : 0;
            }
        }

        assertEquals(expectedArea, mask.getArea());
        assertNotNull(changed);
        assertTrue(changed[0] <= mask.getMinX() && changed[2] >= mask.getMinX() + mask.getWidth());
    }

    @Test
    void onErasingWithCircle_ShouldClearPixels() {
        final MutableMask mutableMask = new MutableMask(40, 40);
        mutableMask.fillCircle(20, 20, 10, true);
        final int fullArea = mutableMask.toBitmap().getArea();

        mutableMask.fillCircle(20, 20, 5, false);

        assertFalse(mutableMask.get(20, 20));
        assertTrue(mutableMask.get(20, 12));
        assertTrue(mutableMask.toBitmap().getArea() < fullArea);
    }

    @Test
    void onStrokingLine_ShouldCoverThePathWithoutGaps() {
        final MutableMask mutableMask = new MutableMask(100, 20);

        mutableMask.strokeLine(5, 10, 95, 10, 2, true);

        for(int x = 5; x < 95; ++x) {
            assertTrue(mutableMask.get(x, 10), "pixel " + x);
        }
    }

    @Test
    void onFillingPolygon_ShouldSetThePixelsWhoseCentersAreInside() {
        final MutableMask mutableMask = new MutableMask(20, 20);

        mutableMask.fillPolygon(new double[]{2, 3, 12, 3, 12, 8, 2, 8}, true);
        final MaskBitmap mask = mutableMask.toBitmap();

        assertEquals(10 * 5, mask.getArea());
        assertEquals(2, mask.getMinX());
        assertEquals(3, mask.getMinY());
        assertEquals(10, mask.getWidth());
        assertEquals(5, mask.getHeight());
    }

    @Test
    void onConvertingToRunLengthsAndBack_ShouldGiveTheSameMask() {
        final MutableMask mutableMask = new MutableMask(31, 17);
        mutableMask.fillCircle(10, 8, 6, true);
        mutableMask.fillPolygon(new double[]{20, 0, 31, 0, 31, 17}, true);
        mutableMask.set(0, 0, true);
        final MaskBitmap mask = mutableMask.toBitmap();

        final long[] counts = mask.toColumnMajorRunLengths();

        assertEquals(0, counts[0], "the first pixel is set, so the first run of unset pixels is empty");
        assertEquals(mask, MaskBitmap.fromColumnMajorRunLengths(counts, 31, 17));
    }

    @Test
    void onReadingRunLengthsThatDontCoverTheImage_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> MaskBitmap.fromColumnMajorRunLengths(new long[]{5, 3},
                                                                                                    4, 4));
        assertThrows(IllegalArgumentException.class, () -> MaskBitmap.fromColumnMajorRunLengths(new long[]{20, -4},
                                                                                                    4, 4));
    }

    @Test
    void onTranslating_ShouldMoveThePixelsAndDropThoseOutsideTheImage() {
        final MutableMask mutableMask = new MutableMask(10, 10);
        mutableMask.set(1, 1, true);
        mutableMask.set(8, 8, true);
        final MaskBitmap mask = mutableMask.toBitmap();

        final MaskBitmap moved = mask.translated(1, -1);

        assertTrue(moved.get(2, 0));
        assertTrue(moved.get(9, 7));
        assertEquals(2, moved.getArea());
        assertEquals(1, mask.translated(2, 0).getArea());
        assertSame(mask, mask.translated(0, 0));
    }

    @Test
    void onResizing_ShouldScaleTheMaskToTheOtherImageSize() {
        final MutableMask mutableMask = new MutableMask(10, 10);
        mutableMask.fillPolygon(new double[]{0, 0, 5, 0, 5, 5, 0, 5}, true);
        final MaskBitmap mask = mutableMask.toBitmap();

        final MaskBitmap resized = mask.resizedTo(20, 40);

        assertEquals(20, resized.getImageWidth());
        assertEquals(40, resized.getImageHeight());
        assertEquals(10 * 20, resized.getArea());
        assertTrue(resized.get(9, 19));
        assertFalse(resized.get(10, 19));
        assertSame(mask, mask.resizedTo(10, 10));
    }

    @Test
    void onComparing_ShouldCompareSizeAndPixels() {
        final MutableMask first = new MutableMask(10, 10);
        first.set(3, 3, true);
        final MutableMask second = new MutableMask(10, 10);
        second.set(3, 3, true);
        final MutableMask otherSize = new MutableMask(11, 10);
        otherSize.set(3, 3, true);

        assertEquals(first.toBitmap(), second.toBitmap());
        assertEquals(first.toBitmap().hashCode(), second.toBitmap().hashCode());
        assertNotEquals(first.toBitmap(), otherSize.toBitmap());

        second.set(4, 3, true);
        assertNotEquals(first.toBitmap(), second.toBitmap());
    }
}
