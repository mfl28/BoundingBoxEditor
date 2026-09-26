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
package com.github.mfl28.boundingboxeditor.ui.statusevents;

/**
 * A status event signifying that a bounding shape was copied or pasted.
 */
public class BoundingShapeClipboardEvent extends StatusEvent {
    private BoundingShapeClipboardEvent(String eventMessage) {
        super(eventMessage);
    }

    /**
     * Creates the event of a copied shape.
     *
     * @param categoryName the category of the shape
     * @return the event
     */
    public static BoundingShapeClipboardEvent copied(String categoryName) {
        return new BoundingShapeClipboardEvent("Copied the " + categoryName + " shape.");
    }

    /**
     * Creates the event of a pasted shape.
     *
     * @param categoryName the category of the shape
     * @return the event
     */
    public static BoundingShapeClipboardEvent pasted(String categoryName) {
        return new BoundingShapeClipboardEvent("Pasted the " + categoryName + " shape.");
    }
}
