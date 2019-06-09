package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import BoundingboxEditor.utils.UiUtils;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

/**
 * Represents a UI-element containing navigation and image-settings controls as
 * part of a {@link ImageBoundingBoxEditorView} object.
 */
public class BoundingBoxEditorToolBarView extends ToolBar implements View {
    private static final String NEXT_BUTTON_ID = "next-button";
    private static final String NEXT_BUTTON_ICON_ID = "next-button-icon";
    private static final String PREVIOUS_BUTTON_ID = "previous-button";
    private static final String PREVIOUS_BUTTON_ICON_ID = "previous-button-icon";
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

    private static final String RESET_ALL_BUTTON_TEXT = "Reset all";

    private final Button nextButton = new IconButton(NEXT_BUTTON_ICON_ID, IconButton.IconType.GRAPHIC);
    private final Label indexLabel = new Label();
    private final Button previousButton = new IconButton(PREVIOUS_BUTTON_ICON_ID, IconButton.IconType.GRAPHIC);

    private final Slider brightnessSlider = new Slider(BRIGHTNESS_SLIDER_MIN, BRIGHTNESS_SLIDER_MAX, BRIGHTNESS_SLIDER_DEFAULT);
    private final Slider contrastSlider = new Slider(CONTRAST_SLIDER_MIN, CONTRAST_SLIDER_MAX, CONTRAST_SLIDER_DEFAULT);
    private final Slider saturationSlider = new Slider(SATURATION_SLIDER_MIN, SATURATION_SLIDER_MAX, SATURATION_SLIDER_DEFAULT);

    private final Label brightnessLabel = new Label();
    private final Label contrastLabel = new Label();
    private final Label saturationLabel = new Label();
    private final Button resetAllButton = new Button(RESET_ALL_BUTTON_TEXT);

    /**
     * Creates a new tool-bar containing controls to navigate images
     * and edit image-settings.
     */
    BoundingBoxEditorToolBarView() {
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

    /**
     * Returns the label showing the current image-file index and the
     * number of total image-files.
     *
     * @return the label
     */
    public Label getIndexLabel() {
        return indexLabel;
    }

    /**
     * Returns the button that allows to select the image with the next file-index (if existing).
     *
     * @return the button
     */
    Button getNextButton() {
        return nextButton;
    }

    /**
     * Returns the button that allows to select the image with the previous file-index (if existing).
     *
     * @return the button
     */
    Button getPreviousButton() {
        return previousButton;
    }

    /**
     * Returns the slider that allows to adjust the image-brightness.
     *
     * @return the brightness-slider
     */
    Slider getBrightnessSlider() {
        return brightnessSlider;
    }

    /**
     * Returns the slider that allows to adjust the image-contrast.
     *
     * @return the contrast-slider
     */
    Slider getContrastSlider() {
        return contrastSlider;
    }

    /**
     * Returns the slider that allows to adjust the image-saturation.
     *
     * @return the saturation-slider
     */
    Slider getSaturationSlider() {
        return saturationSlider;
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
}
