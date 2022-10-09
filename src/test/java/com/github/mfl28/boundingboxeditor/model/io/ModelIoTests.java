/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingPolygonData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictionEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.ModelEntry;
import javafx.geometry.BoundingBox;
import javafx.scene.paint.Color;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

@Tag("unit")
class ModelIoTests {
    @Test
    void onBoundingBoxDataEqualityCheck_ShouldHandleCorrectly() {
        ObjectCategory category1 = new ObjectCategory("foo", Color.AQUA);
        ObjectCategory category2 = new ObjectCategory("bar", Color.RED);

        BoundingBoxData boundingBoxData1 = new BoundingBoxData(category1, new BoundingBox(0.0, 0.0, 100.0, 100.0),
                                                               Collections.emptyList());

        BoundingBoxData boundingBoxData2 = new BoundingBoxData(category1, new BoundingBox(0.000000001, 0.0, 100.0,
                                                                                          100.0),
                                                               Collections.emptyList());
        // Self equality check:
        Assertions.assertEquals(boundingBoxData1, boundingBoxData1);

        // Coordinate difference below threshold:
        Assertions.assertEquals(boundingBoxData1, boundingBoxData2);


        // Coordinate difference above threshold:
        BoundingBoxData boundingBoxData3 = new BoundingBoxData(category1, new BoundingBox(0.0, 0.0, 99.9999999, 100.0),
                                                               Collections.emptyList());

        Assertions.assertNotEquals(boundingBoxData1, boundingBoxData3);

        // Different categories:
        BoundingBoxData boundingBoxData4 = new BoundingBoxData(category2, new BoundingBox(0.0, 0.0, 100.0, 100.0),
                                                               Collections.emptyList());

        Assertions.assertNotEquals(boundingBoxData1, boundingBoxData4);

        // Different tags:
        BoundingBoxData boundingBoxData5 = new BoundingBoxData(category1, new BoundingBox(0.0, 0.0, 100.0, 100.0),
                                                               Collections.singletonList("test"));

        Assertions.assertNotEquals(boundingBoxData1, boundingBoxData5);

        // Different parts:
        BoundingBoxData boundingBoxData6 = new BoundingBoxData(category1, new BoundingBox(0.0, 0.0, 100.0, 100.0),
                                                               Collections.emptyList());
        boundingBoxData1.setParts(Collections.singletonList(boundingBoxData2));
        boundingBoxData6.setParts(Collections.singletonList(boundingBoxData3));

        Assertions.assertNotEquals(boundingBoxData1, boundingBoxData6);
    }

    @Test
    void onBoundingPolygonDataEqualityCheck_ShouldHandleCorrectly() {
        ObjectCategory category1 = new ObjectCategory("foo", Color.AQUA);

        BoundingPolygonData boundingPolygonData1 = new BoundingPolygonData(category1, Arrays.asList(0.0, 0.0, 15.0,
                                                                                                    15.0),
                                                                           Collections.emptyList());
        // Self equality check:
        Assertions.assertEquals(boundingPolygonData1, boundingPolygonData1);

        // Different number of points:
        BoundingPolygonData boundingPolygonData2 = new BoundingPolygonData(category1, Arrays.asList(0.0, 0.0),
                                                                           Collections.emptyList());

        Assertions.assertNotEquals(boundingPolygonData1, boundingPolygonData2);

        // Coordinate difference below threshold:
        BoundingPolygonData boundingPolygonData3 = new BoundingPolygonData(category1, Arrays.asList(0.0, 0.0,
                                                                                                    14.999999999,
                                                                                                    15.0),
                                                                           Collections.emptyList());
        Assertions.assertEquals(boundingPolygonData1, boundingPolygonData3);

        // Coordinate difference above threshold:
        BoundingPolygonData boundingPolygonData4 = new BoundingPolygonData(category1, Arrays.asList(0.0, 0.0,
                                                                                                    14.9999999,
                                                                                                    15.0),
                                                                           Collections.emptyList());
        Assertions.assertNotEquals(boundingPolygonData1, boundingPolygonData4);
    }

    @Test
    void checkRestModelEntryEqualityContract() {
        EqualsVerifier.simple().forClass(ModelEntry.class).verify();
    }

    @Test
    void checkRestBoundingBoxPredictionEntryEqualityContract() {
        EqualsVerifier.simple().forClass(BoundingBoxPredictionEntry.class).verify();
    }
}
