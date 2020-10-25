/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.model.data.*;
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
 * The bounding-box tree UI-element. Shows information about the currently existing bounding shape objects
 * in {@link ObjectTreeElementCell}s.
 * Bounding shape objects are grouped by their category and nested objects are displayed in a hierarchical
 * fashion. Any path running from the root of the tree downwards consists of alternating {@link ObjectCategoryTreeItem} and
 * {@link BoundingShapeTreeItem} objects in that order, i.e.: root, category-item, bounding-shape-item, category-item, ...
 *
 * @see TreeView
 * @see View
 */
public class ObjectTreeView extends TreeView<Object> implements View {
    private static final String INVALID_DRAGGED_OBJECT_CLASS_TYPE_ERROR_MESSAGE = "Invalid dragged object class type.";
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
        final TreeItem<Object> selectedTreeItem = getSelectionModel().getSelectedItem();

        if(selectedTreeItem instanceof IconToggleable) {
            ((IconToggleable) selectedTreeItem).setIconToggledOn(toggleState);
        }
    }

    /**
     * Sets the toggle-icon state of all currently not selected tree-items.
     *
     * @param toggleState the toggle-state to set
     */
    public void setToggleIconStateForNonSelectedObjectTreeItems(boolean toggleState) {
        final TreeItem<Object> selectedTreeItem = getSelectionModel().getSelectedItem();
        setToggleIconStateForAllTreeItemsExcept(selectedTreeItem, toggleState);
    }

    void setToggleIconStateForAllTreeItemsExcept(TreeItem<Object> exemption, boolean toggleState) {
        if(exemption instanceof IconToggleable) {
            final boolean selectedItemToggledOn = ((IconToggleable) exemption).isIconToggledOn();

            setToggleIconStateForAllTreeItems(toggleState);

            ((IconToggleable) exemption).setIconToggledOn(selectedItemToggledOn);
        }
    }

    /**
     * Takes a tree-item, detaches it from its parent and attaches it to a new target tree-item.
     *
     * @param treeItemToMove the tree-item to relocate
     * @param targetTreeItem the target parent tree-item
     */
    void reattachTreeItemToNewTargetTreeItem(TreeItem<Object> treeItemToMove, TreeItem<Object> targetTreeItem) {
        detachTreeItemFromParent(treeItemToMove);
        attachTreeItemToTarget(treeItemToMove, targetTreeItem);

        getSelectionModel().select(treeItemToMove);
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
     * {@link BoundingShapeData} objects and constructs the tree-structure of {@link ObjectCategoryTreeItem}
     * and {@link BoundingShapeViewable} objects making up the displayed tree. At the same time bounding shape
     * objects are extracted from the encountered {@link BoundingShapeData} objects and the resulting list
     * is returned.
     *
     * @param annotation the image-annotation
     * @return the list of extracted {@link BoundingShapeViewable} objects
     */
    List<BoundingShapeViewable> extractBoundingShapesAndBuildTreeFromAnnotation(ImageAnnotation annotation) {
        ImageMetaData metaData = annotation.getImageMetaData();
        annotation.getBoundingShapeData().forEach(boundingShapeData ->
                                                          generateNewTreeItem(getRoot(), boundingShapeData,
                                                                              metaData.getImageWidth(),
                                                                              metaData.getImageHeight()));

        return IteratorUtils.toList(new BoundingShapeTreeItemIterator(getRoot())).stream()
                            .filter(treeItem -> treeItem.getValue() instanceof BoundingShapeViewable)
                            .map(item -> (BoundingShapeViewable) item.getValue())
                            .collect(Collectors.toList());
    }

    /**
     * Returns a list containing all {@link BoundingShapeViewable} objects contained in the tree
     * of {@link TreeItem} objects with the provided root.
     *
     * @param root the root of the tree
     * @return the list containing the {@link BoundingShapeViewable} objects
     */
    static List<BoundingShapeViewable> getBoundingShapesRecursively(TreeItem<Object> root) {
        if(root.isLeaf()) {
            return root instanceof BoundingShapeTreeItem ? List.of((BoundingShapeViewable) root.getValue()) :
                    Collections.emptyList();
        }

        BoundingShapeTreeItemIterator iterator = new BoundingShapeTreeItemIterator(root);

        return IteratorUtils.toList(iterator).stream()
                            .filter(child -> child.getValue() instanceof BoundingShapeViewable)
                            .map(child -> (BoundingShapeViewable) child.getValue())
                            .collect(Collectors.toList());
    }

    /**
     * Adds tree-items assigned to the provided {@link BoundingShapeViewable} objects to the current tree.
     *
     * @param boundingShapes the bounding-shapes for which to add the tree-items
     */
    void addTreeItemsFromBoundingShapeViews(Collection<? extends BoundingShapeViewable> boundingShapes) {
        boundingShapes.forEach(viewable -> createTreeItemFromBoundingShape(getRoot(), viewable));
    }

    /**
     * Returns the first {@link ObjectCategoryTreeItem} whose category is equal to the provided objectCategory
     * searching from the provided searchRoot downward.
     *
     * @param searchRoot     the start tree-item to search from
     * @param objectCategory the category of the sought {@link ObjectCategoryTreeItem}
     * @return the {@link ObjectCategoryTreeItem} if it is found, otherwise null
     */
    ObjectCategoryTreeItem findParentCategoryTreeItemForCategory(TreeItem<Object> searchRoot,
                                                                 ObjectCategory objectCategory) {
        return (ObjectCategoryTreeItem) searchRoot.getChildren().stream()
                                                  .filter(category -> category.getValue().equals(objectCategory))
                                                  .findFirst()
                                                  .orElse(null);
    }

    /**
     * Expands all currently existing tree-items.
     */
    void expandAllTreeItems() {
        IteratorUtils.toList(new BoundingShapeTreeItemIterator(getRoot()))
                     .forEach(treeItem -> treeItem.setExpanded(true));
    }

    private void generateNewTreeItem(TreeItem<Object> root, BoundingShapeData boundingShapeData,
                                     double imageWidth, double imageHeight) {
        ObjectCategoryTreeItem objectCategoryTreeItem = findObjectCategoryTreeItem(root, boundingShapeData);
        BoundingShapeViewable newBoundingShape = boundingShapeData.toBoundingShapeView(imageWidth, imageHeight);
        BoundingShapeTreeItem newTreeItem = newBoundingShape.toTreeItem();
        newBoundingShape.getViewData().setTreeItem(newTreeItem);

        objectCategoryTreeItem.attachBoundingShapeTreeItemChild(newTreeItem);
        boundingShapeData.getParts().forEach(part -> generateNewTreeItem(newTreeItem, part, imageWidth, imageHeight));
    }

    private ObjectCategoryTreeItem findObjectCategoryTreeItem(TreeItem<Object> root,
                                                              BoundingShapeData boundingShapeData) {
        ObjectCategoryTreeItem objectCategoryTreeItem =
                findParentCategoryTreeItemForCategory(root, boundingShapeData.getCategory());

        if(objectCategoryTreeItem == null) {
            objectCategoryTreeItem = new ObjectCategoryTreeItem(boundingShapeData.getCategory());
            root.getChildren().add(objectCategoryTreeItem);
        }

        return objectCategoryTreeItem;
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

    private void createTreeItemFromBoundingShape(TreeItem<Object> root, BoundingShapeViewable boundingShape) {
        BoundingShapeTreeItem boundingShapeTreeItem = boundingShape.toTreeItem();
        boundingShape.getViewData().setTreeItem(boundingShapeTreeItem);

        ObjectCategoryTreeItem parentObjectCategoryTreeItem = findParentCategoryTreeItemForCategory(root,
                                                                                                    boundingShape
                                                                                                            .getViewData()
                                                                                                            .getObjectCategory());

        if(parentObjectCategoryTreeItem != null) {
            parentObjectCategoryTreeItem.attachBoundingShapeTreeItemChild(boundingShapeTreeItem);
        } else {
            ObjectCategoryTreeItem objectCategoryTreeItem =
                    new ObjectCategoryTreeItem(boundingShape.getViewData().getObjectCategory());
            root.getChildren().add(objectCategoryTreeItem);
            objectCategoryTreeItem.attachBoundingShapeTreeItemChild(boundingShapeTreeItem);
        }
    }

    private BoundingShapeData treeItemToBoundingShapeData(TreeItem<Object> treeItem) {
        if(!(treeItem.getValue() instanceof BoundingShapeDataConvertible)) {
            throw new IllegalStateException("Invalid tree item class type.");
        }

        BoundingShapeData boundingShapeData =
                ((BoundingShapeDataConvertible) treeItem.getValue()).toBoundingShapeData();

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

    private void detachTreeItemFromParent(TreeItem<Object> itemToDetach) {
        TreeItem<Object> itemParent = itemToDetach.getParent();

        if(itemParent instanceof ObjectCategoryTreeItem
                && itemToDetach instanceof BoundingShapeTreeItem) {
            ((ObjectCategoryTreeItem) itemParent)
                    .detachBoundingShapeTreeItemChild((BoundingShapeTreeItem) itemToDetach);
        } else {
            itemParent.getChildren().remove(itemToDetach);
        }

        if(itemParent instanceof ObjectCategoryTreeItem
                && itemParent.getChildren().isEmpty()) {
            itemParent.getParent().getChildren().remove(itemParent);
        }
    }

    private void attachTreeItemToTarget(TreeItem<Object> treeItemToAttach, TreeItem<Object> targetItem) {
        ObjectCategory draggedItemCategory;

        if(treeItemToAttach instanceof ObjectCategoryTreeItem) {
            draggedItemCategory = ((ObjectCategoryTreeItem) treeItemToAttach).getObjectCategory();
        } else if(treeItemToAttach instanceof BoundingShapeTreeItem) {
            draggedItemCategory =
                    ((BoundingShapeViewable) treeItemToAttach.getValue()).getViewData().getObjectCategory();
        } else {
            throw new IllegalStateException(INVALID_DRAGGED_OBJECT_CLASS_TYPE_ERROR_MESSAGE);
        }

        // If the target is an empty cell, add the dragged item to a (possibly new) category that is a child of the tree-root.
        if(targetItem == null) {
            targetItem = getRoot();
        }
        // Add to new location
        ObjectCategoryTreeItem newParentItem = findParentCategoryTreeItemForCategory(targetItem, draggedItemCategory);

        if(newParentItem == null) {
            // Category does not exist in new location
            if(treeItemToAttach instanceof ObjectCategoryTreeItem) {
                // Full category is added:
                targetItem.getChildren().add(treeItemToAttach);
            } else {
                ObjectCategoryTreeItem newCategoryParent =
                        new ObjectCategoryTreeItem(((BoundingShapeViewable) treeItemToAttach.getValue())
                                                           .getViewData().getObjectCategory());
                newCategoryParent.attachBoundingShapeTreeItemChild((BoundingShapeTreeItem) treeItemToAttach);
                targetItem.getChildren().add(newCategoryParent);
            }
        } else {
            // Category already exists in new location
            if(treeItemToAttach instanceof ObjectCategoryTreeItem) {
                // Full category is added:
                for(TreeItem<Object> child : treeItemToAttach.getChildren()) {
                    newParentItem.attachBoundingShapeTreeItemChild((BoundingShapeTreeItem) child);
                }
            } else {
                newParentItem.attachBoundingShapeTreeItemChild((BoundingShapeTreeItem) treeItemToAttach);
            }
        }
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
}
