/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.utils.UiUtils;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * Represents a UI-element containing navigation and image-settings controls as
 * part of a {@link EditorView} object.
 */
public class EditorToolBarView extends ToolBar implements View {
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
    private static final String BOUNDING_SHAPE_EDITOR_TOOLBOX_ID = "bounding-box-editor-toolbox";
    private static final String RECTANGLE_DRAWING_MODE_TOOLTIP_TEXT = "Select Rectangle Drawing-Mode";
    private static final String POLYGON_DRAWING_MODE_TOOLTIP_TEXT = "Select Polygon Drawing-Mode";
    private static final String SHOW_BOUNDING_BOXES_ICON_BUTTON_ID = "show-bounding-boxes-icon";
    private static final String HIDE_BOUNDING_BOXES_ICON_BUTTON_ID = "hide-bounding-boxes-icon";
    private static final String RESET_IMAGE_SIZE_ICON_BUTTON_ID = "reset-image-size-icon";
    private static final String RECTANGLE_MODE_BUTTON_TEXT = "Rectangle";
    private static final String POLYGON_MODE_BUTTON_TEXT = "Polygon";
    private static final String FREEHAND_MODE_BUTTON_TEXT = "Freehand";
    private static final String RECTANGLE_MODE_BUTTON_ICON_ID = "rectangle-mode-button-icon";
    private static final String FREEHAND_MODE_BUTTON_ICON_ID = "freehand-mode-button-icon";
    private static final String POLYGON_MODE_BUTTON_ICON_ID = "polygon-mode-button-icon";
    private static final String PREDICT_BUTTON_TEXT = "Predict";
    private static final String PREDICT_BUTTON_ID = "predict-button";

    private final IconButton showBoundingShapesButton =
            new IconButton(SHOW_BOUNDING_BOXES_ICON_BUTTON_ID, IconButton.IconType.BACKGROUND);
    private final IconButton hideBoundingShapesButton =
            new IconButton(HIDE_BOUNDING_BOXES_ICON_BUTTON_ID, IconButton.IconType.BACKGROUND);
    private final ToggleButton rectangleModeButton =
            createDrawModeButton(RECTANGLE_MODE_BUTTON_TEXT, RECTANGLE_MODE_BUTTON_ICON_ID);
    private final ToggleButton polygonModeButton =
            createDrawModeButton(POLYGON_MODE_BUTTON_TEXT, POLYGON_MODE_BUTTON_ICON_ID);
    private final ToggleButton freehandModeButton =
            createDrawModeButton(FREEHAND_MODE_BUTTON_TEXT, FREEHAND_MODE_BUTTON_ICON_ID);
    private final ToggleGroup modeToggleGroup = new ToggleGroup();

    private final IconButton resetSizeAndCenterImageButton =
            new IconButton(RESET_IMAGE_SIZE_ICON_BUTTON_ID, IconButton.IconType.BACKGROUND);

    private final Button nextButton = new IconButton(NEXT_BUTTON_ICON_ID, IconButton.IconType.GRAPHIC);
    private final Label indexLabel = new Label();
    private final Button previousButton = new IconButton(PREVIOUS_BUTTON_ICON_ID, IconButton.IconType.GRAPHIC);

    private final Slider brightnessSlider =
            new Slider(BRIGHTNESS_SLIDER_MIN, BRIGHTNESS_SLIDER_MAX, BRIGHTNESS_SLIDER_DEFAULT);
    private final Slider contrastSlider = new Slider(CONTRAST_SLIDER_MIN, CONTRAST_SLIDER_MAX, CONTRAST_SLIDER_DEFAULT);
    private final Slider saturationSlider =
            new Slider(SATURATION_SLIDER_MIN, SATURATION_SLIDER_MAX, SATURATION_SLIDER_DEFAULT);

    private final Label brightnessLabel = new Label();
    private final Label contrastLabel = new Label();
    private final Label saturationLabel = new Label();
    private final Button resetAllButton = new Button(RESET_ALL_BUTTON_TEXT);

    private final HBox drawingModeToolBox = new HBox(rectangleModeButton, polygonModeButton, freehandModeButton);
    private final HBox symmetryBox = new HBox();
    private final HBox boundingShapeToolBox = new HBox(hideBoundingShapesButton, showBoundingShapesButton);
    private final HBox imageSettingsToolBox = new HBox(resetSizeAndCenterImageButton, createImageSettingsButton());
    private final Button predictButton = new Button(PREDICT_BUTTON_TEXT);

    /**
     * Creates a new toolbar containing controls to navigate images
     * and change image and bounding-shape related settings.
     */
    EditorToolBarView() {
        getItems().addAll(
                boundingShapeToolBox,
                drawingModeToolBox,
                UiUtils.createHSpacer(),
                previousButton,
                indexLabel,
                nextButton,
                UiUtils.createHSpacer(),
                symmetryBox,
                predictButton,
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
        predictButton.setOnAction(action -> controller.onRegisterPerformCurrentImageBoundingBoxPredictionAction());
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

    public ToggleButton getRectangleModeButton() {
        return rectangleModeButton;
    }

    public ToggleButton getPolygonModeButton() {
        return polygonModeButton;
    }

    public ToggleButton getFreehandModeButton() {
        return freehandModeButton;
    }

    public Button getPredictButton() {
        return predictButton;
    }

    /**
     * Returns the button that allows to show all existing bounding-shapes corresponding to the current image.
     *
     * @return the button
     */
    Button getShowBoundingShapesButton() {
        return showBoundingShapesButton;
    }

    /**
     * Returns the button that allows to hide all existing bounding-shapes corresponding to the current image.
     *
     * @return the button
     */
    Button getHideBoundingShapesButton() {
        return hideBoundingShapesButton;
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

    public ToggleGroup getModeToggleGroup() {
        return modeToggleGroup;
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

    private ToggleButton createDrawModeButton(String text, String iconCssId) {
        ToggleButton button = new ToggleButton(text);

        Region icon = new Region();
        icon.setId(iconCssId);
        icon.setPickOnBounds(true);
        icon.backgroundProperty().bind(Bindings.createObjectBinding(() ->
                                                                            new Background(new BackgroundFill(
                                                                                    button.getTextFill(), null, null)),
                                                                    button.textFillProperty()));

        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.RIGHT);

        button.setFocusTraversable(false);
        button.setPickOnBounds(true);

        return button;
    }

    private void setUpButtonsAndLabels() {
        nextButton.setId(NEXT_BUTTON_ID);
        nextButton.setTooltip(UiUtils.createTooltip(NEXT_BUTTON_TOOLTIP_TEXT,
                                                    Controller.KeyCombinations.navigateNext));

        previousButton.setId(PREVIOUS_BUTTON_ID);
        previousButton.setTooltip(UiUtils.createTooltip(PREVIOUS_BUTTON_TOOLTIP_TEXT,
                                                        Controller.KeyCombinations.navigatePrevious));

        brightnessLabel.setId(BRIGHTNESS_LABEL_ID);
        brightnessLabel.setTooltip(UiUtils.createTooltip(BRIGHTNESS_LABEL_TOOLTIP));

        contrastLabel.setId(CONTRAST_LABEL_ID);
        contrastLabel.setTooltip(UiUtils.createTooltip(CONTRAST_LABEL_TOOLTIP));

        saturationLabel.setId(SATURATION_LABEL_ID);
        saturationLabel.setTooltip(UiUtils.createTooltip(SATURATION_TOOLTIP));

        boundingShapeToolBox.setId(BOUNDING_SHAPE_EDITOR_TOOLBOX_ID);
        imageSettingsToolBox.setId(BOUNDING_SHAPE_EDITOR_TOOLBOX_ID);

        showBoundingShapesButton.setTooltip(UiUtils.createTooltip(SHOW_BOUNDING_BOXES_BUTTON_TOOLTIP_TEXT,
                                                                  Controller.KeyCombinations.showAllBoundingShapes));
        hideBoundingShapesButton.setTooltip(UiUtils.createTooltip(HIDE_BOUNDING_BOXES_BUTTON_TOOLTIP,
                                                                  Controller.KeyCombinations.hideAllBoundingShapes));
        resetSizeAndCenterImageButton.setTooltip(UiUtils.createTooltip(RESET_IMAGE_SIZE_BUTTON_TOOLTIP,
                                                                       Controller.KeyCombinations.resetSizeAndCenterImage));

        rectangleModeButton.setToggleGroup(modeToggleGroup);
        polygonModeButton.setToggleGroup(modeToggleGroup);
        freehandModeButton.setToggleGroup(modeToggleGroup);

        rectangleModeButton.setTooltip(UiUtils.createTooltip(RECTANGLE_DRAWING_MODE_TOOLTIP_TEXT,
                                                             Controller.KeyCombinations.selectRectangleDrawingMode));
        polygonModeButton.setTooltip(UiUtils.createTooltip(POLYGON_DRAWING_MODE_TOOLTIP_TEXT,
                                                           Controller.KeyCombinations.selectPolygonDrawingMode));
        //TODO: add freehand tooltip


        modeToggleGroup.selectToggle(rectangleModeButton);

        predictButton.setId(PREDICT_BUTTON_ID);
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

        boundingShapeToolBox.prefWidthProperty().bind(Bindings.max(boundingShapeToolBox.widthProperty(),
                                                                   imageSettingsToolBox.widthProperty()));
        imageSettingsToolBox.prefWidthProperty().bind(Bindings.max(boundingShapeToolBox.widthProperty(),
                                                                   imageSettingsToolBox.widthProperty()));

        symmetryBox.prefWidthProperty()
                   .bind(drawingModeToolBox.widthProperty().subtract(predictButton.widthProperty()));
    }
}
