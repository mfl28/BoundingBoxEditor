package BoundingboxEditor;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;

class NavigationBarView extends ToolBar implements View {
    private static final String NEXT_ICON_PATH = "/icons/arrow_right.png";
    private static final String PREVIOUS_ICON_PATH = "/icons/arrow_left.png";
    private static final String NEXT_BUTTON_ID = "next-button";
    private static final String PREVIOUS_BUTTON_ID = "previous-button";
    private static final double ICON_WIDTH = 20.0;
    private static final double ICON_HEIGHT = 20.0;

    private final Button nextButton = Utils.createIconButton(getClass().getResource(NEXT_ICON_PATH).toExternalForm(), ICON_WIDTH, ICON_HEIGHT);
    private final Label indexLabel = new Label();
    private final Button previousButton = Utils.createIconButton(getClass().getResource(PREVIOUS_ICON_PATH).toExternalForm(), ICON_WIDTH, ICON_HEIGHT);

    NavigationBarView() {
        getItems().addAll(Utils.createHSpacer(), previousButton, indexLabel, nextButton, Utils.createHSpacer());
        setVisible(false);
        setManaged(false);

        nextButton.setId(NEXT_BUTTON_ID);
        previousButton.setId(PREVIOUS_BUTTON_ID);
    }

    @Override
    public void connectToController(Controller controller) {
        nextButton.setOnAction(controller::onRegisterNextAction);
        previousButton.setOnAction(controller::onRegisterPreviousAction);
    }

    public Button getNextButton() {
        return nextButton;
    }

    public Label getIndexLabel() {
        return indexLabel;
    }

    public Button getPreviousButton() {
        return previousButton;
    }
}
