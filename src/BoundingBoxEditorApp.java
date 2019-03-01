import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The class representing the entry point of the application.
 */
public class BoundingBoxEditorApp extends Application {
    private static final double INITIAL_WINDOW_SCALE = 0.75;
    private static final String STYLESHEET_PATH = "stylesheets/styles.css";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        final Controller controller = new Controller(primaryStage);
        final Scene scene = createScaledSceneFromParent(controller.getView());

        scene.setOnKeyPressed(controller::handleSceneKeyPress);
        scene.getStylesheets().add(STYLESHEET_PATH);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Scene createScaledSceneFromParent(final Parent parent) {
        final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return new Scene(parent, INITIAL_WINDOW_SCALE * screenBounds.getWidth(),
                INITIAL_WINDOW_SCALE * screenBounds.getHeight());
    }
}
