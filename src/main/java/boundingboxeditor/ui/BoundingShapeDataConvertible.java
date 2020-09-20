package boundingboxeditor.ui;

import boundingboxeditor.model.data.BoundingShapeData;

/**
 * Interface for objects that can be converted to a {@link BoundingShapeData} object.
 */
public interface BoundingShapeDataConvertible {
    BoundingShapeData toBoundingShapeData();
}
