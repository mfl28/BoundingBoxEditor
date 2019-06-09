package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.model.BoundingBoxCategory;
import BoundingboxEditor.model.ImageMetaData;
import BoundingboxEditor.utils.MathUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A UI-element responsible for displaying the currently selected image on which the
 * user can draw bounding-boxes.
 *
 * @see StackPane
 * @see View
 * @see BoundingBoxView
 */
public class BoundingBoxEditorImagePaneView extends StackPane implements View {
    private static final double IMAGE_PADDING = 10.0;
    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;
    private static final String IMAGE_PANE_ID = "image-pane-view";
    private static final String INITIALIZER_RECTANGLE_ID = "bounding-rectangle";

    private final ImageView imageView = new ImageView();
    private final ObjectProperty<Image> currentImageObject = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty maximizeImageView = new SimpleBooleanProperty(true);
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final ColorAdjust colorAdjust = new ColorAdjust();

    private final Group boundingBoxSceneGroup = new Group();
    private final ToggleGroup boundingBoxSelectionGroup = new ToggleGroup();
    private final ObservableList<BoundingBoxView> currentBoundingBoxes = FXCollections.observableArrayList();
    private final ObjectProperty<BoundingBoxCategory> selectedCategory = new SimpleObjectProperty<>(null);

    private final Rectangle initializerRectangle = createInitializerRectangle();
    private final DragAnchor dragAnchor = new DragAnchor();

    /**
     * Creates a new image-pane UI-element responsible for displaying the currently selected image on which the
     * user can draw bounding-boxes.
     */
    BoundingBoxEditorImagePaneView() {
        getChildren().addAll(
                imageView,
                boundingBoxSceneGroup,
                initializerRectangle,
                progressIndicator
        );

        setId(IMAGE_PANE_ID);

        boundingBoxSceneGroup.setManaged(false);
        progressIndicator.setVisible(false);

        setUpImageView();
        setUpInternalListeners();
    }

    @Override
    public void connectToController(Controller controller) {
        imageView.setOnMouseReleased(controller::onImageViewMouseReleasedEvent);
    }

    /**
     * Constructs a new {@link BoundingBoxView} object which is initialized from the current coordinates and size of the
     * initializerRectangle member.
     *
     * @param imageMetaData the image-meta data to be registered with the new {@link BoundingBoxView} object
     */
    public void constructAndAddNewBoundingBox(ImageMetaData imageMetaData) {
        final BoundingBoxView newBoundingBox = new BoundingBoxView(selectedCategory.get(), imageMetaData);

        newBoundingBox.setCoordinatesAndSizeFromInitializer(initializerRectangle);
        newBoundingBox.autoScaleWithBounds(imageView.boundsInParentProperty());
        newBoundingBox.setToggleGroup(boundingBoxSelectionGroup);

        currentBoundingBoxes.add(newBoundingBox);
        boundingBoxSelectionGroup.selectToggle(newBoundingBox);
        initializerRectangle.setVisible(false);
    }

    /**
     * Clears the list of current {@link BoundingBoxView} objects.
     */
    public void removeAllCurrentBoundingBoxes() {
        currentBoundingBoxes.clear();
    }

    /**
     * Removes all provided {@link BoundingBoxView} objects from the list
     * of current {@link BoundingBoxView} objects.
     *
     * @param boundingBoxes the list of objects to remove
     */
    void removeAllFromCurrentBoundingBoxes(Collection<BoundingBoxView> boundingBoxes) {
        currentBoundingBoxes.removeAll(boundingBoxes);
    }

    /**
     * Clears the list of current {@link BoundingBoxView} objects and adds all
     * objects from the provided {@link Collection}.
     *
     * @param boundingBoxes the {@link BoundingBoxView} objects to set
     */
    void setAllCurrentBoundingBoxes(Collection<BoundingBoxView> boundingBoxes) {
        currentBoundingBoxes.setAll(boundingBoxes);
    }

    /**
     * Adds the provided {@link BoundingBoxView} objects to the boundingBoxSceneGroup which is
     * a node in the scene-graph. Should only be called from within a change-listener registered
     * to currentBoundingBoxes.
     *
     * @param boundingBoxes the objects to add
     */
    void addBoundingBoxViewsToSceneGroup(Collection<? extends BoundingBoxView> boundingBoxes) {
        boundingBoxSceneGroup.getChildren().addAll(boundingBoxes.stream()
                .map(BoundingBoxView::getNodeGroup)
                .collect(Collectors.toList()));
    }

    /**
     * Removes the provided {@link BoundingBoxView} objects from a the boundingBoxSceneGroup which is
     * a node in the scene-graph. Should only be called from within a change-listener registered
     * to currentBoundingBoxes.
     *
     * @param boundingBoxes the objects to remove
     */
    void removeBoundingBoxViewsFromSceneGroup(Collection<? extends BoundingBoxView> boundingBoxes) {
        boundingBoxSceneGroup.getChildren().removeAll(boundingBoxes.stream()
                .map(BoundingBoxView::getNodeGroup)
                .collect(Collectors.toList()));
    }

    /**
     * Updates the displayed image from a provided image-{@link File}.
     *
     * @param imageFile the file of the new image
     */
    void updateImageFromFile(File imageFile) {
        currentImageObject.set(new Image(imageFile.toURI().toString(), true));

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

    /**
     * Sets the image-view's width and height to their maximum allowed values.
     */
    void setImageViewToMaxAllowedSize() {
        imageView.setFitWidth(getMaxAllowedImageWidth());
        imageView.setFitHeight(getMaxAllowedImageHeight());
    }

    /**
     * Sets the image-view's width and height to the original width and height values of the currently loaded
     * image if these values are less than or equal to the maximum allowed values, otherwise
     * sets the maximum allowed values.
     */
    void setImageViewToPreferOriginalImageSize() {
        imageView.setFitWidth(Math.min(imageView.getImage().getWidth(), getMaxAllowedImageWidth()));
        imageView.setFitHeight(Math.min(imageView.getImage().getHeight(), getMaxAllowedImageHeight()));
    }

    /**
     * Returns the {@link ToggleGroup} object used to realize the
     * single-selection mechanism for {@link BoundingBoxView} objects.
     *
     * @return the toggle-group
     */
    ToggleGroup getBoundingBoxSelectionGroup() {
        return boundingBoxSelectionGroup;
    }

    /**
     * Returns the {@link ObservableList} of current {@link BoundingBoxView} objects.
     *
     * @return the list
     */
    ObservableList<BoundingBoxView> getCurrentBoundingBoxes() {
        return currentBoundingBoxes;
    }

    /**
     * Returns the property of the currently selected {@link BoundingBoxCategory}.
     *
     * @return the property
     */
    ObjectProperty<BoundingBoxCategory> selectedCategoryProperty() {
        return selectedCategory;
    }

    /**
     * Returns the {@link ColorAdjust} member which can be used to register effects
     * on the images.
     *
     * @return the {@link ColorAdjust} object
     */
    ColorAdjust getColorAdjust() {
        return colorAdjust;
    }

    /**
     * Returns the {@link ImageView} member which is responsible for displaying the
     * currently selected image.
     *
     * @return the image-view
     */
    ImageView getImageView() {
        return imageView;
    }

    /**
     * Returns the {@link Rectangle} object used to initialize new {@link BoundingBoxView} objects.
     *
     * @return the initializer-rectangle
     */
    Rectangle getInitializerRectangle() {
        return initializerRectangle;
    }

    /**
     * Returns the currently loaded {@link Image} object.
     *
     * @return the image
     */
    Image getCurrentImage() {
        return currentImageObject.get();
    }

    private void setUpImageView() {
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setPickOnBounds(true);
        imageView.setPreserveRatio(true);
        imageView.setEffect(colorAdjust);
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

                initializerRectangle.setX(parentCoordinates.getX());
                initializerRectangle.setY(parentCoordinates.getY());
                initializerRectangle.setWidth(0);
                initializerRectangle.setHeight(0);
                initializerRectangle.setVisible(true);
            }
        });

        imageView.setOnMouseDragged(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY) && isCategorySelected()) {
                Point2D clampedEventXY = MathUtils.clampWithinBounds(event.getX(), event.getY(), imageView.getBoundsInLocal());
                Point2D parentCoordinates = imageView.localToParent(Math.min(clampedEventXY.getX(), dragAnchor.getX()),
                        Math.min(clampedEventXY.getY(), dragAnchor.getY()));

                initializerRectangle.setX(parentCoordinates.getX());
                initializerRectangle.setY(parentCoordinates.getY());
                initializerRectangle.setWidth(Math.abs(clampedEventXY.getX() - dragAnchor.getX()));
                initializerRectangle.setHeight(Math.abs(clampedEventXY.getY() - dragAnchor.getY()));
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

    private boolean isMaximizeImageView() {
        return maximizeImageView.get();
    }

    /**
     * Sets the maximize-image-view property to a new value.
     *
     * @param maximizeImageView the new value
     */
    void setMaximizeImageView(boolean maximizeImageView) {
        this.maximizeImageView.set(maximizeImageView);
    }

    private double getMaxAllowedImageWidth() {
        return Math.max(0, getWidth() - 2 * IMAGE_PADDING);
    }

    private double getMaxAllowedImageHeight() {
        return Math.max(0, getHeight() - 2 * IMAGE_PADDING);
    }

    private boolean isCategorySelected() {
        return selectedCategory.get() != null;
    }

    private void resetProgressIndicatorAnimation() {
        progressIndicator.setProgress(0);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    }

    private Rectangle createInitializerRectangle() {
        Rectangle initializer = new Rectangle();
        initializer.setManaged(false);
        initializer.setVisible(false);
        initializer.setFill(Color.TRANSPARENT);
        initializer.setId(INITIALIZER_RECTANGLE_ID);
        return initializer;
    }
}
