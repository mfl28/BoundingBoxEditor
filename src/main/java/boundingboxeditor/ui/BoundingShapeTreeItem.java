package boundingboxeditor.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.shape.Shape;

/**
 * Base class of all shape tree items.
 */
public abstract class BoundingShapeTreeItem extends TreeItem<Object> {
    final protected Toggleable toggleIcon;
    protected int id = 1;

    BoundingShapeTreeItem(Toggleable toggleIcon, BoundingShapeViewable shape) {
        super(shape);
        this.toggleIcon = toggleIcon;
    }

    /**
     * Returns the tree-item's id. This id is always kept equivalent to the tree-item's index in the
     * children-list of its parent-{@link ObjectCategoryTreeItem} plus 1.
     * It is displayed as part of the name of the tree-cell this tree-item is assigned to.
     *
     * @return the id
     */
    int getId() {
        return id;
    }

    /**
     * Sets the tree-item's id. This id is always kept equivalent to the tree-item's index in the
     * children-list of its parent-{@link ObjectCategoryTreeItem} plus 1.
     * It is displayed as part of the name of the tree-cell this tree-item is assigned to.
     *
     * @param id the id to set
     */
    void setId(int id) {
        this.id = id;
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
     * Sets the toggle-state of the tree-item's toggle-square (and all its children)
     * and updates the parent {@link ObjectCategoryTreeItem} object's number of
     * toggled-on children.
     *
     * @param toggledOn true to toggle on, false to toggle off
     */
    void setIconToggledOn(boolean toggledOn) {
        if(toggledOn != isIconToggledOn()) {
            // If the toggle-state changes, update the parent-category-item's
            // toggled children count.
            if(toggledOn) {
                ((ObjectCategoryTreeItem) getParent()).incrementNrToggledOnChildren();
            } else {
                ((ObjectCategoryTreeItem) getParent()).decrementNrToggledOnChildren();
            }
        }

        toggleIcon.setToggledOn(toggledOn);

        ((Shape) getValue()).setVisible(toggledOn);

        // A BoundingShapeTreeItem either does not have any children, or
        // every child is an instance of ObjectCategoryTreeItem.
        for(TreeItem<Object> child : getChildren()) {
            ((ObjectCategoryTreeItem) child).setIconToggledOn(toggledOn);
        }
    }

    void setHighlightShape(boolean value) {
        ((BoundingShapeViewable) getValue()).getViewData().setHighlighted(value);
    }
}
