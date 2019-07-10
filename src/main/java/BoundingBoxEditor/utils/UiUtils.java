package BoundingBoxEditor.utils;

import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

/***
 * A class that comprises general ui utility-functions.
 */
public class UiUtils {
    /***
     * Creates a pane that fills any available horizontal space
     * in it's parent.
     * @return pane
     */
    public static Pane createHSpacer() {
        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Creates a Tooltip.
     *
     * @param text the text content of the tooltip
     * @return the tooltip
     */
    public static Tooltip createTooltip(String text) {
        return new Tooltip(text);
    }

    /**
     * Creates a Tooltip with a key combination.
     *
     * @param text           the text content of the tooltip
     * @param keyCombination a key combination which will be displayed besides the text
     * @return the tooltip
     */
    public static Tooltip createTooltip(String text, KeyCombination keyCombination) {
        return new Tooltip((text.isEmpty() ? "" : (text + " "))
                + "(" + keyCombination.getDisplayText() + ")");
    }

    /**
     * Creates a Tooltip showing the key combination to focus the node it is installed on.
     *
     * @param keyCombination a key combination which will be displayed besides the text
     * @return the tooltip
     */
    public static Tooltip createFocusTooltip(KeyCombination keyCombination) {
        return new Tooltip("(" + keyCombination.getDisplayText() + " to focus)");
    }
}
