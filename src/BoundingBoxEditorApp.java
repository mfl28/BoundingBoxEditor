import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class BoundingBoxEditorApp extends Application {
    private static final double INITIAL_WINDOW_SCALE = 0.75;
    private static final String STYLE_PATH = "stylesheets/styles.css";

    @Override
    public void start(Stage primaryStage) throws Exception {
        final Controller controller = new Controller(primaryStage);
        final Scene scene = getScaledScene(controller.getView());

        scene.getStylesheets().add(STYLE_PATH);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private Scene getScaledScene(Parent view){
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        int width = (int)(INITIAL_WINDOW_SCALE * screenBounds.getWidth());
        int height = (int)(INITIAL_WINDOW_SCALE * screenBounds.getHeight());

        return new Scene(view, width, height);
    }
}
