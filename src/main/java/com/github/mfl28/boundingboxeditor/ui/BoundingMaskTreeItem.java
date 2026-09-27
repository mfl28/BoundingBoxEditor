/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * The tree-item of a {@link BoundingMaskView}. Its icon is a rounded square in the category's color.
 */
class BoundingMaskTreeItem extends BoundingShapeTreeItem {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 10;
    private static final double TOGGLE_ICON_CORNER_ARC = 6;

    BoundingMaskTreeItem(BoundingMaskView boundingMaskView) {
        super(new ToggleSquare(TOGGLE_ICON_SIDE_LENGTH), boundingMaskView);
        setGraphic((ToggleSquare) toggleIcon);
        setUpInternalListeners();
    }

    @Override
    public int hashCode() {
        // The shape is left out, as equals() compares it by its data.
        return Objects.hash(id, getChildren());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingMaskTreeItem other)) {
            return false;
        }

        return id == other.id && haveEqualShapeData(getValue(), other.getValue())
                && getChildren().equals(other.getChildren());
    }

    private void setUpInternalListeners() {
        final ToggleSquare icon = (ToggleSquare) toggleIcon;
        final BoundingShapeViewData viewData = ((BoundingMaskView) getValue()).getViewData();

        icon.setArcWidth(TOGGLE_ICON_CORNER_ARC);
        icon.setArcHeight(TOGGLE_ICON_CORNER_ARC);
        bindFillToCategoryColor(icon, viewData.getObjectCategory());
        viewData.objectCategoryProperty().addListener(
                (observable, oldValue, newValue) -> bindFillToCategoryColor(icon, newValue));

        icon.setOnMouseClicked(event -> {
            setIconToggledOn(!isIconToggledOn());
            event.consume();
        });
    }

    private static void bindFillToCategoryColor(ToggleSquare icon, ObjectCategory category) {
        icon.fillProperty().unbind();

        if(category != null) {
            icon.fillProperty().bind(category.colorProperty());
        } else {
            icon.setFill(Color.GRAY);
        }
    }
}
