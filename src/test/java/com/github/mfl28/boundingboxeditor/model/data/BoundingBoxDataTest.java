package com.github.mfl28.boundingboxeditor.model.data;

import javafx.geometry.BoundingBox;
import javafx.scene.paint.Color;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;

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
                .verify();
    }

}