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

    @Override
    public String toString() {
        return "DragAnchor " + "[x = " + getX() + ", y = " + getY() + "]";
    }

    /**
     * Convenience method to set both coordinates from
     * a {@link MouseEvent} object's position relative to the event's source.
     *
     * @param event
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
     * Sets the x-coordinate.
     *
     * @param x the value to set
     */
    void setX(double x) {
        this.x = x;
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
     * Sets the y-coordinate.
     *
     * @param y the value to set
     */
    void setY(double y) {
        this.y = y;
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
