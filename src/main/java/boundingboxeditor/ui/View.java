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

import boundingboxeditor.controller.Controller;

/**
 * The minimal interface of all view-components of the app. Views can
 * be connected to a {@link Controller} to register handling of events
 * which need interaction with the model-component of the app.
 */
public interface View {
    /**
     * Connects the view to a controller. By overriding this method, views can
     * register event-handler functions (implemented in a {@link Controller} object)
     * which interact with the model-data.
     *
     * @param controller the controller serving as the even-handler
     */
    default void connectToController(Controller controller) {
    }

    /**
     * Resets the View. Classes implementing the View interface can optionally
     * implement this method to reset their state/data.
     */
    default void reset() {
    }
}
