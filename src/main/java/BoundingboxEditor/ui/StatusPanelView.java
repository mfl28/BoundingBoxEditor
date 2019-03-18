package BoundingboxEditor.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

class StatusPanelView extends HBox implements View {
    private static final String BOTTOM_BAR_STYLE = "status-panel";

    private final Label bottomLabel = new Label();

    StatusPanelView() {
        this.getChildren().add(bottomLabel);
        this.getStyleClass().add(BOTTOM_BAR_STYLE);
    }

    Label getBottomLabel() {
        return bottomLabel;
    }
}
