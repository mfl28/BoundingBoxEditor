package boundingboxeditor.ui;

import javafx.beans.property.BooleanProperty;

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
