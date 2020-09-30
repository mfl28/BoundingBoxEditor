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
package boundingboxeditor.ui;

import javafx.beans.property.BooleanProperty;

/**
 * Interface of a toggleable class object.
 */
interface Toggleable {
    /**
     * Returns the toggled-on property.
     *
     * @return the toggled-on property
     */
    BooleanProperty toggledOnProperty();

    /**
     * Returns a boolean indicating the toggle-state.
     *
     * @return true if currently toggled-on, false otherwise
     */
    boolean isToggledOn();

    /**
     * Sets the toggle-state.
     *
     * @param toggledOn true sets the toggle-state to on, otherwise to off
     */
    void setToggledOn(boolean toggledOn);
}
