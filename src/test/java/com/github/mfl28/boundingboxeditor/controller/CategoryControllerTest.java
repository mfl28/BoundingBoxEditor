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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
class CategoryControllerTest {
    private final Model model = new Model();
    private final DialogService dialogService = mock(DialogService.class);
    private final CategoryController categoryController = new CategoryController(model, dialogService, null);

    @Test
    void onAddingACategory_ShouldAddItWithNoShapes() {
        assertTrue(categoryController.addCategory("boat", Color.RED));

        assertEquals(List.of("boat"), categoryNames());
        assertEquals(Color.RED, model.getObjectCategories().getFirst().getColor());
        assertEquals(Map.of("boat", 0), model.getCategoryToAssignedBoundingShapesCountMap());
        verifyNoInteractions(dialogService);
    }

    @Test
    void onAddingACategory_WithABlankName_ShouldReportItAndNotAddIt() {
        assertFalse(categoryController.addCategory("  ", Color.RED));
        assertFalse(categoryController.addCategory(null, Color.RED));

        assertTrue(model.getObjectCategories().isEmpty());
        verify(dialogService, times(2)).displayErrorAlert(CategoryController.CREATION_ERROR_DIALOG_TITLE,
                                                          CategoryController.INVALID_NAME_ERROR_DIALOG_CONTENT, null);
    }

    @Test
    void onAddingACategory_WithAnExistingName_ShouldReportItAndNotAddIt() {
        categoryController.addCategory("boat", Color.RED);

        assertFalse(categoryController.addCategory("boat", Color.BLUE));

        assertEquals(List.of("boat"), categoryNames());
        verify(dialogService).displayErrorAlert(CategoryController.CREATION_ERROR_DIALOG_TITLE,
                                                "The category \"boat\" already exists.", null);
    }

    @Test
    void onRenamingACategory_ShouldMoveItsShapeCount() {
        categoryController.addCategory("boat", Color.RED);
        final ObjectCategory category = model.getObjectCategories().getFirst();
        categoryController.onShapeAdded("boat");
        categoryController.onShapeAdded("boat");

        assertTrue(categoryController.renameCategory(category, "boat", "ship"));

        assertEquals("ship", category.getName());
        assertEquals(Map.of("ship", 2), model.getCategoryToAssignedBoundingShapesCountMap());
    }

    @Test
    void onRenamingACategory_ToTheSameName_ShouldChangeNothing() {
        categoryController.addCategory("boat", Color.RED);
        final ObjectCategory category = model.getObjectCategories().getFirst();

        assertTrue(categoryController.renameCategory(category, "boat", "boat"));

        assertEquals(Map.of("boat", 0), model.getCategoryToAssignedBoundingShapesCountMap());
        verifyNoInteractions(dialogService);
    }

    @Test
    void onRenamingACategory_ToABlankOrExistingName_ShouldReportItAndKeepTheOldName() {
        categoryController.addCategory("boat", Color.RED);
        categoryController.addCategory("car", Color.BLUE);
        final ObjectCategory boat = model.getObjectCategories().getFirst();

        // The table cell has already set the edited name.
        boat.setName(" ");
        assertFalse(categoryController.renameCategory(boat, "boat", " "));
        assertEquals("boat", boat.getName());

        boat.setName("car");
        assertFalse(categoryController.renameCategory(boat, "boat", "car"));
        assertEquals("boat", boat.getName());

        assertEquals(Map.of("boat", 0, "car", 0), model.getCategoryToAssignedBoundingShapesCountMap());
        verify(dialogService).displayErrorAlert(CategoryController.CREATION_ERROR_DIALOG_TITLE,
                                                CategoryController.INVALID_NAME_ERROR_DIALOG_CONTENT, null);
        verify(dialogService).displayErrorAlert(CategoryController.CREATION_ERROR_DIALOG_TITLE,
                                                "The category \"car\" already exists.", null);
    }

    @Test
    void onNamingACategoryWithoutAName_ShouldStartItsShapeCount() {
        final ObjectCategory category = new ObjectCategory(null, Color.RED);

        assertTrue(categoryController.renameCategory(category, null, "boat"));

        assertEquals("boat", category.getName());
        assertEquals(0, model.getCategoryToAssignedBoundingShapesCountMap().get("boat"));
    }

    @Test
    void onDeletingACategory_WithoutShapes_ShouldRemoveIt() {
        categoryController.addCategory("boat", Color.RED);

        assertTrue(categoryController.deleteCategory(model.getObjectCategories().getFirst()));

        assertTrue(model.getObjectCategories().isEmpty());
        assertTrue(model.getCategoryToAssignedBoundingShapesCountMap().isEmpty());
        verifyNoInteractions(dialogService);
    }

    @Test
    void onDeletingACategory_WithShapes_ShouldReportHowManyAndKeepIt() {
        categoryController.addCategory("boat", Color.RED);
        final ObjectCategory boat = model.getObjectCategories().getFirst();
        categoryController.onShapeAdded("boat");

        assertFalse(categoryController.deleteCategory(boat));
        verify(dialogService).displayErrorAlert(eq(CategoryController.DELETION_ERROR_DIALOG_TITLE),
                                                endsWith("Currently there is 1 object with the category \"boat\"."),
                                                any());

        categoryController.onShapeAdded("boat");
        assertFalse(categoryController.deleteCategory(boat));
        verify(dialogService).displayErrorAlert(eq(CategoryController.DELETION_ERROR_DIALOG_TITLE),
                                                endsWith("Currently there are 2 objects with the category \"boat\"."),
                                                any());

        assertEquals(List.of("boat"), categoryNames());
    }

    @Test
    void onShapesChanging_ShouldUpdateTheCountsPerCategory() {
        categoryController.addCategory("boat", Color.RED);
        categoryController.addCategory("car", Color.BLUE);

        categoryController.onShapeAdded("boat");
        categoryController.onShapeAdded("boat");
        categoryController.onShapeCategoryChanged("boat", "car");
        categoryController.onShapeRemoved("boat");
        // A category that no longer exists isn't counted again.
        categoryController.onShapeRemoved("plane");

        assertEquals(Map.of("boat", 0, "car", 1), model.getCategoryToAssignedBoundingShapesCountMap());
    }

    private List<String> categoryNames() {
        return model.getObjectCategories().stream().map(ObjectCategory::getName).toList();
    }
}
