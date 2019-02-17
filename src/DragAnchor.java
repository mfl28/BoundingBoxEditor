import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

public class DragAnchor {
    private double x, y;

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setFromMouseEvent(MouseEvent event) {
        x = event.getX();
        y = event.getY();
    }

    public void setFromPoint2D(Point2D point){
        x = point.getX();
        y = point.getY();
    }
}
