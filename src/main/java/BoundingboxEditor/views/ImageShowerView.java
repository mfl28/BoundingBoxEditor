package BoundingboxEditor.views;

import BoundingboxEditor.Controller;
import javafx.scene.layout.BorderPane;

public class ImageShowerView extends BorderPane implements View {
    private final NavigationBarView navigationBar = new NavigationBarView();
    private final ImagePaneView imagePane = new ImagePaneView();

    ImageShowerView(){
        setTop(navigationBar);
        setCenter(imagePane);
    }


    public NavigationBarView getNavigationBar() {
        return navigationBar;
    }

    public ImagePaneView getImagePane() {
        return imagePane;
    }

    @Override
    public void connectToController(Controller controller) {
        navigationBar.connectToController(controller);
        imagePane.connectToController(controller);
    }
}
