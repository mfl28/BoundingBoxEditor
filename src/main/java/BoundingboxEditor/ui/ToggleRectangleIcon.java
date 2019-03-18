package BoundingboxEditor.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.shape.Rectangle;

class ToggleRectangleIcon extends Rectangle {
    private static final double TOGGLED_ON_OPACITY = 1.0;
    private static final double TOGGLED_OFF_OPACITY = 0.3;

    private final BooleanProperty toggledOn = new SimpleBooleanProperty(true);

    ToggleRectangleIcon(double width, double height) {
        super(0, 0, width, height);

        setUpInternalListeners();
    }

    BooleanProperty toggledOnProperty() {
        return toggledOn;
    }

    boolean isToggledOn() {
        return toggledOn.get();
    }

    void setToggledOn(boolean toggledOn) {
        this.toggledOn.set(toggledOn);
    }

    private void setUpInternalListeners() {
        setOnMousePressed(event -> toggledOn.set(!toggledOn.get()));

        toggledOn.addListener(((observable, oldValue, newValue) ->
                setOpacity(newValue ? TOGGLED_ON_OPACITY : TOGGLED_OFF_OPACITY)));
    }
}
