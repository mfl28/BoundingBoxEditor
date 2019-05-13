package BoundingboxEditor.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


public class BoundingBoxExplorerView extends TreeView<BoundingBoxView> implements View {
    private static final String BOUNDING_BOX_TREE_VIEW_STYLE = "bounding-box-tree-view";

    BoundingBoxExplorerView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        setRoot(new TreeItem<>());
        setShowRoot(false);

        getStyleClass().add(BOUNDING_BOX_TREE_VIEW_STYLE);
    }

    @Override
    public void reset() {
        setRoot(new TreeItem<>());
    }

    public void addTreeItemsFromBoundingBoxes(Iterable<? extends BoundingBoxView> boundingBoxes) {
        for(BoundingBoxView boundingBox : boundingBoxes) {

            BoundingBoxTreeItem boundingBoxTreeItem = new BoundingBoxTreeItem(boundingBox);
            CategoryTreeItem parentCategoryTreeItem = findParentCategoryTreeItemForBoundingBox(boundingBox);

            if(parentCategoryTreeItem != null) {
                attachBoundingBoxTreeItemToCategoryTreeItem(boundingBoxTreeItem, parentCategoryTreeItem);
            } else {
                CategoryTreeItem categoryTreeItem = new CategoryTreeItem(boundingBox.getBoundingBoxCategory());
                getRoot().getChildren().add(categoryTreeItem);
                attachBoundingBoxTreeItemToCategoryTreeItem(boundingBoxTreeItem, categoryTreeItem);
            }
        }
    }

    private CategoryTreeItem findParentCategoryTreeItemForBoundingBox(BoundingBoxView item) {
        return (CategoryTreeItem) getRoot().getChildren().stream()
                .filter(category -> ((CategoryTreeItem) category).getBoundingBoxCategory().equals(item.getBoundingBoxCategory()))
                .findFirst()
                .orElse(null);
    }

    private void attachBoundingBoxTreeItemToCategoryTreeItem(BoundingBoxTreeItem boundingBoxTreeItem, CategoryTreeItem categoryTreeItem) {
        boundingBoxTreeItem.setId(categoryTreeItem.getChildren().size() + 1);
        categoryTreeItem.getChildren().add(boundingBoxTreeItem);
        getSelectionModel().select(boundingBoxTreeItem);
    }
}
