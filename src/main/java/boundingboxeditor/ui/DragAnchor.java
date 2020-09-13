package boundingboxeditor.ui;

import javafx.scene.input.MouseEvent;

/**
 * A class used to save a starting point of a mouse-drag-event.
 */
public class DragAnchor {
    private double x = 0.0;
    private double y = 0.0;

    /**
     * Creates a new drag-anchor.
     */
    DragAnchor() {
    }

    /**
     * Convenience method to set both coordinates from
     * a {@link MouseEvent} object's position relative to the event's source.
     *
     * @param event The mouse event.
     */
    public void setFromMouseEvent(MouseEvent event) {
        x = event.getX();
        y = event.getY();
    }

    /**
     * Returns the x-coordinate.
     *
     * @return the x-coordinate
     */
    double getX() {
        return x;
    }

    /**
     * Returns the y-coordinate.
     *
     * @return the y-coordinate
     */
    double getY() {
        return y;
    }

    /**
     * Convenience method to set both coordinates.
     *
     * @param x the value for the x-coordinate
     * @param y the value for the y-coordinate
     */
    void setCoordinates(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
