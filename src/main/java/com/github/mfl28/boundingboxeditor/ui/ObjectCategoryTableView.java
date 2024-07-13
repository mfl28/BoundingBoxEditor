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

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;


/**
 * The object category-table UI-element. Shows information about the currently existing {@link ObjectCategory} objects
 * such as color and name. Each category row-entry in the table also includes a button to delete the corresponding category.
 *
 * @see TableView
 * @see View
 */
public class ObjectCategoryTableView extends TableView<ObjectCategory> implements View {
    private static final String PLACEHOLDER_TEXT = "No categories";
    private static final String TABLE_NAME_COLUMN_FACTORY_NAME = "name";
    private static final String TABLE_VIEW_STYLE = "no-header-table-view";
    private static final int TABLE_VIEW_COLOR_COLUMN_WIDTH = 5;
    private static final int TABLE_VIEW_DELETE_COLUMN_WIDTH = 18;
    private static final String OBJECT_CATEGORY_TABLE_VIEW_ID = "category-selector";
    private static final String NO_CUSTOM_COLORS_PALETTE_STYLE_SHEET = ObjectCategoryTableView.class
            .getResource("/stylesheets/css/noCustomColorsPalette.css").toExternalForm();

    private final TableColumn<ObjectCategory, ObjectCategory> deleteColumn = createDeleteColumn();
    private final TableColumn<ObjectCategory, Color> colorColumn = createColorColumn();
    private final TableColumn<ObjectCategory, String> nameColumn = createNameColumn();

    private final ColorPicker categoryColorPicker = new ColorPicker();
    private final MenuItem colorChangeItem = new MenuItem("Color", categoryColorPicker);
    private final ContextMenu contextMenu = new ContextMenu(colorChangeItem);

    /**
     * Creates a new object category table UI-element.
     */
    ObjectCategoryTableView() {
        getColumns().add(colorColumn);
        getColumns().add(nameColumn);
        getColumns().add(deleteColumn);

        getStyleClass().add(TABLE_VIEW_STYLE);
        setId(OBJECT_CATEGORY_TABLE_VIEW_ID);
        setEditable(true);
        setFocusTraversable(false);
        setPlaceholder(new Label(PLACEHOLDER_TEXT));
        getSortOrder().add(nameColumn);
        setUpInternalListeners();
        setUpFactories();
    }

    @Override
    public void connectToController(Controller controller) {
        nameColumn.setOnEditCommit(controller::onSelectorCellEditEvent);
    }

    /**
     * Returns the column containing the category-delete-button.
     *
     * @return the table column
     */
    public TableColumn<ObjectCategory, ObjectCategory> getDeleteColumn() {
        return deleteColumn;
    }

    /**
     * Returns a boolean indicating if a category is currently selected.
     *
     * @return true if a category is currently selected, otherwise false
     */
    public boolean isCategorySelected() {
        return !getSelectionModel().isEmpty();
    }

    /**
     * Returns the currently selected category.
     *
     * @return the selected category
     */
    public ObjectCategory getSelectedCategory() {
        return getSelectionModel().getSelectedItem();
    }

    ContextMenu getRowContextMenu() {
        return contextMenu;
    }

    private TableColumn<ObjectCategory, Color> createColorColumn() {
        final TableColumn<ObjectCategory, Color> tableColumn = new TableColumn<>();
        tableColumn.setMinWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        tableColumn.setMaxWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        tableColumn.setCellFactory(factory -> new ColorTableCell());
        tableColumn.setSortable(false);
        return tableColumn;
    }

    private TableColumn<ObjectCategory, String> createNameColumn() {
        final TableColumn<ObjectCategory, String> tableColumn = new TableColumn<>();
        tableColumn.setCellValueFactory(new PropertyValueFactory<>(TABLE_NAME_COLUMN_FACTORY_NAME));
        bindNameColumnWidth();

        tableColumn.setEditable(true);
        tableColumn.setSortable(true);
        tableColumn.setSortType(TableColumn.SortType.ASCENDING);
        tableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        return tableColumn;
    }

    private void bindNameColumnWidth() {
        // Looking up the needed width of the vertical scrollbar (if it is visible) is only possible
        // after the Skin has been attached to the TableView.
        ChangeListener<Skin<?>> skinChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Skin<?>> observable, Skin<?> oldValue, Skin<?> newValue) {
                skinProperty().removeListener(this);

                ScrollBar verticalScrollBar = (ScrollBar) lookup(".scroll-bar:vertical");

                // ".add(3)" is need because there seems to be some extra padding with a TableView
                // which is not taken into account by its width-property. If left out, a horizontal scroll-bar
                // is shown even though the widths of all columns add up perfectly.
                nameColumn.prefWidthProperty().bind(widthProperty()
                                                            .subtract(colorColumn.widthProperty()
                                                                                 .add(deleteColumn.widthProperty())
                                                                                 .add(3)
                                                                                 .add(Bindings
                                                                                              .when(verticalScrollBar
                                                                                                            .visibleProperty())
                                                                                              .then(verticalScrollBar
                                                                                                            .widthProperty())
                                                                                              .otherwise(0))));
            }
        };

        skinProperty().addListener(skinChangeListener);
    }

    private TableColumn<ObjectCategory, ObjectCategory> createDeleteColumn() {
        final TableColumn<ObjectCategory, ObjectCategory> tableColumn = new TableColumn<>();
        tableColumn.setMinWidth(TABLE_VIEW_DELETE_COLUMN_WIDTH);
        tableColumn.setMaxWidth(TABLE_VIEW_DELETE_COLUMN_WIDTH);
        tableColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        tableColumn.setSortable(false);
        return tableColumn;
    }

    private void setUpInternalListeners() {
        // Due to a javafx-bug a null-pointer exception is thrown when clicking
        // on the "Custom Color" hyperlink of the color-palette of a color-picker that is part of a menu-item.
        // To remove the "Custom Color" hyperlink, a stylesheet needs to be loaded when the color-picker is requested,
        // (and removed afterwards). See https://community.oracle.com/thread/2562936.

        contextMenu.setOnAction(event -> {
            getSelectedCategory().setColor(categoryColorPicker.getValue());
            categoryColorPicker.getStylesheets().remove(NO_CUSTOM_COLORS_PALETTE_STYLE_SHEET);
        });
    }

    private void setUpFactories() {
        setRowFactory(tableView -> {
            final TableRow<ObjectCategory> row = new TableRow<>();

            row.contextMenuProperty().bind(Bindings
                                                   .when(row.emptyProperty())
                                                   .then((ContextMenu) null)
                                                   .otherwise(contextMenu));

            row.setOnContextMenuRequested(event -> {
                categoryColorPicker.getStylesheets().add(NO_CUSTOM_COLORS_PALETTE_STYLE_SHEET);
                categoryColorPicker.setValue(getSelectedCategory().getColor());
            });

            return row;
        });
    }

    private static class ColorTableCell extends TableCell<ObjectCategory, Color> {
        @Override
        protected void updateItem(Color item, boolean empty) {
            super.updateItem(item, empty);

            backgroundProperty().unbind();

            if(empty || getTableRow() == null) {
                setText(null);
                setGraphic(null);
                setBackground(null);
            } else {
                final ObjectCategory categoryItem = getTableRow().getItem();

                if(categoryItem != null) {
                    backgroundProperty().bind(Bindings.createObjectBinding(() ->
                                                                                   new Background(new BackgroundFill(
                                                                                           categoryItem.getColor(),
                                                                                           null, null)),
                                                                           categoryItem.colorProperty()));
                }
            }
        }
    }
}
