package com.github.mfl28.boundingboxeditor.ui;

import javafx.scene.shape.Shape;

import java.util.Objects;

public class BoundingFreehandShapeTreeItem extends BoundingShapeTreeItem {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 10;

    BoundingFreehandShapeTreeItem(BoundingShapeViewable shape) {
        super(new TogglePolygon(TOGGLE_ICON_SIDE_LENGTH), shape);
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

        if(!(obj instanceof BoundingFreehandShapeTreeItem)) {
            return false;
        }

        BoundingFreehandShapeTreeItem other = (BoundingFreehandShapeTreeItem) obj;

        return id == other.id && getValue().equals(other.getValue()) && getChildren().equals(other.getChildren());
    }

    private void setUpInternalListeners() {
        ((Shape) toggleIcon).fillProperty().bind(((BoundingFreehandShapeView) getValue()).strokeProperty());

        ((Shape) toggleIcon).setOnMouseClicked(event -> {
            setIconToggledOn(!isIconToggledOn());
            event.consume();
        });
    }
}
