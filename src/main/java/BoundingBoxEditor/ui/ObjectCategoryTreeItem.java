package boundingboxeditor.ui;

import boundingboxeditor.model.ObjectCategory;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.Objects;

/**
 * A tree-item representing an existing {@link ObjectCategory} in a {@link ObjectTreeElementCell} of a {@link ObjectTreeView}.
 *
 * @see TreeItem
 */
class ObjectCategoryTreeItem extends TreeItem<Object> {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 9.5;

    private final ToggleSquare toggleIcon = new ToggleSquare(TOGGLE_ICON_SIDE_LENGTH);
    private final IntegerProperty nrToggledOnChildren = new SimpleIntegerProperty(0);
    private final ObjectCategory objectCategory;

    /**
     * Creates a new tree-item representing a {@link ObjectCategory} in a {@link ObjectTreeElementCell} that is part of
     * a {@link ObjectTreeView}.
     *
     * @param objectCategory the {@link ObjectCategory} that should be associated with the tree-item
     */
    ObjectCategoryTreeItem(ObjectCategory objectCategory) {
        // TreeItems require a non-null value-item for them not to be considered 'empty', therefore
        // a static 'dummy'-object of type BoundingBoxView is passed to every ObjectCategoryTreeItem.
        super(objectCategory);

        this.objectCategory = objectCategory;
        setGraphic(toggleIcon);

        setUpInternalListeners();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue(), getChildren());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof ObjectCategoryTreeItem)) {
            return false;
        }

        ObjectCategoryTreeItem other = (ObjectCategoryTreeItem) obj;

        return objectCategory.equals(other.objectCategory) && getChildren().equals(other.getChildren());
    }

    /**
     * Returns the {@link ObjectCategory} object associated with the tree-item.
     *
     * @return the bounding-box category
     */
    ObjectCategory getObjectCategory() {
        return objectCategory;
    }

    /**
     * Detaches a specific {@link BoundingShapeTreeItem} child from the tree-item and updates the ids of
     * all subsequent children.
     *
     * @param child the child to detach
     */
    void detachBoundingShapeTreeItemChild(BoundingShapeTreeItem child) {
        detachChildId(child.getId());
        getChildren().remove(child);
    }

    /**
     * Attaches a new {@link BoundingShapeTreeItem} to the end of the list of children and assigns it
     * an appropriate id.
     *
     * @param boundingBoxShapeTreeItem the {@link BoundingShapeTreeItem} to attach
     */
    void attachBoundingShapeTreeItemChild(BoundingShapeTreeItem boundingBoxShapeTreeItem) {
        boundingBoxShapeTreeItem.setId(getChildren().size() + 1);
        getChildren().add(boundingBoxShapeTreeItem);
    }

    /**
     * Returns the toggle-state of the tree-item's toggle-square.
     *
     * @return true if toggled on, false otherwise
     */
    boolean isIconToggledOn() {
        return toggleIcon.isToggledOn();
    }

    /**
     * Sets the toggled-on state of the tree-item's toggle-square (and all its children).
     *
     * @param toggledOn if true, the toggle-square is toggled on, otherwise off
     */
    void setIconToggledOn(boolean toggledOn) {
        toggleIcon.setToggledOn(toggledOn);

        for(TreeItem<Object> child : getChildren()) {
            if(child instanceof BoundingShapeTreeItem) {
                ((BoundingShapeTreeItem) child).setIconToggledOn(toggledOn);
            }
        }
    }

    /**
     * Adds 1 to the number of currently toggled-on children. Should be called
     * when a direct child of this tree-item was toggled on.
     */
    void incrementNrToggledOnChildren() {
        nrToggledOnChildren.set(nrToggledOnChildren.get() + 1);
    }

    /**
     * Subtracts 1 from the number of currently toggled-on children. Should be called
     * when a direct child of this tree-item was toggled off.
     */
    void decrementNrToggledOnChildren() {
        nrToggledOnChildren.set(nrToggledOnChildren.get() - 1);
    }

    private void setUpInternalListeners() {
        toggleIcon.fillProperty().bind(((ObjectCategory) getValue()).colorProperty());

        toggleIcon.setOnMousePressed(event -> {
            setIconToggledOn(!isIconToggledOn());
            event.consume();
        });

        getChildren().addListener((ListChangeListener<TreeItem<Object>>) c -> {
            while(c.next()) {
                int numToggledChildrenToAdd = c.getAddedSize();

                int numToggledChildrenToRemove = 0;

                for(TreeItem<Object> treeItem : c.getRemoved()) {
                    if(treeItem instanceof BoundingShapeTreeItem && ((BoundingShapeTreeItem) treeItem).isIconToggledOn()) {
                        numToggledChildrenToRemove++;
                    }
                }

                nrToggledOnChildren.setValue(nrToggledOnChildren.get() + numToggledChildrenToAdd - numToggledChildrenToRemove);
            }
        });

        nrToggledOnChildren.addListener((observable, oldValue, newValue) -> {
            if(newValue.intValue() == 0) {
                toggleIcon.setToggledOn(false);
            } else if(newValue.intValue() == getChildren().size()) {
                toggleIcon.setToggledOn(true);
            }
        });
    }

    private void detachChildId(int id) {
        List<TreeItem<Object>> children = getChildren();
        for(int i = id; i < children.size(); ++i) {
            if(children.get(i) instanceof BoundingShapeTreeItem) {
                ((BoundingShapeTreeItem) children.get(i)).setId(i);
            }
        }
    }
}