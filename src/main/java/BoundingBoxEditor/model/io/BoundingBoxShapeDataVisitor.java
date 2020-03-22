package boundingboxeditor.model.io;

public interface BoundingBoxShapeDataVisitor<T> {
    T visit(BoundingBoxData boundingBoxData);

    T visit(BoundingPolygonData boundingPolygonData);
}
