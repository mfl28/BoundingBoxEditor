package BoundingboxEditor;

import static org.junit.jupiter.api.Assertions.*;

import BoundingboxEditor.model.io.BoundingBoxElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoundingBoxElementTest {
    @Test
    @DisplayName("A simple dummy test to show junit 5 functionality.")
    void simpleTest() {
        BoundingBoxElement boundingBox = new BoundingBoxElement("Dummy", 2.5, 6.7, 230.0, 250.0);

        assertAll("boundingBox",
                () -> assertEquals(boundingBox.getCategoryName(), "Dummy"),
                () -> assertEquals(boundingBox.getxMin(), 2.5),
                () -> assertEquals(boundingBox.getyMin(), 6.7),
                () -> assertEquals(boundingBox.getxMax(), 230.0),
                () -> assertEquals(boundingBox.getyMax(), 250.0)
        );
    }
}