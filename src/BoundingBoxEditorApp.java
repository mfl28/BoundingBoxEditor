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
    private static final String STYLE_PATH = "stylesheets/styles.css";

    @Override
    public void start(Stage primaryStage) {
        final Controller controller = new Controller(primaryStage);
        final Scene scene = getScaledScene(controller.getView());
        scene.setOnKeyPressed(controller::handleSceneKeyPress);

        scene.getStylesheets().add(STYLE_PATH);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * From here the application is launched.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }


    private Scene getScaledScene(Parent view) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = INITIAL_WINDOW_SCALE * screenBounds.getWidth();
        double height = INITIAL_WINDOW_SCALE * screenBounds.getHeight();

        return new Scene(view, width, height);
    }
}
