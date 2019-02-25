import javafx.beans.property.*;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectionRectangle extends Rectangle {

    private static final String SELECTION_RECTANGLE_STYLE = "selectionRectangle";

    private enum CompassPoint {NW, N, NE, E, SE, S, SW, W}

    private final List<ResizeHandle> resizeHandles;
    private final DragAnchor dragAnchor = new DragAnchor();

    private final BooleanProperty selected = new SimpleBooleanProperty(true);
    private final Property<Bounds> confinementBounds = new SimpleObjectProperty<>();
    private BoundingBoxCategory boundingBoxCategory;

    public SelectionRectangle(BoundingBoxCategory category) {
        this.getStyleClass().add(SELECTION_RECTANGLE_STYLE);
        boundingBoxCategory = category;
        setVisible(false);

        resizeHandles = createResizeHandles();
        addMoveFunctionality();
    }

    public BoundingBoxCategory getBoundingBoxCategory() {
        return boundingBoxCategory;
    }

    public void setBoundingBoxCategory(final BoundingBoxCategory item) {
        this.boundingBoxCategory = item;
    }

    public List<Double> getScaledLocalCoordinatesInSiblingImage(ImageView sibling) {
        final Bounds localBounds = sibling.parentToLocal(this.getBoundsInParent());
        final Image image = sibling.getImage();
        double scaleWidth = image.getWidth();
        double scaleHeight = image.getHeight();
        double topLeftX = Utils.clamp(localBounds.getMinX() * scaleWidth / localBounds.getWidth(),
                0, scaleWidth);
        double topLeftY = Utils.clamp(localBounds.getMinY() * scaleHeight / localBounds.getHeight(),
                0, scaleHeight);
        double bottomRightX = Utils.clamp(localBounds.getMaxX() * scaleWidth / localBounds.getWidth(),
                0, scaleWidth);
        double bottomRightY = Utils.clamp(localBounds.getMaxY() * scaleHeight / localBounds.getHeight(),
                0, scaleHeight);

        return Arrays.asList(topLeftX, topLeftY, bottomRightX, bottomRightY);
    }

    public void confineTo(final ReadOnlyObjectProperty<Bounds> bounds) {
        confinementBounds.bind(bounds);

        bounds.addListener((observable, oldValue, newValue) -> {
            this.setWidth(this.getWidth() * newValue.getWidth() / oldValue.getWidth());
            this.setHeight(this.getHeight() * newValue.getHeight() / oldValue.getHeight());

            this.setX(newValue.getMinX() + (this.getX()
                    - oldValue.getMinX()) * newValue.getWidth() / oldValue.getWidth());
            this.setY(newValue.getMinY() + (this.getY()
                    - oldValue.getMinY()) * newValue.getHeight() / oldValue.getHeight());
        });
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setXYWH(double x, double y, double w, double h) {
        this.setX(x);
        this.setY(y);
        this.setWidth(w);
        this.setHeight(h);
    }

    public List<Node> getNodes() {
        this.setManaged(false);
        for (Rectangle rect : resizeHandles)
            rect.setManaged(false);

        ArrayList<Node> nodeList = new ArrayList<>();
        nodeList.add(this);
        nodeList.addAll(resizeHandles);

        return nodeList;
    }

    // Testing
    public void showBBData() {
        System.out.println(Arrays.asList(getX(), getY(), getX() + getWidth(), getY() + getHeight()));
    }

    // Testing
    public void showConfinement() {
        System.out.println(confinementBounds.getValue());
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    private double getMaxX() {
        return this.getX() + this.getWidth();
    }

    private double getMaxY() {
        return this.getY() + this.getHeight();
    }

    private List<ResizeHandle> createResizeHandles() {
        final List<ResizeHandle> resizeHandlesList = new ArrayList<>();

        for (CompassPoint compass_point : CompassPoint.values()) {
            resizeHandlesList.add(new ResizeHandle(compass_point));
        }

        return resizeHandlesList;
    }

    private void addMoveFunctionality() {
        this.setOnMouseEntered(event -> {
            this.setCursor(Cursor.MOVE);
            event.consume();
        });

        this.setOnMousePressed(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                dragAnchor.setFromPoint2D(eventXY.subtract(this.getX(), this.getY()));
            }
            event.consume();
        });

        this.setOnMouseDragged(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                final Point2D newXY = eventXY.subtract(dragAnchor.getX(), dragAnchor.getY());
                final Bounds regionBounds = confinementBounds.getValue();
                final Bounds moveBounds = new BoundingBox(regionBounds.getMinX(), regionBounds.getMinY(),
                        regionBounds.getWidth() - this.getWidth(),
                        regionBounds.getHeight() - this.getHeight());
                final Point2D newConfinedXY = Utils.clampWithinBounds(newXY, moveBounds);

                this.setX(newConfinedXY.getX());
                this.setY(newConfinedXY.getY());
            }
            event.consume();
        });
    }

    private class ResizeHandle extends Rectangle {

        private static final double SIDE_LENGTH = 8.0;
        private final CompassPoint compassPoint;
        private final DragAnchor dragAnchor = new DragAnchor();

        ResizeHandle(CompassPoint compassPoint) {
            super(SIDE_LENGTH, SIDE_LENGTH);
            this.compassPoint = compassPoint;
            bindToParentRectangle();
            addResizeFunctionality();
        }

        private void bindToParentRectangle() {
            final SelectionRectangle rectangle = SelectionRectangle.this;
            final DoubleProperty rectangle_x = rectangle.xProperty();
            final DoubleProperty rectangle_y = rectangle.yProperty();
            final DoubleProperty rectangle_w = rectangle.widthProperty();
            final DoubleProperty rectangle_h = rectangle.heightProperty();

            fillProperty().bind(rectangle.strokeProperty());
            visibleProperty().bind(rectangle.selectedProperty().and(rectangle.visibleProperty()));

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
                if (event.getButton().equals(MouseButton.PRIMARY)) {
                    dragAnchor.setFromMouseEvent(event);
                }
                event.consume();
            });

            final SelectionRectangle rectangle = SelectionRectangle.this;

            switch (compassPoint) {
                case NW:
                    setOnMouseDragged(event -> {
                        if (event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(parentBounds.getMinX(), parentBounds.getMinY(),
                                    rectangle.getMaxX() - parentBounds.getMinX(),
                                    rectangle.getMaxY() - parentBounds.getMinY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                            rectangle.setX(clampedEventXY.getX());
                            rectangle.setY(clampedEventXY.getY());
                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
                        }

                        event.consume();
                    });
                    break;
                case N:
                    setOnMouseDragged(event -> {
                        if (event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), parentBounds.getMinY(),
                                    rectangle.getWidth(), rectangle.getMaxY() - parentBounds.getMinY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                            rectangle.setY(clampedEventXY.getY());
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
                        }

                        event.consume();
                    });
                    break;
                case NE:
                    setOnMouseDragged(event -> {
                        if (event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), parentBounds.getMinY(),
                                    parentBounds.getMaxX() - rectangle.getX(),
                                    rectangle.getMaxY() - parentBounds.getMinY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                            rectangle.setY(clampedEventXY.getY());
                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
                        }

                        event.consume();
                    });
                    break;
                case E:
                    setOnMouseDragged(event -> {
                        if (event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                    parentBounds.getMaxX() - rectangle.getX(), rectangle.getHeight());
                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                        }

                        event.consume();
                    });
                    break;
                case SE:
                    setOnMouseDragged(event -> {
                        if (event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                    parentBounds.getMaxX() - rectangle.getX(),
                                    parentBounds.getMaxY() - rectangle.getY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
                        }

                        event.consume();
                    });
                    break;
                case S:
                    setOnMouseDragged(event -> {
                        if (event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                    rectangle.getWidth(),
                                    parentBounds.getMaxY() - rectangle.getY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
                        }

                        event.consume();
                    });
                    break;
                case SW:
                    setOnMouseDragged(event -> {
                        if (event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(parentBounds.getMinX(), rectangle.getY(),
                                    rectangle.getMaxX() - parentBounds.getMinX(),
                                    parentBounds.getMaxY() - rectangle.getY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                            rectangle.setX(clampedEventXY.getX());
                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
                        }

                        event.consume();
                    });
                    break;
                case W:
                    setOnMouseDragged(event -> {
                        if (event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(parentBounds.getMinX(), rectangle.getY(),
                                    rectangle.getMaxX() - parentBounds.getMinX(),
                                    rectangle.getHeight());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = Utils.clampWithinBounds(eventXY, bounds);

                            rectangle.setX(clampedEventXY.getX());
                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
                        }

                        event.consume();
                    });
                    break;
            }
        }
    }
}
