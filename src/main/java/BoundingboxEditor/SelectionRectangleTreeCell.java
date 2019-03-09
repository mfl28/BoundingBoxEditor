package BoundingboxEditor;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

import java.util.List;

class SelectionRectangleTreeCell extends TreeCell<SelectionRectangle> {
    private static final String DELETE_CONTEXT_MENU_STYLE = "delete-context-menu";
    private static final String DELETE_CONTEXT_MENU_TEXT = "Delete";
    private final MenuItem deleteSelectionRectangleItem = new MenuItem(DELETE_CONTEXT_MENU_TEXT);
    private final ContextMenu contextMenu = new ContextMenu();

    SelectionRectangleTreeCell() {
        deleteSelectionRectangleItem.setId(DELETE_CONTEXT_MENU_STYLE);
        contextMenu.getItems().add(deleteSelectionRectangleItem);

        this.emptyProperty().addListener((value, oldValue, newValue) -> this.setContextMenu(newValue ? null : contextMenu));

        setUpInternalListeners();
    }

    public MenuItem getDeleteSelectionRectangleItem() {
        return deleteSelectionRectangleItem;
    }

    @Override
    protected void updateItem(SelectionRectangle newSelectionRectangle, boolean empty) {
        super.updateItem(newSelectionRectangle, empty);

        if(empty || newSelectionRectangle == null) {
            this.textProperty().unbind();
            this.setText(null);
            this.setGraphic(null);
            return;
        }

        final TreeItem<SelectionRectangle> treeItem = getTreeItem();

        setGraphic(treeItem.getGraphic());

        if(!textProperty().isBound()) {
            if(treeItem instanceof CategoryTreeItem) {
                textProperty().bind(((CategoryTreeItem) treeItem).getBoundingBoxCategory().nameProperty());
            } else if(treeItem instanceof SelectionRectangleTreeItem) {
                textProperty().bind(newSelectionRectangle.getBoundingBoxCategory().nameProperty().concat(((SelectionRectangleTreeItem) treeItem).getId()));
            }
        }

    }

    private void setUpInternalListeners() {
        this.setOnMouseEntered(event -> {
            if(!this.isEmpty()) {
                final List<TreeItem<SelectionRectangle>> childList = this.getTreeItem().getChildren();

                if(!childList.isEmpty()) {
                    childList.forEach(child -> child.getValue().fillOpaque());
                } else {
                    this.getItem().fillOpaque();
                }
            }
        });

        this.setOnMouseExited(event -> {
            if(!this.isEmpty()) {
                final List<TreeItem<SelectionRectangle>> childList = this.getTreeItem().getChildren();

                if(!childList.isEmpty()) {
                    childList.forEach(child -> child.getValue().setFill(Color.TRANSPARENT));
                } else {
                    this.getItem().setFill(Color.TRANSPARENT);
                }
            }
        });
    }
}
