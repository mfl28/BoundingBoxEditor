package BoundingboxEditor.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

class BoundingBoxTreeCell extends TreeCell<BoundingBoxView> {
    private static final String DELETE_CONTEXT_MENU_STYLE = "delete-context-menu";
    private static final String DELETE_CONTEXT_MENU_TEXT = "Delete";
    private static final String NAME_TEXT_STYLE = "default-text";
    private static final String INFO_TEXT_ID = "info-text";
    private static final String TAG_ICON_REGION_ID = "tag-icon";
    private static final PseudoClass draggedOverPseudoClass = PseudoClass.getPseudoClass("dragged-over");
    private static final String CATEGORY_NAME_TEXT_ID = "category-name-text";
    private static final String TREE_CELL_CONTENT_ID = "tree-cell-content";

    private final BooleanProperty draggedOver = new SimpleBooleanProperty(false);
    private final MenuItem deleteBoundingBoxMenuItem = new MenuItem(DELETE_CONTEXT_MENU_TEXT);
    private final ContextMenu contextMenu = new ContextMenu();
    private final Text nameText = new Text();
    private final Text additionalInfoText = new Text();
    private final Region tagIconRegion = createTagIconRegion();

    BoundingBoxTreeCell() {
        contextMenu.getItems().add(deleteBoundingBoxMenuItem);
        deleteBoundingBoxMenuItem.setId(DELETE_CONTEXT_MENU_STYLE);

        nameText.getStyleClass().add(NAME_TEXT_STYLE);
        additionalInfoText.setId(INFO_TEXT_ID);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setUpInternalListeners();
    }

    @Override
    protected void updateItem(BoundingBoxView newBoundingBoxView, boolean empty) {
        super.updateItem(newBoundingBoxView, empty);

        nameText.textProperty().unbind();
        nameText.setUnderline(false);
        nameText.setId(null);
        additionalInfoText.textProperty().unbind();
        tagIconRegion.visibleProperty().unbind();

        if(empty || newBoundingBoxView == null) {
            setGraphic(null);
            setContextMenu(null);
            setDraggedOver(false);
        } else {
            setGraphic(createContentBox());
            setContextMenu(contextMenu);
        }
    }

    void setDraggedOver(boolean draggedOver) {
        this.draggedOver.set(draggedOver);
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

        draggedOver.addListener((observable, oldValue, newValue) -> pseudoClassStateChanged(draggedOverPseudoClass, newValue));
    }

    private void fillBoundingBoxesOpaque() {
        TreeItem<BoundingBoxView> treeItem = getTreeItem();

        if(treeItem instanceof BoundingBoxTreeItem) {
            if(!treeItem.getValue().isSelected()) {
                treeItem.getValue().fillOpaque();
            }
        } else {
            treeItem.getChildren().stream().filter(child -> !child.getValue().isSelected()).forEach(child -> child.getValue().fillOpaque());
        }
    }

    private void fillBoundingBoxesTransparent() {
        TreeItem<BoundingBoxView> treeItem = getTreeItem();

        if(treeItem instanceof BoundingBoxTreeItem) {
            if(!treeItem.getValue().isSelected()) {
                treeItem.getValue().fillTransparent();
            }
        } else {
            treeItem.getChildren().stream().filter(child -> !child.getValue().isSelected()).forEach(child -> child.getValue().fillTransparent());
        }
    }

    private Region createTagIconRegion() {
        Region region = new Region();
        region.setId(TAG_ICON_REGION_ID);
        return region;
    }

    private HBox createContentBox() {
        final TreeItem<BoundingBoxView> treeItem = getTreeItem();
        final HBox content = new HBox(treeItem.getGraphic(), nameText);
        content.setId(TREE_CELL_CONTENT_ID);
        content.setAlignment(Pos.CENTER_LEFT);

        if(treeItem instanceof BoundingBoxTreeItem) {
            nameText.textProperty().bind(treeItem.getValue().getBoundingBoxCategory()
                    .nameProperty().concat(((BoundingBoxTreeItem) treeItem).getId()));
            tagIconRegion.visibleProperty().bind(Bindings.size(treeItem.getValue().getTags()).greaterThan(0));
            content.getChildren().add(tagIconRegion);
        } else {
            nameText.textProperty().bind(((CategoryTreeItem) treeItem).getBoundingBoxCategory().nameProperty());
            nameText.setId(CATEGORY_NAME_TEXT_ID);
            additionalInfoText.textProperty().bind(Bindings.format("(%d)", treeItem.getChildren().size()));
            content.getChildren().add(additionalInfoText);
        }

        return content;
    }
}
