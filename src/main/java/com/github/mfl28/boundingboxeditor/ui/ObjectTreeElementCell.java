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
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import org.controlsfx.control.PopOver;

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
    private static final String REFINE_CONTEXT_MENU_ITEM_ID = "add-vertices-context-menu";
    private static final String DELETE_VERTICES_MENU_ITEM_TEXT = "Remove Vertices";
    private static final String DELETE_VERTICES_CONTEXT_MENU_ITEM_ID = "delete-vertices-context-menu";
    private static final String REFINE_MENU_ITEM_TOOLTIP_TEXT = "Add new vertices between selected vertices";
    private static final String DELETE_VERTICES_MENU_ITEM_TOOLTIP_TEXT = "Delete selected vertices";
    private static final String CHANGE_CATEGORY_MENU_ITEM_TEXT = "Change Category";
    private static final String CHANGE_CATEGORY_CONTEXT_MENU_ITEM_ID = "change-category-menu";
    private static final String HIDE_OTHERS_CONTEXT_MENU_ITEM_ID = "hide-others-context-menu";
    private static final String HIDE_OTHERS_MENU_ITEM_TEXT = "Hide others";
    private static final String HIDE_ALL_CONTEXT_MENU_ITEM_ID = "hide-all-context-menu";
    private static final String HIDE_ALL_MENU_ITEM_TEXT = "Hide all";
    private static final String SHOW_CONTEXT_MENU_ITEM_ID = "show-context-menu";
    private static final String SHOW_MENU_ITEM_TEXT = "Show";
    private static final String SHOW_ALL_CONTEXT_MENU_ID = "show-all-context-menu";
    private static final String SHOW_ALL_MENU_ITEM_TEXT = "Show all";

    private final MenuItem deleteBoundingShapeMenuItem = createDeleteBoundingShapeMenuItem();
    private final MenuItem hideBoundingShapeMenuItem = createHideBoundingShapeMenuItem();
    private final MenuItem hideAllBoundingShapesMenuItem = createHideAllBoundingShapesMenuItem();
    private final MenuItem hideOtherBoundingShapesMenuItem = createHideOtherBoundingShapesMenuItem();
    private final MenuItem showBoundingShapeMenuItem = createShowBoundingShapeMenuItem();
    private final MenuItem showAllBoundingShapesMenuItem = createShowAllBoundingShapesMenuItem();
    private final MenuItem addVerticesMenuItem = createRefineMenuItem();
    private final MenuItem deleteVerticesMenuItem = createDeleteVerticesMenuItem();
    private final MenuItem changeObjectCategoryMenuItem = createChangeObjectCategoryMenuItem();
    private final SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
    private final BooleanProperty draggedOver = new SimpleBooleanProperty(false);
    private final Text nameText = new Text();
    private final Text additionalInfoText = new Text();
    private final Region tagIconRegion = createTagIconRegion();
    private final ObjectTreeElementContextMenu contextMenu = new ObjectTreeElementContextMenu();
    private final EventHandler<ContextMenuEvent> showContextMenuEventHandler = createShowContextMenuEventHandler();
    private final ChangeListener<Boolean> boundingShapeVisibilityListener = createBoundingShapeVisibilityListener();
    private final PopOver popOver = new PopOver();
    private final ImageView popOverImageView = new ImageView();

    /**
     * Creates a new tree-cell object responsible for the visual representation of a {@link ObjectCategoryTreeItem}
     * or a {@link BoundingShapeTreeItem} in a {@link ObjectTreeView}.
     */
    ObjectTreeElementCell() {
        nameText.getStyleClass().add(NAME_TEXT_STYLE);
        additionalInfoText.setId(INFO_TEXT_ID);

        popOver.setAutoHide(true);
        popOver.setHideOnEscape(true);
        popOver.setArrowLocation(PopOver.ArrowLocation.LEFT_CENTER);
        popOver.setContentNode(popOverImageView);
        popOverImageView.setPreserveRatio(true);

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
            popOverImageView.setImage(null);
            popOverImageView.setViewport(null);
            popOverImageView.setClip(null);
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

    /**
     * Returns the menu-item of the context-menu which allows
     * the user to change the object category of the currently associated bounding shape view object.
     *
     * @return the menu-item
     */
    MenuItem getChangeObjectCategoryMenuItem() {
        return changeObjectCategoryMenuItem;
    }

    /**
     * Returns the menu-item of the context-menu which allows
     * the user to hide all currently existing bounding shapes.
     *
     * @return the menu-item
     */
    MenuItem getHideAllBoundingShapesMenuItem() {
        return hideAllBoundingShapesMenuItem;
    }

    /**
     * Returns the menu-item of the context-menu which allows
     * the user to hide all bounding shapes except the one
     * associated with this cell's current TreeItem or its children.
     *
     * @return the menu-item
     */
    MenuItem getHideOtherBoundingShapesMenuItem() {
        return hideOtherBoundingShapesMenuItem;
    }

    /**
     * Returns the menu-item of the context-menu which allows
     * the user to show all currently existing bounding shapes.
     *
     * @return the menu-item
     */
    MenuItem getShowAllBoundingShapesMenuItem() {
        return showAllBoundingShapesMenuItem;
    }

    /**
     * Returns the popover of this cell.
     *
     * @return the popover
     */
    PopOver getPopOver() {
        return popOver;
    }

    /**
     * Returns the ImageView associated withe this cell's
     * popover.
     *
     * @return the imageview
     */
    ImageView getPopOverImageView() {
        return popOverImageView;
    }

    void setHighlightStatusIncludingChildren(boolean highlightStatus) {
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

    private MenuItem createShowAllBoundingShapesMenuItem() {
        final CustomMenuItem showAllMenuItem = new CustomMenuItem(new Label(SHOW_ALL_MENU_ITEM_TEXT));
        showAllMenuItem.setId(SHOW_ALL_CONTEXT_MENU_ID);
        Tooltip.install(showAllMenuItem.getContent(),
                        UiUtils.createTooltip("",
                                              Controller.KeyCombinations.showAllBoundingShapes));
        return showAllMenuItem;
    }

    private MenuItem createShowBoundingShapeMenuItem() {
        final CustomMenuItem showMenuItem = new CustomMenuItem(new Label(SHOW_MENU_ITEM_TEXT));
        showMenuItem.setId(SHOW_CONTEXT_MENU_ITEM_ID);
        Tooltip.install(showMenuItem.getContent(),
                        UiUtils.createTooltip("",
                                              Controller.KeyCombinations.showSelectedBoundingShape));
        return showMenuItem;
    }

    private MenuItem createHideAllBoundingShapesMenuItem() {
        final CustomMenuItem hideAllMenuItem = new CustomMenuItem(new Label(HIDE_ALL_MENU_ITEM_TEXT));
        hideAllMenuItem.setId(HIDE_ALL_CONTEXT_MENU_ITEM_ID);
        Tooltip.install(hideAllMenuItem.getContent(),
                        UiUtils.createTooltip("",
                                              Controller.KeyCombinations.hideAllBoundingShapes));
        return hideAllMenuItem;
    }

    private MenuItem createHideOtherBoundingShapesMenuItem() {
        final CustomMenuItem hideOthersMenuItem = new CustomMenuItem(new Label(HIDE_OTHERS_MENU_ITEM_TEXT));
        hideOthersMenuItem.setId(HIDE_OTHERS_CONTEXT_MENU_ITEM_ID);
        Tooltip.install(hideOthersMenuItem.getContent(),
                        UiUtils.createTooltip("",
                                              Controller.KeyCombinations.hideNonSelectedBoundingShapes));
        return hideOthersMenuItem;
    }

    private void setUpInternalListeners() {
        setOnScroll(event -> {
            if(!isEmpty() && !contextMenu.isShowing()) {
                setHighlightStatusIncludingChildren(false);
            }
        });

        hideBoundingShapeMenuItem.setOnAction(this::handleHide);
        showBoundingShapeMenuItem.setOnAction(this::handleShow);

        draggedOver.addListener(
                (observable, oldValue, newValue) -> pseudoClassStateChanged(draggedOverPseudoClass, newValue));
        addVerticesMenuItem.setOnAction(event -> ((BoundingPolygonView) getItem()).refine());
        deleteVerticesMenuItem.setOnAction(event -> ((BoundingPolygonView) getItem()).removeEditingVertices());
    }

    private HBox createContentBox() {
        final TreeItem<Object> treeItem = getTreeItem();
        final HBox content = new HBox(treeItem.getGraphic(), nameText);
        content.setId(TREE_CELL_CONTENT_ID);
        content.setAlignment(Pos.CENTER_LEFT);

        if(treeItem instanceof BoundingShapeTreeItem) {
            final BoundingShapeViewData boundingShapeViewData =
                    ((BoundingShapeViewable) treeItem.getValue()).getViewData();

            nameText.textProperty().bind(boundingShapeViewData.getObjectCategory()
                                                              .nameProperty().concat(" ")
                                                              .concat(((BoundingShapeTreeItem) treeItem).getId()));
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
                        UiUtils.createTooltip("",
                                              Controller.KeyCombinations.deleteSelectedBoundingShape));
        return deleteMenuItem;
    }

    private MenuItem createHideBoundingShapeMenuItem() {
        CustomMenuItem hideMenuItem = new CustomMenuItem(new Label(HIDE_BOUNDING_SHAPE_MENU_ITEM_TEXT));
        hideMenuItem.setId(HIDE_BOUNDING_SHAPE_CONTEXT_MENU_ITEM_ID);
        Tooltip.install(hideMenuItem.getContent(),
                        UiUtils.createTooltip("",
                                              Controller.KeyCombinations.hideSelectedBoundingShape));
        return hideMenuItem;
    }

    private MenuItem createRefineMenuItem() {
        CustomMenuItem menuItem = new CustomMenuItem(new Label(REFINE_MENU_ITEM_TEXT));
        menuItem.setId(REFINE_CONTEXT_MENU_ITEM_ID);

        Tooltip.install(menuItem.getContent(),
                        UiUtils.createTooltip(REFINE_MENU_ITEM_TOOLTIP_TEXT,
                                              Controller.KeyCombinations.addVerticesToPolygon));
        return menuItem;
    }

    private MenuItem createDeleteVerticesMenuItem() {
        CustomMenuItem menuItem = new CustomMenuItem(new Label(DELETE_VERTICES_MENU_ITEM_TEXT));
        menuItem.setId(DELETE_VERTICES_CONTEXT_MENU_ITEM_ID);

        Tooltip.install(menuItem.getContent(),
                        UiUtils.createTooltip(DELETE_VERTICES_MENU_ITEM_TOOLTIP_TEXT,
                                              Controller.KeyCombinations.removeEditingVerticesWhenBoundingPolygonSelected));
        return menuItem;
    }

    private MenuItem createChangeObjectCategoryMenuItem() {
        CustomMenuItem menuItem = new CustomMenuItem(new Label(CHANGE_CATEGORY_MENU_ITEM_TEXT));
        menuItem.setId(CHANGE_CATEGORY_CONTEXT_MENU_ITEM_ID);

        Tooltip.install(menuItem.getContent(),
                        UiUtils.createTooltip("", Controller.KeyCombinations.changeSelectedBoundingShapeCategory));
        return menuItem;
    }

    private void handleHide(ActionEvent event) {
        final TreeItem<Object> treeItem = getTreeItem();

        if(treeItem instanceof IconToggleable) {
            ((IconToggleable) treeItem).setIconToggledOn(false);
        }
    }

    private void handleShow(ActionEvent event) {
        final TreeItem<Object> treeItem = getTreeItem();

        if(treeItem instanceof IconToggleable) {
            ((IconToggleable) treeItem).setIconToggledOn(true);
        }
    }

    private class ObjectTreeElementContextMenu extends ContextMenu {
        ObjectTreeElementContextMenu() {
            super(hideBoundingShapeMenuItem,
                  hideOtherBoundingShapesMenuItem,
                  hideAllBoundingShapesMenuItem,
                  showBoundingShapeMenuItem,
                  showAllBoundingShapesMenuItem,
                  changeObjectCategoryMenuItem,
                  deleteBoundingShapeMenuItem);
            setUpInternalListeners();
        }

        void addPolygonFeatures() {
            if(!getItems().contains(separatorMenuItem)) {
                getItems().add(separatorMenuItem);
            }

            if(!getItems().contains(addVerticesMenuItem)) {
                getItems().add(addVerticesMenuItem);
            }

            if(!getItems().contains(deleteVerticesMenuItem)) {
                getItems().add(deleteVerticesMenuItem);
            }

            addVerticesMenuItem.disableProperty()
                               .bind(Bindings.size(((BoundingPolygonView) ObjectTreeElementCell.this.getItem())
                                                           .getEditingIndices()).lessThan(2));
            deleteVerticesMenuItem.disableProperty().bind(
                    Bindings.size(((BoundingPolygonView) ObjectTreeElementCell.this.getItem()).getEditingIndices())
                            .isEqualTo(0)
                            .or(Bindings.size(((BoundingPolygonView) ObjectTreeElementCell.this.getItem())
                                                      .getEditingIndices()).greaterThan(
                                    Bindings.size(((BoundingPolygonView) ObjectTreeElementCell.this.getItem())
                                                          .getVertexHandles()).subtract(2))));
        }

        void removePolygonFeatures() {
            getItems().remove(addVerticesMenuItem);
            getItems().remove(deleteVerticesMenuItem);

            addVerticesMenuItem.disableProperty().unbind();
            deleteVerticesMenuItem.disableProperty().unbind();
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
