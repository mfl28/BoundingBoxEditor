package boundingboxeditor.ui;

import boundingboxeditor.controller.Controller;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.utils.UiUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

/**
 * Represents a tree-cell in a {@link ObjectTreeView}. Instances of this class are either associated
 * with a {@link ObjectCategoryTreeItem} or a {@link BoundingBoxTreeItem} and are responsible for the
 * visual representation of these items in the {@link ObjectTreeView}.
 *
 * @see TreeCell
 */
class BoundingShapeTreeCell extends TreeCell<Object> {
    private static final String DELETE_CONTEXT_MENU_ITEM_ID = "delete-context-menu";
    private static final String DELETE_BOUNDING_BOX_MENU_ITEM_TEXT = "Delete";
    private static final String NAME_TEXT_STYLE = "default-text";
    private static final String INFO_TEXT_ID = "info-text";
    private static final String TAG_ICON_REGION_ID = "tag-icon";
    private static final PseudoClass draggedOverPseudoClass = PseudoClass.getPseudoClass("dragged-over");
    private static final String CATEGORY_NAME_TEXT_ID = "category-name-text";
    private static final String TREE_CELL_CONTENT_ID = "tree-cell-content";
    private static final String HIDE_BOUNDING_BOX_MENU_ITEM_TEXT = "Hide";

    private final BooleanProperty draggedOver = new SimpleBooleanProperty(false);
    private final MenuItem deleteBoundingBoxMenuItem = createDeleteBoundingBoxMenuItem();
    private final MenuItem hideBoundingBoxMenuItem = createHideBoundingBoxMenuItem();
    private final ContextMenu contextMenu = new ContextMenu(hideBoundingBoxMenuItem, deleteBoundingBoxMenuItem);
    private final Text nameText = new Text();
    private final Text additionalInfoText = new Text();
    private final Region tagIconRegion = createTagIconRegion();

    private final EventHandler<ContextMenuEvent> showContextMenuEventHandler = createShowContextMenuEventHandler();
    private final ChangeListener<Boolean> boundingBoxVisibilityListener = createBoundingBoxVisibilityListener();

    /**
     * Creates a new tree-cell object responsible for the visual representation of a {@link ObjectCategoryTreeItem}
     * or a {@link BoundingBoxTreeItem} in a {@link ObjectTreeView}.
     */
    BoundingShapeTreeCell() {
        nameText.getStyleClass().add(NAME_TEXT_STYLE);
        additionalInfoText.setId(INFO_TEXT_ID);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setUpInternalListeners();
    }

    @Override
    protected void updateItem(Object newCellObject, boolean empty) {
        Object oldCellObject = getItem();

        if(oldCellObject instanceof BoundingBoxView) {
            BoundingBoxView oldItem = (BoundingBoxView) oldCellObject;
            // Remove the old item's context-menu event-handler.
            oldItem.removeEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, showContextMenuEventHandler);
            oldItem.visibleProperty().removeListener(boundingBoxVisibilityListener);
        } else if(oldCellObject instanceof BoundingPolygonView) {
            BoundingPolygonView oldItem = (BoundingPolygonView) oldCellObject;
            // Remove the old item's context-menu event-handler.
            oldItem.removeEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, showContextMenuEventHandler);
            oldItem.visibleProperty().removeListener(boundingBoxVisibilityListener);
        }

        super.updateItem(newCellObject, empty);

        nameText.textProperty().unbind();
        nameText.setId(null);
        additionalInfoText.textProperty().unbind();
        tagIconRegion.visibleProperty().unbind();

        if(empty || newCellObject == null) {
            setGraphic(null);
            contextMenu.hide();
            setContextMenu(null);
            setDraggedOver(false);
        } else {
            setGraphic(createContentBox());
            // Register the contextMenu with the cell.
            setContextMenu(contextMenu);

            if(newCellObject instanceof BoundingBoxView) {
                // Register the contextMenu with the BoundingBoxView associated with the cell. This
                // allows to display the contextMenu by right-clicking on the bounding-box itself.
                ((BoundingBoxView) newCellObject).setOnContextMenuRequested(showContextMenuEventHandler);
                // The context menu should be hidden when the BoundingBoxView associated with this cell
                // is hidden.
                ((BoundingBoxView) newCellObject).visibleProperty().addListener(boundingBoxVisibilityListener);
            } else if(newCellObject instanceof BoundingPolygonView) {
                ((BoundingPolygonView) newCellObject).setOnContextMenuRequested(showContextMenuEventHandler);
                ((BoundingPolygonView) newCellObject).visibleProperty().addListener(boundingBoxVisibilityListener);
            }
        }
    }

    /**
     * Sets the dragged-over state.
     *
     * @param draggedOver true or false
     */
    void setDraggedOver(boolean draggedOver) {
        this.draggedOver.set(draggedOver);
    }

    /**
     * Returns the menu-item of the context-menu which allows
     * the user to delete the currently associated {@link BoundingBoxView}.
     *
     * @return the menu-item
     */
    MenuItem getDeleteBoundingShapeMenuItem() {
        return deleteBoundingBoxMenuItem;
    }

    private MenuItem createDeleteBoundingBoxMenuItem() {
        CustomMenuItem deleteMenuItem = new CustomMenuItem(new Label(DELETE_BOUNDING_BOX_MENU_ITEM_TEXT));
        deleteMenuItem.setId(DELETE_CONTEXT_MENU_ITEM_ID);
        Tooltip.install(deleteMenuItem.getContent(),
                UiUtils.createTooltip("", Controller.KeyCombinations.deleteSelectedBoundingBox));
        return deleteMenuItem;
    }

    private MenuItem createHideBoundingBoxMenuItem() {
        CustomMenuItem hideMenuItem = new CustomMenuItem(new Label(HIDE_BOUNDING_BOX_MENU_ITEM_TEXT));
        Tooltip.install(hideMenuItem.getContent(),
                UiUtils.createTooltip("", Controller.KeyCombinations.hideSelectedBoundingBox));
        return hideMenuItem;
    }

    private void setUpInternalListeners() {
        setOnMouseEntered(event -> {
            if(!isEmpty()) {
                setHighlightStatusIncludingChildren(true);
            }
        });

        contextMenu.showingProperty().addListener((observable, oldValue, newValue) -> {
            if(!isEmpty() && !Boolean.TRUE.equals(newValue)) {
                setHighlightStatusIncludingChildren(false);
            }
        });

        setOnMouseExited(event -> {
            if(!isEmpty() && !contextMenu.isShowing()) {
                setHighlightStatusIncludingChildren(false);
            }
        });

        setOnScroll(event -> {
            if(!isEmpty() && !contextMenu.isShowing()) {
                setHighlightStatusIncludingChildren(false);
            }
        });

        draggedOver.addListener((observable, oldValue, newValue) -> pseudoClassStateChanged(draggedOverPseudoClass, newValue));

        hideBoundingBoxMenuItem.setOnAction(event -> {
            if(getTreeItem() instanceof ObjectCategoryTreeItem) {
                ((ObjectCategoryTreeItem) getTreeItem()).setIconToggledOn(false);
            } else if(getTreeItem() instanceof BoundingBoxTreeItem) {
                ((BoundingBoxTreeItem) getTreeItem()).setIconToggledOn(false);
            } else if(getTreeItem() instanceof BoundingPolygonTreeItem) {
                ((BoundingPolygonTreeItem) getTreeItem()).setIconToggledOn(false);
            }
        });
    }

    private void setHighlightStatusIncludingChildren(boolean highlightStatus) {
        TreeItem<Object> treeItem = getTreeItem();

        if(treeItem instanceof BoundingBoxTreeItem) {
            final BoundingBoxView boundingBox = (BoundingBoxView) treeItem.getValue();
            if(!boundingBox.isSelected()) {
                boundingBox.setHighlighted(highlightStatus);
            }
        } else if(treeItem instanceof ObjectCategoryTreeItem) {
            for(TreeItem<Object> child : treeItem.getChildren()) {
                if(child instanceof BoundingBoxTreeItem) {
                    final BoundingBoxView childBoundingBox = (BoundingBoxView) child.getValue();

                    if(!childBoundingBox.isSelected()) {
                        childBoundingBox.setHighlighted(highlightStatus);
                    }
                } else if(child instanceof BoundingPolygonTreeItem) {
                    final BoundingPolygonView childBoundingPolygon = (BoundingPolygonView) child.getValue();

                    if(!childBoundingPolygon.isSelected()) {
                        childBoundingPolygon.setHighlighted(highlightStatus);
                    }
                }
            }
        } else if(treeItem instanceof BoundingPolygonTreeItem) {
            final BoundingPolygonView boundingPolygon = (BoundingPolygonView) treeItem.getValue();

            if(!boundingPolygon.isSelected()) {
                boundingPolygon.setHighlighted(highlightStatus);
            }
        }
    }

    private HBox createContentBox() {
        final TreeItem<Object> treeItem = getTreeItem();
        final HBox content = new HBox(treeItem.getGraphic(), nameText);
        content.setId(TREE_CELL_CONTENT_ID);
        content.setAlignment(Pos.CENTER_LEFT);

        if(treeItem instanceof BoundingBoxTreeItem) {
            final BoundingBoxView boundingBox = (BoundingBoxView) treeItem.getValue();

            nameText.textProperty().bind(boundingBox.getObjectCategory()
                    .nameProperty().concat(" ").concat(((BoundingBoxTreeItem) treeItem).getId()));
            tagIconRegion.visibleProperty().bind(Bindings.size(boundingBox.getTags()).greaterThan(0));
            content.getChildren().add(tagIconRegion);
        } else if(treeItem instanceof BoundingPolygonTreeItem) {
            final BoundingPolygonView boundingPolygonView = (BoundingPolygonView) treeItem.getValue();

            nameText.textProperty().bind(boundingPolygonView.getObjectCategory()
                    .nameProperty().concat(" ").concat(((BoundingPolygonTreeItem) treeItem).getId()));
            tagIconRegion.visibleProperty().bind(Bindings.size(boundingPolygonView.getTags()).greaterThan(0));
            content.getChildren().add(tagIconRegion);
        } else if(treeItem instanceof ObjectCategoryTreeItem) {
            nameText.textProperty().bind(((ObjectCategory) treeItem.getValue()).nameProperty());
            nameText.setId(CATEGORY_NAME_TEXT_ID);
            additionalInfoText.textProperty().bind(Bindings.format("(%d)", treeItem.getChildren().size()));
            content.getChildren().add(additionalInfoText);
        }

        return content;
    }

    private Region createTagIconRegion() {
        Region region = new Region();
        region.setId(TAG_ICON_REGION_ID);
        return region;
    }

    @SuppressWarnings("UnnecessaryLambda")
    private EventHandler<ContextMenuEvent> createShowContextMenuEventHandler() {
        return event -> {
            if(getItem() instanceof BoundingBoxView) {
                contextMenu.show((BoundingBoxView) getItem(), event.getScreenX(), event.getScreenY());
            } else if(getItem() instanceof BoundingPolygonView) {
                contextMenu.show((BoundingPolygonView) getItem(), event.getScreenX(), event.getScreenY());
            }
        };
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ChangeListener<Boolean> createBoundingBoxVisibilityListener() {
        return ((observable, oldValue, newValue) -> {
            if(!Boolean.TRUE.equals(newValue)) {
                contextMenu.hide();
            }
        });
    }
}
