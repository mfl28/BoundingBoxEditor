package boundingboxeditor.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.shape.Polygon;


public class TogglePolygon extends Polygon {
    private static final String TOGGLED_ON_PSEUDO_CLASS_NAME = "toggled-on";
    private static final String TOGGLE_SQUARE_ID = "toggle-polygon";
    private static final PseudoClass TOGGLED_ON_PSEUDO_CLASS = PseudoClass.getPseudoClass(TOGGLED_ON_PSEUDO_CLASS_NAME);

    private final BooleanProperty toggledOn = createToggledOnProperty();

    /**
     * Constructs a new toggleable square UI-element which has a toggled-on CSS-pseudo-state that can be used
     * to alter its appearance based on the current value of a toggledOn-property.
     *
     * @param sideLength the side-length of the square
     */
    TogglePolygon(double sideLength) {
        super(
                sideLength * 0.5, 0,
                0, sideLength * 0.4,
                0.2 * sideLength, sideLength,
                0.8 * sideLength, sideLength,
                sideLength, sideLength * 0.4
        );

        setId(TOGGLE_SQUARE_ID);
        pseudoClassStateChanged(TOGGLED_ON_PSEUDO_CLASS, true);
    }

    /**
     * Returns the toggled-on property.
     *
     * @return the toggled-on property
     */
    BooleanProperty toggledOnProperty() {
        return toggledOn;
    }

    /**
     * Returns a boolean indicating the toggle-state of the square.
     *
     * @return true if the square is currently toggled-on, false otherwise
     */
    boolean isToggledOn() {
        return toggledOn.get();
    }

    /**
     * Sets the toggle-state of the square.
     *
     * @param toggledOn true sets the toggle-state to on, otherwise to off
     */
    void setToggledOn(boolean toggledOn) {
        this.toggledOn.set(toggledOn);
    }

    private BooleanProperty createToggledOnProperty() {
        return new BooleanPropertyBase(true) {
            @Override
            public Object getBean() {
                return TogglePolygon.this;
            }

            @Override
            public String getName() {
                return TOGGLED_ON_PSEUDO_CLASS_NAME;
            }

            @Override
            protected void invalidated() {
                pseudoClassStateChanged(TOGGLED_ON_PSEUDO_CLASS, get());
            }
        };
    }
}
