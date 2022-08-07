/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

/**
 * Base class of all shape tree items.
 */
public abstract class BoundingShapeTreeItem extends TreeItem<Object> implements IconToggleable {
    protected final Toggleable toggleIcon;
    protected int id = 1;

    BoundingShapeTreeItem(Toggleable toggleIcon, BoundingShapeViewable shape) {
        super(shape);
        this.toggleIcon = toggleIcon;
    }

    /**
     * Returns the toggle-state of the tree-item's toggle-square.
     *
     * @return true if toggled on, false otherwise
     */
    @Override
    public boolean isIconToggledOn() {
        return toggleIcon.isToggledOn();
    }

    /**
     * Sets the toggle-state of the tree-item's toggle-square (and all its children)
     * and updates the parent {@link ObjectCategoryTreeItem} object's number of
     * toggled-on children.
     *
     * @param toggledOn true to toggle on, false to toggle off
     */
    @Override
    public void setIconToggledOn(boolean toggledOn) {
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

    void setHighlightShape(boolean value) {
        ((BoundingShapeViewable) getValue()).getViewData().setHighlighted(value);
    }
}
