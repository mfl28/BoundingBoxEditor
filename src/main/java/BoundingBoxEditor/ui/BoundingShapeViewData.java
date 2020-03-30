package boundingboxeditor.ui;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.ObjectCategory;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Shape;

import java.util.Objects;

/**
 * Class holding common data and methods of bounding shape objects.
 */
public class BoundingShapeViewData {
    private final Property<Bounds> autoScaleBounds = new SimpleObjectProperty<>();
    private final Group nodeGroup = new Group();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final BooleanProperty highlighted = new SimpleBooleanProperty(false);
    private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<>();
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final Shape baseShape;
    private ObjectCategory objectCategory;
    private ImageMetaData imageMetaData;
    private BoundingShapeTreeItem treeItem;

    public BoundingShapeViewData(Shape shape, ObjectCategory objectCategory, ImageMetaData imageMetaData) {
        this.baseShape = shape;
        this.objectCategory = objectCategory;
        this.imageMetaData = imageMetaData;
        nodeGroup.getChildren().add(shape);
    }

    public Property<Bounds> autoScaleBounds() {
        return autoScaleBounds;
    }

    public BooleanProperty getSelected() {
        return selected;
    }

    public BooleanProperty getHighlighted() {
        return highlighted;
    }


    public BooleanProperty highlightedProperty() {
        return highlighted;
    }

    public ImageMetaData getImageMetaData() {
        return imageMetaData;
    }

    public void setImageMetaData(ImageMetaData imageMetaData) {
        this.imageMetaData = imageMetaData;
    }

    public Shape getBaseShape() {
        return baseShape;
    }

    public ToggleGroup getToggleGroup() {
        return toggleGroup.get();
    }

    public void setToggleGroup(ToggleGroup toggleGroup) {
        this.toggleGroup.set(toggleGroup);
    }

    public boolean isHighlighted() {
        return highlighted.get();
    }

    /**
     * Sets the highlighted-status of the bounding-box.
     *
     * @param highlighted true to set highlighting on, otherwise off
     */
    void setHighlighted(boolean highlighted) {
        this.highlighted.set(highlighted);
    }

    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroup;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectCategory, imageMetaData, tags);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingShapeViewData)) {
            return false;
        }

        BoundingShapeViewData other = (BoundingShapeViewData) obj;

        return Objects.equals(imageMetaData, other.imageMetaData) && Objects.equals(tags, other.tags)
                && Objects.equals(objectCategory, other.objectCategory);
    }

    /**
     * Returns the associated {@link ObjectCategory} object.
     *
     * @return the {@link ObjectCategory} object
     */
    public ObjectCategory getObjectCategory() {
        return objectCategory;
    }

    public void setObjectCategory(ObjectCategory objectCategory) {
        this.objectCategory = objectCategory;
    }

    /**
     * Returns the currently assigned tags.
     *
     * @return the tags
     */
    ObservableList<String> getTags() {
        return tags;
    }

    /**
     * Returns the associated {@link TreeItem} object.
     *
     * @return the {@link TreeItem} object
     */
    BoundingShapeTreeItem getTreeItem() {
        return treeItem;
    }

    /**
     * Sets the associated {@link TreeItem} object.
     */
    void setTreeItem(BoundingShapeTreeItem treeItem) {
        this.treeItem = treeItem;
    }

    /**
     * Returns a {@link Group} object whose children are the components that make
     * up this {@link BoundingBoxView} UI-element (the rectangle itself as well as the resize-handles).
     * This function is used when the bounding-box should be added to the scene-graph.
     *
     * @return the group
     */
    Group getNodeGroup() {
        return nodeGroup;
    }
}