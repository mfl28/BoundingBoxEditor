package boundingboxeditor.ui;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.model.io.*;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.TreeViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The bounding-box tree UI-element. Shows information about the currently existing bounding shape objects
 * in {@link BoundingShapeTreeCell}s.
 * Bounding shape objects are grouped by their category and nested objects are displayed in a hierarchical
 * fashion. Any path running from the root of the tree downwards consists of alternating {@link ObjectCategoryTreeItem} and
 * {@link BoundingBoxTreeItem} objects in that order, i.e.: root, category-item, bounding-box-item, category-item, ...
 *
 * @see TreeView
 * @see View
 */
public class ObjectTreeView extends TreeView<Object> implements View {
    private static final int FIXED_CELL_SIZE = 20;
    private VirtualFlow<?> virtualFlow;

    /**
     * Creates a new object tree UI-element.
     */
    ObjectTreeView() {
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
     * Extracts {@link BoundingShapeData} objects from the {@link BoundingBoxView}
     * or {@link BoundingPolygonView} objects
     * currently represented by the tree, keeping the nesting structure.
     *
     * @return a list of {@link BoundingBoxData} objects corresponding to the top-level
     * bounding shape objects (possible child elements are included in
     * the "parts" member variable of the respective {@link BoundingBoxData} object)
     */
    public List<BoundingShapeData> extractCurrentBoundingShapeData() {
        return getRoot().getChildren().stream()
                .map(TreeItem::getChildren)
                .flatMap(Collection::stream)
                .filter(child -> child.getValue() instanceof BoundingShapeDataConvertible)
                .map(this::treeItemToBoundingShapeData)
                .collect(Collectors.toList());
    }

    /**
     * Sets the toggle-icon-state of all tree-items.
     *
     * @param toggleState true to to toggle on, otherwise off
     */
    public void setToggleIconStateForAllTreeItems(boolean toggleState) {
        for(TreeItem<Object> child : getRoot().getChildren()) {
            ((ObjectCategoryTreeItem) child).setIconToggledOn(toggleState);
        }
    }

    /**
     * Sets the toggle-icon-state of the currently selected tree-item (if one is selected).
     *
     * @param toggleState the toggle-icon-state to set
     */
    public void setToggleIconStateForSelectedObjectTreeItem(boolean toggleState) {
        TreeItem<Object> selectedTreeItem = getSelectionModel().getSelectedItem();

        if(selectedTreeItem instanceof BoundingShapeTreeItem) {
            ((BoundingShapeTreeItem) selectedTreeItem).setIconToggledOn(toggleState);
        }
    }

    /**
     * Keeps the provided tree-item in the currently visible part of the tree-view.
     *
     * @param item the tree-item that should be kept in view
     */
    void keepTreeItemInView(TreeItem<Object> item) {
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
     * {@link BoundingBoxData} objects and constructs the tree-structure of {@link ObjectCategoryTreeItem}
     * and {@link BoundingBoxTreeItem} objects making up the displayed tree. At the same time bounding shape
     * objects are extracted from the encountered {@link BoundingBoxData} objects and the resulting list
     * is returned.
     *
     * @param annotation the image-annotation
     * @return the list of extracted {@link BoundingBoxView} objects
     */
    Pair<List<BoundingBoxView>, List<BoundingPolygonView>> extractViewsAndBuildTreeFromAnnotation(ImageAnnotation annotation) {
        ImageMetaData metaData = annotation.getImageMetaData();
        annotation.getBoundingShapeData().forEach(boundingShapeData ->
                boundingShapeData.accept(new TreeItemGenerator(getRoot(), boundingShapeData, metaData)));

        List<BoundingBoxView> boundingBoxViews = IteratorUtils.toList(new BoundingShapeTreeItemIterator(getRoot())).stream()
                .filter(treeItem -> treeItem instanceof BoundingBoxTreeItem)
                .map(item -> (BoundingBoxView) item.getValue())
                .collect(Collectors.toList());

        List<BoundingPolygonView> boundingPolygonViews = IteratorUtils.toList(new BoundingShapeTreeItemIterator(getRoot())).stream()
                .filter(treeItem -> treeItem instanceof BoundingPolygonTreeItem)
                .map(item -> (BoundingPolygonView) item.getValue())
                .collect(Collectors.toList());

        return new ImmutablePair<>(boundingBoxViews, boundingPolygonViews);
    }

    /**
     * Returns a list containing all {@link BoundingBoxView} objects contained in the tree
     * of {@link TreeItem} objects with the provided root.
     *
     * @param root the root of the tree
     * @return the list containing the {@link BoundingBoxView} objects
     */
    static List<BoundingBoxView> getBoundingBoxViewsRecursively(TreeItem<Object> root) {
        if(root.isLeaf()) {
            return root instanceof BoundingBoxTreeItem ? List.of((BoundingBoxView) root.getValue()) : Collections.emptyList();
        }

        BoundingShapeTreeItemIterator iterator = new BoundingShapeTreeItemIterator(root);

        return IteratorUtils.toList(iterator).stream()
                .filter(child -> child instanceof BoundingBoxTreeItem)
                .map(item -> (BoundingBoxView) item.getValue()).collect(Collectors.toList());
    }

    static List<BoundingPolygonView> getBoundingPolygonViewsRecursively(TreeItem<Object> root) {
        if(root.isLeaf()) {
            return root instanceof BoundingPolygonTreeItem ? List.of((BoundingPolygonView) root.getValue()) : Collections.emptyList();
        }

        BoundingShapeTreeItemIterator iterator = new BoundingShapeTreeItemIterator(root);

        return IteratorUtils.toList(iterator).stream()
                .filter(child -> child instanceof BoundingPolygonTreeItem)
                .map(item -> (BoundingPolygonView) item.getValue()).collect(Collectors.toList());
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
     * Adds tree-items assigned to the provided {@link BoundingBoxView} objects to the current tree.
     *
     * @param boundingPolygonViews the bounding-boxes for which to add the tree-items
     */
    void addTreeItemsFromBoundingPolygonViews(Collection<? extends BoundingPolygonView> boundingPolygonViews) {
        boundingPolygonViews.forEach(boundingPolygonView -> createTreeItemFromBoundingPolygonView(getRoot(), boundingPolygonView));
    }

    /**
     * Returns the first {@link ObjectCategoryTreeItem} whose category is equal to the provided objectCategory
     * searching from the provided searchRoot downward.
     *
     * @param searchRoot     the start tree-item to search from
     * @param objectCategory the category of the sought {@link ObjectCategoryTreeItem}
     * @return the {@link ObjectCategoryTreeItem} if it is found, otherwise null
     */
    ObjectCategoryTreeItem findParentCategoryTreeItemForCategory(TreeItem<Object> searchRoot, ObjectCategory objectCategory) {
        return (ObjectCategoryTreeItem) searchRoot.getChildren().stream()
                .filter(category -> category.getValue().equals(objectCategory))
                .findFirst()
                .orElse(null);
    }

    /**
     * Expands all currently existing tree-items.
     */
    void expandAllTreeItems() {
        IteratorUtils.toList(new BoundingShapeTreeItemIterator(getRoot())).forEach(treeItem -> treeItem.setExpanded(true));
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

    private void createTreeItemFromBoundingBoxView(TreeItem<Object> root, BoundingBoxView boundingBox) {
        BoundingBoxTreeItem boundingBoxTreeItem = new BoundingBoxTreeItem(boundingBox);
        boundingBox.setTreeItem(boundingBoxTreeItem);
        ObjectCategoryTreeItem parentObjectCategoryTreeItem = findParentCategoryTreeItemForCategory(root, boundingBox.getObjectCategory());

        if(parentObjectCategoryTreeItem != null) {
            parentObjectCategoryTreeItem.attachBoundingShapeTreeItemChild(boundingBoxTreeItem);
        } else {
            ObjectCategoryTreeItem objectCategoryTreeItem = new ObjectCategoryTreeItem(boundingBox.getObjectCategory());
            root.getChildren().add(objectCategoryTreeItem);
            objectCategoryTreeItem.attachBoundingShapeTreeItemChild(boundingBoxTreeItem);
        }
    }

    private void createTreeItemFromBoundingPolygonView(TreeItem<Object> root, BoundingPolygonView boundingPolygonView) {
        BoundingPolygonTreeItem boundingPolygonTreeItem = new BoundingPolygonTreeItem(boundingPolygonView);
        boundingPolygonView.setTreeItem(boundingPolygonTreeItem);
        ObjectCategoryTreeItem parentObjectCategoryTreeItem = findParentCategoryTreeItemForCategory(root, boundingPolygonView.getObjectCategory());

        if(parentObjectCategoryTreeItem != null) {
            parentObjectCategoryTreeItem.attachBoundingShapeTreeItemChild(boundingPolygonTreeItem);
        } else {
            ObjectCategoryTreeItem objectCategoryTreeItem = new ObjectCategoryTreeItem(boundingPolygonView.getObjectCategory());
            root.getChildren().add(objectCategoryTreeItem);
            objectCategoryTreeItem.attachBoundingShapeTreeItemChild(boundingPolygonTreeItem);
        }
    }

    private BoundingShapeData treeItemToBoundingShapeData(TreeItem<Object> treeItem) {
        if(!(treeItem.getValue() instanceof BoundingShapeDataConvertible)) {
            throw new IllegalStateException("Invalid tree item class type.");
        }

        BoundingShapeData boundingShapeData = ((BoundingShapeDataConvertible) treeItem.getValue()).toBoundingShapeData();

        if(!treeItem.isLeaf()) {
            List<BoundingShapeData> parts = treeItem.getChildren().stream()
                    .map(TreeItem::getChildren)
                    .flatMap(Collection::stream)
                    .map(this::treeItemToBoundingShapeData)
                    .collect(Collectors.toList());

            boundingShapeData.setParts(parts);
        }

        return boundingShapeData;
    }


    private static class BoundingShapeTreeItemIterator implements Iterator<TreeItem<Object>> {
        private final ArrayDeque<TreeItem<Object>> stack = new ArrayDeque<>();

        BoundingShapeTreeItemIterator(TreeItem<Object> root) {
            stack.push(root);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public TreeItem<Object> next() {
            if(stack.isEmpty()) {
                throw new NoSuchElementException();
            }

            TreeItem<Object> nextItem = stack.pop();
            nextItem.getChildren().forEach(stack::push);

            return nextItem;
        }
    }

    private class TreeItemGenerator implements BoundingShapeDataVisitor<Object> {
        TreeItem<Object> root;
        BoundingShapeData boundingShapeData;
        ImageMetaData metaData;

        public TreeItemGenerator(TreeItem<Object> root, BoundingShapeData boundingShapeData, ImageMetaData metaData) {
            this.root = root;
            this.boundingShapeData = boundingShapeData;
            this.metaData = metaData;
        }

        @Override
        public Object visit(BoundingBoxData boundingBoxData) {
            ObjectCategoryTreeItem objectCategoryTreeItem = findObjectCategoryTreeItem();
            BoundingBoxView newBoundingBoxView = BoundingBoxView.fromData(boundingBoxData, metaData);
            BoundingShapeTreeItem newTreeItem = new BoundingBoxTreeItem(newBoundingBoxView);
            newBoundingBoxView.setTreeItem(newTreeItem);

            objectCategoryTreeItem.attachBoundingShapeTreeItemChild(newTreeItem);
            boundingShapeData.getParts().forEach(part -> part.accept(new TreeItemGenerator(newTreeItem, part, metaData)));
            return null;
        }

        @Override
        public Object visit(BoundingPolygonData boundingPolygonData) {
            ObjectCategoryTreeItem objectCategoryTreeItem = findObjectCategoryTreeItem();
            BoundingPolygonView newBoundingPolygonView = BoundingPolygonView.fromData(boundingPolygonData, metaData);
            BoundingShapeTreeItem newTreeItem = new BoundingPolygonTreeItem(newBoundingPolygonView);
            newBoundingPolygonView.setTreeItem(newTreeItem);

            objectCategoryTreeItem.attachBoundingShapeTreeItemChild(newTreeItem);
            boundingShapeData.getParts().forEach(part -> part.accept(new TreeItemGenerator(newTreeItem, part, metaData)));
            return null;
        }

        private ObjectCategoryTreeItem findObjectCategoryTreeItem() {
            ObjectCategoryTreeItem objectCategoryTreeItem = findParentCategoryTreeItemForCategory(root, boundingShapeData.getCategory());

            if(objectCategoryTreeItem == null) {
                objectCategoryTreeItem = new ObjectCategoryTreeItem(boundingShapeData.getCategory());
                root.getChildren().add(objectCategoryTreeItem);
            }

            return objectCategoryTreeItem;
        }
    }
}
