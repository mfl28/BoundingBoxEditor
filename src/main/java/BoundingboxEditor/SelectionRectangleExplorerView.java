package BoundingboxEditor;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SelectionRectangleExplorerView extends TreeView<SelectionRectangle> implements View {
    private static final String BOUNDING_BOX_TREE_VIEW_STYLE = "bounding-box-tree-view";
    private static final String DELETE_CONTEXT_MENU_STYLE = "delete-context-menu";
    private static final String DELETE_CONTEXT_MENU_TEXT = "Delete";

    private final TreeItem<SelectionRectangle> root = new TreeItem<>();


    SelectionRectangleExplorerView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        this.setRoot(root);
        this.setShowRoot(false);

        this.getStyleClass().add(BOUNDING_BOX_TREE_VIEW_STYLE);
    }
}
