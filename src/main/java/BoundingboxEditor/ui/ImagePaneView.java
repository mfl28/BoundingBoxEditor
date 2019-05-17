package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.utils.MathUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;

import java.util.Collection;
import java.util.stream.Collectors;

public class ImagePaneView extends StackPane implements View {
    static final double IMAGE_PADDING = 10.0;
    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;
    private static final String IMAGE_PANE_STYLE = "pane";
    private static final String IMAGE_VIEW_ID = "image-pane";

    private final ImageView imageView = new ImageView();
    private final ColorAdjust colorAdjust = new ColorAdjust();
    private final DragAnchor dragAnchor = new DragAnchor();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ObjectProperty<Image> currentImageObject = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty maximizeImageView = new SimpleBooleanProperty(true);
    private final ObjectProperty<BoundingBoxCategory> selectedCategory = new SimpleObjectProperty<>(null);
    private final Group boundingBoxGroup = new Group();

    private BoundingBoxView initializerBoundingBox = new BoundingBoxView(null, null);
    private BoundingBoxViewDatabase boundingBoxDataBase;

    ImagePaneView() {
        getChildren().addAll(imageView, initializerBoundingBox.getNodeGroup(), boundingBoxGroup, progressIndicator);
        getStyleClass().add(IMAGE_PANE_STYLE);

        boundingBoxGroup.setManaged(false);
        progressIndicator.setVisible(false);

        initializerBoundingBox.confineTo(imageView.boundsInParentProperty());

        setUpImageView();
        setUpInternalListeners();
    }

    @Override
    public void connectToController(Controller controller) {
        imageView.setOnMouseReleased(controller::onImageViewMouseReleasedEvent);
    }

    public void addBoundingBoxesToView(Collection<? extends BoundingBoxView> boundingBoxes) {
        boundingBoxGroup.getChildren().addAll(boundingBoxes.stream()
                .map(BoundingBoxView::getNodeGroup)
                .collect(Collectors.toList()));
    }

    public void removeAllBoundingBoxesFromView() {
        boundingBoxGroup.getChildren().clear();
    }

    public void resetBoundingBoxDatabase(int size) {
        removeAllBoundingBoxesFromView();
        boundingBoxDataBase = new BoundingBoxViewDatabase(size);
    }

    public void resetCurrentBoundingBoxes() {
        getCurrentBoundingBoxes().forEach(item -> item.setVisible(true));
        removeAllBoundingBoxesFromView();
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    public BoundingBoxCategory getSelectedCategory() {
        return selectedCategory.get();
    }

    public BoundingBoxView getBoundingBoxInitializer() {
        return initializerBoundingBox;
    }

    public BoundingBoxViewDatabase getBoundingBoxDataBase() {
        return boundingBoxDataBase;
    }

    public ObservableList<BoundingBoxView> getCurrentBoundingBoxes() {
        return boundingBoxDataBase.getCurrentBoundingBoxes();
    }

    ObjectProperty<BoundingBoxCategory> selectedCategoryProperty() {
        return selectedCategory;
    }

    void removeBoundingBoxesFromView(Collection<? extends BoundingBoxView> boundingBoxes) {
        boundingBoxGroup.getChildren().removeAll(boundingBoxes.stream()
                .map(BoundingBoxView::getNodeGroup)
                .collect(Collectors.toList()));
    }

    ColorAdjust getColorAdjust() {
        return colorAdjust;
    }

    DragAnchor getDragAnchor() {
        return dragAnchor;
    }

    ImageView getImageView() {
        return imageView;
    }

    void updateImage(final Image image) {
        // FIXME: Image loading does not keep up (ie the wrong image is loaded) when using  pressed up/down keys to quickly iterate through images in image gallery.
        currentImageObject.set(image);

        resetProgressIndicatorAnimation();
        progressIndicator.setVisible(true);

        currentImageObject.getValue().progressProperty().addListener((value, oldValue, newValue) -> {
            if(newValue.intValue() == 1) {
                imageView.setImage(currentImageObject.getValue());
                progressIndicator.setVisible(false);

                if(isMaximizeImageView()) {
                    setImageViewToMaxAllowedSize();
                } else {
                    setImageViewToPreferOriginalImageSize();
                }
            }
        });
    }

    BoundingBoxView getInitializerBoundingBox() {
        return initializerBoundingBox;
    }

    Image getCurrentImage() {
        return currentImageObject.get();
    }

    void setImageViewToMaxAllowedSize() {
        imageView.setFitWidth(getMaxAllowedImageWidth());
        imageView.setFitHeight(getMaxAllowedImageHeight());
    }

    void setImageViewToPreferOriginalImageSize() {
        imageView.setFitWidth(Math.min(imageView.getImage().getWidth(), getMaxAllowedImageWidth()));
        imageView.setFitHeight(Math.min(imageView.getImage().getHeight(), getMaxAllowedImageHeight()));
    }

    boolean isMaximizeImageView() {
        return maximizeImageView.get();
    }

    void setMaximizeImageView(boolean maximizeImageView) {
        this.maximizeImageView.set(maximizeImageView);
    }

    double getMaxAllowedImageWidth() {
        return Math.max(0, getWidth() - 2 * IMAGE_PADDING);
    }

    double getMaxAllowedImageHeight() {
        return Math.max(0, getHeight() - 2 * IMAGE_PADDING);
    }

    private boolean isCategorySelected() {
        return selectedCategory.get() != null;
    }

    private void setUpImageView() {
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setPickOnBounds(true);
        imageView.setPreserveRatio(true);
        imageView.setEffect(colorAdjust);
        imageView.setId(IMAGE_VIEW_ID);
    }

    private void resetProgressIndicatorAnimation() {
        progressIndicator.setProgress(0);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    }

    private void setUpInternalListeners() {
        widthProperty().addListener((value, oldValue, newValue) -> {
            if(!isMaximizeImageView()) {
                final Image image = imageView.getImage();
                double prefWidth = image != null ? image.getWidth() : 0;
                imageView.setFitWidth(Math.min(prefWidth, getMaxAllowedImageWidth()));
            } else {
                imageView.setFitWidth(getMaxAllowedImageWidth());
            }
        });

        heightProperty().addListener((value, oldValue, newValue) -> {
            if(!isMaximizeImageView()) {
                final Image image = imageView.getImage();
                double prefHeight = image != null ? image.getHeight() : 0;
                imageView.setFitHeight(Math.min(prefHeight, getMaxAllowedImageHeight()));
            } else {
                imageView.setFitHeight(getMaxAllowedImageHeight());
            }
        });

        imageView.setOnMousePressed(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY) && isCategorySelected()) {
                Point2D parentCoordinates = imageView.localToParent(event.getX(), event.getY());
                dragAnchor.setFromMouseEvent(event);

                initializerBoundingBox.setXYWH(parentCoordinates.getX(), parentCoordinates.getY(), 0, 0);
                initializerBoundingBox.setVisible(true);
            }
        });

        imageView.setOnMouseDragged(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY) && isCategorySelected()) {
                Point2D clampedEventXY = MathUtils.clampWithinBounds(event.getX(), event.getY(), imageView.getBoundsInLocal());
                Point2D parentCoordinates = imageView.localToParent(Math.min(clampedEventXY.getX(), dragAnchor.getX()),
                        Math.min(clampedEventXY.getY(), dragAnchor.getY()));

                initializerBoundingBox.setXYWH(parentCoordinates.getX(),
                        parentCoordinates.getY(),
                        Math.abs(clampedEventXY.getX() - dragAnchor.getX()),
                        Math.abs(clampedEventXY.getY() - dragAnchor.getY()));
            }
        });

//        imageView.setOnMouseReleased(event -> {
//            if(event.getButton().equals(MouseButton.PRIMARY) && isCategorySelected()) {
//                BoundingBoxView newBoundingBox = new BoundingBoxView(selectedCategory.get(),
//                        ImageMetaData.fromImage(getCurrentImage()));
//
//                newBoundingBox.setUpFromInitializer(initializerBoundingBox);
//                newBoundingBox.setVisible(true);
//                newBoundingBox.confineTo(imageView.boundsInParentProperty());
//
//                getBoundingBoxDataBase().addToCurrentBoundingBoxes(newBoundingBox);
//                initializerBoundingBox.setVisible(false);
//            }
//        });

        setOnScroll(event -> {
            if(event.isControlDown()) {
                final Image image = imageView.getImage();
                double newFitWidth = MathUtils.clamp(imageView.getFitWidth() + event.getDeltaY(),
                        Math.min(ZOOM_MIN_WINDOW_RATIO * getWidth(), image.getWidth()),
                        getMaxAllowedImageWidth());
                double newFitHeight = MathUtils.clamp(imageView.getFitHeight() + event.getDeltaY(),
                        Math.min(ZOOM_MIN_WINDOW_RATIO * getHeight(), image.getHeight()),
                        getMaxAllowedImageHeight());

                imageView.setFitWidth(newFitWidth);
                imageView.setFitHeight(newFitHeight);
            }
        });
    }
}
