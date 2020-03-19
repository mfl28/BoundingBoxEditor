package boundingboxeditor.ui;

import boundingboxeditor.controller.Controller;
import boundingboxeditor.model.ObjectCategory;
import javafx.collections.ListChangeListener;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.util.Callback;

import java.util.List;

/**
 * A UI-element representing the main workspace for the user to interact with or navigate through images
 * as well as for creating/editing/deleting bounding-boxes.
 *
 * @see SplitPane
 * @see View
 */
class WorkspaceSplitPaneView extends SplitPane implements View {
    private static final double DEFAULT_FIRST_DIVIDER_RATIO = 0.12;
    private static final double DEFAULT_SECOND_DIVIDER_RATIO = 0.88;
    private static final String WORK_SPACE_ID = "work-space";
    private static final SnapshotParameters snapShotParameters = new SnapshotParameters();
    private static final DataFormat dragDataFormat = new DataFormat("box-item");

    static {
        snapShotParameters.setTransform(Transform.scale(1.5, 1.5));
        snapShotParameters.setFill(Color.TRANSPARENT);
    }

    private final EditorsSplitPaneView editorsSplitPane = new EditorsSplitPaneView();
    private final BoundingBoxEditorView boundingBoxEditor = new BoundingBoxEditorView();
    private final ImageFileExplorerView imageFileExplorer = new ImageFileExplorerView();
    private boolean treeUpdateEnabled = true;
    private double[] savedDividerPositions = {DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO};

    /**
     * Creates a new UI-element representing the main workspace for the user to interact with or navigate through images
     * as well as for creating/editing/deleting bounding-boxes.
     */
    WorkspaceSplitPaneView() {
        getItems().addAll(editorsSplitPane, boundingBoxEditor, imageFileExplorer);

        SplitPane.setResizableWithParent(editorsSplitPane, false);
        SplitPane.setResizableWithParent(imageFileExplorer, false);

        setDividerPositions(DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO);
        setVisible(false);
        setId(WORK_SPACE_ID);

        setUpInternalListeners();
        setBoundingBoxExplorerCellFactory();
    }

    @Override
    public void connectToController(final Controller controller) {
        editorsSplitPane.connectToController(controller);
        boundingBoxEditor.connectToController(controller);
    }

    @Override
    public void reset() {
        setVisible(true);
        editorsSplitPane.reset();
    }

    /**
     * Returns the image-bounding-box-editor.
     *
     * @return the image-bounding-box-editor
     */
    BoundingBoxEditorView getBoundingBoxEditor() {
        return boundingBoxEditor;
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
     * Recursively removes {@link BoundingBoxView} objects starting with the object
     * assigned to the provided tree-item.
     *
     * @param treeItem the root-tree-item for the removal
     */
    void removeBoundingBoxWithTreeItemRecursively(TreeItem<Object> treeItem) {
        boundingBoxEditor.getBoundingBoxEditorImagePane().removeAllFromCurrentBoundingBoxes(
                BoundingBoxTreeView.getBoundingBoxViewsRecursively(treeItem));
        boundingBoxEditor.getBoundingBoxEditorImagePane().removeAllFromCurrentBoundingPolygons(
                BoundingBoxTreeView.getBoundingPolygonViewsRecursively(treeItem));

        if(treeItem instanceof ObjectCategoryTreeItem) {
            treeItem.getParent().getChildren().remove(treeItem);
        } else if(treeItem instanceof BoundingBoxTreeItem) {
            ObjectCategoryTreeItem parentTreeItem = (ObjectCategoryTreeItem) treeItem.getParent();
            parentTreeItem.detachBoundingBoxTreeItemChild((BoundingBoxTreeItem) treeItem);

            if(parentTreeItem.getChildren().isEmpty()) {
                parentTreeItem.getParent().getChildren().remove(parentTreeItem);
            }
        } else if(treeItem instanceof BoundingPolygonTreeItem) {
            ObjectCategoryTreeItem parentTreeItem = (ObjectCategoryTreeItem) treeItem.getParent();
            parentTreeItem.detachBoundingPolygonTreeItemChild((BoundingPolygonTreeItem) treeItem);

            if(parentTreeItem.getChildren().isEmpty()) {
                parentTreeItem.getParent().getChildren().remove(parentTreeItem);
            }
        }

        getEditorsSplitPane().getBoundingBoxTree().getSelectionModel().clearSelection();
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
     * Sets the treeUpdateEnabled boolean which indicates if the {@link BoundingBoxTreeView} object
     * should be updated when {@link BoundingBoxView} objects are added to the current list of objects
     * in the {@link BoundingBoxEditorView} member.
     *
     * @param treeUpdateEnabled true means tree-updates are enabled, false means disabled
     */
    void setTreeUpdateEnabled(boolean treeUpdateEnabled) {
        this.treeUpdateEnabled = treeUpdateEnabled;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        setUpEditorSplitPaneListeners();
        setUpBoundingBoxEditorListeners();
    }

    private void setUpEditorSplitPaneListeners() {
        editorsSplitPane.getBoundingBoxTree()
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    BoundingBoxTreeView boundingBoxTreeView = getEditorsSplitPane().getBoundingBoxTree();

                    if(oldValue instanceof ObjectCategoryTreeItem) {
                        for(TreeItem<Object> child : oldValue.getChildren()) {
                            if(child instanceof BoundingBoxTreeItem) {
                                ((BoundingBoxView) child.getValue()).setHighlighted(false);
                            } else if(child instanceof BoundingPolygonTreeItem) {
                                ((BoundingPolygonView) child.getValue()).setHighlighted(false);
                            }
                        }
                    }

                    if(newValue instanceof BoundingBoxTreeItem) {
                        boundingBoxTreeView.keepTreeItemInView(newValue);

                        getBoundingBoxEditor().getBoundingBoxEditorImagePane()
                                .getBoundingBoxSelectionGroup().selectToggle((BoundingBoxView) newValue.getValue());
                    } else if(newValue instanceof ObjectCategoryTreeItem) {
                        getBoundingBoxEditor().getBoundingBoxEditorImagePane()
                                .getBoundingBoxSelectionGroup().selectToggle(null);

                        for(TreeItem<Object> child : newValue.getChildren()) {
                            if(child instanceof BoundingBoxTreeItem) {
                                ((BoundingBoxView) child.getValue()).setHighlighted(true);
                            } else if(child instanceof BoundingPolygonTreeItem) {
                                ((BoundingPolygonView) child.getValue()).setHighlighted(true);
                            }
                        }
                    } else if(newValue instanceof BoundingPolygonTreeItem) {
                        boundingBoxTreeView.keepTreeItemInView(newValue);
                        getBoundingBoxEditor().getBoundingBoxEditorImagePane()
                                .getBoundingBoxSelectionGroup().selectToggle((BoundingPolygonView) newValue.getValue());
                    }

                });
    }

    private void setUpBoundingBoxEditorListeners() {
        boundingBoxEditor.getBoundingBoxEditorImagePane().setOnMousePressed(event -> {
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                getEditorsSplitPane().getBoundingBoxTree().getSelectionModel().clearSelection();
            }
        });

        boundingBoxEditor.getBoundingBoxEditorImagePane()
                .getBoundingBoxSelectionGroup()
                .selectedToggleProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if(newValue != null) {
                        if(newValue instanceof BoundingBoxView) {
                            getEditorsSplitPane().getBoundingBoxTree()
                                    .getSelectionModel()
                                    .select(((BoundingBoxView) newValue).getTreeItem());
                        } else if(newValue instanceof BoundingPolygonView) {
                            getEditorsSplitPane().getBoundingBoxTree()
                                    .getSelectionModel()
                                    .select(((BoundingPolygonView) newValue).getTreeItem());
                        }
                    }
                });

        boundingBoxEditor.getBoundingBoxEditorImagePane()
                .getCurrentBoundingBoxes().addListener(new CurrentBoundingBoxListChangeListener());

        boundingBoxEditor.getBoundingBoxEditorImagePane()
                .getCurrentBoundingPolygons().addListener(new CurrentBoundingPolygonListChangeListener());

        boundingBoxEditor.getBoundingBoxEditorImagePane().selectedCategoryProperty()
                .bind(editorsSplitPane.getBoundingBoxCategoryTable().getSelectionModel().selectedItemProperty());

        boundingBoxEditor.getBoundingBoxEditorToolBar().getShowBoundingBoxesButton().setOnAction(event ->
                editorsSplitPane.getBoundingBoxTree().setToggleIconStateForAllTreeItems(true));

        boundingBoxEditor.getBoundingBoxEditorToolBar().getHideBoundingBoxesButton().setOnAction(event ->
                editorsSplitPane.getBoundingBoxTree().setToggleIconStateForAllTreeItems(false));
    }

    private void setBoundingBoxExplorerCellFactory() {
        editorsSplitPane.getBoundingBoxTree().setCellFactory(new BoundingBoxTreeCellFactory());
    }

    private class CurrentBoundingBoxListChangeListener implements ListChangeListener<BoundingBoxView> {
        @Override
        public void onChanged(Change<? extends BoundingBoxView> c) {
            while(c.next()) {
                final ImageFileListView.FileInfo currentSelectedItem = imageFileExplorer.getImageFileListView()
                        .getSelectionModel().getSelectedItem();

                if(c.wasAdded()) {
                    List<? extends BoundingBoxView> addedItems = c.getAddedSubList();

                    boundingBoxEditor.getBoundingBoxEditorImagePane().addBoundingBoxViewsToSceneGroup(addedItems);

                    if(treeUpdateEnabled) {
                        editorsSplitPane.getBoundingBoxTree().addTreeItemsFromBoundingBoxViews(addedItems);
                    }

                    currentSelectedItem.setHasAssignedBoundingShapes(true);
                }

                if(c.wasRemoved()) {
                    boundingBoxEditor.getBoundingBoxEditorImagePane().removeBoundingBoxViewsFromSceneGroup(c.getRemoved());

                    if(boundingBoxEditor.getBoundingBoxEditorImagePane().getCurrentBoundingBoxes().isEmpty() &&
                            boundingBoxEditor.getBoundingBoxEditorImagePane().getCurrentBoundingPolygons().isEmpty() &&
                            boundingBoxEditor.getBoundingBoxEditorImagePane().getCurrentImage().getUrl().equals(
                                    currentSelectedItem.getFile().toURI().toString())
                    ) {
                        currentSelectedItem.setHasAssignedBoundingShapes(false);
                    }
                }
            }
        }
    }

    private class CurrentBoundingPolygonListChangeListener implements ListChangeListener<BoundingPolygonView> {
        @Override
        public void onChanged(Change<? extends BoundingPolygonView> c) {
            while(c.next()) {
                final ImageFileListView.FileInfo currentSelectedItem = imageFileExplorer.getImageFileListView()
                        .getSelectionModel().getSelectedItem();

                if(c.wasAdded()) {
                    List<? extends BoundingPolygonView> addedItems = c.getAddedSubList();

                    boundingBoxEditor.getBoundingBoxEditorImagePane().addBoundingPolygonViewsToSceneGroup(addedItems);

                    if(treeUpdateEnabled) {
                        editorsSplitPane.getBoundingBoxTree().addTreeItemsFromBoundingPolygonViews(addedItems);
                    }

                    currentSelectedItem.setHasAssignedBoundingShapes(true);
                }

                if(c.wasRemoved()) {
                    boundingBoxEditor.getBoundingBoxEditorImagePane().removeBoundingPolygonViewsFromSceneGroup(c.getRemoved());

                    if(boundingBoxEditor.getBoundingBoxEditorImagePane().getCurrentBoundingPolygons().isEmpty() &&
                            boundingBoxEditor.getBoundingBoxEditorImagePane().getCurrentBoundingBoxes().isEmpty() &&
                            boundingBoxEditor.getBoundingBoxEditorImagePane().getCurrentImage().getUrl().equals(
                                    currentSelectedItem.getFile().toURI().toString())
                    ) {
                        currentSelectedItem.setHasAssignedBoundingShapes(false);
                    }
                }
            }
        }
    }

    private class BoundingBoxTreeCellFactory implements Callback<TreeView<Object>, TreeCell<Object>> {
        private TreeItem<Object> draggedItem;

        @Override
        public TreeCell<Object> call(TreeView<Object> treeView) {
            final BoundingBoxTreeCell cell = new BoundingBoxTreeCell();

            applyOnDeleteBoundingBoxMenuItemListener(cell);
            applyOnDragDetectedListener(cell);
            applyOnDragOverListener(cell, treeView);
            applyOnDragEnteredListener(cell);
            applyOnDragExitedListener(cell);
            applyOnDragDroppedListener(cell, treeView);

            return cell;
        }

        private void applyOnDeleteBoundingBoxMenuItemListener(BoundingBoxTreeCell cell) {
            cell.getDeleteBoundingBoxMenuItem().setOnAction(event -> {
                if(!cell.isEmpty()) {
                    removeBoundingBoxWithTreeItemRecursively(cell.getTreeItem());
                }
            });
        }

        private void applyOnDragDetectedListener(BoundingBoxTreeCell cell) {
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

        private void applyOnDragOverListener(BoundingBoxTreeCell cell, TreeView<Object> treeView) {
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
                        && draggedItem.getParent().equals(treeView.getRoot()))
                        || draggedItem.getParent().getParent().equals(treeView.getRoot()))) {
                    return;
                }

                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            });
        }

        private void applyOnDragEnteredListener(BoundingBoxTreeCell cell) {
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

        private void applyOnDragExitedListener(BoundingBoxTreeCell cell) {
            cell.setOnDragExited(event -> {
                cell.setDraggedOver(false);
                event.consume();
            });
        }

        private void applyOnDragDroppedListener(BoundingBoxTreeCell cell, TreeView<Object> treeView) {
            cell.setOnDragDropped(event -> {
                if(!event.getDragboard().hasContent(dragDataFormat)) {
                    return;
                }

                TreeItem<Object> targetItem = cell.getTreeItem();

                // Cannot drop on CategoryTreeItems
                if(targetItem instanceof ObjectCategoryTreeItem) {
                    return;
                }

                detachDraggedItemFromParent();
                dropDraggedItemOnTarget(targetItem, treeView);

                treeView.getSelectionModel().select(draggedItem);

                event.setDropCompleted(true);
                event.consume();
            });
        }

        private void detachDraggedItemFromParent() {
            TreeItem<Object> draggedItemParent = draggedItem.getParent();

            if(draggedItemParent instanceof ObjectCategoryTreeItem) {
                ((ObjectCategoryTreeItem) draggedItemParent).detachBoundingBoxTreeItemChild((BoundingBoxTreeItem) draggedItem);
            } else {
                draggedItemParent.getChildren().remove(draggedItem);
            }

            if(draggedItemParent instanceof ObjectCategoryTreeItem && draggedItemParent.getChildren().isEmpty()) {
                draggedItemParent.getParent().getChildren().remove(draggedItemParent);
            }
        }

        private void dropDraggedItemOnTarget(TreeItem<Object> targetItem, TreeView<Object> treeView) {
            ObjectCategory draggedItemCategory = (draggedItem instanceof ObjectCategoryTreeItem) ?
                    ((ObjectCategoryTreeItem) draggedItem).getObjectCategory() :
                    ((BoundingBoxView) draggedItem.getValue()).getObjectCategory();

            BoundingBoxTreeView boundingBoxExplorer = (BoundingBoxTreeView) treeView;
            // If the target is an empty cell, add the dragged item to a (possibly new) category that is a child of the tree-root.
            if(targetItem == null) {
                targetItem = boundingBoxExplorer.getRoot();
            }
            // Add to new location
            ObjectCategoryTreeItem newParentItem = boundingBoxExplorer.findParentCategoryTreeItemForCategory(targetItem, draggedItemCategory);

            if(newParentItem == null) {
                // Category does not exits in new location
                if(draggedItem instanceof ObjectCategoryTreeItem) {
                    // Full category is added:
                    targetItem.getChildren().add(draggedItem);
                } else {
                    // Create new category part:
                    ObjectCategoryTreeItem newCategoryParent = new ObjectCategoryTreeItem(((BoundingBoxView) draggedItem.getValue()).getObjectCategory());
                    newCategoryParent.attachBoundingBoxTreeItemChild((BoundingBoxTreeItem) draggedItem);
                    targetItem.getChildren().add(newCategoryParent);
                }
            } else {
                // Category already exists in new location
                if(draggedItem instanceof ObjectCategoryTreeItem) {
                    // Full category is added:
                    for(TreeItem<Object> child : draggedItem.getChildren()) {
                        newParentItem.attachBoundingBoxTreeItemChild((BoundingBoxTreeItem) child);
                    }

                } else {
                    newParentItem.attachBoundingBoxTreeItemChild((BoundingBoxTreeItem) draggedItem);
                }
            }
        }
    }
}
