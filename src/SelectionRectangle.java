import javafx.beans.property.DoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class SelectionRectangle extends Rectangle {

    private enum CompassPoint {NW, N, NE, E, SE, S, SW, W}

    private List<ResizeHandle> resizeHandles;

    private DragAnchor dragAnchor = new DragAnchor();

    SelectionRectangle() {
        super();
        this.getStyleClass().add("selectionRectangle");
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
            dragAnchor.setFromMouseEvent(event);
            event.consume();
        });

        this.setOnMouseDragged(event -> {
            this.setX(this.getX() + event.getX() - dragAnchor.getX());
            this.setY(this.getY() + event.getY() - dragAnchor.getY());
            dragAnchor.setFromMouseEvent(event);
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
        DragAnchor dragAnchor = new DragAnchor();

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
            visibleProperty().bind(rectangle.visibleProperty());

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
                        double offsetX = event.getX() - dragAnchor.getX();
                        double offsetY = event.getY() - dragAnchor.getY();
                        double newX = rectangle.getX() + offsetX;
                        double newY = rectangle.getY() + offsetY;
                        double newW = rectangle.getWidth() - offsetX;
                        double newH = rectangle.getHeight() - offsetY;

                        if (newW >= 0) {
                            rectangle.setX(newX);
                            rectangle.setWidth(newW);
                        }

                        if (newH >= 0) {
                            rectangle.setY(newY);
                            rectangle.setHeight(newH);
                        }
                        dragAnchor.setFromMouseEvent(event);
                        event.consume();
                    });
                    break;
                case N:
                    setOnMouseDragged(event -> {
                        double offsetY = event.getY() - dragAnchor.getY();
                        double newY = rectangle.getY() + offsetY;
                        double newH = rectangle.getHeight() - offsetY;

                        if (newH >= 0) {
                            rectangle.setY(newY);
                            rectangle.setHeight(newH);
                        }
                        dragAnchor.setFromMouseEvent(event);
                        event.consume();
                    });
                    break;
                case NE:
                    setOnMouseDragged(event -> {
                        double offsetX = event.getX() - dragAnchor.getX();
                        double offsetY = event.getY() - dragAnchor.getY();
                        double newY = rectangle.getY() + offsetY;
                        double newW = rectangle.getWidth() + offsetX;
                        double newH = rectangle.getHeight() - offsetY;

                        if (newW >= 0)
                            rectangle.setWidth(newW);

                        if (newH >= 0) {
                            rectangle.setY(newY);
                            rectangle.setHeight(newH);
                        }
                        dragAnchor.setFromMouseEvent(event);
                        event.consume();
                    });
                    break;
                case E:
                    setOnMouseDragged(event -> {
                        double offsetX = event.getX() - dragAnchor.getX();
                        double newW = rectangle.getWidth() + offsetX;

                        if (newW >= 0)
                            rectangle.setWidth(newW);

                        dragAnchor.setFromMouseEvent(event);
                        event.consume();
                    });
                    break;
                case SE:
                    setOnMouseDragged(event -> {
                        double offsetX = event.getX() - dragAnchor.getX();
                        double offsetY = event.getY() - dragAnchor.getY();
                        double newW = rectangle.getWidth() + offsetX;
                        double newH = rectangle.getHeight() + offsetY;

                        if (newW >= 0)
                            rectangle.setWidth(newW);

                        if (newH >= 0)
                            rectangle.setHeight(newH);

                        dragAnchor.setFromMouseEvent(event);
                        event.consume();
                    });
                    break;
                case S:
                    setOnMouseDragged(event -> {
                        double offsetY = event.getY() - dragAnchor.getY();
                        double newH = rectangle.getHeight() + offsetY;

                        if (newH >= 0)
                            rectangle.setHeight(newH);

                        dragAnchor.setFromMouseEvent(event);
                        event.consume();
                    });
                    break;
                case SW:
                    setOnMouseDragged(event -> {
                        double offsetX = event.getX() - dragAnchor.getX();
                        double offsetY = event.getY() - dragAnchor.getY();
                        double newX = rectangle.getX() + offsetX;
                        double newW = rectangle.getWidth() - offsetX;
                        double newH = rectangle.getHeight() + offsetY;

                        if (newW >= 0) {
                            rectangle.setX(newX);
                            rectangle.setWidth(newW);
                        }

                        if (newH >= 0)
                            rectangle.setHeight(newH);

                        dragAnchor.setFromMouseEvent(event);
                        event.consume();
                    });
                    break;
                case W:
                    setOnMouseDragged(event -> {
                        double offsetX = event.getX() - dragAnchor.getX();
                        double newX = rectangle.getX() + offsetX;
                        double newW = rectangle.getWidth() - offsetX;

                        if (newW >= 0) {
                            rectangle.setX(newX);
                            rectangle.setWidth(newW);
                        }

                        dragAnchor.setFromMouseEvent(event);
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

    public List<Double> getBoundingBox() {
        List<Double> boundingBoxData = new ArrayList<>();
        // Wrong
        boundingBoxData.add(this.getX());
        boundingBoxData.add(this.getY());
        boundingBoxData.add(this.getX() + this.getWidth());
        boundingBoxData.add(this.getY() + this.getHeight());

        return boundingBoxData;
    }
}
