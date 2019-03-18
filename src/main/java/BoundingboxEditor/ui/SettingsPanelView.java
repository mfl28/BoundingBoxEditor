package BoundingboxEditor.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsPanelView extends VBox implements View {
    private static final int ZOOM_SLIDER_MIN = 1;
    private static final double ZOOM_SLIDER_MAX = 1.5;
    private static final int ZOOM_SLIDER_DEFAULT = 1;
    private static final String SETTINGS_BOX_STYLE = "settings-box";
    private static final String ZOOM_ICON_PATH = "/icons/zoom.png";
    private static final String BRIGHTNESS_ICON_PATH = "/icons/brightness.png";
    private static final String IMAGE_SETTINGS_LABEL_TEXT = "Image";
    private static final String SETTINGS_ITEM_STYLE = "settings-item-box";
    private static final double BRIGHTNESS_SLIDER_MIN = -0.5;
    private static final double BRIGHTNESS_SLIDER_MAX = 0.5;
    private static final int BRIGHTNESS_SLIDER_DEFAULT = 0;
    private static final int SIDE_PANEL_SPACING = 5;
    private static final int SETTINGS_ITEM_SPACING = 10;
    private static final double ICON_WIDTH = 20.0;
    private static final double ICON_HEIGHT = 20.0;

    private final Slider zoomSlider = new Slider(ZOOM_SLIDER_MIN, ZOOM_SLIDER_MAX, ZOOM_SLIDER_DEFAULT);
    private final Label brightnessLabel = createIconLabel(BRIGHTNESS_ICON_PATH);
    private final Slider brightnessSlider = new Slider(BRIGHTNESS_SLIDER_MIN, BRIGHTNESS_SLIDER_MAX, BRIGHTNESS_SLIDER_DEFAULT);

    SettingsPanelView() {
        this.getStyleClass().add(SETTINGS_BOX_STYLE);

        final Label imageSettingsLabel = new Label(IMAGE_SETTINGS_LABEL_TEXT);

        final Label zoomLabel = createIconLabel(ZOOM_ICON_PATH);


        final HBox zoomHBox = new HBox(zoomLabel, zoomSlider);
        zoomHBox.getStyleClass().add(SETTINGS_ITEM_STYLE);
        zoomHBox.setSpacing(SETTINGS_ITEM_SPACING);

        final HBox brightnessHBox = new HBox(brightnessLabel, brightnessSlider);
        brightnessHBox.getStyleClass().add(SETTINGS_ITEM_STYLE);
        brightnessHBox.setSpacing(SETTINGS_ITEM_SPACING);

        this.getChildren().addAll(new Separator(),
                imageSettingsLabel, zoomHBox, brightnessHBox, new Separator());
        this.setSpacing(SIDE_PANEL_SPACING);
    }

    public Slider getZoomSlider() {
        return zoomSlider;
    }

    public Label getBrightnessLabel() {
        return brightnessLabel;
    }

    public Slider getBrightnessSlider() {
        return brightnessSlider;
    }

    private Label createIconLabel(final String iconPath) {
        final Label label = new Label();
        final ImageView iconView = new ImageView(getClass().getResource(iconPath).toExternalForm());

        iconView.setFitWidth(ICON_WIDTH);
        iconView.setFitHeight(ICON_HEIGHT);
        iconView.setPreserveRatio(true);
        label.setGraphic(iconView);

        return label;
    }
}
