package com.github.mfl28.boundingboxeditor.model.data;

import javafx.geometry.BoundingBox;
import javafx.scene.paint.Color;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

@Tag("unit")
class BoundingPolygonDataTest {
    @Test
    void checkEqualsContract() {
        EqualsVerifier.simple().forClass(BoundingPolygonData.class)
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
                .verify();
    }

    @Test
    void onGetRelativePointsInImage_ShouldReturnPointsList() {
        List<Double> points = List.of(1.0, 2.0, 3.0, 4.0);

        final BoundingPolygonData boundingPolygonData = new BoundingPolygonData(
                new ObjectCategory("foo", Color.RED),
                points,
                Collections.emptyList());

        Assertions.assertEquals(points, boundingPolygonData.getRelativePointsInImage());
    }

}