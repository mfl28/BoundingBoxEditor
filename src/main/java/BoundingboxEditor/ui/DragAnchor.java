package BoundingboxEditor.ui;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

public class DragAnchor {
    private double x, y;

    public DragAnchor() {
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
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
