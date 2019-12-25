package boundingboxeditor.ui;

import boundingboxeditor.ui.statusevents.StatusEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A UI-element used to display information from {@link StatusEvent}s, as well as the time when the event happened.
 */
public class StatusBarView extends HBox implements View {
    private static final String STATUS_PANEL_ID = "status-panel";
    private static final String EVENT_TIME_STAMP_LABEL_ID = "event-time-stamp-label";

    private final Label timeStampLabel = new Label();
    private final Label eventMessageLabel = new Label();

    /**
     * Creates a new status-bar UI-element used to display information from
     * {@link StatusEvent}s, as well as the time when the event happened.
     */
    StatusBarView() {
        getChildren().addAll(timeStampLabel, eventMessageLabel);
        setId(STATUS_PANEL_ID);
        timeStampLabel.setId(EVENT_TIME_STAMP_LABEL_ID);
    }

    /**
     * Sets the {@link StatusEvent} object whose information to display.
     *
     * @param statusEvent the status-event
     */
    public void setStatusEvent(StatusEvent statusEvent) {
        timeStampLabel.setText(ZonedDateTime.now(ZoneId.systemDefault()).toLocalTime().truncatedTo(ChronoUnit.MINUTES) + " - ");
        eventMessageLabel.setText(statusEvent.getEventMessage());
    }
}
