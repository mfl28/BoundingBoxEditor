/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.data;

import com.github.mfl28.boundingboxeditor.ui.BoundingShapeViewable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base class of data-components of a bounding-shape view objects.
 */
public abstract class BoundingShapeData {
    private final ObjectCategory category;
    private final List<String> tags;
    private List<BoundingShapeData> parts = Collections.emptyList();

    protected BoundingShapeData(ObjectCategory category, List<String> tags) {
        this.category = category;
        this.tags = tags;
    }

    public abstract BoundingShapeViewable toBoundingShapeView(double imageWidth, double imageHeight);

    /**
     * Returns the name of the category of the bounding-box.
     *
     * @return the category name
     */
    public String getCategoryName() {
        return category.getName();
    }

    /**
     * Returns the category of the bounding-box.
     *
     * @return the category
     */
    public ObjectCategory getCategory() {
        return category;
    }

    /**
     * Returns a list of the tags that are registered with the bounding-box.
     *
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Returns the {@link BoundingBoxData}-objects that are registered as nested parts of the bounding-box.
     *
     * @return the parts
     */
    public List<BoundingShapeData> getParts() {
        return parts;
    }

    /**
     * Registers {@link BoundingBoxData}-objects as nested parts of the bounding-box.
     *
     * @param parts the BoundingBoxData-objects to be registered
     */
    public void setParts(List<BoundingShapeData> parts) {
        this.parts = parts;
    }

    public abstract <T> T accept(BoundingShapeDataVisitor<T> visitor);

    @Override
    public int hashCode() {
        return Objects.hash(category, tags, parts);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(!(o instanceof BoundingShapeData that)) {
            return false;
        }

        return Objects.equals(category, that.category) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(parts, that.parts);
    }
}
