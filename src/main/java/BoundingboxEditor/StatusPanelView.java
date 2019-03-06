package BoundingboxEditor;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class StatusPanelView extends HBox implements View {
    private static final String BOTTOM_BAR_STYLE = "bottom-bar";
    private final Label bottomLabel = new Label();

    StatusPanelView() {
        this.getChildren().add(bottomLabel);
        this.getStyleClass().add(BOTTOM_BAR_STYLE);
    }

    public Label getBottomLabel() {
        return bottomLabel;
    }
}
