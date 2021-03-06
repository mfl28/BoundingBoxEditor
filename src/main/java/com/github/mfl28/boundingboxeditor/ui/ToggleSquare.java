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
import javafx.scene.shape.Rectangle;

/**
 * Represents a square UI-element which has a toggled-on CSS-pseudo-state that can be used
 * to alter its appearance based on the current value of a toggledOn property.
 *
 * @see Rectangle
 */
class ToggleSquare extends Rectangle implements Toggleable {
    private static final String TOGGLED_ON_PSEUDO_CLASS_NAME = "toggled-on";
    private static final PseudoClass TOGGLED_ON_PSEUDO_CLASS = PseudoClass.getPseudoClass(TOGGLED_ON_PSEUDO_CLASS_NAME);
    private static final String TOGGLE_SQUARE_ID = "toggle-square";

    private final BooleanProperty toggledOn = createToggledOnProperty();

    /**
     * Constructs a new toggleable square UI-element which has a toggled-on CSS-pseudo-state that can be used
     * to alter its appearance based on the current value of a toggledOn-property.
     *
     * @param sideLength the side-length of the square
     */
    ToggleSquare(double sideLength) {
        super(0, 0, sideLength, sideLength);

        setId(TOGGLE_SQUARE_ID);
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
                return ToggleSquare.this;
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
