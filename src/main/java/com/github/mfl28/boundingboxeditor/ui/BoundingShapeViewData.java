/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Shape;

import java.util.Objects;

/**
 * Class holding common data and methods of bounding shape objects.
 */
public class BoundingShapeViewData {
    private final Property<Bounds> autoScaleBounds = new SimpleObjectProperty<>();
    private final Group nodeGroup = new Group();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final BooleanProperty highlighted = new SimpleBooleanProperty(false);
    private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<>();
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final Shape baseShape;
    private final ObjectProperty<ObjectCategory> objectCategory = new SimpleObjectProperty<>();
    private String previousObjectCategoryName;
    private BoundingShapeTreeItem treeItem;

    public BoundingShapeViewData(Shape shape, ObjectCategory objectCategory) {
        this.baseShape = shape;
        nodeGroup.getChildren().add(shape);

        setUpInternalListeners();
        this.objectCategory.set(objectCategory);
    }

    public Property<Bounds> autoScaleBounds() {
        return autoScaleBounds;
    }

    public BooleanProperty getSelected() {
        return selected;
    }

    public BooleanProperty getHighlighted() {
        return highlighted;
    }


    public BooleanProperty highlightedProperty() {
        return highlighted;
    }

    public Shape getBaseShape() {
        return baseShape;
    }

    public ToggleGroup getToggleGroup() {
        return toggleGroup.get();
    }

    public void setToggleGroup(ToggleGroup toggleGroup) {
        this.toggleGroup.set(toggleGroup);
    }

    /**
     * Sets the highlighted-status of the bounding-box.
     *
     * @param highlighted true to set highlighting on, otherwise off
     */
    void setHighlighted(boolean highlighted) {
        this.highlighted.set(highlighted);
    }

    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroup;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectCategory, tags);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof BoundingShapeViewData other)) {
            return false;
        }

        return Objects.equals(tags, other.tags)
                && Objects.equals(objectCategory.get(), other.objectCategory.get());
    }

    /**
     * Returns the associated {@link ObjectCategory} object.
     *
     * @return the {@link ObjectCategory} object
     */
    public ObjectCategory getObjectCategory() {
        return objectCategory.get();
    }

    public void setObjectCategory(ObjectCategory objectCategory) {
        this.objectCategory.set(objectCategory);
    }

    public ObjectProperty<ObjectCategory> objectCategoryProperty() {
        return objectCategory;
    }

    public String getPreviousObjectCategoryName() {
        return previousObjectCategoryName;
    }

    /**
     * Returns the currently assigned tags.
     *
     * @return the tags
     */
    ObservableList<String> getTags() {
        return tags;
    }

    /**
     * Returns the associated {@link TreeItem} object.
     *
     * @return the {@link TreeItem} object
     */
    BoundingShapeTreeItem getTreeItem() {
        return treeItem;
    }

    /**
     * Sets the associated {@link TreeItem} object.
     */
    void setTreeItem(BoundingShapeTreeItem treeItem) {
        this.treeItem = treeItem;
    }

    /**
     * Returns a {@link Group} object whose children are the components that make
     * up this {@link BoundingBoxView} UI-element (the rectangle itself as well as the resize-handles).
     * This function is used when the bounding-box should be added to the scene-graph.
     *
     * @return the group
     */
    Group getNodeGroup() {
        return nodeGroup;
    }

    private void setUpInternalListeners() {
        objectCategory.addListener((observable, oldValue, newValue) -> {

            if(oldValue != null) {
                baseShape.strokeProperty().unbind();
            }

            if(newValue != null) {
                baseShape.strokeProperty().bind(newValue.colorProperty());
                previousObjectCategoryName = newValue.getName();
            }
        });
    }
}