package BoundingboxEditor.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class SelectionRectangleExplorerView extends TreeView<SelectionRectangle> implements View {
    private static final String BOUNDING_BOX_TREE_VIEW_STYLE = "bounding-box-tree-view";

    private final TreeItem<SelectionRectangle> root = new TreeItem<>();

    SelectionRectangleExplorerView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        this.setRoot(root);
        this.setShowRoot(false);

        this.getStyleClass().add(BOUNDING_BOX_TREE_VIEW_STYLE);
    }

    public void addTreeItemsFromSelectionRectangles(Iterable<? extends SelectionRectangle> selectionRectangles) {
        for(SelectionRectangle selectionRectangle : selectionRectangles) {
            SelectionRectangleTreeItem treeItem = new SelectionRectangleTreeItem(selectionRectangle);

            final List<TreeItem<SelectionRectangle>> categoryItems = root.getChildren();

            boolean found = false;
            for(TreeItem<SelectionRectangle> category : categoryItems) {
                if(((CategoryTreeItem) category).getBoundingBoxCategory().equals(selectionRectangle.getBoundingBoxCategory())) {
                    treeItem.setId(category.getChildren().size() + 1);
                    category.getChildren().add(treeItem);
                    found = true;
                    break;
                }
            }
            if(!found) {
                CategoryTreeItem category = new CategoryTreeItem(selectionRectangle.getBoundingBoxCategory());
                treeItem.setId(1);
                category.getChildren().add(treeItem);
                categoryItems.add(category);
            }
        }
    }
}
