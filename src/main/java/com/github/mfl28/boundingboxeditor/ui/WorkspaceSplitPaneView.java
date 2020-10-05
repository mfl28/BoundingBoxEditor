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

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.collections.ListChangeListener;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.util.Callback;

import java.util.List;
import java.util.Objects;

/**
 * A UI-element representing the main workspace for the user to interact with or navigate through images
 * as well as for creating/editing/deleting bounding-shapes.
 *
 * @see SplitPane
 * @see View
 */
class WorkspaceSplitPaneView extends SplitPane implements View {
    private static final double DEFAULT_FIRST_DIVIDER_RATIO = 0.0;
    private static final double DEFAULT_SECOND_DIVIDER_RATIO = 1.0;
    private static final String WORK_SPACE_ID = "work-space";
    private static final String CHANGE_CATEGORY_DIALOG_TITLE = "Change Category";
    private static final String CATEGORY_CHANGE_DIALOG_CONTENT_TEXT = "New Category:";
    private static final SnapshotParameters snapShotParameters = new SnapshotParameters();
    private static final DataFormat dragDataFormat = new DataFormat("box-item");

    static {
        snapShotParameters.setTransform(Transform.scale(1.5, 1.5));
        snapShotParameters.setFill(Color.TRANSPARENT);
    }

    private final EditorsSplitPaneView editorsSplitPane = new EditorsSplitPaneView();
    private final EditorView editor = new EditorView();
    private final ImageFileExplorerView imageFileExplorer = new ImageFileExplorerView();
    private boolean treeUpdateEnabled = true;
    private double[] savedDividerPositions = {DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO};

    /**
     * Creates a new UI-element representing the main workspace for the user to interact with or navigate through images
     * as well as for creating/editing/deleting bounding-shapes.
     */
    WorkspaceSplitPaneView() {
        getItems().addAll(editorsSplitPane, editor, imageFileExplorer);

        SplitPane.setResizableWithParent(editorsSplitPane, false);
        SplitPane.setResizableWithParent(imageFileExplorer, false);

        setDividerPositions(DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO);
        setVisible(false);
        setId(WORK_SPACE_ID);

        setUpInternalListeners();
        setObjectTreeCellFactory();
    }

    @Override
    public void connectToController(final Controller controller) {
        editorsSplitPane.connectToController(controller);
        editor.connectToController(controller);
    }

    @Override
    public void reset() {
        setVisible(true);
        editorsSplitPane.reset();
    }

    /**
     * Initiates a category change for the provided {@link BoundingShapeViewable}.
     *
     * @param boundingShapeViewable the {@link BoundingShapeViewable} associated with the
     *                              bounding shape whose category should be changed
     */
    void initiateObjectCategoryChange(BoundingShapeViewable boundingShapeViewable) {
        ObjectCategory currentCategory = boundingShapeViewable.getViewData().getObjectCategory();

        MainView.displayChoiceDialogAndGetResult(currentCategory,
                                                 editorsSplitPane.getObjectCategoryTable().getItems(),
                                                 CHANGE_CATEGORY_DIALOG_TITLE,
                                                 "Select new Category (current: \"" + currentCategory.getName() + "\")",
                                                 CATEGORY_CHANGE_DIALOG_CONTENT_TEXT)
                .ifPresent(newChoice -> {
                    if(!Objects.equals(newChoice, currentCategory)) {
                        // Set new object category.
                        boundingShapeViewable.getViewData().setObjectCategory(newChoice);
                        // Get tree-item that should be moved.
                        TreeItem<Object> treeItemToMove = boundingShapeViewable.getViewData().getTreeItem();
                        // Get target tree-item.
                        TreeItem<Object> targetItem = treeItemToMove.getParent().getParent();
                        // Attach tree-item to new target tree-item.
                        editorsSplitPane.getObjectTree()
                                        .reattachTreeItemToNewTargetTreeItem(treeItemToMove, targetItem);
                    }
                });
    }

    /**
     * Returns the editor.
     *
     * @return the editor
     */
    EditorView getEditor() {
        return editor;
    }

    /**
     * Returns the editors-panel UI-element.
     *
     * @return the editors-panel
     */
    EditorsSplitPaneView getEditorsSplitPane() {
        return editorsSplitPane;
    }

    /**
     * Returns the image-file-explorer.
     *
     * @return the image-file-explorer
     */
    ImageFileExplorerView getImageFileExplorer() {
        return imageFileExplorer;
    }

    /**
     * Recursively removes objects starting with the object
     * assigned to the provided tree-item.
     *
     * @param treeItem the root-tree-item for the removal
     */
    void removeBoundingShapeWithTreeItemRecursively(TreeItem<Object> treeItem) {
        editor.getEditorImagePane()
              .removeAllFromCurrentBoundingShapes(ObjectTreeView.getBoundingShapesRecursively(treeItem));

        if(treeItem instanceof ObjectCategoryTreeItem) {
            treeItem.getParent().getChildren().remove(treeItem);
        } else if(treeItem instanceof BoundingShapeTreeItem) {
            ObjectCategoryTreeItem parentTreeItem = (ObjectCategoryTreeItem) treeItem.getParent();
            parentTreeItem.detachBoundingShapeTreeItemChild((BoundingShapeTreeItem) treeItem);

            if(parentTreeItem.getChildren().isEmpty()) {
                parentTreeItem.getParent().getChildren().remove(parentTreeItem);
            }
        }

        getEditorsSplitPane().getObjectTree().getSelectionModel().clearSelection();
    }

    /**
     * Saves the current divider-positions to a member-variable.
     */
    void saveDividerPositions() {
        savedDividerPositions = getDividerPositions();
    }

    /**
     * Applies the currently-saved divider-positions.
     */
    void applySavedDividerPositions() {
        setDividerPositions(savedDividerPositions);
    }

    /**
     * Sets the treeUpdateEnabled boolean which indicates if the {@link ObjectTreeView} object
     * should be updated when bounding shape objects are added to the current list of objects
     * in the {@link EditorView} member.
     *
     * @param treeUpdateEnabled true means tree-updates are enabled, false means disabled
     */
    void setTreeUpdateEnabled(boolean treeUpdateEnabled) {
        this.treeUpdateEnabled = treeUpdateEnabled;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        setUpEditorSplitPaneListeners();
        setUpEditorListeners();
    }

    private void setUpEditorSplitPaneListeners() {
        editorsSplitPane.getObjectTree()
                        .getSelectionModel()
                        .selectedItemProperty()
                        .addListener((observable, oldValue, newValue) -> {
                            ObjectTreeView objectTreeView = getEditorsSplitPane().getObjectTree();

                            if(oldValue instanceof ObjectCategoryTreeItem) {
                                for(TreeItem<Object> child : oldValue.getChildren()) {
                                    ((BoundingShapeTreeItem) child).setHighlightShape(false);
                                }
                            }

                            if(newValue instanceof ObjectCategoryTreeItem) {
                                getEditor().getEditorImagePane()
                                           .getBoundingShapeSelectionGroup().selectToggle(null);

                                for(TreeItem<Object> child : newValue.getChildren()) {
                                    ((BoundingShapeTreeItem) child).setHighlightShape(true);
                                }
                            } else if(newValue instanceof BoundingShapeTreeItem) {
                                objectTreeView.keepTreeItemInView(newValue);
                                getEditor().getEditorImagePane()
                                           .getBoundingShapeSelectionGroup().selectToggle((Toggle) newValue.getValue());
                            } else if(newValue == null) {
                                getEditor().getEditorImagePane().getBoundingShapeSelectionGroup().selectToggle(null);
                            }
                        });
    }

    private void setUpEditorListeners() {
        editor.getEditorImagePane().setOnMousePressed(event -> {
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                getEditorsSplitPane().getObjectTree().getSelectionModel().clearSelection();
            }
        });

        editor.getEditorImagePane()
              .getBoundingShapeSelectionGroup()
              .selectedToggleProperty()
              .addListener((observable, oldValue, newValue) -> {
                  if(newValue instanceof BoundingShapeViewable) {
                      getEditorsSplitPane().getObjectTree()
                                           .getSelectionModel()
                                           .select(((BoundingShapeViewable) newValue).getViewData().getTreeItem());
                  }
              });

        editor.getEditorImagePane().getCurrentBoundingShapes()
              .addListener(new CurrentBoundingShapeListChangeListener());

        editor.getEditorImagePane().selectedCategoryProperty()
              .bind(editorsSplitPane.getObjectCategoryTable().getSelectionModel().selectedItemProperty());

        editor.getEditorToolBar().getShowBoundingShapesButton().setOnAction(event ->
                                                                                    editorsSplitPane.getObjectTree()
                                                                                                    .setToggleIconStateForAllTreeItems(
                                                                                                            true));

        editor.getEditorToolBar().getHideBoundingShapesButton().setOnAction(event ->
                                                                                    editorsSplitPane.getObjectTree()
                                                                                                    .setToggleIconStateForAllTreeItems(
                                                                                                            false));
    }

    private void setObjectTreeCellFactory() {
        editorsSplitPane.getObjectTree().setCellFactory(new ObjectTreeElementCellFactory());
    }

    private class CurrentBoundingShapeListChangeListener implements ListChangeListener<BoundingShapeViewable> {
        @Override
        public void onChanged(Change<? extends BoundingShapeViewable> c) {
            while(c.next()) {
                final ImageFileListView.FileInfo currentSelectedItem = imageFileExplorer.getImageFileListView()
                                                                                        .getSelectionModel()
                                                                                        .getSelectedItem();

                if(c.wasAdded()) {
                    List<? extends BoundingShapeViewable> addedItems = c.getAddedSubList();

                    editor.getEditorImagePane().addBoundingShapesToSceneGroup(addedItems);

                    if(treeUpdateEnabled) {
                        editorsSplitPane.getObjectTree().addTreeItemsFromBoundingShapeViews(addedItems);
                    }

                    currentSelectedItem.setHasAssignedBoundingShapes(true);
                }

                if(c.wasRemoved()) {
                    editor.getEditorImagePane().removeBoundingShapesFromSceneGroup(c.getRemoved());

                    if(editor.getEditorImagePane().getCurrentBoundingShapes().isEmpty() &&
                            editor.getEditorImagePane().getCurrentImage().getUrl().equals(
                                    currentSelectedItem.getFile().toURI().toString())
                    ) {
                        currentSelectedItem.setHasAssignedBoundingShapes(false);
                    }
                }
            }
        }
    }

    private class ObjectTreeElementCellFactory implements Callback<TreeView<Object>, TreeCell<Object>> {

        private TreeItem<Object> draggedItem;

        @Override
        public TreeCell<Object> call(TreeView<Object> treeView) {
            final ObjectTreeElementCell cell = new ObjectTreeElementCell();

            applyOnDeleteBoundingShapeMenuItemListener(cell);
            applyChangeObjectCategoryMenuItemListeners(cell);
            applyOnDragDetectedListener(cell);
            applyOnDragOverListener(cell);
            applyOnDragEnteredListener(cell);
            applyOnDragExitedListener(cell);
            applyOnDragDroppedListener(cell);

            return cell;
        }

        private void applyOnDeleteBoundingShapeMenuItemListener(ObjectTreeElementCell cell) {
            cell.getDeleteBoundingShapeMenuItem().setOnAction(event -> {
                if(!cell.isEmpty()) {
                    removeBoundingShapeWithTreeItemRecursively(cell.getTreeItem());
                }
            });
        }

        private void applyChangeObjectCategoryMenuItemListeners(ObjectTreeElementCell cell) {
            cell.getChangeObjectCategoryMenuItem().setOnAction(event -> {
                if(!cell.isEmpty()) {
                    initiateObjectCategoryChange(((BoundingShapeViewable) cell.getItem()));
                }
            });
        }

        private void applyOnDragDetectedListener(ObjectTreeElementCell cell) {
            cell.setOnDragDetected(event -> {
                if(cell.isEmpty()) {
                    return;
                }

                draggedItem = cell.getTreeItem();

                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.put(dragDataFormat, "");

                Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
                dragboard.setContent(clipboardContent);
                dragboard.setDragView(cell.getGraphic().snapshot(snapShotParameters, null));
                event.consume();
            });
        }

        private void applyOnDragOverListener(ObjectTreeElementCell cell) {
            cell.setOnDragOver(event -> {
                if(!event.getDragboard().hasContent(dragDataFormat)) {
                    return;
                }

                TreeItem<Object> thisItem = cell.getTreeItem();

                if(draggedItem == null || draggedItem == thisItem || thisItem instanceof ObjectCategoryTreeItem) {
                    return;
                }

                if(thisItem != null &&
                        (thisItem.getChildren().contains(draggedItem) || (draggedItem instanceof ObjectCategoryTreeItem
                                && draggedItem.getChildren().contains(thisItem)))) {
                    return;
                }

                if(thisItem == null && ((draggedItem instanceof ObjectCategoryTreeItem
                        && draggedItem.getParent().equals(cell.getTreeView().getRoot()))
                        || draggedItem.getParent().getParent().equals(cell.getTreeView().getRoot()))) {
                    return;
                }

                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            });
        }

        private void applyOnDragEnteredListener(ObjectTreeElementCell cell) {
            cell.setOnDragEntered(event -> {
                TreeItem<Object> thisItem = cell.getTreeItem();

                if(draggedItem == null || thisItem == null || draggedItem == thisItem
                        || thisItem instanceof ObjectCategoryTreeItem
                        || thisItem.getChildren().contains(draggedItem)
                        || (draggedItem instanceof ObjectCategoryTreeItem
                        && draggedItem.getChildren().contains(thisItem))) {
                    return;
                }

                cell.setDraggedOver(true);
                event.consume();
            });
        }

        private void applyOnDragExitedListener(ObjectTreeElementCell cell) {
            cell.setOnDragExited(event -> {
                cell.setDraggedOver(false);
                event.consume();
            });
        }

        private void applyOnDragDroppedListener(ObjectTreeElementCell cell) {
            cell.setOnDragDropped(event -> {
                if(!event.getDragboard().hasContent(dragDataFormat)) {
                    return;
                }

                TreeItem<Object> targetItem = cell.getTreeItem();

                // Cannot drop on ObjectCategoryTreeItem
                if(targetItem instanceof ObjectCategoryTreeItem) {
                    return;
                }

                ((ObjectTreeView) cell.getTreeView()).reattachTreeItemToNewTargetTreeItem(draggedItem, targetItem);

                event.setDropCompleted(true);
                event.consume();
            });
        }
    }
}
