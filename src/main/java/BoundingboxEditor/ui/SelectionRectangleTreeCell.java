package BoundingboxEditor.ui;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

class SelectionRectangleTreeCell extends TreeCell<SelectionRectangle> {
    private static final String DELETE_CONTEXT_MENU_STYLE = "delete-context-menu";
    private static final String DELETE_CONTEXT_MENU_TEXT = "Delete";
    private final MenuItem deleteSelectionRectangleItem = new MenuItem(DELETE_CONTEXT_MENU_TEXT);
    private final ContextMenu contextMenu = new ContextMenu();

    SelectionRectangleTreeCell() {
        deleteSelectionRectangleItem.setId(DELETE_CONTEXT_MENU_STYLE);
        contextMenu.getItems().add(deleteSelectionRectangleItem);

        setUpInternalListeners();
    }

    @Override
    protected void updateItem(SelectionRectangle newSelectionRectangle, boolean empty) {
        super.updateItem(newSelectionRectangle, empty);
        this.textProperty().unbind();

        if(empty || newSelectionRectangle == null) {
            this.setText(null);
            this.setGraphic(null);
            this.setContextMenu(null);
            return;
        }

        this.setContextMenu(contextMenu);
        final TreeItem<SelectionRectangle> treeItem = getTreeItem();

        this.setGraphic(treeItem.getGraphic());

        if(!textProperty().isBound()) {
            if(treeItem instanceof CategoryTreeItem) {
                textProperty().bind(((CategoryTreeItem) treeItem).getBoundingBoxCategory().nameProperty());
            } else if(treeItem instanceof SelectionRectangleTreeItem) {
                SelectionRectangleTreeItem selectionRectangleTreeItem = (SelectionRectangleTreeItem) treeItem;
                textProperty().bind(selectionRectangleTreeItem.getValue().getBoundingBoxCategory().nameProperty().concat(selectionRectangleTreeItem.getId()));
            }
        }

    }

    MenuItem getDeleteSelectionRectangleItem() {
        return deleteSelectionRectangleItem;
    }

    private void setUpInternalListeners() {
        this.setOnMouseEntered(event -> {
            if(!this.isEmpty()) {
                final TreeItem<SelectionRectangle> treeItem = this.getTreeItem();

                if(treeItem instanceof SelectionRectangleTreeItem) {
                    treeItem.getValue().fillOpaque();
                } else {
                    treeItem.getChildren().forEach(child -> child.getValue().fillOpaque());
                }
            }
        });

        this.setOnMouseExited(event -> {
            if(!this.isEmpty()) {
                final TreeItem<SelectionRectangle> treeItem = this.getTreeItem();

                if(treeItem instanceof SelectionRectangleTreeItem) {
                    treeItem.getValue().setFill(Color.TRANSPARENT);
                } else {
                    treeItem.getChildren().forEach(child -> child.getValue().setFill(Color.TRANSPARENT));
                }
            }
        });
    }
}
