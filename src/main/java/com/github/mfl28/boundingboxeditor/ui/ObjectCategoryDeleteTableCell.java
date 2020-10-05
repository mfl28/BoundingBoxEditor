/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

/**
 * A {@link TableCell} used in a {@link ObjectCategoryTableView}, which contains the user-controls to delete
 * a {@link ObjectCategory} object.
 */
public class ObjectCategoryDeleteTableCell extends TableCell<ObjectCategory, ObjectCategory> {
    private static final String DELETE_BUTTON_ID = "delete-button";
    private static final String DELETE_ICON_ID = "delete-icon";

    private final Button deleteButton = new IconButton(DELETE_ICON_ID, IconButton.IconType.GRAPHIC);

    /**
     * Creates a new table-cell containing the user-controls to delete a {@link ObjectCategory} object.
     */
    public ObjectCategoryDeleteTableCell() {
        deleteButton.setId(DELETE_BUTTON_ID);
    }

    /**
     * Returns the object-category delete-button.
     *
     * @return the button
     */
    public Button getDeleteButton() {
        return deleteButton;
    }

    @Override
    protected void updateItem(ObjectCategory item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(deleteButton);
        }
    }
}
