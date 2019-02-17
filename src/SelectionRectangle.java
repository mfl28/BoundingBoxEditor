import javafx.beans.property.*;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectionRectangle extends Rectangle {

    private enum CompassPoint {NW, N, NE, E, SE, S, SW, W}

    private List<ResizeHandle> resizeHandles;

    private DragAnchor dragAnchor = new DragAnchor();

    private Property<Bounds> confinementBounds;
    private BooleanProperty selected;


    SelectionRectangle() {
        super();
        this.getStyleClass().add("selectionRectangle");
        selected = new SimpleBooleanProperty(true);
        setVisible(false);
        createResizeHandles();
        addMoveFunctionality();
    }

    private void createResizeHandles() {
        resizeHandles = new ArrayList<>();

        for (CompassPoint compass_point : CompassPoint.values()) {
            resizeHandles.add(new ResizeHandle(compass_point));
        }
    }

    private void addMoveFunctionality() {
        this.setOnMouseEntered(event -> this.setCursor(Cursor.MOVE));

        this.setOnMousePressed(event -> {
            Point2D eventXY = new Point2D(event.getX(), event.getY());

            dragAnchor.setFromPoint2D(eventXY.subtract(this.getX(), this.getY()));
            event.consume();
        });

        this.setOnMouseDragged(event -> {
            // add boolean "dragFromOutside" detector
            Point2D eventXY = new Point2D(event.getX(), event.getY());

//            if (!this.getBoundsInParent().contains(eventXY)) {
//                dragAnchor.setFromPoint2D(new Point2D(0.5 * this.getWidth(),
//                        0.5 * this.getHeight()));
//                return;
//            }

            Point2D newXY = eventXY.subtract(dragAnchor.getX(), dragAnchor.getY());
            Bounds regionBounds = confinementBounds.getValue();
            Bounds moveBounds = new BoundingBox(regionBounds.getMinX(), regionBounds.getMinY(),
                    regionBounds.getWidth() - this.getWidth(), regionBounds.getHeight() - this.getHeight());
            Point2D newConfinedXY = Utils.clampWithinBounds(newXY, moveBounds);

            this.setX(newConfinedXY.getX());
            this.setY(newConfinedXY.getY());
            event.consume();
        });
    }

    List<Node> getNodes() {
        this.setManaged(false);
        for (Rectangle rect : resizeHandles)
            rect.setManaged(false);

        ArrayList<Node> nodeList = new ArrayList<>();
        nodeList.add(this);
        nodeList.addAll(resizeHandles);

        return nodeList;
    }

    private class ResizeHandle extends Rectangle {

        private static final double SIDE_LENGTH = 8.0;
        private final CompassPoint compassPoint;
        private DragAnchor dragAnchor = new DragAnchor();

        ResizeHandle(CompassPoint p) {
            super(SIDE_LENGTH, SIDE_LENGTH);
            compassPoint = p;
            bindToParentRectangle();
            addResizeFunctionality();
        }

        private void bindToParentRectangle() {
            SelectionRectangle rectangle = SelectionRectangle.this;
            DoubleProperty rectangle_x = rectangle.xProperty();
            DoubleProperty rectangle_y = rectangle.yProperty();
            DoubleProperty rectangle_w = rectangle.widthProperty();
            DoubleProperty rectangle_h = rectangle.heightProperty();

            fillProperty().bind(rectangle.strokeProperty());
            visibleProperty().bind(rectangle.selectedProperty());

            switch (compassPoint) {
                case NW:
                    xProperty().bind(rectangle_x.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangle_y.subtract(SIDE_LENGTH / 2));
                    break;
                case N:
                    xProperty().bind(rectangle_x.add(rectangle_w.subtract(SIDE_LENGTH).divide(2)));
                    yProperty().bind(rectangle_y.subtract(SIDE_LENGTH / 2));
                    break;
                case NE:
                    xProperty().bind(rectangle_x.add(rectangle_w).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangle_y.subtract(SIDE_LENGTH / 2));
                    break;
                case E:
                    xProperty().bind(rectangle_x.add(rectangle_w).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangle_y.add(rectangle_h.subtract(SIDE_LENGTH).divide(2)));
                    break;
                case SE:
                    xProperty().bind(rectangle_x.add(rectangle_w).subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangle_y.add(rectangle_h).subtract(SIDE_LENGTH / 2));
                    break;
                case S:
                    xProperty().bind(rectangle_x.add(rectangle_w.subtract(SIDE_LENGTH).divide(2)));
                    yProperty().bind(rectangle_y.add(rectangle_h).subtract(SIDE_LENGTH / 2));
                    break;
                case SW:
                    xProperty().bind(rectangle_x.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangle_y.add(rectangle_h).subtract(SIDE_LENGTH / 2));
                    break;
                case W:
                    xProperty().bind(rectangle_x.subtract(SIDE_LENGTH / 2));
                    yProperty().bind(rectangle_y.add(rectangle_h.subtract(SIDE_LENGTH).divide(2)));
            }
        }

        private void addResizeFunctionality() {
            setOnMouseEntered(event -> {
                setCursor(Cursor.cursor(compassPoint.toString() + "_RESIZE"));
                event.consume();
            });

            setOnMousePressed(event -> {
                dragAnchor.setFromMouseEvent(event);
                event.consume();
            });

            SelectionRectangle rectangle = SelectionRectangle.this;

            switch (compassPoint) {
                case NW:
                    setOnMouseDragged(event -> {
                        Bounds parentBounds = rectangle.confinementBounds.getValue();
                        Bounds bounds = new BoundingBox(parentBounds.getMinX(), parentBounds.getMinY(),
                                rectangle.getMaxX() - parentBounds.getMinX(),
                                rectangle.getMaxY() - parentBounds.getMinY());

                        Point2D eventXY = new Point2D(event.getX(), event.getY());
                        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                        rectangle.setX(clampedEventXY.getX());
                        rectangle.setY(clampedEventXY.getY());
                        rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
                        rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));

                        event.consume();
                    });
                    break;
                case N:
                    setOnMouseDragged(event -> {
                        Bounds parentBounds = rectangle.confinementBounds.getValue();
                        Bounds bounds = new BoundingBox(rectangle.getX(), parentBounds.getMinY(),
                                rectangle.getWidth(), rectangle.getMaxY() - parentBounds.getMinY());

                        Point2D eventXY = new Point2D(event.getX(), event.getY());
                        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                        rectangle.setY(clampedEventXY.getY());
                        rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));

                        event.consume();
                    });
                    break;
                case NE:
                    setOnMouseDragged(event -> {
                        Bounds parentBounds = rectangle.confinementBounds.getValue();
                        Bounds bounds = new BoundingBox(rectangle.getX(), parentBounds.getMinY(),
                                parentBounds.getMaxX() - rectangle.getX(),
                                rectangle.getMaxY() - parentBounds.getMinY());

                        Point2D eventXY = new Point2D(event.getX(), event.getY());
                        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                        rectangle.setY(clampedEventXY.getY());
                        rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                        rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));

                        event.consume();
                    });
                    break;
                case E:
                    setOnMouseDragged(event -> {
                        Bounds parentBounds = rectangle.confinementBounds.getValue();
                        Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                parentBounds.getMaxX() - rectangle.getX(), rectangle.getHeight());
                        Point2D eventXY = new Point2D(event.getX(), event.getY());
                        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                        rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));

                        event.consume();
                    });
                    break;
                case SE:
                    setOnMouseDragged(event -> {
                        Bounds parentBounds = rectangle.confinementBounds.getValue();
                        Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                parentBounds.getMaxX() - rectangle.getX(),
                                parentBounds.getMaxY() - rectangle.getY());

                        Point2D eventXY = new Point2D(event.getX(), event.getY());
                        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                        rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                        rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));

                        event.consume();
                    });
                    break;
                case S:
                    setOnMouseDragged(event -> {
                        Bounds parentBounds = rectangle.confinementBounds.getValue();
                        Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                rectangle.getWidth(),
                                parentBounds.getMaxY() - rectangle.getY());

                        Point2D eventXY = new Point2D(event.getX(), event.getY());
                        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                        rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));

                        event.consume();
                    });
                    break;
                case SW:
                    setOnMouseDragged(event -> {
                        Bounds parentBounds = rectangle.confinementBounds.getValue();
                        Bounds bounds = new BoundingBox(parentBounds.getMinX(), rectangle.getY(),
                                rectangle.getMaxX() - parentBounds.getMinX(),
                                parentBounds.getMaxY() - rectangle.getY());

                        Point2D eventXY = new Point2D(event.getX(), event.getY());
                        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                        rectangle.setX(clampedEventXY.getX());
                        rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
                        rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));

                        event.consume();
                    });
                    break;
                case W:
                    setOnMouseDragged(event -> {
                        Bounds parentBounds = rectangle.confinementBounds.getValue();
                        Bounds bounds = new BoundingBox(parentBounds.getMinX(), rectangle.getY(),
                                rectangle.getMaxX() - parentBounds.getMinX(),
                                rectangle.getHeight());

                        Point2D eventXY = new Point2D(event.getX(), event.getY());
                        Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                        rectangle.setX(clampedEventXY.getX());
                        rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));

                        event.consume();
                    });
                    break;

            }
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    private double getMaxX(){
        return this.getX() + this.getWidth();
    }

    private double getMaxY(){
        return this.getY() + this.getHeight();
    }

    public List<Double> getBoundingBoxCoordinates() {
        List<Double> boundingBoxData = new ArrayList<>();
        boundingBoxData.add(this.getX());
        boundingBoxData.add(this.getY());
        boundingBoxData.add(this.getX() + this.getWidth());
        boundingBoxData.add(this.getY() + this.getHeight());

        return boundingBoxData;
    }

    public List<Double> getScaledLocalCoordinatesInSiblingImage(ImageView sibling) {
        Bounds localBounds = sibling.parentToLocal(this.getBoundsInParent());
        Image image = sibling.getImage();
        double scaleWidth = image.getWidth();
        double scaleHeight = image.getHeight();
        double topLeftX = Utils.clamp(localBounds.getMinX() * scaleWidth / localBounds.getWidth(), 0, scaleWidth);
        double topLeftY = Utils.clamp(localBounds.getMinY() * scaleHeight / localBounds.getHeight(), 0, scaleHeight);
        double bottomRightX = Utils.clamp(localBounds.getMaxX() * scaleWidth / localBounds.getWidth(), 0, scaleWidth);
        double bottomRightY = Utils.clamp(localBounds.getMaxY() * scaleHeight / localBounds.getHeight(), 0, scaleHeight);

        return Arrays.asList(topLeftX, topLeftY, bottomRightX, bottomRightY);
    }

    public void confineTo(ReadOnlyObjectProperty<Bounds> confineTo){
        confinementBounds = new SimpleObjectProperty<>();
        confinementBounds.bind(confineTo);
    }

    // Testing
    public void showBBData() {
        System.out.println(Arrays.asList(getX(), getY(), getX() + getWidth(), getY() + getHeight()));
    }

    public void showConfinement(){
        System.out.println(confinementBounds.getValue());
    }

    public DragAnchor getDragAnchor() {
        return dragAnchor;
    }

    public BooleanProperty selectedProperty(){
        return selected;
    }
}
