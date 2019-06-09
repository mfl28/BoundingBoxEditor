package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.BoundingBoxCategory;
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
    private static final double DEFAULT_FIRST_DIVIDER_RATIO = 0.15;
    private static final double DEFAULT_SECOND_DIVIDER_RATIO = 0.85;
    private static final String WORK_SPACE_ID = "work-space";
    private static final SnapshotParameters snapShotParameters = new SnapshotParameters();
    private static final DataFormat dragDataFormat = new DataFormat("box-item");

    static {
        snapShotParameters.setTransform(Transform.scale(1.5, 1.5));
        snapShotParameters.setFill(Color.TRANSPARENT);
    }

    private final EditorsPanelView editorsPanel = new EditorsPanelView();
    private final ImageBoundingBoxEditorView imageBoundingBoxEditor = new ImageBoundingBoxEditorView();
    private final ImageFileExplorerView imageFileExplorer = new ImageFileExplorerView();
    private boolean treeUpdateEnabled = true;

    /**
     * Creates a new UI-element representing the main workspace for the user to interact with or navigate through images
     * as well as for creating/editing/deleting bounding-boxes.
     */
    WorkspaceSplitPaneView() {
        getItems().addAll(editorsPanel, imageBoundingBoxEditor, imageFileExplorer);

        SplitPane.setResizableWithParent(editorsPanel, false);
        SplitPane.setResizableWithParent(imageFileExplorer, false);

        setDividerPositions(DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO);
        setVisible(false);
        setId(WORK_SPACE_ID);

        setUpInternalListeners();
        setBoundingBoxExplorerCellFactory();
    }

    @Override
    public void connectToController(final Controller controller) {
        editorsPanel.connectToController(controller);
        imageBoundingBoxEditor.connectToController(controller);
    }

    @Override
    public void reset() {
        setVisible(true);
        editorsPanel.reset();
    }

    /**
     * Returns the image-bounding-box-editor.
     *
     * @return the image-bounding-box-editor
     */
    ImageBoundingBoxEditorView getImageBoundingBoxEditor() {
        return imageBoundingBoxEditor;
    }

    /**
     * Returns the editors-panel UI-element.
     *
     * @return the editors-panel
     */
    EditorsPanelView getEditorsPanel() {
        return editorsPanel;
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
        imageBoundingBoxEditor.getImagePane().removeAllFromCurrentBoundingBoxes(
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

        getEditorsPanel().getBoundingBoxExplorer().getSelectionModel().clearSelection();
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        getImageBoundingBoxEditor().getImagePane().setOnMousePressed(event -> {
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                getEditorsPanel().getBoundingBoxExplorer().getSelectionModel().clearSelection();
            }
        });

        getEditorsPanel().getBoundingBoxExplorer().getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if(getEditorsPanel().getBoundingBoxExplorer().isOnlyShowSelectedBoundingBox()) {
                getEditorsPanel().getBoundingBoxExplorer().getRoot().getChildren()
                        .forEach(category -> category.getChildren()
                                .stream()
                                .map(child -> (BoundingBoxTreeItem) child)
                                .forEach(child -> child.setIconToggledOn(false)));
            }

            if(newValue instanceof BoundingBoxTreeItem) {
                getEditorsPanel().getBoundingBoxExplorer().scrollTo(getEditorsPanel().getBoundingBoxExplorer().getRow(newValue));
                getImageBoundingBoxEditor().getImagePane().getBoundingBoxSelectionGroup().selectToggle(newValue.getValue());

                if(getEditorsPanel().getBoundingBoxExplorer().isOnlyShowSelectedBoundingBox()) {
                    ((BoundingBoxTreeItem) newValue).setIconToggledOn(true);
                }
            } else {
                getImageBoundingBoxEditor().getImagePane().getBoundingBoxSelectionGroup().selectToggle(null);
            }

        }));

        getImageBoundingBoxEditor().getImagePane()
                .getBoundingBoxSelectionGroup()
                .selectedToggleProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    if(newValue != null) {
                        getEditorsPanel().getBoundingBoxExplorer()
                                .getSelectionModel()
                                .select(((BoundingBoxView) newValue).getTreeItem());
                    }
                }));

        imageBoundingBoxEditor.getImagePane()
                .getCurrentBoundingBoxes().addListener((ListChangeListener<BoundingBoxView>) c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    List<? extends BoundingBoxView> addedItems = c.getAddedSubList();

                    imageBoundingBoxEditor.getImagePane().addBoundingBoxViewsToSceneGroup(addedItems);

                    if(isTreeUpdateEnabled()) {
                        editorsPanel.getBoundingBoxExplorer().addTreeItemsFromBoundingBoxViews(addedItems);
                    }
                }

                if(c.wasRemoved()) {
                    imageBoundingBoxEditor.getImagePane().removeBoundingBoxViewsFromSceneGroup(c.getRemoved());
                }
            }
        });

        editorsPanel.getCategorySelector().getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
            if(newValue != null) {
                imageBoundingBoxEditor.getImagePane().getInitializerRectangle().setStroke(newValue.getColor());
            }
        });

        imageBoundingBoxEditor.getImagePane().selectedCategoryProperty()
                .bind(editorsPanel.getCategorySelector()
                        .getSelectionModel()
                        .selectedItemProperty());
    }

    private void setBoundingBoxExplorerCellFactory() {
        editorsPanel.getBoundingBoxExplorer().setCellFactory(new Callback<>() {
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
                            draggedItem.getChildren().stream()
                                    .map(child -> (BoundingBoxTreeItem) child)
                                    .forEach(newParentItem::attachBoundingBoxTreeItemChild);

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
     * in the {@link ImageBoundingBoxEditorView} member.
     *
     * @param treeUpdateEnabled true means tree-updates are enabled, false means disabled
     */
    void setTreeUpdateEnabled(boolean treeUpdateEnabled) {
        this.treeUpdateEnabled = treeUpdateEnabled;
    }
}
