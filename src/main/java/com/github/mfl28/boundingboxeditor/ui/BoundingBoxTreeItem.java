/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.shape.Shape;

import java.util.Objects;

/**
 * A tree-item representing an existing {@link BoundingBoxView} in a {@link ObjectTreeElementCell} of a {@link ObjectTreeView}.
 *
 * @see TreeItem
 */
class BoundingBoxTreeItem extends BoundingShapeTreeItem {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 9.5;

    /**
     * Creates a new tree-item representing a {@link BoundingBoxView} in a {@link ObjectTreeElementCell} that is part of
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

        if(!(obj instanceof BoundingBoxTreeItem other)) {
            return false;
        }

        return id == other.id && getValue().equals(other.getValue()) && getChildren().equals(other.getChildren());
    }

    private void setUpInternalListeners() {
        ((Shape) toggleIcon).fillProperty().bind(((BoundingBoxView) getValue()).strokeProperty());

        ((Shape) toggleIcon).setOnMouseClicked(event -> {
            setIconToggledOn(!isIconToggledOn());
            event.consume();
        });
    }
}
