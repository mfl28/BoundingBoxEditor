import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class View extends BorderPane {
    private final Controller controller;

    private StackPane imagePane;
    private ImageView imageView;
    private Button nextButton;
    private Button previousButton;
    private Slider brightnessSlider;
    private Slider zoomSlider;
    private SelectionRectangle selectionRectangle;
    private VBox topBox;

    ChangeListener<Bounds> changeListener;

    MenuItem fileOpenFolderItem;
    MenuItem viewFitWindowItem;
    MenuItem fileSaveItem;

    private Vector2D mousePressed = new Vector2D();

    // Maybe replace with enums
    private static final String NEXT_ICON_PATH = "icons/arrow_right.png";
    private static final String PREVIOUS_ICON_PATH = "icons/arrow_left.png";
    private static final String ZOOM_ICON_PATH = "icons/zoom.png";
    private static final String BRIGHTNESS_ICON_PATH = "icons/brightness.png";
    private static final String TOP_BOX_STYLE = "topBox";
    private static final String IMAGE_PANE_STYLE = "pane";
    private static final String FILE_MENU_TEXT = "_File";
    private static final String VIEW_MENU_TEXT = "_View";
    private static final String OPEN_FOLDER_TEXT = "_Open Folder...";
    private static final String SAVE_TEXT = "_Save...";
    private static final String FIT_WINDOW_TEXT = "_Fit Window";
    private static final double ICON_WIDTH = 20.0;
    private static final double ICON_HEIGHT = 20.0;
    private static final double IMAGE_PADDING = 30.0;


    public View(Controller controller) {
        this.controller = controller;
        imagePane = createImagePane();
        topBox = createTopBox();

        this.setTop(topBox);
        this.setCenter(imagePane);
    }

    private StackPane createImagePane() {
        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add(IMAGE_PANE_STYLE);
        selectionRectangle = new SelectionRectangle();
        imageView = new ImageView();

        imagePane.getChildren().add(imageView);
        imagePane.getChildren().addAll(selectionRectangle.getNodes());

        return imagePane;
    }

    private VBox createTopBox() {
        VBox topBox = new VBox();

        topBox.getChildren().addAll(createMenuBar(), new Separator(), createToolBar());
        topBox.getStyleClass().add(TOP_BOX_STYLE);

        return topBox;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu(FILE_MENU_TEXT);
        Menu viewMenu = new Menu(VIEW_MENU_TEXT);

        fileOpenFolderItem = new MenuItem(OPEN_FOLDER_TEXT);

        viewFitWindowItem = new MenuItem(FIT_WINDOW_TEXT);
        fileSaveItem = new MenuItem(SAVE_TEXT);

        fileMenu.getItems().addAll(fileOpenFolderItem, fileSaveItem);
        viewMenu.getItems().add(viewFitWindowItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu);

        fileOpenFolderItem.setOnAction(controller);
        fileSaveItem.setOnAction(controller);
        viewFitWindowItem.setOnAction(controller);

        return menuBar;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        Pane leftSpace = new Pane();
        Pane rightSpace = new Pane();

        HBox.setHgrow(leftSpace, Priority.ALWAYS);
        HBox.setHgrow(rightSpace, Priority.ALWAYS);

        zoomSlider = new Slider(1, 1.2, 1);
        Label zoomLabel = createIconLabel(ZOOM_ICON_PATH);

        brightnessSlider = new Slider(-0.5, 0.5, 0);
        Label brightnessLabel = createIconLabel(BRIGHTNESS_ICON_PATH);

        nextButton = createIconButton(NEXT_ICON_PATH);
        previousButton = createIconButton(PREVIOUS_ICON_PATH);
        toolBar.getItems().addAll(zoomLabel, zoomSlider,
                leftSpace, previousButton, nextButton, rightSpace, brightnessLabel, brightnessSlider);

        nextButton.setOnAction(controller);
        previousButton.setOnAction(controller);

        return toolBar;
    }

    private Button createIconButton(String iconPath) {
        Button button = new Button();
        ImageView iconView = new ImageView(getClass().getResource(iconPath).toString());

        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);
        button.setGraphic(iconView);

        return button;
    }

    private Label createIconLabel(String iconPath) {
        Label label = new Label();
        ImageView iconView = new ImageView(getClass().getResource(iconPath).toString());

        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);
        label.setGraphic(iconView);

        return label;
    }

    public MenuItem getFileOpenFolderItem() {
        return fileOpenFolderItem;
    }

    public MenuItem getViewFitWindowItem() {
        return viewFitWindowItem;
    }

    public MenuItem getFileSaveItem() {
        return fileSaveItem;
    }

    public Button getPreviousButton() {
        return previousButton;
    }

    public Button getNextButton() {
        return nextButton;
    }

    public Rectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(Image image) {

        imageView.setImage(image);
        imageView.setPreserveRatio(true);
        //imageView.setSmooth(true);

        setInitialImageViewSize();

        imagePane.widthProperty().addListener((value, oldValue, newValue) -> {
            imageView.setFitWidth(Math.min(imageView.getImage().getWidth(), newValue.doubleValue() - 2 * IMAGE_PADDING));

        });


        imagePane.heightProperty().addListener((value, oldValue, newValue) -> {
            imageView.setFitHeight(Math.min(imageView.getImage().getHeight(), newValue.doubleValue() - 2 * IMAGE_PADDING));

        });

        imagePane.setOnMousePressed(controller::onMousePressed);
        imagePane.setOnMouseDragged(controller::onMouseDragged);

//        imagePane.getChildren().removeAll(selectionRectangle.getNodes());
//        selectionRectangle = new SelectionRectangle();
//        imagePane.getChildren().addAll(selectionRectangle.getNodes());
        if (changeListener != null) {
            imageView.boundsInParentProperty().removeListener(changeListener);
        }

        changeListener = (observable, oldValue, newValue) -> {
            selectionRectangle.setWidth(selectionRectangle.getWidth() * newValue.getWidth() / oldValue.getWidth());
            selectionRectangle.setHeight(selectionRectangle.getHeight() * newValue.getHeight() / oldValue.getHeight());

            selectionRectangle.setX(newValue.getMinX() + (selectionRectangle.getX() - oldValue.getMinX()) * newValue.getWidth() / oldValue.getWidth());
            selectionRectangle.setY(newValue.getMinY() + (selectionRectangle.getY() - oldValue.getMinY()) * newValue.getHeight() / oldValue.getHeight());
        };

        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.brightnessProperty().bind(brightnessSlider.valueProperty());
        imageView.setEffect(colorAdjust);

//        imageView.setOnScroll(e -> {
//            double delta = e.getDeltaY();
//            imageView.setFitWidth(imageView.getFitWidth() + delta);
//            imageView.setFitHeight(imageView.getFitHeight() + delta);
//            System.out.println(delta);
//        });


        imageView.boundsInParentProperty().addListener(changeListener);
    }

    private void setInitialImageViewSize() {
        double imageWidth = imageView.getImage().getWidth();
        double imageHeight = imageView.getImage().getHeight();
        double maxAllowedWidth = imagePane.getWidth() - 2 * IMAGE_PADDING;
        double maxAllowedHeight = imagePane.getHeight() - 2 * IMAGE_PADDING;

//        System.out.println("Image Width: " + imageWidth);
//        System.out.println("Image Height: " + imageHeight);
//        System.out.println("ImageView before Width: " + imageView.getFitWidth());
//        System.out.println("ImageView before Height: " + imageView.getFitHeight());
//        System.out.println("Max Allowed Width: " + maxAllowedWidth);
//        System.out.println("Max Allowed Height: " + maxAllowedHeight);

        imageView.setFitWidth(Math.min(imageWidth, maxAllowedWidth));
        imageView.setFitHeight(Math.min(imageHeight, maxAllowedHeight));

//        System.out.println("ImageView after Width: " + imageView.getFitWidth());
//        System.out.println("ImageView after Height: " + imageView.getFitHeight());
    }

    public void setMousePressed(double x, double y) {
        mousePressed.x = x;
        mousePressed.y = y;
    }

    public double getMousePressedX() {
        return mousePressed.x;
    }

    public double getMousePressedY() {
        return mousePressed.y;
    }

    private class Vector2D {
        public double x, y;
    }
}
