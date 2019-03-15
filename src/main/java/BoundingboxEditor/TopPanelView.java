package BoundingboxEditor;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;

class TopPanelView extends VBox implements View {

    private static final String TOP_PANEL_STYLE = "topBox";
    private final Separator separator = new Separator();

    private final MenuBarView menuBar = new MenuBarView();
    //private final NavigationBarView navigationBar = new NavigationBarView();

    TopPanelView() {
        getChildren().addAll(menuBar, separator);
        getStyleClass().add(TOP_PANEL_STYLE);
    }

//    public ToolBar getNavigationBar() {
//        return navigationBar;
//    }
//
//    public Button getNextButton() {
//        return navigationBar.getNextButton();
//    }
//
//    public Button getPreviousButton() {
//        return navigationBar.getPreviousButton();
//    }
//
//    public Label getIndexLabel() {
//        return navigationBar.getIndexLabel();
//    }


    public Separator getSeparator() {
        return separator;
    }

    @Override
    public void connectToController(final Controller controller) {
        menuBar.connectToController(controller);
//        navigationBar.connectToController(controller);
    }
}
