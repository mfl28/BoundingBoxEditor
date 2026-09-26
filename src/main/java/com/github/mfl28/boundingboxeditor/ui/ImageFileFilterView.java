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

import com.github.mfl28.boundingboxeditor.model.ImageFileFilter;
import com.github.mfl28.boundingboxeditor.model.ImageFileFilter.AnnotationStatus;
import com.github.mfl28.boundingboxeditor.model.ImageFileFilter.CategoryMatch;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckListView;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The controls of the image file filter (annotation status and categories), shown in a popup of the
 * {@link ImageFileExplorerView}. The file name part of the filter comes from the explorer's search field.
 */
public class ImageFileFilterView extends VBox implements View {
    private static final String FILTER_BOX_ID = "image-file-filter-box";
    private static final String STATUS_LABEL_TEXT = "Status";
    private static final String CATEGORIES_LABEL_TEXT = "Categories";
    private static final String ALL_STATUS_TEXT = "All";
    private static final String ANNOTATED_STATUS_TEXT = "Annotated";
    private static final String NOT_ANNOTATED_STATUS_TEXT = "Not annotated";
    private static final String ANY_CATEGORY_TEXT = "Any";
    private static final String ALL_CATEGORIES_TEXT = "All";
    private static final String CLEAR_FILTER_TEXT = "Clear filter";

    private final ToggleGroup statusGroup = new ToggleGroup();
    private final RadioButton allStatusButton = createStatusButton(ALL_STATUS_TEXT, AnnotationStatus.ALL,
            "image-file-filter-status-all");
    private final RadioButton annotatedStatusButton = createStatusButton(ANNOTATED_STATUS_TEXT,
            AnnotationStatus.ANNOTATED, "image-file-filter-status-annotated");
    private final RadioButton notAnnotatedStatusButton = createStatusButton(NOT_ANNOTATED_STATUS_TEXT,
            AnnotationStatus.NOT_ANNOTATED, "image-file-filter-status-not-annotated");
    private final ToggleGroup categoryMatchGroup = new ToggleGroup();
    private final ToggleButton anyCategoryButton = createCategoryMatchButton(ANY_CATEGORY_TEXT, CategoryMatch.ANY,
            "image-file-filter-category-any");
    private final ToggleButton allCategoriesButton = createCategoryMatchButton(ALL_CATEGORIES_TEXT,
            CategoryMatch.ALL, "image-file-filter-category-all");
    private final CheckListView<ObjectCategory> categoryList = new CheckListView<>();
    private final Button clearButton = new Button(CLEAR_FILTER_TEXT);
    private final ListChangeListener<ObjectCategory> checkedCategoriesListener = change -> fireFilterChanged();
    private Consumer<ImageFileFilter> onFilterChanged = filter -> {};
    private boolean updatingControls;

    ImageFileFilterView() {
        setId(FILTER_BOX_ID);

        categoryList.setId("image-file-filter-category-list");
        categoryList.setCellFactory(listView -> new CategoryCell(categoryList));
        clearButton.setId("image-file-filter-clear-button");

        final HBox categoriesHeader = new HBox(6, new Label(CATEGORIES_LABEL_TEXT), anyCategoryButton,
                allCategoriesButton);
        categoriesHeader.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(new Label(STATUS_LABEL_TEXT), allStatusButton, annotatedStatusButton,
                notAnnotatedStatusButton, categoriesHeader, categoryList, clearButton);

        allStatusButton.setSelected(true);
        anyCategoryButton.setSelected(true);

        setUpInternalListeners();
    }

    /**
     * Resets all controls to "no filter" without reporting a filter change.
     */
    @Override
    public void reset() {
        updatingControls = true;
        try {
            allStatusButton.setSelected(true);
            anyCategoryButton.setSelected(true);
            categoryList.getCheckModel().clearChecks();
        } finally {
            updatingControls = false;
        }
    }

    /**
     * Sets the categories that can be selected.
     *
     * @param categories the categories
     */
    public void setCategories(ObservableList<ObjectCategory> categories) {
        categoryList.setItems(categories);
    }

    /**
     * Sets the function that receives the status/category criteria whenever the user changes them.
     * The name query of the passed filter is empty.
     *
     * @param onFilterChanged the function
     */
    void setOnFilterChanged(Consumer<ImageFileFilter> onFilterChanged) {
        this.onFilterChanged = onFilterChanged;
    }

    /**
     * Returns the status/category criteria that the controls currently show (with an empty name query).
     * Category names are read at call time, so renamed categories are handled.
     *
     * @return the filter
     */
    ImageFileFilter getFilter() {
        final ObservableList<ObjectCategory> items =
                categoryList.getItems() == null ? FXCollections.emptyObservableList() : categoryList.getItems();

        return new ImageFileFilter((AnnotationStatus) statusGroup.getSelectedToggle().getUserData(),
                categoryList.getCheckModel().getCheckedItems().stream()
                            .filter(items::contains)
                            .map(ObjectCategory::getName)
                            .collect(Collectors.toSet()),
                (CategoryMatch) categoryMatchGroup.getSelectedToggle().getUserData(),
                "");
    }

    RadioButton getAnnotatedStatusButton() {
        return annotatedStatusButton;
    }

    RadioButton getNotAnnotatedStatusButton() {
        return notAnnotatedStatusButton;
    }

    ToggleButton getAllCategoriesButton() {
        return allCategoriesButton;
    }

    /**
     * Updates the shown category names, which may have been renamed since the list was last shown.
     */
    void refreshCategoryNames() {
        categoryList.refresh();
    }

    CheckListView<ObjectCategory> getCategoryList() {
        return categoryList;
    }

    Button getClearButton() {
        return clearButton;
    }

    private RadioButton createStatusButton(String text, AnnotationStatus status, String id) {
        final RadioButton button = new RadioButton(text);
        button.setUserData(status);
        button.setToggleGroup(statusGroup);
        button.setId(id);
        return button;
    }

    private ToggleButton createCategoryMatchButton(String text, CategoryMatch match, String id) {
        final ToggleButton button = new ToggleButton(text);
        button.setUserData(match);
        button.setToggleGroup(categoryMatchGroup);
        button.setId(id);
        return button;
    }

    private void setUpInternalListeners() {
        keepOneSelected(statusGroup);
        keepOneSelected(categoryMatchGroup);

        statusGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> fireFilterChanged());
        categoryMatchGroup.selectedToggleProperty()
                          .addListener((observable, oldValue, newValue) -> fireFilterChanged());
        // The list view creates a new check model whenever its items are set.
        categoryList.getCheckModel().getCheckedItems().addListener(checkedCategoriesListener);
        categoryList.checkModelProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue != null) {
                oldValue.getCheckedItems().removeListener(checkedCategoriesListener);
            }

            if(newValue != null) {
                newValue.getCheckedItems().addListener(checkedCategoriesListener);
            }
        });
        categoryList.itemsProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                newValue.addListener(this::onCategoriesChanged);
            }
        });

        clearButton.setOnAction(event -> {
            reset();
            fireFilterChanged();
        });
    }

    private void onCategoriesChanged(ListChangeListener.Change<? extends ObjectCategory> change) {
        boolean checkedRemoved = false;

        while(change.next()) {
            if(change.wasRemoved() && !change.wasReplaced()) {
                final List<ObjectCategory> removed = List.copyOf(change.getRemoved());
                checkedRemoved |= removed.stream().anyMatch(categoryList.getCheckModel()::isChecked);
            }
        }

        if(checkedRemoved) {
            fireFilterChanged();
        }
    }

    private void fireFilterChanged() {
        if(!updatingControls) {
            onFilterChanged.accept(getFilter());
        }
    }

    private static void keepOneSelected(ToggleGroup group) {
        group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null && oldValue != null) {
                group.selectToggle(oldValue);
            }
        });
    }

    private static class CategoryCell extends CheckBoxListCell<ObjectCategory> {
        CategoryCell(CheckListView<ObjectCategory> listView) {
            super(listView::getItemBooleanProperty, new StringConverter<>() {
                @Override
                public String toString(ObjectCategory category) {
                    return category == null ? "" : category.getName();
                }

                @Override
                public ObjectCategory fromString(String string) {
                    return null;
                }
            });
        }
    }
}
