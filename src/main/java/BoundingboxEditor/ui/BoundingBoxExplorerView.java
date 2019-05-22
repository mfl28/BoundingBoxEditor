package BoundingboxEditor.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


public class BoundingBoxExplorerView extends TreeView<BoundingBoxView> implements View {
    private static final String BOUNDING_BOX_TREE_VIEW_STYLE = "bounding-box-tree-view";
    private boolean autoHideNonSelected = false;

    BoundingBoxExplorerView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        setRoot(new TreeItem<>());
        setShowRoot(false);
        setFixedCellSize(20);

        getStyleClass().add(BOUNDING_BOX_TREE_VIEW_STYLE);
        setUpInternalListeners();
    }

    @Override
    public void reset() {
        setRoot(new TreeItem<>());
    }

    public boolean isAutoHideNonSelected() {
        return autoHideNonSelected;
    }

    public void setAutoHideNonSelected(boolean autoHideNonSelected) {
        this.autoHideNonSelected = autoHideNonSelected;
    }

    void addTreeItemsFromBoundingBoxes(Iterable<? extends BoundingBoxView> boundingBoxes) {
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

    private void setUpInternalListeners() {
        getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if(oldValue instanceof BoundingBoxTreeItem) {
                oldValue.getValue().setSelected(false);
            }

            if(autoHideNonSelected) {
                getRoot().getChildren()
                        .forEach(category -> category.getChildren()
                                .stream()
                                .map(child -> (BoundingBoxTreeItem) child)
                                .forEach(child -> child.setIconToggledOn(false)));
            }

            if(newValue instanceof BoundingBoxTreeItem) {
                newValue.getValue().setSelected(true);

                if(autoHideNonSelected) {
                    ((BoundingBoxTreeItem) newValue).setIconToggledOn(true);
                }
            }
        }));
    }
}
