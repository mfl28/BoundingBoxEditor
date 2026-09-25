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

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("unit")
class BoundingShapeDataTest {
    private static final ObjectCategory CATEGORY = new ObjectCategory("category", Color.RED);

    @Test
    void onFlatten_WhenShapeHasNoParts_ShouldReturnOnlyTheShape() {
        final BoundingShapeData shape = createBox();

        assertEquals(List.of(shape), shape.flatten().toList());
    }

    /**
     * The export formats rely on this order: the shape, then all of its parts level by level (breadth-first).
     */
    @Test
    void onFlatten_WhenShapeHasNestedParts_ShouldReturnShapesLevelByLevel() {
        final BoundingShapeData root = createBox();
        final BoundingShapeData part1 = createBox();
        final BoundingShapeData part2 = new BoundingPolygonData(CATEGORY, List.of(0.1, 0.1, 0.2, 0.2, 0.1, 0.2),
                List.of());
        final BoundingShapeData part1Child = createBox();
        final BoundingShapeData part2Child = createBox();

        root.setParts(List.of(part1, part2));
        part1.setParts(List.of(part1Child));
        part2.setParts(List.of(part2Child));

        final List<BoundingShapeData> flattened = root.flatten().toList();

        assertEquals(5, flattened.size());
        assertSameElements(List.of(root, part1, part2, part1Child, part2Child), flattened);
    }

    private static BoundingShapeData createBox() {
        return new BoundingBoxData(CATEGORY, 0, 0, 0.5, 0.5, List.of());
    }

    // Identity comparison: the boxes above are equal to each other, so list equality would not check the order.
    private static void assertSameElements(List<BoundingShapeData> expected, List<BoundingShapeData> actual) {
        for(int i = 0; i < expected.size(); ++i) {
            assertSame(expected.get(i), actual.get(i), "Unexpected shape at index " + i);
        }
    }
}
