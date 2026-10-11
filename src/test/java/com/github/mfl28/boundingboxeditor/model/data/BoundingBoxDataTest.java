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

import javafx.geometry.BoundingBox;
import javafx.scene.paint.Color;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class BoundingBoxDataTest {
    @Test
    void checkEqualsContract() {
        EqualsVerifier.simple().forClass(BoundingBoxData.class)
                .withPrefabValues(BoundingShapeData.class,
                        new BoundingBoxData(new ObjectCategory("foo", Color.RED),
                                new BoundingBox(0, 0, 10, 20),
                                Collections.emptyList()),
                        new BoundingBoxData(new ObjectCategory("bar", Color.BLUE),
                                new BoundingBox(0, 0, 30, 40),
                                Collections.emptyList())
                )
                .withPrefabValues(ObjectCategory.class,
                        new ObjectCategory("foo", Color.RED),
                        new ObjectCategory("bar", Color.BLUE))
                // equals() compares the coordinates with a tolerance, so hashCode() can't use them (see the test below).
                .suppress(Warning.STRICT_HASHCODE)
                .verify();
    }

    @Test
    void onComparingBoxesWithinTheTolerance_ShouldBeEqualWithEqualHashCodes() {
        final ObjectCategory category = new ObjectCategory("foo", Color.RED);
        final BoundingBoxData box = new BoundingBoxData(category, 0.1, 0.2, 0.5, 0.6, Collections.emptyList());
        // Differs by less than MathUtils.DOUBLE_EQUAL_THRESHOLD (e.g. after rounding in a format).
        final BoundingBoxData almostSameBox = new BoundingBoxData(category, 0.1 + 1e-10, 0.2, 0.5, 0.6 - 1e-10,
                                                                  Collections.emptyList());

        assertEquals(box, almostSameBox);
        assertEquals(box.hashCode(), almostSameBox.hashCode());
        assertEquals(1, new HashSet<>(List.of(box, almostSameBox)).size());
    }
}