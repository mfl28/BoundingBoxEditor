package BoundingboxEditor.ui;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;


class ToggleIconButton extends ToggleButton {
    private static final String TOGGLE_ICON_BUTTON_STYLE = "toggle-icon-button";

    ToggleIconButton(String cssId) {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setFocusTraversable(false);
        setPickOnBounds(true);
        getStyleClass().add(TOGGLE_ICON_BUTTON_STYLE);
        setId(cssId);
    }
}
