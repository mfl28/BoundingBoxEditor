package boundingboxeditor.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.shape.Shape;

import java.util.Objects;

/**
 * A tree-item representing an existing {@link BoundingPolygonView} in a {@link ObjectTreeElementCell} of a {@link ObjectTreeView}.
 *
 * @see TreeItem
 */
public class BoundingPolygonTreeItem extends BoundingShapeTreeItem {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 10;

    /**
     * Creates a new tree-item representing a {@link BoundingPolygonView} in a {@link ObjectTreeElementCell} that is part of
     * a {@link ObjectTreeView}.
     *
     * @param boundingPolygon the {@link BoundingPolygonView} that should be associated with the tree-item
     */
    BoundingPolygonTreeItem(BoundingPolygonView boundingPolygon) {
        super(new TogglePolygon(TOGGLE_ICON_SIDE_LENGTH), boundingPolygon);
        setGraphic((TogglePolygon) toggleIcon);

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

    private void setUpInternalListeners() {
        ((Shape) toggleIcon).fillProperty().bind(((BoundingPolygonView) getValue()).strokeProperty());

        ((Shape) toggleIcon).setOnMousePressed(event -> {
            setIconToggledOn(!isIconToggledOn());
            event.consume();
        });
    }
}
