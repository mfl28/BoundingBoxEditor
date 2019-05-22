package BoundingboxEditor.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class StatusPanelView extends HBox implements View {
    private static final String BOTTOM_BAR_STYLE = "status-panel";
    private static final String STATUS_PANEL_ID = "status-panel";

    private final Label timeStampLabel = new Label();
    private final Label eventMessageLabel = new Label();

    StatusPanelView() {
        getChildren().addAll(timeStampLabel, eventMessageLabel);
        getStyleClass().add(BOTTOM_BAR_STYLE);
        setId(STATUS_PANEL_ID);
        timeStampLabel.setId("event-time-stamp-label");
    }

    public void setStatus(String eventMessage) {
        timeStampLabel.setText(ZonedDateTime.now().toLocalTime().truncatedTo(ChronoUnit.MINUTES) + " - ");
        eventMessageLabel.setText(eventMessage);
    }

}
