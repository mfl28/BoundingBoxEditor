package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.utils.MathUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.Collection;

public class ImagePaneView extends StackPane implements View {
    private static final double IMAGE_PADDING = 10.0;
    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;
    private static final String IMAGE_PANE_STYLE = "pane";
    private static final String IMAGE_VIEW_ID = "image-pane";

    private final ImageView imageView = new ImageView();
    private final ColorAdjust colorAdjust = new ColorAdjust();
    private final DragAnchor dragAnchor = new DragAnchor();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ObjectProperty<Image> currentImageObject = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty maximizeImageView = new SimpleBooleanProperty(true);
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

    public void addBoundingBoxesToView(Collection<? extends BoundingBoxView> selectionRectangles) {
        selectionRectangles.forEach(item -> boundingBoxGroup.getChildren().addAll(item.getNodeGroup()));
    }

    public void removeAllBoundingBoxesFromView() {
        boundingBoxGroup.getChildren().clear();
    }

    public void resetSelectionRectangleDatabase(int size) {
        removeAllBoundingBoxesFromView();
        boundingBoxDataBase = new BoundingBoxViewDatabase(size);
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    @Override
    public void connectToController(Controller controller) {
        imageView.setOnMousePressed(controller::onMousePressed);
        imageView.setOnMouseDragged(controller::onMouseDragged);
        imageView.setOnMouseReleased(controller::onMouseReleased);
    }

    public BoundingBoxViewDatabase getBoundingBoxDataBase() {
        return boundingBoxDataBase;
    }

    public ObservableList<BoundingBoxView> getCurrentBoundingBoxes() {
        return boundingBoxDataBase.getCurrentBoundingBoxes();
    }

    void removeBoundingBoxesFromView(Collection<? extends BoundingBoxView> boundingBoxes) {
        boundingBoxes.forEach(item -> boundingBoxGroup.getChildren().removeAll(item.getNodeGroup()));
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
            if(newValue.equals(1.0)) {
                imageView.setImage(currentImageObject.getValue());
                progressIndicator.setVisible(false);

                if(getMaximizeImageView()) {
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

    private boolean getMaximizeImageView() {
        return maximizeImageView.get();
    }

    void setMaximizeImageView(boolean maximizeImageView) {
        this.maximizeImageView.set(maximizeImageView);
    }

    private void setUpInternalListeners() {
        widthProperty().addListener((value, oldValue, newValue) -> {
            if(!getMaximizeImageView()) {
                final Image image = imageView.getImage();
                double prefWidth = image != null ? image.getWidth() : 0;
                imageView.setFitWidth(Math.min(prefWidth, getMaxAllowedImageWidth()));
            } else {
                imageView.setFitWidth(getMaxAllowedImageWidth());
            }
        });

        heightProperty().addListener((value, oldValue, newValue) -> {
            if(!getMaximizeImageView()) {
                final Image image = imageView.getImage();
                double prefHeight = image != null ? image.getHeight() : 0;
                imageView.setFitHeight(Math.min(prefHeight, getMaxAllowedImageHeight()));
            } else {
                imageView.setFitHeight(getMaxAllowedImageHeight());
            }
        });

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

    private double getMaxAllowedImageWidth() {
        return Math.max(0, getWidth() - 2 * IMAGE_PADDING);
    }

    private double getMaxAllowedImageHeight() {
        return Math.max(0, getHeight() - 2 * IMAGE_PADDING);
    }
}
