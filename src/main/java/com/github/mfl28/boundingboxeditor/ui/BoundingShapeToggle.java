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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

/**
 * A bounding shape view that can be selected as part of a {@link ToggleGroup}, which allows a single-selection
 * mechanism. The toggle state is kept in the shape's {@link BoundingShapeViewData}.
 */
interface BoundingShapeToggle extends Toggle, BoundingShapeViewable {
    @Override
    default ToggleGroup getToggleGroup() {
        return getViewData().getToggleGroup();
    }

    @Override
    default void setToggleGroup(ToggleGroup toggleGroup) {
        getViewData().setToggleGroup(toggleGroup);
    }

    @Override
    default ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return getViewData().toggleGroupProperty();
    }

    @Override
    default boolean isSelected() {
        return getViewData().isSelected();
    }

    @Override
    default void setSelected(boolean selected) {
        getViewData().setSelected(selected);
    }

    @Override
    default BooleanProperty selectedProperty() {
        return getViewData().selectedProperty();
    }
}
