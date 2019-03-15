package BoundingboxEditor.views;

import BoundingboxEditor.Controller;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

class TopPanelView extends VBox implements View {

    private static final String TOP_PANEL_STYLE = "top-panel";

    private final MenuBarView menuBar = new MenuBarView();
    private final Separator separator = new Separator();

    TopPanelView() {
        getChildren().addAll(menuBar, separator);
        getStyleClass().add(TOP_PANEL_STYLE);
    }

    @Override
    public void connectToController(final Controller controller) {
        menuBar.connectToController(controller);
    }

    Separator getSeparator() {
        return separator;
    }
}
