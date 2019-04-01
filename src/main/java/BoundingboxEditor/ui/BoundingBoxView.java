package BoundingboxEditor.ui;

import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.utils.MathUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BoundingBoxView extends Rectangle implements View {
    private static final String SELECTION_RECTANGLE_STYLE = "selectionRectangle";
    private static final double DEFAULT_FILL_OPACITY = 0.4;
    private static final BoundingBoxView NULL_BOUNDING_BOX_VIEW = new BoundingBoxView();

    private final DragAnchor dragAnchor = new DragAnchor();
    private final Property<Bounds> confinementBounds = new SimpleObjectProperty<>();
    private final Group nodeGroup = new Group(this);

    private BoundingBoxCategory boundingBoxCategory;
    private ImageMetaData imageMetaData;

    public BoundingBoxView(BoundingBoxCategory boundingBoxCategory, ImageMetaData imageMetaData) {
        this.imageMetaData = imageMetaData;
        this.boundingBoxCategory = boundingBoxCategory;

        setManaged(false);
        setVisible(false);
        getStyleClass().add(SELECTION_RECTANGLE_STYLE);

        nodeGroup.setManaged(false);
        nodeGroup.getChildren().addAll(createResizeHandles());

        addMoveFunctionality();
    }

    private BoundingBoxView() {
    }

    public BoundingBoxCategory getBoundingBoxCategory() {
        return boundingBoxCategory;
    }

    public ImageMetaData getImageMetaData() {
        return imageMetaData;
    }

    public Bounds getImageRelativeBounds() {
        final Bounds imageViewBounds = confinementBounds.getValue();
        final Bounds selectionRectangleBounds = this.getBoundsInParent();

        double imageWidth = imageMetaData.getImageWidth();
        double imageHeight = imageMetaData.getImageHeight();

        double xMinRelative = selectionRectangleBounds.getMinX() * imageWidth / imageViewBounds.getWidth();
        double yMinRelative = selectionRectangleBounds.getMinY() * imageHeight / imageViewBounds.getHeight();
        double widthRelative = selectionRectangleBounds.getWidth() * imageWidth / imageViewBounds.getWidth();
        double heightRelative = selectionRectangleBounds.getHeight() * imageHeight / imageViewBounds.getHeight();

        return new BoundingBox(xMinRelative, yMinRelative, widthRelative, heightRelative);
    }

    public void confineTo(final ReadOnlyObjectProperty<Bounds> bounds) {
        confinementBounds.bind(bounds);

        bounds.addListener((observable, oldValue, newValue) -> {
            setWidth(getWidth() * newValue.getWidth() / oldValue.getWidth());
            setHeight(getHeight() * newValue.getHeight() / oldValue.getHeight());

            setX(newValue.getMinX() + (getX() - oldValue.getMinX()) * newValue.getWidth() / oldValue.getWidth());
            setY(newValue.getMinY() + (getY() - oldValue.getMinY()) * newValue.getHeight() / oldValue.getHeight());
        });
    }

    public void setXYWH(double x, double y, double w, double h) {
        setX(x);
        setY(y);
        setWidth(w);
        setHeight(h);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof BoundingBoxView) {
            return super.equals(obj) && Objects.equals(boundingBoxCategory, ((BoundingBoxView) obj).boundingBoxCategory) &&
                    Objects.equals(imageMetaData, ((BoundingBoxView) obj).imageMetaData);
        }
        return false;
    }

    static BoundingBoxView getDummy() {
        return NULL_BOUNDING_BOX_VIEW;
    }

    Group getNodeGroup() {
        return nodeGroup;
    }

    void fillOpaque() {
        setFill(Color.web(getStroke().toString(), DEFAULT_FILL_OPACITY));
    }

    void fillTransparent() {
        setFill(Color.TRANSPARENT);
    }

    private double getMaxX() {
        return getX() + getWidth();
    }

    private double getMaxY() {
        return getY() + getHeight();
    }

    private List<ResizeHandle> createResizeHandles() {
        return Arrays.stream(CompassPoint.values())
                .map(ResizeHandle::new)
                .collect(Collectors.toList());
    }

    private void addMoveFunctionality() {
        this.setOnMouseEntered(event -> {
            this.setCursor(Cursor.MOVE);
            event.consume();
        });

        this.setOnMousePressed(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                final Point2D eventXY = new Point2D(event.getX(), event.getY());
                dragAnchor.setFromPoint2D(eventXY.subtract(getX(), getY()));
            }
            event.consume();
        });

        this.setOnMouseDragged(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY)) {
                Point2D eventXY = new Point2D(event.getX(), event.getY());
                Point2D newXY = eventXY.subtract(dragAnchor.getX(), dragAnchor.getY());
                Bounds regionBounds = confinementBounds.getValue();
                Bounds moveBounds = new BoundingBox(regionBounds.getMinX(), regionBounds.getMinY(),
                        regionBounds.getWidth() - getWidth(),
                        regionBounds.getHeight() - getHeight());
                Point2D newConfinedXY = MathUtils.clampWithinBounds(newXY, moveBounds);

                setX(newConfinedXY.getX());
                setY(newConfinedXY.getY());
            }
            event.consume();
        });
    }

    private enum CompassPoint {NW, N, NE, E, SE, S, SW, W}

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
            final BoundingBoxView rectangle = BoundingBoxView.this;
            final DoubleProperty rectangle_x = rectangle.xProperty();
            final DoubleProperty rectangle_y = rectangle.yProperty();
            final DoubleProperty rectangle_w = rectangle.widthProperty();
            final DoubleProperty rectangle_h = rectangle.heightProperty();

            fillProperty().bind(rectangle.strokeProperty());
            managedProperty().bind(rectangle.managedProperty());
            visibleProperty().bind(rectangle.visibleProperty());

            switch(compassPoint) {
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
                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    dragAnchor.setFromMouseEvent(event);
                }
                event.consume();
            });

            final BoundingBoxView rectangle = BoundingBoxView.this;

            switch(compassPoint) {
                case NW:
                    setOnMouseDragged(event -> {
                        if(event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(parentBounds.getMinX(), parentBounds.getMinY(),
                                    rectangle.getMaxX() - parentBounds.getMinX(),
                                    rectangle.getMaxY() - parentBounds.getMinY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

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
                        if(event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), parentBounds.getMinY(),
                                    rectangle.getWidth(), rectangle.getMaxY() - parentBounds.getMinY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                            rectangle.setY(clampedEventXY.getY());
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
                        }

                        event.consume();
                    });
                    break;
                case NE:
                    setOnMouseDragged(event -> {
                        if(event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), parentBounds.getMinY(),
                                    parentBounds.getMaxX() - rectangle.getX(),
                                    rectangle.getMaxY() - parentBounds.getMinY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                            rectangle.setY(clampedEventXY.getY());
                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMaxY()));
                        }

                        event.consume();
                    });
                    break;
                case E:
                    setOnMouseDragged(event -> {
                        if(event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                    parentBounds.getMaxX() - rectangle.getX(), rectangle.getHeight());
                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                        }

                        event.consume();
                    });
                    break;
                case SE:
                    setOnMouseDragged(event -> {
                        if(event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                    parentBounds.getMaxX() - rectangle.getX(),
                                    parentBounds.getMaxY() - rectangle.getY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMinX()));
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
                        }

                        event.consume();
                    });
                    break;
                case S:
                    setOnMouseDragged(event -> {
                        if(event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(rectangle.getX(), rectangle.getY(),
                                    rectangle.getWidth(),
                                    parentBounds.getMaxY() - rectangle.getY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
                        }

                        event.consume();
                    });
                    break;
                case SW:
                    setOnMouseDragged(event -> {
                        if(event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(parentBounds.getMinX(), rectangle.getY(),
                                    rectangle.getMaxX() - parentBounds.getMinX(),
                                    parentBounds.getMaxY() - rectangle.getY());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

                            rectangle.setX(clampedEventXY.getX());
                            rectangle.setWidth(Math.abs(clampedEventXY.getX() - bounds.getMaxX()));
                            rectangle.setHeight(Math.abs(clampedEventXY.getY() - bounds.getMinY()));
                        }

                        event.consume();
                    });
                    break;
                case W:
                    setOnMouseDragged(event -> {
                        if(event.getButton().equals(MouseButton.PRIMARY)) {
                            final Bounds parentBounds = rectangle.confinementBounds.getValue();
                            final Bounds bounds = new BoundingBox(parentBounds.getMinX(), rectangle.getY(),
                                    rectangle.getMaxX() - parentBounds.getMinX(),
                                    rectangle.getHeight());

                            final Point2D eventXY = new Point2D(event.getX(), event.getY());
                            final Point2D clampedEventXY = MathUtils.clampWithinBounds(eventXY, bounds);

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
