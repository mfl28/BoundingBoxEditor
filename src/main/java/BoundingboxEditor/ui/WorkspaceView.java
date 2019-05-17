package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.BoundingBoxCategory;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.stream.Collectors;

class WorkspaceView extends SplitPane implements View {
    private static final double DEFAULT_FIRST_DIVIDER_RATIO = 0.15;
    private static final double DEFAULT_SECOND_DIVIDER_RATIO = 0.85;
    private static final String CATEGORY_DELETION_ERROR_TITLE = "Category Deletion Error";
    private static final String CATEGORY_DELETION_ERROR_CONTENT = "You cannot delete a category that has existing bounding-boxes assigned to it.";
    private static final String WORK_SPACE_ID = "work-space";

    private final ProjectSidePanelView projectSidePanel = new ProjectSidePanelView();
    private final ImageShowerView imageShower = new ImageShowerView();
    private final ImageExplorerPanelView imageExplorer = new ImageExplorerPanelView();

    private final ListChangeListener<BoundingBoxView> boundingBoxDatabaseListener = createBoundingBoxDatabaseListener();
    private final ChangeListener<Number> imageFullyLoadedListener = createImageFullyLoadedListener();

    WorkspaceView() {
        getItems().addAll(projectSidePanel, imageShower, imageExplorer);

        SplitPane.setResizableWithParent(projectSidePanel, false);
        SplitPane.setResizableWithParent(imageExplorer, false);

        setDividerPositions(DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO);
        setVisible(false);
        setId(WORK_SPACE_ID);

        setUpInternalListeners();
        setBoundingBoxCategorySelectorDeleteCellFactory();
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

    void addFullyLoadedImageListener() {
        imageShower.getImagePane().getCurrentImage().progressProperty().addListener(imageFullyLoadedListener);
    }

    void removeFullyLoadedImageListener() {
        imageShower.getImagePane().getCurrentImage().progressProperty().removeListener(imageFullyLoadedListener);
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

    void setDatabaseListener() {
        imageShower.getImagePane().getCurrentBoundingBoxes().addListener(boundingBoxDatabaseListener);
    }

    void removeDatabaseListener(int index) {
        imageShower.getImagePane().getBoundingBoxDataBase().get(index).removeListener(boundingBoxDatabaseListener);
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        projectSidePanel.getCategorySelector().getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
            if(newValue != null) {
                imageShower.getImagePane().getInitializerBoundingBox().setStroke(newValue.getColor());
            }
        });

        imageShower.getImagePane().selectedCategoryProperty().bind(projectSidePanel.getCategorySelector().getSelectionModel().selectedItemProperty());
    }

    private ListChangeListener<BoundingBoxView> createBoundingBoxDatabaseListener() {
        return c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    List<? extends BoundingBoxView> addedItemsList = c.getAddedSubList();

                    imageShower.getImagePane().addBoundingBoxesToView(addedItemsList);
                    projectSidePanel.getBoundingBoxExplorer().addTreeItemsFromBoundingBoxes(addedItemsList);
                }

                if(c.wasRemoved()) {
                    imageShower.getImagePane().removeBoundingBoxesFromView(c.getRemoved());
                }
            }
        };
    }

    private ChangeListener<Number> createImageFullyLoadedListener() {
        return (observable, oldValue, newValue) -> {
            if(newValue.intValue() == 1) {
                resetBoundingBoxesInView();
                loadCurrentBoundingBoxes();
                setDatabaseListener();
            }
        };
    }

    private void resetBoundingBoxesInView() {
        imageShower.getImagePane().resetCurrentBoundingBoxes();
    }

    private void loadCurrentBoundingBoxes() {
        List<BoundingBoxView> loadedBoundingBoxViews = imageShower.getImagePane().getCurrentBoundingBoxes();

        if(!loadedBoundingBoxViews.isEmpty()) {
            loadedBoundingBoxViews.stream().filter(item -> !item.hasConfinementListener())
                    .forEach(item -> {
                        item.confinementBoundsProperty().bind(getImageShower().getImagePane().getImageView().boundsInParentProperty());
                        item.initializeFromBoundsInImage();
                        item.addConfinementListener();
                    });
            // FIXME: Bounding-Box explorer is not cleared properly when switching between images.

            imageShower.getImagePane().addBoundingBoxesToView(loadedBoundingBoxViews);
            getProjectSidePanel().getBoundingBoxExplorer().addTreeItemsFromBoundingBoxes(loadedBoundingBoxViews);
        }
    }

    private void setBoundingBoxCategorySelectorDeleteCellFactory() {
        projectSidePanel.getCategorySelector().getDeleteColumn().setCellFactory(factory -> {
            final DeleteTableCell cell = new DeleteTableCell();

            cell.getDeleteButton().setOnAction(action -> {
                final BoundingBoxCategory cellItem = cell.getItem();

                if(imageShower.getImagePane().getBoundingBoxDataBase().containsBoundingBoxWithCategory(cellItem)) {
                    MainView.displayErrorAlert(CATEGORY_DELETION_ERROR_TITLE, CATEGORY_DELETION_ERROR_CONTENT);
                } else {
                    cell.getTableView().getItems().remove(cellItem);
                }
            });

            return cell;
        });
    }

    private void setBoundingBoxExplorerCellFactory() {
        projectSidePanel.getBoundingBoxExplorer().setCellFactory(treeView -> {
            final BoundingBoxTreeCell cell = new BoundingBoxTreeCell();

            cell.getDeleteBoundingBoxMenuItem().setOnAction(event -> {
                if(!cell.isEmpty()) {
                    final TreeItem<BoundingBoxView> treeItem = cell.getTreeItem();

                    if(treeItem instanceof CategoryTreeItem) {
                        imageShower.getImagePane().getBoundingBoxDataBase().removeAllFromCurrentBoundingBoxes(
                                treeItem.getChildren().stream()
                                        .map(TreeItem::getValue)
                                        .collect(Collectors.toList())
                        );

                        treeView.getRoot().getChildren().remove(treeItem);
                    } else {
                        CategoryTreeItem parentTreeItem = (CategoryTreeItem) treeItem.getParent();
                        parentTreeItem.detachChildId(((BoundingBoxTreeItem) treeItem).getId());

                        imageShower.getImagePane().getBoundingBoxDataBase().removeFromCurrentBoundingBoxes(treeItem.getValue());
                        parentTreeItem.getChildren().remove(treeItem);

                        if(parentTreeItem.getChildren().isEmpty()) {
                            treeView.getRoot().getChildren().remove(parentTreeItem);
                        }
                    }
                }
            });

            return cell;
        });
    }
}
