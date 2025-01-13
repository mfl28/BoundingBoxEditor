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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

import java.util.Objects;


/**
 * Represents the category (e.g. "Person", "Car" etc.) an annotated object belongs to.
 */
public class ObjectCategory {
    private final StringProperty name;
    private final ObjectProperty<Color> color;

    /**
     * Creates a new object category.
     *
     * @param name  the name of the object category.
     * @param color the color used for the visual representation of
     *              the category in the program.
     */
    public ObjectCategory(String name, Color color) {
        this.name = new SimpleStringProperty(name);
        this.color = new SimpleObjectProperty<>(color);
    }

    /**
     * Returns the category-name property.
     *
     * @return the string-property of the name
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the category-color-property .
     *
     * @return the color-property
     */
    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.get(), color.get());
    }

    @Override
    public boolean equals(Object other) {
        if(this == other) {
            return true;
        }

        if(!(other instanceof ObjectCategory otherCategory)) {
            return false;
        }

        return Objects.equals(otherCategory.getName(), this.getName()) && Objects.equals(otherCategory.getColor(), this.getColor());
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the name of the object category.
     *
     * @return the name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the name of the object category.
     *
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * Returns the color used for the visual representation of the object category.
     *
     * @return the color
     */
    public Color getColor() {
        return color.get();
    }

    /**
     * Sets the color used for the visual representation of the object category.
     *
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color.set(color);
    }
}
