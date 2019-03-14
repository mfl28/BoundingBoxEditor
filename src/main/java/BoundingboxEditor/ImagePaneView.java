package BoundingboxEditor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.CacheHint;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImagePaneView extends StackPane implements View {

    private static final double IMAGE_PADDING = 30.0;
    private static final double ZOOM_MIN_WINDOW_RATIO = 0.25;
    private static final String IMAGE_PANE_STYLE = "pane";

    private final DragAnchor mousePressed = new DragAnchor();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private ImageView imageView = new ImageView();
    private ObservableList<SelectionRectangle> selectionRectangleList = FXCollections.observableArrayList();
    private List<ObservableList<SelectionRectangle>> imageSelectionRectangles;
    private SelectionRectangle selectionRectangle = new SelectionRectangle(null, null);
    //FIXME: should be in model
    private ObjectProperty<Image> currentImageObject = new SimpleObjectProperty<>();

    ImagePaneView() {
        this.getStyleClass().add(IMAGE_PANE_STYLE);

        // The intermediary selectionRectangle, will be used to construct actual bounding boxes.

        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setCacheHint(CacheHint.SPEED);
        /* So that events are registered even on transparent image parts. */
        imageView.setPickOnBounds(true);
        imageView.setId("image-pane");

        this.getChildren().add(imageView);
        this.getChildren().addAll(selectionRectangle.getNodes());
        this.getChildren().add(progressIndicator);

        progressIndicator.setVisible(false);

        setUpInternalListeners();
    }

    public void removeSelectionRectanglesFromChildren(Iterable<? extends SelectionRectangle> selectionRectangles) {
        selectionRectangles.forEach(item -> this.getChildren().removeAll(item.getNodes()));
    }

    public void addSelectionRectanglesAsChildren(Iterable<? extends SelectionRectangle> selectionRectangles) {
        selectionRectangles.forEach(item -> this.getChildren().addAll(item.getNodes()));
    }

    public DragAnchor getMousePressed() {
        return mousePressed;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(final Image image) {
        // FIXME: Image loading does not keep up (ie the wrong image is loaded) when using  pressed up/down keys to quickly iterate through images in image gallery.
        // reset progress indicator animation
        currentImageObject.set(image);
        //currentImage = image;
        progressIndicator.setProgress(0);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.setVisible(true);

        currentImageObject.getValue().progressProperty().addListener((value, oldValue, newValue) -> {
            if(newValue.equals(1.0)) {
                imageView.setImage(image);
                imageView.setPreserveRatio(true);
                setInitialImageViewSize();
                progressIndicator.setVisible(false);
            }
        });
    }

    public void resetSelectionRectangleDatabase(int numItems) {
        imageSelectionRectangles = new ArrayList<>(Collections.nCopies(numItems, null));
    }

    public ObservableList<SelectionRectangle> getSelectionRectangleList() {
        return selectionRectangleList;
    }

    public void setSelectionRectangleList(ObservableList<SelectionRectangle> selectionRectangleList) {
        this.selectionRectangleList = selectionRectangleList;
    }

    public List<ObservableList<SelectionRectangle>> getImageSelectionRectangles() {
        return imageSelectionRectangles;
    }

    public void setImageSelectionRectangles(List<ObservableList<SelectionRectangle>> imageSelectionRectangles) {
        this.imageSelectionRectangles = imageSelectionRectangles;
    }

    public SelectionRectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    public Image getCurrentImage() {
        return currentImageObject.get();
    }

    public ObjectProperty<Image> imageObjectProperty() {
        return currentImageObject;
    }

    @Override
    public void connectToController(Controller controller) {
        imageView.setOnMousePressed(controller::onMousePressed);
        imageView.setOnMouseDragged(controller::onMouseDragged);
        imageView.setOnMouseReleased(controller::onMouseReleased);
    }

    private void setInitialImageViewSize() {
        final double imageWidth = imageView.getImage().getWidth();
        final double imageHeight = imageView.getImage().getHeight();
        final double maxAllowedWidth = this.getWidth() - 2 * IMAGE_PADDING;
        final double maxAllowedHeight = this.getHeight() - 2 * IMAGE_PADDING;

        imageView.setFitWidth(Math.min(imageWidth, maxAllowedWidth));
        imageView.setFitHeight(Math.min(imageHeight, maxAllowedHeight));
    }

    private void setUpInternalListeners() {
        this.widthProperty().addListener((value, oldValue, newValue) -> {
            double prefWidth = 0;
            if(imageView.getImage() != null)
                prefWidth = imageView.getImage().getWidth();
            imageView.setFitWidth(Math.min(prefWidth, newValue.doubleValue() - 2 * IMAGE_PADDING));
        });

        this.heightProperty().addListener((value, oldValue, newValue) -> {
            double prefHeight = 0;
            if(imageView.getImage() != null)
                prefHeight = imageView.getImage().getHeight();
            imageView.setFitHeight(Math.min(prefHeight, newValue.doubleValue() - 2 * IMAGE_PADDING));
        });

        this.setOnScroll(event -> {
            if(event.isControlDown()) {
                final Image image = imageView.getImage();
                final double delta = event.getDeltaY();

                final double newFitWidth = Utils.clamp(imageView.getFitWidth() + delta,
                        Math.min(ZOOM_MIN_WINDOW_RATIO * this.getWidth(), image.getWidth()),
                        this.getWidth() - 2 * IMAGE_PADDING);
                final double newFitHeight = Utils.clamp(imageView.getFitHeight() + delta,
                        Math.min(ZOOM_MIN_WINDOW_RATIO * this.getHeight(), image.getHeight()),
                        this.getHeight() - 2 * IMAGE_PADDING);

                imageView.setFitWidth(newFitWidth);
                imageView.setFitHeight(newFitHeight);
            }
        });

        selectionRectangle.confineTo(imageView.boundsInParentProperty());
    }
}
