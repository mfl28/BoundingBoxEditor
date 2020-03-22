package boundingboxeditor.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.shape.Shape;

import java.util.Objects;

/**
 * A tree-item representing an existing {@link BoundingBoxView} in a {@link BoundingShapeTreeCell} of a {@link ObjectTreeView}.
 *
 * @see TreeItem
 */
class BoundingBoxTreeItem extends BoundingShapeTreeItem {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 9.5;

    /**
     * Creates a new tree-item representing a {@link BoundingBoxView} in a {@link BoundingShapeTreeCell} that is part of
     * a {@link ObjectTreeView}.
     *
     * @param boundingBoxView the {@link BoundingBoxView} that should be associated with the tree-item
     */
    BoundingBoxTreeItem(BoundingBoxView boundingBoxView) {
        super(new ToggleSquare(TOGGLE_ICON_SIDE_LENGTH), boundingBoxView);
        setGraphic((ToggleSquare) toggleIcon);

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

        if(!(obj instanceof BoundingBoxTreeItem)) {
            return false;
        }

        BoundingBoxTreeItem other = (BoundingBoxTreeItem) obj;

        return id == other.id && getValue().equals(other.getValue()) && getChildren().equals(other.getChildren());
    }

    @Override
    void setHighlightShape(boolean value) {
        ((BoundingBoxView) getValue()).setHighlighted(value);
    }

    private void setUpInternalListeners() {
        ((Shape) toggleIcon).fillProperty().bind(((BoundingBoxView) getValue()).getObjectCategory().colorProperty());

        ((Shape) toggleIcon).setOnMousePressed(event -> {
            setIconToggledOn(!isIconToggledOn());
            event.consume();
        });
    }
}
