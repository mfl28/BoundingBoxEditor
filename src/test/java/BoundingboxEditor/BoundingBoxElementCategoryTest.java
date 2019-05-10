package BoundingboxEditor;

import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.io.BoundingBoxElement;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BoundingBoxElementCategoryTest {

    @Test
    void equals_IdenticalBoundingBoxCategories_ReturnsEqual() {
        final BoundingBoxCategory boundingBoxCategory1 = new BoundingBoxCategory("Category1", Color.RED);
        final BoundingBoxCategory boundingBoxCategory2 = new BoundingBoxCategory("Category1", Color.RED);

        assertEquals(boundingBoxCategory1, boundingBoxCategory2);
    }

    @Test
    void equals_BoundingBoxCategoriesWithMismatchedName_ReturnsNotEqual() {
        final BoundingBoxCategory boundingBoxCategory1 = new BoundingBoxCategory("Category1", Color.RED);
        final BoundingBoxCategory boundingBoxCategory2 = new BoundingBoxCategory("Category2", Color.RED);

        assertNotEquals(boundingBoxCategory1, boundingBoxCategory2);
    }

    @Test
    void equals_BoundingBoxCategoriesWithMismatchedColors_ReturnsEqual() {
        final BoundingBoxCategory boundingBoxCategory1 = new BoundingBoxCategory("Category1", Color.RED);
        final BoundingBoxCategory boundingBoxCategory2 = new BoundingBoxCategory("Category1", Color.BLUE);

        assertNotEquals(boundingBoxCategory1, boundingBoxCategory2);
    }

    @Test
    void equals_MismatchedObjectTypeAsSecondParameter_ReturnsNotEqual() {
        final BoundingBoxCategory boundingBoxCategory1 = new BoundingBoxCategory("Category1", Color.RED);
        final BoundingBoxElement boundingBox = new BoundingBoxElement("name", 1, 2, 3, 4);

        assertNotEquals(boundingBoxCategory1, boundingBox);
    }

    @Test
    void equals_NullAsSecondParameter_ReturnsNotEqual() {
        final BoundingBoxCategory boundingBoxCategory1 = new BoundingBoxCategory("Category1", Color.RED);

        assertNotEquals(boundingBoxCategory1, null);
    }
}