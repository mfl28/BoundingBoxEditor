package BoundingboxEditor;

import BoundingboxEditor.model.io.BoundingBoxElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundingBoxElementTest {
    @Test
    @DisplayName("A simple dummy test to show junit 5 functionality.")
    void simpleTest() {
        BoundingBoxElement boundingBox = new BoundingBoxElement("Dummy", 2.5, 6.7, 230.0, 250.0);

        assertAll("boundingBox",
                () -> assertEquals(boundingBox.getCategoryName(), "Dummy"),
                () -> assertEquals(boundingBox.getXMin(), 2.5),
                () -> assertEquals(boundingBox.getYMin(), 6.7),
                () -> assertEquals(boundingBox.getXMax(), 230.0),
                () -> assertEquals(boundingBox.getYMax(), 250.0)
        );
    }
}