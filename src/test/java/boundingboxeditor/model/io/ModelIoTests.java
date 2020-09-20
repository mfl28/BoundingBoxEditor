package boundingboxeditor.model.io;

import boundingboxeditor.model.data.BoundingBoxData;
import boundingboxeditor.model.data.BoundingPolygonData;
import boundingboxeditor.model.data.ObjectCategory;
import javafx.geometry.BoundingBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

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

}
