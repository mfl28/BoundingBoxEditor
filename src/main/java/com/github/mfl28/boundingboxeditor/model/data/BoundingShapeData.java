/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import java.util.*;
import java.util.stream.Stream;

/**
 * Base class of data-components of a bounding-shape view objects.
 */
public abstract sealed class BoundingShapeData permits BoundingBoxData, BoundingPolygonData {
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

    /**
     * Returns this bounding-shape followed by all of its (transitively) nested parts,
     * level by level (breadth-first).
     *
     * @return the stream of bounding-shapes
     */
    public Stream<BoundingShapeData> flatten() {
        final List<BoundingShapeData> result = new ArrayList<>();
        final Deque<BoundingShapeData> queue = new ArrayDeque<>();
        queue.add(this);

        while(!queue.isEmpty()) {
            final BoundingShapeData current = queue.poll();
            result.add(current);
            queue.addAll(current.getParts());
        }

        return result.stream();
    }

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
