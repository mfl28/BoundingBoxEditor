package BoundingboxEditor.ui;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

class DragAnchor {
    private double x, y;

    DragAnchor() {
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    void setFromMouseEvent(MouseEvent event) {
        x = event.getX();
        y = event.getY();
    }

    void setFromPoint2D(Point2D point) {
        x = point.getX();
        y = point.getY();
    }
}
