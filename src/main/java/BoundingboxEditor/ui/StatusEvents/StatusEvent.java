package BoundingboxEditor.ui.StatusEvents;

import BoundingboxEditor.ui.StatusBarView;

/**
 * Abstract base class that represents a status event that can be registered and displayed
 * in a {@link StatusBarView StatusBarView}.
 */
public abstract class StatusEvent {
    private String eventMessage;

    /**
     * Base class constructor
     *
     * @param eventMessage the message that should be displayed to describe the event
     */
    StatusEvent(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    /**
     * Returns the event's message.
     *
     * @return the event-message
     */
    public String getEventMessage() {
        return eventMessage;
    }

    @Override
    public String toString() {
        return "StatusEvent: " + eventMessage;
    }
}
