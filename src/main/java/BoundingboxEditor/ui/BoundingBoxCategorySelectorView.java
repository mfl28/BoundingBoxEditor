package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.BoundingBoxCategory;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;

class BoundingBoxCategorySelectorView extends TableView<BoundingBoxCategory> implements View {
    private static final String TABLE_NAME_COLUMN_FACTORY_NAME = "name";
    private static final int TABLE_VIEW_COLOR_COLUMN_WIDTH = 5;
    private static final int TABLE_VIEW_DELETE_COLUMN_WIDTH = 19;
    private static final String TABLE_VIEW_STYLE = "noheader-table-view";
    private static final String PLACEHOLDER_TEXT = "No categories";
    private static final String BOUNDING_BOX_CATEGORY_SELECTOR_ID = "category-selector";

    private final TableColumn<BoundingBoxCategory, BoundingBoxCategory> deleteColumn = createDeleteColumn();
    private final TableColumn<BoundingBoxCategory, String> nameColumn = createNameColumn();

    BoundingBoxCategorySelectorView() {
        getColumns().add(createColorColumn());
        getColumns().add(nameColumn);
        getColumns().add(deleteColumn);

        getStyleClass().add(TABLE_VIEW_STYLE);
        setEditable(true);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setFocusTraversable(false);
        setPlaceholder(new Label(PLACEHOLDER_TEXT));
        setId(BOUNDING_BOX_CATEGORY_SELECTOR_ID);
    }

    @Override
    public void connectToController(Controller controller) {
        nameColumn.setOnEditCommit(controller::onSelectorCellEditEvent);
    }

    TableColumn<BoundingBoxCategory, BoundingBoxCategory> getDeleteColumn() {
        return deleteColumn;
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
        //FIXME: Sometimes the horizontal scrollbar is shown even though theoretically the width is correct
        //       TableView seems to add some padding/border by default.
        final TableColumn<BoundingBoxCategory, String> nameColumn = new TableColumn<>();
        nameColumn.setCellValueFactory(new PropertyValueFactory<>(TABLE_NAME_COLUMN_FACTORY_NAME));
        nameColumn.setEditable(true);
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        return nameColumn;
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
            if(empty || getTableRow() == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                final BoundingBoxCategory row = getTableRow().getItem();
                if(row != null) {
                    setStyle("-fx-background-color: " + rgbaFromColor(row.getColor()) + ";");
                }
            }
        }

        private String rgbaFromColor(Color color) {
            return String.format("rgba(%d, %d, %d, %f)",
                    (int) (255 * color.getRed()),
                    (int) (255 * color.getGreen()),
                    (int) (255 * color.getBlue()),
                    color.getOpacity());
        }
    }

}
