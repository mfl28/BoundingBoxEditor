package BoundingboxEditor;

import BoundingboxEditor.controller.Controller;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The main app-class and entry point of the application.
 * <br><br/>
 * <h2>Bounding Box Editor</h2>
 * This is an application that allows a user to load images from an image-folder which can then be
 * annotated with bounding-boxes. It is furthermore possible to import and save image-annotation data.
 * Bounding-boxes drawn or imported by the user can be tagged (e.g.: difficult, truncated, pose: frontal,
 * action: walking etc.) and nested (meaning part-whole relations can be created by dragging items of a
 * tree-like UI-element onto each other).
 */
public class BoundingBoxEditorApp extends Application {
    private static final double INITIAL_WINDOW_SCALE = 0.75;
    private static final String STYLESHEET_PATH = "/stylesheets/css/styles.css";

    /**
     * Launches the application.
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {
        final Controller controller = new Controller(primaryStage);
        final Scene scene = createSceneFromParent(controller.getView());

        scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
        scene.setOnKeyPressed(controller::onRegisterSceneKeyPressed);
        scene.setOnKeyReleased(controller::onRegisterSceneKeyReleased);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Scene createSceneFromParent(Parent parent) {
        final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return new Scene(parent, INITIAL_WINDOW_SCALE * screenBounds.getWidth(),
                INITIAL_WINDOW_SCALE * screenBounds.getHeight());
    }
}
