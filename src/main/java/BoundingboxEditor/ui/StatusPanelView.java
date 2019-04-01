package BoundingboxEditor.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

class StatusPanelView extends HBox implements View {
    private static final String BOTTOM_BAR_STYLE = "status-panel";
    private static final String STATUS_PANEL_ID = "status-panel";

    private final Label bottomLabel = new Label();

    StatusPanelView() {
        getChildren().add(bottomLabel);
        getStyleClass().add(BOTTOM_BAR_STYLE);
        setId(STATUS_PANEL_ID);
    }

    Label getBottomLabel() {
        return bottomLabel;
    }
}
