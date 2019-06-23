package BoundingBoxEditor.ui;

import BoundingBoxEditor.controller.Controller;
import BoundingBoxEditor.model.BoundingBoxCategory;
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
 * The bounding-box category-table UI-element. Shows information about the currently existing {@link BoundingBoxCategory} objects
 * such as color and name. Each category row-entry in the table also includes a button to delete the corresponding category.
 *
 * @see TableView
 * @see View
 */
public class BoundingBoxCategoryTableView extends TableView<BoundingBoxCategory> implements View {
    private static final String PLACEHOLDER_TEXT = "No categories";
    private static final String TABLE_NAME_COLUMN_FACTORY_NAME = "name";
    private static final String TABLE_VIEW_STYLE = "no-header-table-view";
    private static final int TABLE_VIEW_COLOR_COLUMN_WIDTH = 5;
    private static final int TABLE_VIEW_DELETE_COLUMN_WIDTH = 18;
    private static final String BOUNDING_BOX_CATEGORY_TABLE_VIEW_ID = "category-selector";
    private static final String NO_CUSTOM_COLORS_PALETTE_STYLE_SHEET = BoundingBoxCategoryTableView.class
            .getResource("/stylesheets/css/noCustomColorsPalette.css").toExternalForm();

    private final TableColumn<BoundingBoxCategory, BoundingBoxCategory> deleteColumn = createDeleteColumn();
    private final TableColumn<BoundingBoxCategory, Color> colorColumn = createColorColumn();
    private final TableColumn<BoundingBoxCategory, String> nameColumn = createNameColumn();

    private final ColorPicker categoryColorPicker = new ColorPicker();
    private final MenuItem colorChangeItem = new MenuItem("Color", categoryColorPicker);
    private final ContextMenu contextMenu = new ContextMenu(colorChangeItem);

    /**
     * Creates a new bounding-box category table UI-element.
     */
    BoundingBoxCategoryTableView() {
        getColumns().add(colorColumn);
        getColumns().add(nameColumn);
        getColumns().add(deleteColumn);

        getStyleClass().add(TABLE_VIEW_STYLE);
        setId(BOUNDING_BOX_CATEGORY_TABLE_VIEW_ID);
        setEditable(true);
        setFocusTraversable(false);
        setPlaceholder(new Label(PLACEHOLDER_TEXT));
        getSortOrder().add(nameColumn);
        setContextMenu(contextMenu);
        setUpInternalListeners();
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
    public TableColumn<BoundingBoxCategory, BoundingBoxCategory> getDeleteColumn() {
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
    public BoundingBoxCategory getSelectedCategory() {
        return getSelectionModel().getSelectedItem();
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

        setOnContextMenuRequested(event -> {
            categoryColorPicker.getStylesheets().add(NO_CUSTOM_COLORS_PALETTE_STYLE_SHEET);
            categoryColorPicker.setValue(getSelectedCategory().getColor());
        });
    }

    private TableColumn<BoundingBoxCategory, Color> createColorColumn() {
        final TableColumn<BoundingBoxCategory, Color> colorColumn = new TableColumn<>();
        colorColumn.setMinWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        colorColumn.setMaxWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        colorColumn.setCellFactory(factory -> new ColorTableCell());
        colorColumn.setSortable(false);
        return colorColumn;
    }

    private TableColumn<BoundingBoxCategory, String> createNameColumn() {
        final TableColumn<BoundingBoxCategory, String> nameColumn = new TableColumn<>();
        nameColumn.setCellValueFactory(new PropertyValueFactory<>(TABLE_NAME_COLUMN_FACTORY_NAME));
        bindNameColumnWidth();

        nameColumn.setEditable(true);
        nameColumn.setSortable(true);
        nameColumn.setSortType(TableColumn.SortType.ASCENDING);
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        return nameColumn;
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
                                        .when(verticalScrollBar.visibleProperty())
                                        .then(verticalScrollBar.widthProperty())
                                        .otherwise(0))));
            }
        };

        skinProperty().addListener(skinChangeListener);
    }

    private TableColumn<BoundingBoxCategory, BoundingBoxCategory> createDeleteColumn() {
        final TableColumn<BoundingBoxCategory, BoundingBoxCategory> deleteColumn = new TableColumn<>();
        deleteColumn.setMinWidth(TABLE_VIEW_DELETE_COLUMN_WIDTH);
        deleteColumn.setMaxWidth(TABLE_VIEW_DELETE_COLUMN_WIDTH);
        deleteColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        deleteColumn.setSortable(false);
        return deleteColumn;
    }

    private static class ColorTableCell extends TableCell<BoundingBoxCategory, Color> {
        @Override
        protected void updateItem(Color item, boolean empty) {
            super.updateItem(item, empty);

            backgroundProperty().unbind();

            if(empty || getTableRow() == null) {
                setText(null);
                setGraphic(null);
                setBackground(null);
            } else {
                final BoundingBoxCategory categoryItem = getTableRow().getItem();

                if(categoryItem != null) {
                    backgroundProperty().bind(Bindings.createObjectBinding(() ->
                            new Background(new BackgroundFill(categoryItem.getColor(), null, null)), categoryItem.colorProperty()));
                }
            }
        }
    }
}
