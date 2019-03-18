package BoundingboxEditor.utils;

import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class UiUtils {
    public static Button createIconButton(final String iconPath, double fitWidth, double fitHeight) {
        final Button button = new Button();
        final ImageView iconView = new ImageView(iconPath);

        iconView.setFitWidth(fitWidth);
        iconView.setFitHeight(fitHeight);
        iconView.setPreserveRatio(true);
        button.setGraphic(iconView);
        button.setFocusTraversable(false);

        return button;
    }

    public static ToggleButton createToggleIconButton(final String iconPath, double fitWidth, double fitHeight) {
        final ToggleButton button = new ToggleButton();
        final ImageView iconView = new ImageView(iconPath);

        iconView.setFitWidth(fitWidth);
        iconView.setFitHeight(fitHeight);
        iconView.setPreserveRatio(true);
        button.setGraphic(iconView);
        button.setFocusTraversable(false);

        return button;
    }

    public static Pane createHSpacer() {
        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
