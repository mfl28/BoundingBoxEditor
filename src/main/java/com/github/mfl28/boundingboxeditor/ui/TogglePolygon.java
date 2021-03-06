/*
 * Copyright (C) 2021 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.shape.Polygon;


public class TogglePolygon extends Polygon implements Toggleable {
    private static final String TOGGLED_ON_PSEUDO_CLASS_NAME = "toggled-on";
    private static final PseudoClass TOGGLED_ON_PSEUDO_CLASS = PseudoClass.getPseudoClass(TOGGLED_ON_PSEUDO_CLASS_NAME);
    private static final String TOGGLE_POLYGON_ID = "toggle-polygon";

    private final BooleanProperty toggledOn = createToggledOnProperty();

    /**
     * Constructs a new toggleable square UI-element which has a toggled-on CSS-pseudo-state that can be used
     * to alter its appearance based on the current value of a toggledOn-property.
     *
     * @param sideLength the side-length of the square
     */
    TogglePolygon(double sideLength) {
        super(
                sideLength * 0.5, 0,
                0, sideLength * 0.4,
                0.2 * sideLength, sideLength,
                0.8 * sideLength, sideLength,
                sideLength, sideLength * 0.4
        );

        setId(TOGGLE_POLYGON_ID);
        pseudoClassStateChanged(TOGGLED_ON_PSEUDO_CLASS, true);
    }

    /**
     * Returns the toggled-on property.
     *
     * @return the toggled-on property
     */
    @Override
    public BooleanProperty toggledOnProperty() {
        return toggledOn;
    }

    /**
     * Returns a boolean indicating the toggle-state of the square.
     *
     * @return true if the square is currently toggled-on, false otherwise
     */
    @Override
    public boolean isToggledOn() {
        return toggledOn.get();
    }

    /**
     * Sets the toggle-state of the square.
     *
     * @param toggledOn true sets the toggle-state to on, otherwise to off
     */
    @Override
    public void setToggledOn(boolean toggledOn) {
        this.toggledOn.set(toggledOn);
    }

    private BooleanProperty createToggledOnProperty() {
        return new BooleanPropertyBase(true) {
            @Override
            public Object getBean() {
                return TogglePolygon.this;
            }

            @Override
            public String getName() {
                return TOGGLED_ON_PSEUDO_CLASS_NAME;
            }

            @Override
            protected void invalidated() {
                pseudoClassStateChanged(TOGGLED_ON_PSEUDO_CLASS, get());
            }
        };
    }
}
