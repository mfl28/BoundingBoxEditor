package BoundingBoxEditor.ui;

import BoundingBoxEditor.controller.Controller;
import BoundingBoxEditor.model.BoundingBoxCategory;
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
    void removeBoundingBoxWithTreeItemRecursively(TreeItem<BoundingBoxView> treeItem) {
        boundingBoxEditor.getBoundingBoxEditorImagePane().removeAllFromCurrentBoundingBoxes(
                BoundingBoxTreeView.getBoundingBoxViewsRecursively(treeItem));

        if(treeItem instanceof BoundingBoxCategoryTreeItem) {
            treeItem.getParent().getChildren().remove(treeItem);
        } else {
            BoundingBoxCategoryTreeItem parentTreeItem = (BoundingBoxCategoryTreeItem) treeItem.getParent();
            parentTreeItem.detachBoundingBoxTreeItemChild((BoundingBoxTreeItem) treeItem);

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

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        getBoundingBoxEditor().getBoundingBoxEditorImagePane().setOnMousePressed(event -> {
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                getEditorsSplitPane().getBoundingBoxTree().getSelectionModel().clearSelection();
            }
        });

        getEditorsSplitPane().getBoundingBoxTree()
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    BoundingBoxTreeView boundingBoxTreeView = getEditorsSplitPane().getBoundingBoxTree();

                    if(oldValue instanceof BoundingBoxCategoryTreeItem) {
                        oldValue.getChildren().forEach(child -> child.getValue().setHighlighted(false));
                    }

                    if(newValue instanceof BoundingBoxTreeItem) {
                        boundingBoxTreeView.keepTreeItemInView(newValue);

                        getBoundingBoxEditor().getBoundingBoxEditorImagePane()
                                .getBoundingBoxSelectionGroup().selectToggle(newValue.getValue());
                    } else {
                        getBoundingBoxEditor().getBoundingBoxEditorImagePane()
                                .getBoundingBoxSelectionGroup().selectToggle(null);

                        if(newValue != null) {
                            newValue.getChildren().forEach(child -> child.getValue().setHighlighted(true));
                        }
                    }

                }));

        getBoundingBoxEditor().getBoundingBoxEditorImagePane()
                .getBoundingBoxSelectionGroup()
                .selectedToggleProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    if(newValue != null) {
                        getEditorsSplitPane().getBoundingBoxTree()
                                .getSelectionModel()
                                .select(((BoundingBoxView) newValue).getTreeItem());
                    }
                }));

        boundingBoxEditor.getBoundingBoxEditorImagePane()
                .getCurrentBoundingBoxes().addListener((ListChangeListener<BoundingBoxView>) c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    List<? extends BoundingBoxView> addedItems = c.getAddedSubList();

                    boundingBoxEditor.getBoundingBoxEditorImagePane().addBoundingBoxViewsToSceneGroup(addedItems);

                    if(isTreeUpdateEnabled()) {
                        editorsSplitPane.getBoundingBoxTree().addTreeItemsFromBoundingBoxViews(addedItems);
                    }
                }

                if(c.wasRemoved()) {
                    boundingBoxEditor.getBoundingBoxEditorImagePane().removeBoundingBoxViewsFromSceneGroup(c.getRemoved());
                }
            }
        });

        boundingBoxEditor.getBoundingBoxEditorImagePane().selectedCategoryProperty()
                .bind(editorsSplitPane.getBoundingBoxCategoryTable().getSelectionModel().selectedItemProperty());

        boundingBoxEditor.getBoundingBoxEditorToolBar().getShowBoundingBoxesButton().setOnAction(event ->
                editorsSplitPane.getBoundingBoxTree().setToggleIconStateForAllTreeItems(true));

        boundingBoxEditor.getBoundingBoxEditorToolBar().getHideBoundingBoxesButton().setOnAction(event ->
                editorsSplitPane.getBoundingBoxTree().setToggleIconStateForAllTreeItems(false));
    }

    private void setBoundingBoxExplorerCellFactory() {
        editorsSplitPane.getBoundingBoxTree().setCellFactory(new Callback<>() {
            private TreeItem<BoundingBoxView> draggedItem;

            @Override
            public TreeCell<BoundingBoxView> call(TreeView<BoundingBoxView> treeView) {
                final BoundingBoxTreeCell cell = new BoundingBoxTreeCell();

                cell.getDeleteBoundingBoxMenuItem().setOnAction(event -> {
                    if(!cell.isEmpty()) {
                        removeBoundingBoxWithTreeItemRecursively(cell.getTreeItem());
                    }
                });

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

                cell.setOnDragOver(event -> {

                    if(!event.getDragboard().hasContent(dragDataFormat)) {
                        return;
                    }

                    TreeItem thisItem = cell.getTreeItem();

                    if(draggedItem == null || draggedItem == thisItem || thisItem instanceof BoundingBoxCategoryTreeItem) {
                        return;
                    }

                    if(thisItem != null &&
                            (thisItem.getChildren().contains(draggedItem) || (draggedItem instanceof BoundingBoxCategoryTreeItem
                                    && draggedItem.getChildren().contains(thisItem)))) {
                        return;
                    }

                    if(thisItem == null && ((draggedItem instanceof BoundingBoxCategoryTreeItem
                            && draggedItem.getParent().equals(treeView.getRoot()))
                            || (draggedItem.getParent().getParent().equals(treeView.getRoot())))) {
                        return;
                    }

                    event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                });

                cell.setOnDragEntered(event -> {
                    TreeItem thisItem = cell.getTreeItem();
                    if(draggedItem == null || thisItem == null || draggedItem == thisItem
                            || thisItem instanceof BoundingBoxCategoryTreeItem
                            || thisItem.getChildren().contains(draggedItem)
                            || (draggedItem instanceof BoundingBoxCategoryTreeItem
                            && draggedItem.getChildren().contains(thisItem))) {
                        return;
                    }

                    cell.setDraggedOver(true);
                    event.consume();
                });

                cell.setOnDragExited(event -> {
                    cell.setDraggedOver(false);
                    event.consume();
                });

                cell.setOnDragDropped(event -> {
                    if(!event.getDragboard().hasContent(dragDataFormat)) {
                        return;
                    }

                    TreeItem<BoundingBoxView> thisItem = cell.getTreeItem();

                    // cannot drop on CategoryTreeItems
                    if(thisItem instanceof BoundingBoxCategoryTreeItem) {
                        return;
                    }

                    TreeItem<BoundingBoxView> draggedItemParent = draggedItem.getParent();

                    if(draggedItemParent instanceof BoundingBoxCategoryTreeItem) {
                        ((BoundingBoxCategoryTreeItem) draggedItemParent).detachBoundingBoxTreeItemChild((BoundingBoxTreeItem) draggedItem);
                    } else {
                        draggedItemParent.getChildren().remove(draggedItem);
                    }

                    if(draggedItemParent instanceof BoundingBoxCategoryTreeItem && draggedItemParent.getChildren().isEmpty()) {
                        draggedItemParent.getParent().getChildren().remove(draggedItemParent);
                    }

                    BoundingBoxCategory draggedItemCategory = (draggedItem instanceof BoundingBoxCategoryTreeItem) ?
                            ((BoundingBoxCategoryTreeItem) draggedItem).getBoundingBoxCategory() : draggedItem.getValue().getBoundingBoxCategory();

                    BoundingBoxTreeView boundingBoxExplorer = (BoundingBoxTreeView) treeView;
                    // if the target is an empty cell, we add the dragged item to a (possibly new) category that is a child of the tree-root.
                    if(thisItem == null) {
                        thisItem = boundingBoxExplorer.getRoot();
                    }
                    // add to new location
                    BoundingBoxCategoryTreeItem newParentItem = boundingBoxExplorer.findParentCategoryTreeItemForCategory(thisItem, draggedItemCategory);

                    if(newParentItem == null) {
                        // category does not exits in new location
                        if(draggedItem instanceof BoundingBoxCategoryTreeItem) {
                            // full category is added:
                            thisItem.getChildren().add(draggedItem);
                        } else {
                            // create new Category part
                            BoundingBoxCategoryTreeItem newCategoryParent = new BoundingBoxCategoryTreeItem(draggedItem.getValue().getBoundingBoxCategory());
                            newCategoryParent.attachBoundingBoxTreeItemChild((BoundingBoxTreeItem) draggedItem);
                            thisItem.getChildren().add(newCategoryParent);
                        }
                    } else {
                        // category already exists:
                        if(draggedItem instanceof BoundingBoxCategoryTreeItem) {
                            // full category is added:
                            for(TreeItem<BoundingBoxView> child : draggedItem.getChildren()) {
                                newParentItem.attachBoundingBoxTreeItemChild((BoundingBoxTreeItem) child);
                            }

                        } else {
                            newParentItem.attachBoundingBoxTreeItemChild((BoundingBoxTreeItem) draggedItem);
                        }
                    }

                    event.setDropCompleted(true);
                    event.consume();
                });

                return cell;
            }
        });
    }

    private boolean isTreeUpdateEnabled() {
        return treeUpdateEnabled;
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
}
