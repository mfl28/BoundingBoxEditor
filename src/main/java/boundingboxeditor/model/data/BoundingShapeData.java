package boundingboxeditor.model.data;

import boundingboxeditor.ui.BoundingShapeViewable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base class of data-components of a bounding-shape view objects.
 */
public abstract class BoundingShapeData {
    private final ObjectCategory category;
    private final List<String> tags;
    private List<BoundingShapeData> parts = Collections.emptyList();

    protected BoundingShapeData(ObjectCategory category, List<String> tags) {
        this.category = category;
        this.tags = tags;
    }

    public abstract BoundingShapeViewable toBoundingShapeView(double imageWidth, double imageHeight);

    /**
     * Returns the name of the category of the bounding-box.
     *
     * @return the category name
     */
    public String getCategoryName() {
        return category.getName();
    }

    /**
     * Returns the category of the bounding-box.
     *
     * @return the category
     */
    public ObjectCategory getCategory() {
        return category;
    }

    /**
     * Returns a list of the tags that are registered with the bounding-box.
     *
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Returns the {@link BoundingBoxData}-objects that are registered as nested parts of the bounding-box.
     *
     * @return the parts
     */
    public List<BoundingShapeData> getParts() {
        return parts;
    }

    /**
     * Registers {@link BoundingBoxData}-objects as nested parts of the bounding-box.
     *
     * @param parts the BoundingBoxData-objects to be registered
     */
    public void setParts(List<BoundingShapeData> parts) {
        this.parts = parts;
    }

    public abstract <T> T accept(BoundingShapeDataVisitor<T> visitor);

    @Override
    public int hashCode() {
        return Objects.hash(category, tags, parts);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(!(o instanceof BoundingShapeData)) {
            return false;
        }

        BoundingShapeData that = (BoundingShapeData) o;

        return Objects.equals(category, that.category) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(parts, that.parts);
    }
}