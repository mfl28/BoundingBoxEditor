package BoundingboxEditor;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class BoundingBoxCategorySelectorView extends TableView<BoundingBoxCategory> implements View {
    private static final String TABLE_NAME_COLUMN_FACTORY_NAME = "name";
    private static final int TABLE_VIEW_COLOR_COLUMN_WIDTH = 5;
    private static final int TABLE_VIEW_DELETE_COLUMN_WIDTH = 19;
    private static final String TABLE_COLUMN_DELETE_BUTTON_STYLE = "delete-button";
    private static final String TABLE_VIEW_DELETE_ICON_STYLE = "icon";
    private static final String TABLE_VIEW_STYLE = "noheader-table-view";

    BoundingBoxCategorySelectorView() {
        this.setEditable(true);

        this.getColumns().add(createColorColumn());
        this.getColumns().add(createNameColumn());
        this.getColumns().add(createDeleteColumn());

        this.getStyleClass().add(TABLE_VIEW_STYLE);
        this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.setFocusTraversable(false);
    }

    private TableColumn<BoundingBoxCategory, Color> createColorColumn() {
        final TableColumn<BoundingBoxCategory, Color> colorColumn = new TableColumn<>();
        colorColumn.setMinWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        colorColumn.setMaxWidth(TABLE_VIEW_COLOR_COLUMN_WIDTH);
        colorColumn.setCellFactory(factory -> new ColorTableCell());
        return colorColumn;
    }

    private TableColumn<BoundingBoxCategory, String> createNameColumn() {
        //FIXME: Sometimes the horizontal scrollbar is shown even though theoretically the width is correct
        //       TableView seems to add some padding/border by default.
        final TableColumn<BoundingBoxCategory, String> nameColumn = new TableColumn<>();
        // TODO: maybe get the name of the "name" variable from BoundingBoxCategory (by reflection?)
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
        deleteColumn.setCellFactory(value -> new DeleteTableCell());
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
                    setStyle("-fx-background-color: " + Utils.rgbaFromColor(row.getColor()) + ";");
                }
            }
        }
    }

    private static class DeleteTableCell extends TableCell<BoundingBoxCategory, BoundingBoxCategory> {
        private final Button deleteButton = createDeleteButton();

        @Override
        protected void updateItem(BoundingBoxCategory item, boolean empty) {
            super.updateItem(item, empty);

            if(item == null) {
                setGraphic(null);
                return;
            }

            setGraphic(deleteButton);
            deleteButton.setOnAction(event -> getTableView().getItems().remove(item));
        }

        private Button createDeleteButton() {
            final Button deleteButton = new Button();
            deleteButton.getStyleClass().add(TABLE_COLUMN_DELETE_BUTTON_STYLE);
            deleteButton.setFocusTraversable(false);
            deleteButton.setPickOnBounds(true);

            final Region deleteIcon = new Region();
            deleteIcon.getStyleClass().add(TABLE_VIEW_DELETE_ICON_STYLE);
            deleteButton.setGraphic(deleteIcon);

            return deleteButton;
        }
    }
}
