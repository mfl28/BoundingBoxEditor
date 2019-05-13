package BoundingboxEditor.ui;

import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;


class ToggleIconButton extends ToggleButton {
    private static final double ICON_WIDTH = 15.0;
    private static final double ICON_HEIGHT = 15.0;
    private static final String TOGGLE_ICON_BUTTON_ID = "toggle-icon-button";

    private final ImageView toggledOnIcon;
    private final ImageView toggledOffIcon;

    ToggleIconButton(String toggledOnIconPath, String toggledOffIconPath) {
        toggledOnIcon = new ImageView(getClass().getResource(toggledOnIconPath).toExternalForm());
        toggledOffIcon = new ImageView(getClass().getResource(toggledOffIconPath).toExternalForm());

        toggledOnIcon.setFitWidth(ICON_WIDTH);
        toggledOnIcon.setFitHeight(ICON_HEIGHT);
        toggledOnIcon.setPreserveRatio(true);

        toggledOffIcon.setFitWidth(ICON_WIDTH);
        toggledOffIcon.setFitHeight(ICON_HEIGHT);
        toggledOffIcon.setPreserveRatio(true);

        setFocusTraversable(false);
        setGraphic(toggledOffIcon);
        setUpInternalListeners();
        setId(TOGGLE_ICON_BUTTON_ID);
    }

    private void setUpInternalListeners() {
        selectedProperty().addListener((observable, oldValue, newValue) ->
                setGraphic(newValue ? toggledOnIcon : toggledOffIcon));
    }

}
