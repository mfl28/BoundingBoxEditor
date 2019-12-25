package boundingboxeditor;

import boundingboxeditor.controller.Controller;
import boundingboxeditor.model.Model;
import boundingboxeditor.ui.MainView;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.PointQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ExtendWith(ApplicationExtension.class)
public class BoundingBoxEditorTestBase {
    private static final double INITIAL_WINDOW_SCALE = 0.75;
    private static final String STYLESHEET_PATH = "/stylesheets/css/styles.css";

    protected static String TEST_IMAGE_FOLDER_PATH_1 = "/testimages/1";
    protected static String TEST_IMAGE_FOLDER_PATH_2 = "/testimages/2";

    protected Controller controller;
    protected MainView mainView;
    protected Model model;

    protected static MenuItem getSubMenuItem(FxRobot robot, String menuText, String subMenuText) {
        MenuBar menuBar = robot.lookup("#main-menu-bar").query();

        // Underscores in menu-item text are treated as mnemonics, as a convenience they are removed before checking for equality.
        Menu menu = menuBar.getMenus().stream()
                .filter(item -> item.getText() != null && item.getText().replace("_", "").equals(menuText))
                .findFirst()
                .orElseThrow();

        return menu.getItems().stream()
                .filter(item -> item.getText() != null && item.getText().replace("_", "").equals(subMenuText))
                .findFirst()
                .orElseThrow();
    }

    // Source: https://stackoverflow.com/a/48654878
    protected static Stage getTopModalStage(FxRobot robot, String title) {
        // Get a list of windows but ordered from top[0] to bottom[n] ones.
        // It is needed to get the first found modal window.
        return (Stage) robot.listWindows()
                .stream()
                .filter(window -> window instanceof Stage)
                .filter(window -> ((Stage) window).getModality() == Modality.APPLICATION_MODAL)
                .filter(stage -> ((Stage) stage).getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    protected void drawBoundingBox(FxRobot robot, Point2D startPointRatios, Point2D endPointRatios) {
        Point2D startPoint = getScreenPointFromImageViewRatios(startPointRatios);
        Point2D endPoint = getScreenPointFromImageViewRatios(endPointRatios);

        robot.moveTo(startPoint)
                .press(MouseButton.PRIMARY)
                .moveTo(endPoint)
                .release(MouseButton.PRIMARY);
    }

    protected void enterNewCategory(FxRobot robot, String categoryName) {
        robot.clickOn("#category-input-field");

        if(categoryName != null) {
            robot.write(categoryName);
        }

        robot.clickOn("#add-button");
    }

    protected void waitUntilCurrentImageIsLoaded() throws TimeoutException {
        final Image image = mainView.getCurrentImage();

        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> image != null);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, image.progressProperty().isEqualTo(1));
    }

    @Start
    protected void onStart(Stage stage) {
        Platform.setImplicitExit(false);
        controller = new Controller(stage);
        mainView = controller.getView();
        model = controller.getModel();
        // To make sure that the window is on top of all other windows at the start.
        stage.setAlwaysOnTop(true);

        final Scene scene = createSceneFromParent(mainView);
        scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

        stage.setScene(scene);
        stage.show();
        // To allow for correct testing of dialog windows (which should be in front of the main stage)
        // switch the top-positioning of the main stage off after it is shown.
        stage.setAlwaysOnTop(false);
    }

    @AfterEach
    protected void tearDown() throws TimeoutException {
        FxToolkit.hideStage();
    }

    private Point2D getScreenPointFromImageViewRatios(Point2D ratios) {
        final ImageView imageView = mainView.getBoundingBoxEditorImageView();
        final Bounds imageViewScreenBounds = imageView.localToScreen(imageView.getBoundsInLocal());

        return PointQueryUtils.atPositionFactors(imageViewScreenBounds, ratios);
    }

    private Scene createSceneFromParent(final Parent parent) {
        final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return new Scene(parent, INITIAL_WINDOW_SCALE * screenBounds.getWidth(),
                INITIAL_WINDOW_SCALE * screenBounds.getHeight());
    }
}