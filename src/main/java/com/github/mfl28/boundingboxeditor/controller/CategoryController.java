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
package com.github.mfl28.boundingboxeditor.controller;

import com.github.mfl28.boundingboxeditor.model.Model;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.ui.DialogService;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Map;
import java.util.Objects;

/**
 * Adds, renames and deletes object categories, and counts the bounding shapes per category. Names must not be blank
 * or taken by another category, and only categories without bounding shapes can be deleted.
 */
class CategoryController {
    static final String CREATION_ERROR_DIALOG_TITLE = "Category Creation Error";
    static final String DELETION_ERROR_DIALOG_TITLE = "Category Deletion Error";
    static final String INVALID_NAME_ERROR_DIALOG_CONTENT = "Please provide a non-blank category name.";
    private static final String DELETION_ERROR_DIALOG_CONTENT =
            "You cannot delete a category that has existing bounding-boxes assigned to it.";

    private final Model model;
    private final DialogService dialogService;
    private final Stage stage;

    CategoryController(Model model, DialogService dialogService, Stage stage) {
        this.model = model;
        this.dialogService = dialogService;
        this.stage = stage;
    }

    /**
     * Adds a category, or reports why it can't be added.
     *
     * @param name  the name
     * @param color the color
     * @return whether it was added
     */
    boolean addCategory(String name, Color color) {
        if(!isValidNewName(name)) {
            return false;
        }

        model.getObjectCategories().add(new ObjectCategory(name, color));
        return true;
    }

    /**
     * Renames a category (whose name was edited in the category table), or reports why the new name can't be used
     * and keeps the old one.
     *
     * @param category the category
     * @param oldName  its name before the edit (null for a new category)
     * @param newName  the edited name
     * @return whether it was renamed (also if the name didn't change)
     */
    boolean renameCategory(ObjectCategory category, String oldName, String newName) {
        if(Objects.equals(oldName, newName)) {
            return true;
        }

        if(!isValidNewName(newName)) {
            category.setName(oldName);
            return false;
        }

        final Map<String, Integer> shapeCounts = model.getCategoryToAssignedBoundingShapesCountMap();
        final Integer nrShapes = oldName != null ? shapeCounts.remove(oldName) : null;
        shapeCounts.put(newName, nrShapes != null ? nrShapes : 0);
        category.setName(newName);
        return true;
    }

    /**
     * Deletes a category if no bounding shapes have it, otherwise reports how many do.
     *
     * @param category the category
     * @return whether it was deleted
     */
    boolean deleteCategory(ObjectCategory category) {
        final int nrShapes = model.getCategoryToAssignedBoundingShapesCountMap().getOrDefault(category.getName(), 0);

        if(nrShapes != 0) {
            dialogService.displayErrorAlert(DELETION_ERROR_DIALOG_TITLE, DELETION_ERROR_DIALOG_CONTENT
                    + "\nCurrently there " + (nrShapes == 1 ? "is " : "are ") + nrShapes + " object"
                    + (nrShapes == 1 ? " " : "s ") + "with the category \"" + category.getName() + "\".", stage);
            return false;
        }

        model.getObjectCategories().remove(category);
        return true;
    }

    /**
     * Counts a bounding shape that was added to a category.
     *
     * @param categoryName the category's name
     */
    void onShapeAdded(String categoryName) {
        model.getCategoryToAssignedBoundingShapesCountMap().merge(categoryName, 1, Integer::sum);
    }

    /**
     * Stops counting a bounding shape that was removed from a category.
     *
     * @param categoryName the category's name
     */
    void onShapeRemoved(String categoryName) {
        model.getCategoryToAssignedBoundingShapesCountMap().computeIfPresent(categoryName, (key, count) -> count - 1);
    }

    /**
     * Moves the count of a bounding shape whose category was changed.
     *
     * @param previousCategoryName the name of its previous category
     * @param categoryName         the name of its new category
     */
    void onShapeCategoryChanged(String previousCategoryName, String categoryName) {
        onShapeRemoved(previousCategoryName);
        onShapeAdded(categoryName);
    }

    private boolean isValidNewName(String name) {
        if(name == null || name.isBlank()) {
            dialogService.displayErrorAlert(CREATION_ERROR_DIALOG_TITLE, INVALID_NAME_ERROR_DIALOG_CONTENT, stage);
            return false;
        }

        if(model.getCategoryToAssignedBoundingShapesCountMap().containsKey(name)) {
            dialogService.displayErrorAlert(CREATION_ERROR_DIALOG_TITLE,
                    "The category \"" + name + "\" already exists.", stage);
            return false;
        }

        return true;
    }
}
