package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;

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
