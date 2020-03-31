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
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.util.Objects;

/**
 * Represents a tree-cell in a {@link ObjectTreeView}. Instances of this class are either associated
 * with a {@link ObjectCategoryTreeItem} or a {@link BoundingShapeTreeItem} and are responsible for the
 * visual representation of these items in the {@link ObjectTreeView}.
 *
 * @see TreeCell
 */
class ObjectTreeElementCell extends TreeCell<Object> {
    private static final String NAME_TEXT_STYLE = "default-text";
    private static final String INFO_TEXT_ID = "info-text";
    private static final String TAG_ICON_REGION_ID = "tag-icon";
    private static final PseudoClass draggedOverPseudoClass = PseudoClass.getPseudoClass("dragged-over");
    private static final String CATEGORY_NAME_TEXT_ID = "category-name-text";
    private static final String TREE_CELL_CONTENT_ID = "tree-cell-content";
    private static final String DELETE_CONTEXT_MENU_ITEM_ID = "delete-context-menu";
    private static final String DELETE_BOUNDING_SHAPE_MENU_ITEM_TEXT = "Delete";
    private static final String HIDE_BOUNDING_SHAPE_CONTEXT_MENU_ITEM_ID = "hide-context-menu";
    private static final String HIDE_BOUNDING_SHAPE_MENU_ITEM_TEXT = "Hide";
    private static final String REFINE_MENU_ITEM_TEXT = "Add Vertices";
    private static final String REFINE_CONTEXT_MENU_ITEM_ID = "refine-context-menu";
    private static final String DELETE_VERTICES_MENU_ITEM_TEXT = "Remove Vertices";
    private static final String DELETE_VERTICES_CONTEXT_MENU_ITEM_ID = "delete-vertices-context-menu";
    private static final String REFINE_MENU_ITEM_TOOLTIP_TEXT = "Add new vertices between selected vertices";
    private static final String DELETE_VERTICES_MENU_ITEM_TOOLTIP_TEXT = "Delete selected vertices";

    private final MenuItem deleteBoundingShapeMenuItem = createDeleteBoundingShapeMenuItem();
    private final MenuItem hideBoundingShapeMenuItem = createHideBoundingShapeMenuItem();
    private final MenuItem refineMenuItem = createRefineMenuItem();
    private final MenuItem deleteVerticesMenuItem = createDeleteVerticesMenuItem();

    private final BooleanProperty draggedOver = new SimpleBooleanProperty(false);
    private final Text nameText = new Text();
    private final Text additionalInfoText = new Text();
    private final Region tagIconRegion = createTagIconRegion();
    protected ObjectTreeElementContextMenu contextMenu = new ObjectTreeElementContextMenu();
    private final EventHandler<ContextMenuEvent> showContextMenuEventHandler = createShowContextMenuEventHandler();
    private final ChangeListener<Boolean> boundingShapeVisibilityListener = createBoundingShapeVisibilityListener();

    /**
     * Creates a new tree-cell object responsible for the visual representation of a {@link ObjectCategoryTreeItem}
     * or a {@link BoundingShapeTreeItem} in a {@link ObjectTreeView}.
     */
    ObjectTreeElementCell() {
        nameText.getStyleClass().add(NAME_TEXT_STYLE);
        additionalInfoText.setId(INFO_TEXT_ID);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setUpInternalListeners();
    }

    @Override
    protected void updateItem(Object newCellObject, boolean empty) {
        Object oldCellObject = getItem();

        if(Objects.equals(newCellObject, oldCellObject)) {
            return;
        }

        if(oldCellObject instanceof Shape) {
            Shape oldItem = (Shape) oldCellObject;
            // Remove the old item's context-menu event-handler.
            oldItem.removeEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, showContextMenuEventHandler);
            oldItem.visibleProperty().removeListener(boundingShapeVisibilityListener);
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

            if(newCellObject instanceof BoundingBoxView) {
                contextMenu.removePolygonFeatures();
            } else if(newCellObject instanceof BoundingPolygonView) {
                contextMenu.addPolygonFeatures();
            }

            // Register the contextMenu with the cell.
            setContextMenu(contextMenu);

            if(newCellObject instanceof Shape) {
                // Register the contextMenu with the shape associated with the cell. This
                // allows to display the contextMenu by right-clicking on the shape itself.
                ((Shape) newCellObject).setOnContextMenuRequested(showContextMenuEventHandler);
                // The context menu should be hidden when the shape associated with this cell
                // is hidden.
                ((Shape) newCellObject).visibleProperty().addListener(boundingShapeVisibilityListener);
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
     * the user to delete the currently associated bounding shape view object.
     *
     * @return the menu-item
     */
    MenuItem getDeleteBoundingShapeMenuItem() {
        return deleteBoundingShapeMenuItem;
    }

    private void setUpInternalListeners() {
        setOnMouseEntered(event -> {
            if(!isEmpty()) {
                setHighlightStatusIncludingChildren(true);
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

        hideBoundingShapeMenuItem.setOnAction(event -> {
            TreeItem<Object> treeItem = getTreeItem();

            if(treeItem instanceof ObjectCategoryTreeItem) {
                ((ObjectCategoryTreeItem) treeItem).setIconToggledOn(false);
            } else if(treeItem instanceof BoundingShapeTreeItem) {
                ((BoundingShapeTreeItem) treeItem).setIconToggledOn(false);
            }
        });

        draggedOver.addListener((observable, oldValue, newValue) -> pseudoClassStateChanged(draggedOverPseudoClass, newValue));
        refineMenuItem.setOnAction(event -> ((BoundingPolygonView) getItem()).refine());
        deleteVerticesMenuItem.setOnAction(event -> ((BoundingPolygonView) getItem()).removeEditingVertices());
    }

    private void setHighlightStatusIncludingChildren(boolean highlightStatus) {
        TreeItem<Object> treeItem = getTreeItem();

        if(treeItem instanceof ObjectCategoryTreeItem) {
            treeItem.getChildren().stream()
                    .filter(child -> child.getValue() instanceof BoundingShapeViewable)
                    .map(child -> ((BoundingShapeViewable) child.getValue()).getViewData())
                    .filter(viewData -> !viewData.isSelected())
                    .forEach(viewData -> viewData.setHighlighted(highlightStatus));
        } else if(treeItem instanceof BoundingShapeTreeItem) {
            final BoundingShapeViewData viewData = ((BoundingShapeViewable) treeItem.getValue()).getViewData();

            if(!viewData.isSelected()) {
                viewData.setHighlighted(highlightStatus);
            }
        }
    }

    private HBox createContentBox() {
        final TreeItem<Object> treeItem = getTreeItem();
        final HBox content = new HBox(treeItem.getGraphic(), nameText);
        content.setId(TREE_CELL_CONTENT_ID);
        content.setAlignment(Pos.CENTER_LEFT);

        if(treeItem instanceof BoundingShapeTreeItem) {
            final BoundingShapeViewData boundingShapeViewData = ((BoundingShapeViewable) treeItem.getValue()).getViewData();

            nameText.textProperty().bind(boundingShapeViewData.getObjectCategory()
                    .nameProperty().concat(" ").concat(((BoundingShapeTreeItem) treeItem).getId()));
            tagIconRegion.visibleProperty().bind(Bindings.size(boundingShapeViewData.getTags()).greaterThan(0));
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
            if(getItem() instanceof Shape
                    && getItem() instanceof Toggle && ((Toggle) getItem()).isSelected()) {
                contextMenu.show((Shape) getItem(), event.getScreenX(), event.getScreenY());
            }
        };
    }

    @SuppressWarnings("UnnecessaryLambda")
    private ChangeListener<Boolean> createBoundingShapeVisibilityListener() {
        return ((observable, oldValue, newValue) -> {
            if(!Boolean.TRUE.equals(newValue)) {
                contextMenu.hide();
            }
        });
    }

    private MenuItem createDeleteBoundingShapeMenuItem() {
        CustomMenuItem deleteMenuItem = new CustomMenuItem(new Label(DELETE_BOUNDING_SHAPE_MENU_ITEM_TEXT));
        deleteMenuItem.setId(DELETE_CONTEXT_MENU_ITEM_ID);
        Tooltip.install(deleteMenuItem.getContent(),
                UiUtils.createTooltip("", Controller.KeyCombinations.deleteSelectedBoundingShape));
        return deleteMenuItem;
    }

    private MenuItem createHideBoundingShapeMenuItem() {
        CustomMenuItem hideMenuItem = new CustomMenuItem(new Label(HIDE_BOUNDING_SHAPE_MENU_ITEM_TEXT));
        hideMenuItem.setId(HIDE_BOUNDING_SHAPE_CONTEXT_MENU_ITEM_ID);
        Tooltip.install(hideMenuItem.getContent(),
                UiUtils.createTooltip("", Controller.KeyCombinations.hideSelectedBoundingShape));
        return hideMenuItem;
    }

    private MenuItem createRefineMenuItem() {
        CustomMenuItem menuItem = new CustomMenuItem(new Label(REFINE_MENU_ITEM_TEXT));
        menuItem.setId(REFINE_CONTEXT_MENU_ITEM_ID);

        Tooltip.install(menuItem.getContent(),
                UiUtils.createTooltip(REFINE_MENU_ITEM_TOOLTIP_TEXT));
        return menuItem;
    }

    private MenuItem createDeleteVerticesMenuItem() {
        CustomMenuItem menuItem = new CustomMenuItem(new Label(DELETE_VERTICES_MENU_ITEM_TEXT));
        menuItem.setId(DELETE_VERTICES_CONTEXT_MENU_ITEM_ID);

        Tooltip.install(menuItem.getContent(),
                UiUtils.createTooltip(DELETE_VERTICES_MENU_ITEM_TOOLTIP_TEXT));
        return menuItem;
    }

    private class ObjectTreeElementContextMenu extends ContextMenu {
        ObjectTreeElementContextMenu() {
            super(hideBoundingShapeMenuItem, deleteBoundingShapeMenuItem);
            setUpInternalListeners();
        }

        void addPolygonFeatures() {
            if(!getItems().contains(refineMenuItem)) {
                getItems().add(refineMenuItem);
            }

            if(!getItems().contains(deleteVerticesMenuItem)) {
                getItems().add(deleteVerticesMenuItem);
            }
        }

        void removePolygonFeatures() {
            getItems().remove(refineMenuItem);
            getItems().remove(deleteVerticesMenuItem);
        }

        private void setUpInternalListeners() {
            showingProperty().addListener((observable, oldValue, newValue) -> {
                if(!ObjectTreeElementCell.this.isEmpty() && !Boolean.TRUE.equals(newValue)) {
                    ObjectTreeElementCell.this.setHighlightStatusIncludingChildren(false);
                }
            });
        }
    }
}