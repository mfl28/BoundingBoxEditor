package BoundingboxEditor.ui;

import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.model.io.BoundingBoxData;
import BoundingboxEditor.model.io.ImageAnnotationDataElement;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.commons.collections4.IteratorUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;


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

    public List<BoundingBoxData> extractCurrentBoundingBoxData() {
        return getRoot().getChildren().stream()
                .map(TreeItem::getChildren)
                .flatMap(Collection::stream)
                .map(child -> treeItemToBoundingBoxData((BoundingBoxTreeItem) child))
                .collect(Collectors.toList());
    }

    void setAutoHideNonSelected(boolean autoHideNonSelected) {
        this.autoHideNonSelected = autoHideNonSelected;
    }

    List<BoundingBoxView> extractBoundingBoxViewsAndBuildTreeFromAnnotation(ImageAnnotationDataElement annotation) {
        ImageMetaData metaData = annotation.getImageMetaData();

        annotation.getBoundingBoxes().forEach(boundingBoxData -> constructTreeFromBoundingBoxData(getRoot(), boundingBoxData, metaData));

        return IteratorUtils.toList(new BoundingBoxTreeItemIterator(getRoot())).stream()
                .filter(treeItem -> treeItem instanceof BoundingBoxTreeItem)
                .map(TreeItem::getValue)
                .collect(Collectors.toList());
    }

    static List<BoundingBoxView> getBoundingBoxViewsRecursively(TreeItem<BoundingBoxView> root) {
        BoundingBoxTreeItemIterator iterator = new BoundingBoxTreeItemIterator(root);

        return IteratorUtils.toList(iterator).stream()
                .filter(child -> child instanceof BoundingBoxTreeItem)
                .map(TreeItem::getValue).collect(Collectors.toList());
    }

    void addTreeItemsFromBoundingBoxes(Collection<? extends BoundingBoxView> boundingBoxes) {
        boundingBoxes.forEach(boundingBox -> createTreeItemFromBoundingBoxView(getRoot(), boundingBox));
    }

    CategoryTreeItem findParentCategoryTreeItemForCategory(TreeItem<BoundingBoxView> searchRoot, BoundingBoxCategory boundingBoxCategory) {
        return (CategoryTreeItem) searchRoot.getChildren().stream()
                .filter(category -> ((CategoryTreeItem) category).getBoundingBoxCategory().equals(boundingBoxCategory))
                .findFirst()
                .orElse(null);
    }

    void attachBoundingBoxTreeItemToCategoryTreeItem(BoundingBoxTreeItem boundingBoxTreeItem, CategoryTreeItem categoryTreeItem) {
        boundingBoxTreeItem.setId(categoryTreeItem.getChildren().size() + 1);
        categoryTreeItem.getChildren().add(boundingBoxTreeItem);
        getSelectionModel().select(boundingBoxTreeItem);
    }

    private void constructTreeFromBoundingBoxData(TreeItem<BoundingBoxView> root, BoundingBoxData boundingBoxData, ImageMetaData metaData) {
        CategoryTreeItem categoryTreeItem = findParentCategoryTreeItemForCategory(root, boundingBoxData.getCategory());

        if(categoryTreeItem == null) {
            categoryTreeItem = new CategoryTreeItem(boundingBoxData.getCategory());
            root.getChildren().add(categoryTreeItem);
        }

        BoundingBoxTreeItem newTreeItem = new BoundingBoxTreeItem(BoundingBoxView.fromData(boundingBoxData, metaData));

        attachBoundingBoxTreeItemToCategoryTreeItem(newTreeItem, categoryTreeItem);

        boundingBoxData.getParts().forEach(part -> constructTreeFromBoundingBoxData(newTreeItem, part, metaData));
    }

    private BoundingBoxData treeItemToBoundingBoxData(BoundingBoxTreeItem treeItem) {
        BoundingBoxData boundingBoxData = treeItem.getValue().toBoundingBoxData();

        if(!treeItem.isLeaf()) {
            List<BoundingBoxData> parts = treeItem.getChildren().stream()
                    .map(TreeItem::getChildren)
                    .flatMap(Collection::stream)
                    .map(child -> treeItemToBoundingBoxData((BoundingBoxTreeItem) child))
                    .collect(Collectors.toList());

            boundingBoxData.setParts(parts);
        }

        return boundingBoxData;
    }

    private void createTreeItemFromBoundingBoxView(TreeItem<BoundingBoxView> root, BoundingBoxView boundingBox) {
        BoundingBoxTreeItem boundingBoxTreeItem = new BoundingBoxTreeItem(boundingBox);
        CategoryTreeItem parentCategoryTreeItem = findParentCategoryTreeItemForCategory(root, boundingBox.getBoundingBoxCategory());

        if(parentCategoryTreeItem != null) {
            attachBoundingBoxTreeItemToCategoryTreeItem(boundingBoxTreeItem, parentCategoryTreeItem);
        } else {
            CategoryTreeItem categoryTreeItem = new CategoryTreeItem(boundingBox.getBoundingBoxCategory());
            root.getChildren().add(categoryTreeItem);
            attachBoundingBoxTreeItemToCategoryTreeItem(boundingBoxTreeItem, categoryTreeItem);
        }
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

    private static class BoundingBoxTreeItemIterator implements Iterator<TreeItem<BoundingBoxView>> {
        private Stack<TreeItem<BoundingBoxView>> stack = new Stack<>();

        BoundingBoxTreeItemIterator(TreeItem<BoundingBoxView> root) {
            stack.push(root);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public TreeItem<BoundingBoxView> next() {
            TreeItem<BoundingBoxView> nextItem = stack.pop();
            nextItem.getChildren().forEach(stack::push);

            return nextItem;
        }
    }
}
