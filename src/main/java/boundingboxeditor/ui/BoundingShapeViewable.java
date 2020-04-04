package boundingboxeditor.ui;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Bounds;

/**
 * Interface to access common data of bounding shapes.
 */
public interface BoundingShapeViewable {
    BoundingShapeViewData getViewData();

    void autoScaleWithBoundsAndInitialize(ReadOnlyObjectProperty<Bounds> autoScaleBounds);

    BoundingShapeTreeItem toTreeItem();
}
