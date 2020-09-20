package boundingboxeditor.ui;

import boundingboxeditor.model.data.ObjectCategory;
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
