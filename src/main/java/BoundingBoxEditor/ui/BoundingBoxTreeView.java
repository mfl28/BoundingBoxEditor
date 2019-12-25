package BoundingBoxEditor.ui;

import BoundingBoxEditor.model.BoundingBoxCategory;
import BoundingBoxEditor.model.ImageMetaData;
import BoundingBoxEditor.model.io.BoundingBoxData;
import BoundingBoxEditor.model.io.ImageAnnotation;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.TreeViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.commons.collections4.IteratorUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The bounding-box tree UI-element. Shows information about the currently existing {@link BoundingBoxView} objects
 * in {@link BoundingBoxTreeCell}s.
 * {@link BoundingBoxView} objects are grouped by their category and nested objects are displayed in a hierarchical
 * fashion. Any path running from the root of the tree downwards consists of alternating {@link BoundingBoxCategoryTreeItem} and
 * {@link BoundingBoxTreeItem} objects in that order, i.e.: root, category-item, bounding-box-item, category-item, ...
 *
 * @see TreeView
 * @see View
 */
public class BoundingBoxTreeView extends TreeView<BoundingBoxView> implements View {
    private static final int FIXED_CELL_SIZE = 20;
    private VirtualFlow<?> virtualFlow;

    /**
     * Creates a new bounding-box tree UI-element.
     */
    BoundingBoxTreeView() {
        VBox.setVgrow(this, Priority.ALWAYS);

        setRoot(new TreeItem<>());
        setShowRoot(false);
        setFixedCellSize(FIXED_CELL_SIZE);
        setUpInternalListeners();
    }

    /**
     * Resets the tree-root and clears any selection.
     */
    @Override
    public void reset() {
        setRoot(new TreeItem<>());
        getSelectionModel().clearSelection();
    }

    /**
     * Extracts {@link BoundingBoxData} objects from the {@link BoundingBoxView} objects
     * currently represented by the tree, keeping the nesting structure.
     *
     * @return a list of {@link BoundingBoxData} objects corresponding to the top-level
     * {@link BoundingBoxView} objects (possible child elements are included in
     * the "parts" member variable of the respective {@link BoundingBoxData} object)
     */
    public List<BoundingBoxData> extractCurrentBoundingBoxData() {
        return getRoot().getChildren().stream()
                .map(TreeItem::getChildren)
                .flatMap(Collection::stream)
                .map(child -> treeItemToBoundingBoxData((BoundingBoxTreeItem) child))
                .collect(Collectors.toList());
    }

    /**
     * Sets the toggle-icon-state of all tree-items.
     *
     * @param toggleState true to to toggle on, otherwise off
     */
    public void setToggleIconStateForAllTreeItems(boolean toggleState) {
        for(TreeItem<BoundingBoxView> child : getRoot().getChildren()) {
            ((BoundingBoxCategoryTreeItem) child).setIconToggledOn(toggleState);
        }
    }

    /**
     * Sets the toggle-icon-state of the currently selected tree-item (if one is selected).
     *
     * @param toggleState the toggle-icon-state to set
     */
    public void setToggleIconStateForSelectedBoundingBoxTreeItem(boolean toggleState) {
        TreeItem<BoundingBoxView> selectedTreeItem = getSelectionModel().getSelectedItem();

        if(selectedTreeItem instanceof BoundingBoxTreeItem) {
            ((BoundingBoxTreeItem) selectedTreeItem).setIconToggledOn(toggleState);
        }
    }

    /**
     * Keeps the provided tree-item in the currently visible part of the tree-view.
     *
     * @param item the tree-item that should be kept in view
     */
    void keepTreeItemInView(TreeItem<BoundingBoxView> item) {
        if(virtualFlow != null) {
            int rowIndex = getRow(item);
            var firstVisibleCell = virtualFlow.getFirstVisibleCell();
            var lastVisibleCell = virtualFlow.getLastVisibleCell();

            if(firstVisibleCell != null && lastVisibleCell != null &&
                    (rowIndex <= firstVisibleCell.getIndex() || rowIndex >= lastVisibleCell.getIndex())) {
                virtualFlow.scrollTo(rowIndex);
            }
        }
    }

    /**
     * Takes an {@link ImageAnnotation} object and the contained structure of
     * {@link BoundingBoxData} objects and constructs the tree-structure of {@link BoundingBoxCategoryTreeItem}
     * and {@link BoundingBoxTreeItem} objects making up the displayed tree. At the same time {@link BoundingBoxView}
     * objects are extracted from the encountered {@link BoundingBoxData} objects and the resulting list
     * is returned.
     *
     * @param annotation the image-annotation
     * @return the list of extracted {@link BoundingBoxView} objects
     */
    List<BoundingBoxView> extractBoundingBoxViewsAndBuildTreeFromAnnotation(ImageAnnotation annotation) {
        ImageMetaData metaData = annotation.getImageMetaData();

        annotation.getBoundingBoxData().forEach(boundingBoxData -> constructTreeFromBoundingBoxData(getRoot(), boundingBoxData, metaData));

        return IteratorUtils.toList(new BoundingBoxTreeItemIterator(getRoot())).stream()
                .filter(treeItem -> treeItem instanceof BoundingBoxTreeItem)
                .map(TreeItem::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list containing all {@link BoundingBoxView} objects contained in the tree
     * of {@link TreeItem} objects with the provided root.
     *
     * @param root the root of the tree
     * @return the list containing the {@link BoundingBoxView} objects
     */
    static List<BoundingBoxView> getBoundingBoxViewsRecursively(TreeItem<BoundingBoxView> root) {
        if(root.isLeaf()) {
            return root instanceof BoundingBoxTreeItem ? List.of(root.getValue()) : Collections.emptyList();
        }

        BoundingBoxTreeItemIterator iterator = new BoundingBoxTreeItemIterator(root);

        return IteratorUtils.toList(iterator).stream()
                .filter(child -> child instanceof BoundingBoxTreeItem)
                .map(TreeItem::getValue).collect(Collectors.toList());
    }

    /**
     * Adds tree-items assigned to the provided {@link BoundingBoxView} objects to the current tree.
     *
     * @param boundingBoxes the bounding-boxes for which to add the tree-items
     */
    void addTreeItemsFromBoundingBoxViews(Collection<? extends BoundingBoxView> boundingBoxes) {
        boundingBoxes.forEach(boundingBox -> createTreeItemFromBoundingBoxView(getRoot(), boundingBox));
    }

    /**
     * Returns the first {@link BoundingBoxCategoryTreeItem} whose category is equal to the provided boundingBoxCategory
     * searching from the provided searchRoot downward.
     *
     * @param searchRoot          the start tree-item to search from
     * @param boundingBoxCategory the category of the sought {@link BoundingBoxCategoryTreeItem}
     * @return the {@link BoundingBoxCategoryTreeItem} if it is found, otherwise null
     */
    BoundingBoxCategoryTreeItem findParentCategoryTreeItemForCategory(TreeItem<BoundingBoxView> searchRoot, BoundingBoxCategory boundingBoxCategory) {
        return (BoundingBoxCategoryTreeItem) searchRoot.getChildren().stream()
                .filter(category -> ((BoundingBoxCategoryTreeItem) category).getBoundingBoxCategory().equals(boundingBoxCategory))
                .findFirst()
                .orElse(null);
    }

    /**
     * Expands all currently existing tree-items.
     */
    void expandAllTreeItems() {
        IteratorUtils.toList(new BoundingBoxTreeItemIterator(getRoot())).forEach(treeItem -> treeItem.setExpanded(true));
    }

    private void setUpInternalListeners() {
        skinProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue instanceof TreeViewSkin) {
                var skin = (TreeViewSkin<?>) newValue;
                var childNodes = skin.getChildren();

                if(childNodes != null && !childNodes.isEmpty()) {
                    virtualFlow = (VirtualFlow<?>) childNodes.get(0);
                }
            }
        });
    }

    private void createTreeItemFromBoundingBoxView(TreeItem<BoundingBoxView> root, BoundingBoxView boundingBox) {
        BoundingBoxTreeItem boundingBoxTreeItem = new BoundingBoxTreeItem(boundingBox);
        boundingBox.setTreeItem(boundingBoxTreeItem);
        BoundingBoxCategoryTreeItem parentBoundingBoxCategoryTreeItem = findParentCategoryTreeItemForCategory(root, boundingBox.getBoundingBoxCategory());

        if(parentBoundingBoxCategoryTreeItem != null) {
            parentBoundingBoxCategoryTreeItem.attachBoundingBoxTreeItemChild(boundingBoxTreeItem);
        } else {
            BoundingBoxCategoryTreeItem boundingBoxCategoryTreeItem = new BoundingBoxCategoryTreeItem(boundingBox.getBoundingBoxCategory());
            root.getChildren().add(boundingBoxCategoryTreeItem);
            boundingBoxCategoryTreeItem.attachBoundingBoxTreeItemChild(boundingBoxTreeItem);
        }
    }

    private void constructTreeFromBoundingBoxData(TreeItem<BoundingBoxView> root, BoundingBoxData boundingBoxData, ImageMetaData metaData) {
        BoundingBoxCategoryTreeItem boundingBoxCategoryTreeItem = findParentCategoryTreeItemForCategory(root, boundingBoxData.getCategory());

        if(boundingBoxCategoryTreeItem == null) {
            boundingBoxCategoryTreeItem = new BoundingBoxCategoryTreeItem(boundingBoxData.getCategory());
            root.getChildren().add(boundingBoxCategoryTreeItem);
        }

        BoundingBoxView newBoundingBoxView = BoundingBoxView.fromData(boundingBoxData, metaData);
        BoundingBoxTreeItem newTreeItem = new BoundingBoxTreeItem(newBoundingBoxView);
        newBoundingBoxView.setTreeItem(newTreeItem);

        boundingBoxCategoryTreeItem.attachBoundingBoxTreeItemChild(newTreeItem);

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

    private static class BoundingBoxTreeItemIterator implements Iterator<TreeItem<BoundingBoxView>> {
        private final ArrayDeque<TreeItem<BoundingBoxView>> stack = new ArrayDeque<>();

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
