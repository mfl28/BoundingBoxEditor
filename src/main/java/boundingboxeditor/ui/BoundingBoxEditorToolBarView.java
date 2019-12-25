package boundingboxeditor.ui;

import boundingboxeditor.controller.Controller;
import boundingboxeditor.utils.UiUtils;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

/**
 * Represents a UI-element containing navigation and image-settings controls as
 * part of a {@link BoundingBoxEditorView} object.
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

    private static final String RESET_ALL_BUTTON_TEXT = "Reset All";
    private static final String NEXT_BUTTON_TOOLTIP_TEXT = "Next";
    private static final String PREVIOUS_BUTTON_TOOLTIP_TEXT = "Previous";
    private static final String BRIGHTNESS_LABEL_TOOLTIP = "Brightness (Double-click to reset)";
    private static final String CONTRAST_LABEL_TOOLTIP = "Contrast (Double-click to reset)";
    private static final String SATURATION_TOOLTIP = "Saturation (Double-click to reset)";
    private static final String IMAGE_SETTINGS_MENU_BUTTON_TOOLTIP = "Image Settings";
    private static final String SHOW_BOUNDING_BOXES_BUTTON_TOOLTIP_TEXT = "Show all Bounding Boxes";
    private static final String HIDE_BOUNDING_BOXES_BUTTON_TOOLTIP = "Hide all Bounding Boxes";
    private static final String RESET_IMAGE_SIZE_BUTTON_TOOLTIP = "Reset Image Size and Center";
    private static final String BOUNDING_BOX_EDITOR_TOOLBOX_ID = "bounding-box-editor-toolbox";

    private final IconButton showBoundingBoxesButton = new IconButton("show-bounding-boxes-icon", IconButton.IconType.BACKGROUND);
    private final IconButton hideBoundingBoxesButton = new IconButton("hide-bounding-boxes-icon", IconButton.IconType.BACKGROUND);
    private final IconButton resetSizeAndCenterImageButton = new IconButton("reset-image-size-icon", IconButton.IconType.BACKGROUND);

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

    private final HBox boundingBoxToolBox = new HBox(hideBoundingBoxesButton, showBoundingBoxesButton);
    private final HBox imageSettingsToolBox = new HBox(resetSizeAndCenterImageButton, createImageSettingsButton());

    /**
     * Creates a new tool-bar containing controls to navigate images
     * and change image and bounding-box related settings.
     */
    BoundingBoxEditorToolBarView() {
        getItems().addAll(
                boundingBoxToolBox,
                UiUtils.createHSpacer(),
                previousButton,
                indexLabel,
                nextButton,
                UiUtils.createHSpacer(),
                imageSettingsToolBox
        );

        setId(NAVIGATION_BAR_ID);
        // Should always be on-top.
        setViewOrder(-1);
        setUpButtonsAndLabels();
        setUpInternalListeners();
    }

    @Override
    public void connectToController(Controller controller) {
        nextButton.setOnAction(action -> controller.onRegisterNextImageFileRequested());
        previousButton.setOnAction(action -> controller.onRegisterPreviousImageFileRequested());
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
     * Returns the button that allows to show all existing bounding-boxes corresponding to the current image.
     *
     * @return the button
     */
    Button getShowBoundingBoxesButton() {
        return showBoundingBoxesButton;
    }

    /**
     * Returns the button that allows to hide all existing bounding-boxes corresponding to the current image.
     *
     * @return the button
     */
    Button getHideBoundingBoxesButton() {
        return hideBoundingBoxesButton;
    }

    /**
     * Returns the button that allows to reset the image-size using the sizing-mode which is selected in
     * the main menu-bar's view-item.
     *
     * @return the button
     */
    Button getResetSizeAndCenterImageButton() {
        return resetSizeAndCenterImageButton;
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
        menuButton.setTooltip(UiUtils.createTooltip(IMAGE_SETTINGS_MENU_BUTTON_TOOLTIP));
        menuButton.setPickOnBounds(true);
        menuButton.setAlignment(Pos.CENTER);
        menuButton.setFocusTraversable(false);

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

    private void setUpButtonsAndLabels() {
        nextButton.setId(NEXT_BUTTON_ID);
        nextButton.setTooltip(UiUtils.createTooltip(NEXT_BUTTON_TOOLTIP_TEXT, Controller.KeyCombinations.navigateNext));

        previousButton.setId(PREVIOUS_BUTTON_ID);
        previousButton.setTooltip(UiUtils.createTooltip(PREVIOUS_BUTTON_TOOLTIP_TEXT, Controller.KeyCombinations.navigatePrevious));

        brightnessLabel.setId(BRIGHTNESS_LABEL_ID);
        brightnessLabel.setTooltip(UiUtils.createTooltip(BRIGHTNESS_LABEL_TOOLTIP));

        contrastLabel.setId(CONTRAST_LABEL_ID);
        contrastLabel.setTooltip(UiUtils.createTooltip(CONTRAST_LABEL_TOOLTIP));

        saturationLabel.setId(SATURATION_LABEL_ID);
        saturationLabel.setTooltip(UiUtils.createTooltip(SATURATION_TOOLTIP));

        boundingBoxToolBox.setId(BOUNDING_BOX_EDITOR_TOOLBOX_ID);
        imageSettingsToolBox.setId(BOUNDING_BOX_EDITOR_TOOLBOX_ID);

        showBoundingBoxesButton.setTooltip(UiUtils.createTooltip(SHOW_BOUNDING_BOXES_BUTTON_TOOLTIP_TEXT,
                Controller.KeyCombinations.showAllBoundingBoxes));
        hideBoundingBoxesButton.setTooltip(UiUtils.createTooltip(HIDE_BOUNDING_BOXES_BUTTON_TOOLTIP,
                Controller.KeyCombinations.hideAllBoundingBoxes));
        resetSizeAndCenterImageButton.setTooltip(UiUtils.createTooltip(RESET_IMAGE_SIZE_BUTTON_TOOLTIP,
                Controller.KeyCombinations.resetSizeAndCenterImage));
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

        boundingBoxToolBox.prefWidthProperty().bind(Bindings.max(boundingBoxToolBox.widthProperty(), imageSettingsToolBox.widthProperty()));
        imageSettingsToolBox.prefWidthProperty().bind(Bindings.max(boundingBoxToolBox.widthProperty(), imageSettingsToolBox.widthProperty()));
    }
}
