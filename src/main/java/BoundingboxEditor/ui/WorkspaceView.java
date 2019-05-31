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
import java.util.stream.Collectors;

class WorkspaceView extends SplitPane implements View {
    private static final double DEFAULT_FIRST_DIVIDER_RATIO = 0.15;
    private static final double DEFAULT_SECOND_DIVIDER_RATIO = 0.85;
    private static final String WORK_SPACE_ID = "work-space";
    private static final SnapshotParameters snapShotParameters = new SnapshotParameters();
    private static final DataFormat dragDataFormat = new DataFormat("box-item");

    static {
        snapShotParameters.setTransform(Transform.scale(1.5, 1.5));
        snapShotParameters.setFill(Color.TRANSPARENT);
    }

    private final ProjectSidePanelView projectSidePanel = new ProjectSidePanelView();
    private final ImageShowerView imageShower = new ImageShowerView();
    private final ImageExplorerPanelView imageExplorer = new ImageExplorerPanelView();
    private boolean treeUpdateEnabled = true;

    WorkspaceView() {
        getItems().addAll(projectSidePanel, imageShower, imageExplorer);

        SplitPane.setResizableWithParent(projectSidePanel, false);
        SplitPane.setResizableWithParent(imageExplorer, false);

        setDividerPositions(DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO);
        setVisible(false);
        setId(WORK_SPACE_ID);

        setUpInternalListeners();
        setBoundingBoxExplorerCellFactory();
    }

    @Override
    public void connectToController(final Controller controller) {
        projectSidePanel.connectToController(controller);
        imageShower.connectToController(controller);
    }

    @Override
    public void reset() {
        setVisible(true);
        projectSidePanel.reset();
    }

    boolean isTreeUpdateEnabled() {
        return treeUpdateEnabled;
    }

    void setTreeUpdateEnabled(boolean treeUpdateEnabled) {
        this.treeUpdateEnabled = treeUpdateEnabled;
    }

    ProjectSidePanelView getProjectSidePanel() {
        return projectSidePanel;
    }

    ImageShowerView getImageShower() {
        return imageShower;
    }

    ImageExplorerPanelView getImageExplorer() {
        return imageExplorer;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        getImageShower().getImagePane().setOnMousePressed(event -> {
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                getProjectSidePanel().getBoundingBoxExplorer().getSelectionModel().clearSelection();
            }
        });

        getProjectSidePanel().getBoundingBoxExplorer().getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if(getProjectSidePanel().getBoundingBoxExplorer().isAutoHideNonSelected()) {
                getProjectSidePanel().getBoundingBoxExplorer().getRoot().getChildren()
                        .forEach(category -> category.getChildren()
                                .stream()
                                .map(child -> (BoundingBoxTreeItem) child)
                                .forEach(child -> child.setIconToggledOn(false)));
            }

            if(newValue instanceof BoundingBoxTreeItem) {
                getProjectSidePanel().getBoundingBoxExplorer().scrollTo(getProjectSidePanel().getBoundingBoxExplorer().getRow(newValue));
                getImageShower().getImagePane().getBoundingBoxSelectionGroup().selectToggle(newValue.getValue());

                if(getProjectSidePanel().getBoundingBoxExplorer().isAutoHideNonSelected()) {
                    ((BoundingBoxTreeItem) newValue).setIconToggledOn(true);
                }
            } else {
                getImageShower().getImagePane().getBoundingBoxSelectionGroup().selectToggle(null);
            }

        }));

        getImageShower().getImagePane().getBoundingBoxSelectionGroup().selectedToggleProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                getProjectSidePanel().getBoundingBoxExplorer().getSelectionModel().select(((BoundingBoxView) newValue).getTreeItem());
            }
        }));

        imageShower.getImagePane().getCurrentBoundingBoxes().addListener((ListChangeListener<BoundingBoxView>) c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    List<? extends BoundingBoxView> addedItemsList = c.getAddedSubList();

                    imageShower.getImagePane().addBoundingBoxesToView(addedItemsList);

                    if(isTreeUpdateEnabled()) {
                        projectSidePanel.getBoundingBoxExplorer().addTreeItemsFromBoundingBoxes(addedItemsList);
                    }
                }

                if(c.wasRemoved()) {
                    imageShower.getImagePane().removeBoundingBoxesFromView(c.getRemoved());
                }
            }
        });

        projectSidePanel.getCategorySelector().getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
            if(newValue != null) {
                imageShower.getImagePane().getInitializerBoundingBox().setStroke(newValue.getColor());
            }
        });

        imageShower.getImagePane().selectedCategoryProperty().bind(projectSidePanel.getCategorySelector().getSelectionModel().selectedItemProperty());


    }

    private void setBoundingBoxExplorerCellFactory() {
        projectSidePanel.getBoundingBoxExplorer().setCellFactory(new Callback<>() {
            private TreeItem<BoundingBoxView> draggedItem;

            @Override
            public TreeCell<BoundingBoxView> call(TreeView<BoundingBoxView> treeView) {
                final BoundingBoxTreeCell cell = new BoundingBoxTreeCell();

                cell.getDeleteBoundingBoxMenuItem().setOnAction(event -> {
                    if(!cell.isEmpty()) {
                        final TreeItem<BoundingBoxView> treeItem = cell.getTreeItem();

                        if(treeItem instanceof CategoryTreeItem) {
                            if(treeItem.getChildren().stream().allMatch(TreeItem::isLeaf)) {
                                imageShower.getImagePane().removeAllFromCurrentBoundingBoxes(
                                        treeItem.getChildren().stream()
                                                .map(TreeItem::getValue)
                                                .collect(Collectors.toList())
                                );
                            } else {
                                imageShower.getImagePane().removeAllFromCurrentBoundingBoxes(
                                        BoundingBoxExplorerView.getBoundingBoxViewsRecursively(treeItem));
                            }

                            treeItem.getParent().getChildren().remove(treeItem);
                        } else {
                            CategoryTreeItem parentTreeItem = (CategoryTreeItem) treeItem.getParent();
                            parentTreeItem.detachChildId(((BoundingBoxTreeItem) treeItem).getId());

                            if(treeItem.isLeaf()) {
                                imageShower.getImagePane().removeFromCurrentBoundingBoxes(treeItem.getValue());
                            } else {
                                imageShower.getImagePane().removeAllFromCurrentBoundingBoxes(
                                        BoundingBoxExplorerView.getBoundingBoxViewsRecursively(treeItem));
                            }

                            parentTreeItem.getChildren().remove(treeItem);

                            if(parentTreeItem.getChildren().isEmpty()) {
                                parentTreeItem.getParent().getChildren().remove(parentTreeItem);
                            }
                        }

                        getProjectSidePanel().getBoundingBoxExplorer().getSelectionModel().clearSelection();
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

                    if(draggedItem == null || draggedItem == thisItem || thisItem instanceof CategoryTreeItem) {
                        return;
                    }

                    if(thisItem != null &&
                            (thisItem.getChildren().contains(draggedItem) || (draggedItem instanceof CategoryTreeItem
                                    && draggedItem.getChildren().contains(thisItem)))) {
                        return;
                    }

                    if(thisItem == null && ((draggedItem instanceof CategoryTreeItem && draggedItem.getParent().equals(treeView.getRoot())) ||
                            (draggedItem.getParent().getParent().equals(treeView.getRoot())))) {
                        return;
                    }

                    event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                });

                cell.setOnDragEntered(event -> {
                    TreeItem thisItem = cell.getTreeItem();
                    if(draggedItem == null || thisItem == null || draggedItem == thisItem
                            || thisItem instanceof CategoryTreeItem
                            || thisItem.getChildren().contains(draggedItem)
                            || (draggedItem instanceof CategoryTreeItem && draggedItem.getChildren().contains(thisItem))) {
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
                    if(thisItem instanceof CategoryTreeItem) {
                        return;
                    }

                    TreeItem<BoundingBoxView> draggedItemParent = draggedItem.getParent();

                    if(draggedItemParent instanceof CategoryTreeItem) {
                        ((CategoryTreeItem) draggedItemParent).detachChildId(((BoundingBoxTreeItem) draggedItem).getId());
                    }

                    draggedItemParent.getChildren().remove(draggedItem);

                    if(draggedItemParent instanceof CategoryTreeItem && draggedItemParent.getChildren().isEmpty()) {
                        draggedItemParent.getParent().getChildren().remove(draggedItemParent);
                    }

                    BoundingBoxCategory draggedItemCategory = (draggedItem instanceof CategoryTreeItem) ?
                            ((CategoryTreeItem) draggedItem).getBoundingBoxCategory() : draggedItem.getValue().getBoundingBoxCategory();

                    BoundingBoxExplorerView boundingBoxExplorer = (BoundingBoxExplorerView) treeView;
                    // if the target is an empty cell, we add the dragged item to a (possibly new) category that is a child of the tree-root.
                    if(thisItem == null) {
                        thisItem = boundingBoxExplorer.getRoot();
                    }
                    // add to new location
                    CategoryTreeItem newParentItem = boundingBoxExplorer.findParentCategoryTreeItemForCategory(thisItem, draggedItemCategory);

                    if(newParentItem == null) {
                        // category does not exits in new location
                        if(draggedItem instanceof CategoryTreeItem) {
                            // full category is added:
                            thisItem.getChildren().add(draggedItem);
                        } else {
                            // create new Category part
                            CategoryTreeItem newCategoryParent = new CategoryTreeItem(draggedItem.getValue().getBoundingBoxCategory());
                            boundingBoxExplorer.attachBoundingBoxTreeItemToCategoryTreeItem((BoundingBoxTreeItem) draggedItem, newCategoryParent);
                            thisItem.getChildren().add(newCategoryParent);
                        }
                    } else {
                        // category already exists:
                        if(draggedItem instanceof CategoryTreeItem) {
                            // full category is added:
                            draggedItem.getChildren().stream()
                                    .map(child -> (BoundingBoxTreeItem) child)
                                    .forEach(child -> boundingBoxExplorer.attachBoundingBoxTreeItemToCategoryTreeItem(child, newParentItem));

                        } else {
                            boundingBoxExplorer.attachBoundingBoxTreeItemToCategoryTreeItem((BoundingBoxTreeItem) draggedItem, newParentItem);
                        }
                    }

                    event.setDropCompleted(true);
                    event.consume();
                });

                return cell;
            }
        });


    }
}
