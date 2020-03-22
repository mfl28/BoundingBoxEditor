package boundingboxeditor.ui;

import javafx.scene.shape.Shape;

import java.util.Objects;

public class BoundingPolygonTreeItem extends BoundingShapeTreeItem {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 10;

    /**
     * Creates a new tree-item representing a {@link BoundingBoxView} in a {@link BoundingShapeTreeCell} that is part of
     * a {@link ObjectTreeView}.
     *
     * @param boundingPolygon the {@link BoundingBoxView} that should be associated with the tree-item
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

    @Override
    void setHighlightShape(boolean value) {
        ((BoundingPolygonView) getValue()).setHighlighted(value);
    }

    private void setUpInternalListeners() {
        ((Shape) toggleIcon).fillProperty().bind(((BoundingPolygonView) getValue()).getObjectCategory().colorProperty());

        ((Shape) toggleIcon).setOnMousePressed(event -> {
            setIconToggledOn(!isIconToggledOn());
            event.consume();
        });
    }
}
