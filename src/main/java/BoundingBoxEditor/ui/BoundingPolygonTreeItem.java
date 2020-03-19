package boundingboxeditor.ui;

import javafx.scene.control.TreeItem;

import java.util.Objects;

public class BoundingPolygonTreeItem extends TreeItem<Object> {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 11;

    private final TogglePolygon toggleIcon = new TogglePolygon(TOGGLE_ICON_SIDE_LENGTH);
    private int id = 1;

    /**
     * Creates a new tree-item representing a {@link BoundingBoxView} in a {@link BoundingBoxTreeCell} that is part of
     * a {@link BoundingBoxTreeView}.
     *
     * @param boundingPolygon the {@link BoundingBoxView} that should be associated with the tree-item
     */
    BoundingPolygonTreeItem(BoundingPolygonView boundingPolygon) {
        super(boundingPolygon);
        setGraphic(toggleIcon);

        setUpInternalListeners();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getValue(), getChildren());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingPolygonTreeItem)) {
            return false;
        }

        BoundingPolygonTreeItem other = (BoundingPolygonTreeItem) obj;

        return id == other.id && getValue().equals(other.getValue()) && getChildren().equals(other.getChildren());
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
            // If the toggle-state changes, update the parent-category-items's
            // toggled children count.
            if(toggledOn) {
                ((ObjectCategoryTreeItem) getParent()).incrementNrToggledOnChildren();
            } else {
                ((ObjectCategoryTreeItem) getParent()).decrementNrToggledOnChildren();
            }
        }

        toggleIcon.setToggledOn(toggledOn);

        ((BoundingPolygonView) getValue()).setVisible(toggledOn);
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

    private void setUpInternalListeners() {
        toggleIcon.fillProperty().bind(((BoundingPolygonView) getValue()).getObjectCategory().colorProperty());

        toggleIcon.setOnMousePressed(event -> {
            setIconToggledOn(!isIconToggledOn());
            event.consume();
        });
    }
}
