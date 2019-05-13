package BoundingboxEditor.ui;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;

class BoundingBoxTreeCell extends TreeCell<BoundingBoxView> {
    private static final String DELETE_CONTEXT_MENU_STYLE = "delete-context-menu";
    private static final String DELETE_CONTEXT_MENU_TEXT = "Delete";

    private final MenuItem deleteBoundingBoxMenuItem = new MenuItem(DELETE_CONTEXT_MENU_TEXT);
    private final ContextMenu contextMenu = new ContextMenu();

    BoundingBoxTreeCell() {
        contextMenu.getItems().add(deleteBoundingBoxMenuItem);
        deleteBoundingBoxMenuItem.setId(DELETE_CONTEXT_MENU_STYLE);

        setUpInternalListeners();
    }

    @Override
    protected void updateItem(BoundingBoxView newBoundingBoxView, boolean empty) {
        super.updateItem(newBoundingBoxView, empty);
        textProperty().unbind();

        if(empty || newBoundingBoxView == null) {
            setText(null);
            setGraphic(null);
            setContextMenu(null);
        } else {
            setContextMenu(contextMenu);
            final TreeItem<BoundingBoxView> treeItem = getTreeItem();
            setGraphic(treeItem.getGraphic());

            if(treeItem instanceof BoundingBoxTreeItem) {
                textProperty().bind(treeItem.getValue().getBoundingBoxCategory().nameProperty()
                        .concat(((BoundingBoxTreeItem) treeItem).getId()));
            } else {
                textProperty().bind(((CategoryTreeItem) treeItem).getBoundingBoxCategory().nameProperty());
            }
        }
    }

    MenuItem getDeleteBoundingBoxMenuItem() {
        return deleteBoundingBoxMenuItem;
    }

    private void setUpInternalListeners() {
        setOnMouseEntered(event -> {
            if(!isEmpty()) {
                fillBoundingBoxesOpaque();
            }
        });

        contextMenu.showingProperty().addListener(((observable, oldValue, newValue) -> {
            if(!isEmpty() && !newValue) {
                fillBoundingBoxesTransparent();
            }
        }));

        setOnMouseExited(event -> {
            if(!isEmpty() && !contextMenu.isShowing()) {
                fillBoundingBoxesTransparent();
            }
        });
    }

    private void fillBoundingBoxesOpaque() {
        TreeItem<BoundingBoxView> treeItem = getTreeItem();

        if(treeItem instanceof BoundingBoxTreeItem) {
            treeItem.getValue().fillOpaque();
        } else {
            treeItem.getChildren().forEach(child -> child.getValue().fillOpaque());
        }
    }

    private void fillBoundingBoxesTransparent() {
        TreeItem<BoundingBoxView> treeItem = getTreeItem();

        if(treeItem instanceof BoundingBoxTreeItem) {
            treeItem.getValue().fillTransparent();
        } else {
            treeItem.getChildren().forEach(child -> child.getValue().fillTransparent());
        }
    }
}
