package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.utils.UiUtils;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class ImageToolBarView extends ToolBar implements View {
    private static final String NEXT_BUTTON_ID = "next-button";
    private static final String PREVIOUS_BUTTON_ID = "previous-button";
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
    private static final String CUSTOM_MENU_ITEM_HBOX_STYLE = "custom-menu-item-hbox";

    private final Button nextButton = new IconButton("next-button-icon", IconButton.IconType.GRAPHIC);
    private final Label indexLabel = new Label();

    private final Button previousButton = new IconButton("previous-button-icon", IconButton.IconType.GRAPHIC);
    private final Slider brightnessSlider = new Slider(BRIGHTNESS_SLIDER_MIN, BRIGHTNESS_SLIDER_MAX, BRIGHTNESS_SLIDER_DEFAULT);
    private final Slider contrastSlider = new Slider(CONTRAST_SLIDER_MIN, CONTRAST_SLIDER_MAX, CONTRAST_SLIDER_DEFAULT);
    private final Slider saturationSlider = new Slider(SATURATION_SLIDER_MIN, SATURATION_SLIDER_MAX, SATURATION_SLIDER_DEFAULT);

    private final Label brightnessLabel = new Label();
    private final Label contrastLabel = new Label();
    private final Label saturationLabel = new Label();
    private final Button resetAllButton = new Button("Reset all");

    ImageToolBarView() {
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
        nextButton.setOnAction(action -> controller.onRegisterNextButtonClickedAction());
        previousButton.setOnAction(action -> controller.onRegisterPreviousButtonClickedAction());
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

        resetAllButton.setOnAction(event -> {
            brightnessSlider.setValue(BRIGHTNESS_SLIDER_DEFAULT);
            contrastSlider.setValue(CONTRAST_SLIDER_DEFAULT);
            saturationSlider.setValue(SATURATION_SLIDER_DEFAULT);
        });
    }

    private MenuButton createImageSettingsButton() {
        MenuButton menuButton = new MenuButton();
        menuButton.setId(IMAGE_SETTINGS_MENU_BUTTON_ID);
        menuButton.setPickOnBounds(true);

        HBox brightnessControl = new HBox(brightnessSlider, brightnessLabel);
        brightnessControl.getStyleClass().add(CUSTOM_MENU_ITEM_HBOX_STYLE);
        brightnessControl.setAlignment(Pos.CENTER);
        CustomMenuItem brightnessMenuItem = new CustomMenuItem(brightnessControl, false);

        HBox contrastControl = new HBox(contrastSlider, contrastLabel);
        contrastControl.getStyleClass().add(CUSTOM_MENU_ITEM_HBOX_STYLE);
        contrastControl.setAlignment(Pos.CENTER);
        CustomMenuItem contrastMenuItem = new CustomMenuItem(contrastControl, false);

        HBox saturationControl = new HBox(saturationSlider, saturationLabel);
        saturationControl.getStyleClass().add(CUSTOM_MENU_ITEM_HBOX_STYLE);
        saturationControl.setAlignment(Pos.CENTER);
        CustomMenuItem saturationMenuItem = new CustomMenuItem(saturationControl, false);

        CustomMenuItem resetImageEffectsItem = new CustomMenuItem(resetAllButton, false);
        resetAllButton.setFocusTraversable(false);

        menuButton.getItems().addAll(
                brightnessMenuItem,
                contrastMenuItem,
                saturationMenuItem,
                resetImageEffectsItem
        );

        menuButton.setLineSpacing(30);

        return menuButton;
    }
}
