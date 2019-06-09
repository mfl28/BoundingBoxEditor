package BoundingboxEditor.ui;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;

/**
 * A simple toggleable button.
 *
 * @see ToggleButton
 */
class ToggleIconButton extends ToggleButton {
    private static final String TOGGLE_ICON_BUTTON_STYLE = "toggle-icon-button";

    /**
     * Creates a new toggleable button with the provided CSS-id.
     *
     * @param cssId the CSS-id to register with this button
     */
    ToggleIconButton(String cssId) {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setFocusTraversable(false);
        setPickOnBounds(true);
        getStyleClass().add(TOGGLE_ICON_BUTTON_STYLE);
        setId(cssId);
    }
}
