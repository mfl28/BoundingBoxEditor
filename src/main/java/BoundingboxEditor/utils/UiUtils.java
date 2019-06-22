package BoundingboxEditor.utils;

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
}
