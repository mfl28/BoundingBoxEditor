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

import javafx.scene.input.MouseEvent;

/**
 * A class used to save a starting point of a mouse-drag-event.
 */
public class DragAnchor {
    private double x = 0.0;
    private double y = 0.0;

    /**
     * Creates a new drag-anchor.
     */
    DragAnchor() {
    }

    /**
     * Convenience method to set both coordinates from
     * a {@link MouseEvent} object's position relative to the event's source.
     *
     * @param event The mouse event.
     */
    public void setFromMouseEvent(MouseEvent event) {
        x = event.getX();
        y = event.getY();
    }

    /**
     * Returns the x-coordinate.
     *
     * @return the x-coordinate
     */
    double getX() {
        return x;
    }

    /**
     * Returns the y-coordinate.
     *
     * @return the y-coordinate
     */
    double getY() {
        return y;
    }

    /**
     * Convenience method to set both coordinates.
     *
     * @param x the value for the x-coordinate
     * @param y the value for the y-coordinate
     */
    void setCoordinates(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
