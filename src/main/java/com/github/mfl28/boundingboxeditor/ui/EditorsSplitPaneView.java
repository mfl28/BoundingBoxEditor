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

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A UI-element in the form of a side-panel which contains several UI-components which
 * can be used to interact with bounding-box data (the elements themselves, categories, tags).
 *
 * @see VBox
 * @see View
 */
public class EditorsSplitPaneView extends SplitPane implements View {
    private static final String CLASS_SELECTOR_LABEL_TEXT = "Categories";
    private static final String SEARCH_CATEGORY_PROMPT_TEXT = "Search Category";
    private static final String OBJECT_CATEGORY_ADD_BUTTON_TEXT = "Add";
    private static final String OBJECT_CATEGORY_COLOR_PICKER_STYLE = "category-color-picker";
    private static final String OBJECT_SELECTOR_LABEL_TEXT = "Objects";
    private static final String CATEGORY_INPUT_FIELD_PROMPT_TEXT = "Category Name";
    private static final String CATEGORY_INPUT_FIELD_ID = "category-input-field";
    private static final String ADD_BUTTON_ID = "add-button";
    private static final String TAG_EDITOR_LABEL_TEXT = "Tags";
    private static final String BOUNDING_SHAPE_EXPLORER_TOP_PANEL_ID = "bounding-box-explorer-top-panel";
    private static final String SEARCH_ICON_ID = "search-icon";
    private static final String SEARCH_ICON_LABEL_ID = "search-icon-label";
    private static final String CATEGORY_SELECTOR_TOP_PANEL_ID = "category-selector-top-panel";
    private static final String EXPAND_TREE_ITEMS_ICON_ID = "expand-tree-items-icon";
    private static final String COLLAPSE_TREE_ITEMS_ICON_ID = "collapse-tree-items-icon";
    private static final String CATEGORY_COLOR_PICKER_TOOLTIP_TEXT = "Category Color";
    private static final String ADD_CATEGORY_BUTTON_TOOLTIP = "Add new Category";
    private static final String COLLAPSE_TREE_ITEMS_BUTTON_TOOLTIP = "Collapse All";
    private static final String EXPAND_TREE_ITEMS_BUTTON_TOOLTIP = "Expand All";
    private static final String SIDE_PANEL_BOX_STYLE = "side-panel-box";
    private static final String EDITORS_SPLIT_PANE_ID = "editors-split-pane";
    private static final String ADD_CATEGORY_BOX_ID = "add-category-box";
    private static final String TAG_BOX_ID = "tag-box";

    private final TextField categorySearchField = new TextField();
    private final ObjectCategoryTableView objectCategoryTable = new ObjectCategoryTableView();
    private final ColorPicker categoryColorPicker = new ColorPicker();
    private final TextField categoryNameTextField = new TextField();
    private final Button addCategoryButton = new Button(OBJECT_CATEGORY_ADD_BUTTON_TEXT);

    private final ObjectTreeView objectTree = new ObjectTreeView();
    private final IconButton expandTreeItemsButton =
            new IconButton(EXPAND_TREE_ITEMS_ICON_ID, IconButton.IconType.BACKGROUND);
    private final IconButton collapseTreeItemsButton =
            new IconButton(COLLAPSE_TREE_ITEMS_ICON_ID, IconButton.IconType.BACKGROUND);

    private final TagScrollPaneView tagScrollPaneView = new TagScrollPaneView();

    /**
     * Creates a new panel containing UI-components responsible for interactions
     * with bounding-box data (the elements themselves, categories, tags).
     */
    EditorsSplitPaneView() {
        getItems().addAll(
                createCategorySelectorBox(),
                createObjectExplorerBox(),
                createTagBox()
        );

        setOrientation(Orientation.VERTICAL);
        setId(EDITORS_SPLIT_PANE_ID);
        setUpButtonsAndTextFields();
        setUpInternalListeners();
    }

    /**
     * Returns the category name-input text-field.
     *
     * @return the text-field
     */
    public TextField getCategoryNameTextField() {
        return categoryNameTextField;
    }

    /**
     * Returns the button which allows the user to add a
     * {@link ObjectCategory}.
     *
     * @return the button
     */
    public Button getAddCategoryButton() {
        return addCategoryButton;
    }

    /**
     * Returns the {@link ObjectTreeView} object which is responsible
     * for displaying currently existing bounding-shapes. It also provides
     * functionality to interact with the displayed bounding-shapes.
     *
     * @return the bounding-shape tree
     */
    public ObjectTreeView getObjectTree() {
        return objectTree;
    }

    @Override
    public void connectToController(Controller controller) {
        addCategoryButton.setOnAction(action -> controller.onRegisterAddObjectCategoryAction());
        objectCategoryTable.connectToController(controller);
        categoryNameTextField.setOnAction(action -> controller.onRegisterAddObjectCategoryAction());
    }

    @Override
    public void reset() {
        objectTree.reset();
    }

    /**
     * Returns the {@link ObjectCategoryTableView} object which is responsible for
     * displaying the currently existing bounding-shape categories. It also provides
     * functionality to interact with the displayed categories.
     *
     * @return the bounding-shape category table
     */
    ObjectCategoryTableView getObjectCategoryTable() {
        return objectCategoryTable;
    }

    /**
     * Returns the category-name search text-field.
     *
     * @return the text-field
     */
    TextField getCategorySearchField() {
        return categorySearchField;
    }

    /**
     * Returns the {@link ColorPicker} object which allows the user to
     * choose the color which should be associated with a new bounding-shape category.
     *
     * @return the color-picker
     */
    ColorPicker getCategoryColorPicker() {
        return categoryColorPicker;
    }

    /**
     * Returns the tag-input text-field.
     *
     * @return the text-field
     */
    TextField getTagInputField() {
        return tagScrollPaneView.getTagInputField();
    }

    private HBox createCategorySelectorTopPanel() {
        HBox.setHgrow(categorySearchField, Priority.ALWAYS);

        categorySearchField.setPromptText(SEARCH_CATEGORY_PROMPT_TEXT);
        categorySearchField.setFocusTraversable(false);

        Region categorySearchIcon = new Region();
        categorySearchIcon.setId(SEARCH_ICON_ID);

        Label categorySearchIconLabel = new Label();
        categorySearchIconLabel.setGraphic(categorySearchIcon);
        categorySearchIconLabel.setId(SEARCH_ICON_LABEL_ID);

        HBox categorySelectorTopPanel = new HBox(categorySearchIconLabel, categorySearchField);
        categorySelectorTopPanel.setId(CATEGORY_SELECTOR_TOP_PANEL_ID);

        return categorySelectorTopPanel;
    }

    private HBox createAddCategoryControlBox() {
        addCategoryButton.setFocusTraversable(false);

        HBox addCategoryBox = new HBox(categoryColorPicker, UiUtils.createHSpacer(),
                                       categoryNameTextField, UiUtils.createHSpacer(), addCategoryButton);
        categoryNameTextField.setPromptText(CATEGORY_INPUT_FIELD_PROMPT_TEXT);

        addCategoryBox.setId(ADD_CATEGORY_BOX_ID);

        return addCategoryBox;
    }

    private HBox createBoundingShapeExplorerTopPanel() {
        HBox panel = new HBox(
                new Label(OBJECT_SELECTOR_LABEL_TEXT),
                UiUtils.createHSpacer(),
                collapseTreeItemsButton,
                expandTreeItemsButton
        );

        panel.setId(BOUNDING_SHAPE_EXPLORER_TOP_PANEL_ID);
        return panel;
    }

    private VBox createCategorySelectorBox() {
        VBox categorySelectorBox = new VBox(
                new Label(CLASS_SELECTOR_LABEL_TEXT),
                createCategorySelectorTopPanel(),
                objectCategoryTable,
                createAddCategoryControlBox()
        );

        VBox.setVgrow(objectCategoryTable, Priority.ALWAYS);
        categorySelectorBox.getStyleClass().add(SIDE_PANEL_BOX_STYLE);

        return categorySelectorBox;
    }

    private VBox createObjectExplorerBox() {
        VBox objectExplorerBox = new VBox(
                createBoundingShapeExplorerTopPanel(),
                objectTree
        );

        VBox.setVgrow(objectTree, Priority.ALWAYS);
        objectExplorerBox.getStyleClass().add(SIDE_PANEL_BOX_STYLE);

        return objectExplorerBox;
    }

    private VBox createTagBox() {
        VBox tagBox = new VBox(
                new Label(TAG_EDITOR_LABEL_TEXT),
                tagScrollPaneView
        );

        VBox.setVgrow(tagScrollPaneView, Priority.ALWAYS);
        tagBox.getStyleClass().add(SIDE_PANEL_BOX_STYLE);
        tagBox.setId(TAG_BOX_ID);

        SplitPane.setResizableWithParent(tagBox, false);
        return tagBox;
    }

    private void setUpButtonsAndTextFields() {
        categoryNameTextField.setId(CATEGORY_INPUT_FIELD_ID);
        categoryNameTextField
                .setTooltip(UiUtils.createFocusTooltip(Controller.KeyCombinations.focusCategoryNameTextField));

        categoryColorPicker.getStyleClass().add(OBJECT_CATEGORY_COLOR_PICKER_STYLE);
        categoryColorPicker.setValue(ColorUtils.createRandomColor());
        categoryColorPicker.setTooltip(UiUtils.createTooltip(CATEGORY_COLOR_PICKER_TOOLTIP_TEXT));

        addCategoryButton.setId(ADD_BUTTON_ID);
        addCategoryButton.setTooltip(UiUtils.createTooltip(ADD_CATEGORY_BUTTON_TOOLTIP));

        categorySearchField.setTooltip(UiUtils.createFocusTooltip(Controller.KeyCombinations.focusCategorySearchField));

        collapseTreeItemsButton.setTooltip(UiUtils.createTooltip(COLLAPSE_TREE_ITEMS_BUTTON_TOOLTIP));

        expandTreeItemsButton.setTooltip(UiUtils.createTooltip(EXPAND_TREE_ITEMS_BUTTON_TOOLTIP));
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        categorySearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
                objectCategoryTable.getItems().stream()
                                   .filter(item -> item.getName().startsWith(newValue))
                                   .findAny()
                                   .ifPresent(item -> {
                                       objectCategoryTable.getSelectionModel().select(item);
                                       objectCategoryTable.scrollTo(item);
                                   });
            }
        });

        categorySearchField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!Boolean.TRUE.equals(newValue)) {
                categorySearchField.setText(null);
            }
        });

        expandTreeItemsButton.setOnAction(event -> objectTree.expandAllTreeItems());

        collapseTreeItemsButton.setOnAction(event ->
                                                    objectTree.getRoot().getChildren()
                                                              .forEach(child -> child.setExpanded(false)));

        objectTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue instanceof BoundingShapeTreeItem) {
                tagScrollPaneView.setTags(((BoundingShapeViewable) newValue.getValue()).getViewData().getTags());
            } else {
                tagScrollPaneView.setTags(null);
            }
        });
    }
}
