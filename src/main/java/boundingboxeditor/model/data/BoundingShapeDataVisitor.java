package boundingboxeditor.model.data;

/**
 * Visitor interface for {@link BoundingShapeData}-objects.
 *
 * @see BoundingShapeData#accept(BoundingShapeDataVisitor)
 */
public interface BoundingShapeDataVisitor<T> {
    T visit(BoundingBoxData boundingBoxData);

    T visit(BoundingPolygonData boundingPolygonData);
}
