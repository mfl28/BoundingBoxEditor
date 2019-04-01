package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.utils.UiUtils;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class NavigationBarView extends ToolBar implements View {
    private static final String NEXT_ICON_PATH = "/icons/arrow_right.png";
    private static final String PREVIOUS_ICON_PATH = "/icons/arrow_left.png";
    private static final String NEXT_BUTTON_ID = "next-button";
    private static final String PREVIOUS_BUTTON_ID = "previous-button";
    private static final double ICON_WIDTH = 20.0;
    private static final double ICON_HEIGHT = 20.0;
    private static final String NAVIGATION_BAR_ID = "navigation-bar";
    private static final String IMAGE_SETTINGS_MENU_BUTTON_ID = "image-settings-menu-button";
    private static final double BRIGHTNESS_SLIDER_MIN = -0.5;
    private static final double BRIGHTNESS_SLIDER_MAX = 0.5;
    private static final int BRIGHTNESS_SLIDER_DEFAULT = 0;

    private static final double CONTRAST_SLIDER_MIN = -0.5;
    private static final double CONTRAST_SLIDER_MAX = 0.5;
    private static final int CONTRAST_SLIDER_DEFAULT = 0;
    private static final String BRIGHTNESS_LABEL_ID = "brightness-label";
    private static final String CONTRAST_LABEL_ID = "contrast-label";

    private static final double SATURATION_SLIDER_MIN = -0.5;
    private static final double SATURATION_SLIDER_MAX = 0.5;
    private static final int SATURATION_SLIDER_DEFAULT = 0;
    private static final String SATURATION_LABEL_ID = "saturation-label";

    private final Button nextButton = createIconButton(NEXT_ICON_PATH);
    private final Label indexLabel = new Label();
    private final Button previousButton = createIconButton(PREVIOUS_ICON_PATH);
    private final Slider brightnessSlider = new Slider(BRIGHTNESS_SLIDER_MIN, BRIGHTNESS_SLIDER_MAX,
            BRIGHTNESS_SLIDER_DEFAULT);
    private final Slider contrastSlider = new Slider(CONTRAST_SLIDER_MIN, CONTRAST_SLIDER_MAX,
            CONTRAST_SLIDER_DEFAULT);
    private final Slider saturationSlider = new Slider(SATURATION_SLIDER_MIN, SATURATION_SLIDER_MAX,
            SATURATION_SLIDER_DEFAULT);

    private final Label brightnessLabel = new Label();
    private final Label contrastLabel = new Label();
    private final Label saturationLabel = new Label();

    NavigationBarView() {
        getItems().addAll(
                createImageSettingsButton(),
                UiUtils.createHSpacer(),
                previousButton,
                indexLabel,
                nextButton,
                UiUtils.createHSpacer());

        setUpIds();
        setUpInternalListeners();
    }

    @Override
    public void connectToController(Controller controller) {
        nextButton.setOnAction(controller::onRegisterNextButtonClickedAction);
        previousButton.setOnAction(controller::onRegisterPreviousButtonClickedAction);
    }

    public Label getIndexLabel() {
        return indexLabel;
    }

    Button getNextButton() {
        return nextButton;
    }

    Button getPreviousButton() {
        return previousButton;
    }

    Slider getBrightnessSlider() {
        return brightnessSlider;
    }

    Slider getContrastSlider() {
        return contrastSlider;
    }

    Slider getSaturationSlider() {
        return saturationSlider;
    }

    private void setUpIds() {
        setId(NAVIGATION_BAR_ID);
        nextButton.setId(NEXT_BUTTON_ID);
        previousButton.setId(PREVIOUS_BUTTON_ID);
        brightnessLabel.setId(BRIGHTNESS_LABEL_ID);
        contrastLabel.setId(CONTRAST_LABEL_ID);
        saturationLabel.setId(SATURATION_LABEL_ID);
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());

        brightnessLabel.setOnMousePressed(event -> {
            if(event.getClickCount() == 2) {
                brightnessSlider.setValue(BRIGHTNESS_SLIDER_DEFAULT);
            }
        });

        contrastLabel.setOnMousePressed(event -> {
            if(event.getClickCount() == 2) {
                contrastSlider.setValue(CONTRAST_SLIDER_DEFAULT);
            }
        });

        saturationLabel.setOnMousePressed(event -> {
            if(event.getClickCount() == 2) {
                saturationSlider.setValue(SATURATION_SLIDER_DEFAULT);
            }
        });
    }

    private MenuButton createImageSettingsButton() {
        MenuButton menuButton = new MenuButton();
        menuButton.getStyleClass().add("button");
        menuButton.setId(IMAGE_SETTINGS_MENU_BUTTON_ID);
        menuButton.setPickOnBounds(true);

        HBox brightnessControl = new HBox(brightnessSlider, brightnessLabel);
        CustomMenuItem brightnessMenuItem = new CustomMenuItem(brightnessControl);
        brightnessMenuItem.setHideOnClick(false);

        HBox contrastControl = new HBox(contrastSlider, contrastLabel);
        CustomMenuItem contrastMenuItem = new CustomMenuItem(contrastControl);
        contrastMenuItem.setHideOnClick(false);

        HBox saturationControl = new HBox(saturationSlider, saturationLabel);
        CustomMenuItem saturationMenuItem = new CustomMenuItem(saturationControl);
        saturationMenuItem.setHideOnClick(false);

        menuButton.getItems().addAll(brightnessMenuItem, contrastMenuItem, saturationMenuItem);
        menuButton.setLineSpacing(50);

        return menuButton;
    }

    private Button createIconButton(String iconResourcePath) {
        ImageView iconView = new ImageView(getClass().getResource(iconResourcePath).toExternalForm());
        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);

        Button button = new Button();
        button.setGraphic(iconView);
        button.setFocusTraversable(false);

        return button;
    }
}
