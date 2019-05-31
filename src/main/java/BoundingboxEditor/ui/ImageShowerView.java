package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;

public class ImageShowerView extends BorderPane implements View {
    private static final String IMAGE_SHOWER_ID = "image-shower";

    private final ImageToolBarView navigationBar = new ImageToolBarView();
    private final ImagePaneView imagePane = new ImagePaneView();

    ImageShowerView() {
        setTop(navigationBar);
        setCenter(imagePane);
        setId(IMAGE_SHOWER_ID);

        imagePane.setMinSize(0, 0);
        setUpInternalListeners();
    }


    public ImageToolBarView getNavigationBar() {
        return navigationBar;
    }

    @Override
    public void connectToController(Controller controller) {
        navigationBar.connectToController(controller);
        imagePane.connectToController(controller);
    }

    ImagePaneView getImagePane() {
        return imagePane;
    }

    private void setUpInternalListeners() {
        ColorAdjust colorAdjust = imagePane.getColorAdjust();
        colorAdjust.brightnessProperty().bind(navigationBar.getBrightnessSlider().valueProperty());
        colorAdjust.contrastProperty().bind(navigationBar.getContrastSlider().valueProperty());
        colorAdjust.saturationProperty().bind(navigationBar.getSaturationSlider().valueProperty());
    }
}
