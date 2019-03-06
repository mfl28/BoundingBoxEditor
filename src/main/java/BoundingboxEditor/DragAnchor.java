package BoundingboxEditor;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

public class DragAnchor {
    private double x, y;

    public DragAnchor() {
    }

    public DragAnchor(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static DragAnchor createFromMouseEvent(MouseEvent event) {
        return new DragAnchor(event.getX(), event.getY());
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setFromMouseEvent(MouseEvent event) {
        x = event.getX();
        y = event.getY();
    }

    public void setFromPoint2D(Point2D point) {
        x = point.getX();
        y = point.getY();
    }
}
