package BoundingboxEditor.ui;

import BoundingboxEditor.model.BoundingBoxCategory;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Region;

class DeleteTableCell extends TableCell<BoundingBoxCategory, BoundingBoxCategory> {
    private static final String DELETE_BUTTON_ID = "delete-button";
    private static final String TABLE_COLUMN_DELETE_BUTTON_STYLE = "delete-button";
    private static final String TABLE_VIEW_DELETE_ICON_STYLE = "icon";

    private final Button deleteButton = createDeleteButton();

    @Override
    protected void updateItem(BoundingBoxCategory item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(deleteButton);
        }
    }

    Button getDeleteButton() {
        return deleteButton;
    }

    private Button createDeleteButton() {
        final Button deleteButton = new Button();
        deleteButton.getStyleClass().add(TABLE_COLUMN_DELETE_BUTTON_STYLE);
        deleteButton.setFocusTraversable(false);
        deleteButton.setPickOnBounds(true);

        final Region deleteIcon = new Region();
        deleteIcon.getStyleClass().add(TABLE_VIEW_DELETE_ICON_STYLE);
        deleteButton.setId(DELETE_BUTTON_ID);
        deleteButton.setGraphic(deleteIcon);

        return deleteButton;
    }
}
