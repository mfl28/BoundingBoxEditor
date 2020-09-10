package boundingboxeditor;

import boundingboxeditor.controller.Controller;
import boundingboxeditor.model.Model;
import boundingboxeditor.ui.MainView;
import boundingboxeditor.utils.MathUtils;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.PointQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ExtendWith(ApplicationExtension.class)
public class BoundingBoxEditorTestBase {
    protected static final double RATIO_EQUAL_THRESHOLD = 1e-2;
    private static final double INITIAL_WINDOW_SCALE = 0.75;
    private static final String STYLESHEET_PATH = "/stylesheets/css/styles.css";
    protected static int TIMEOUT_DURATION_IN_SEC = 60;
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

    protected static Matcher<Double[]> doubleListCloseTo(Double[] list) {
        List<Matcher<? super Double>> matchers = new ArrayList<>();
        for(double d : list) {
            matchers.add(Matchers.closeTo(d, MathUtils.DOUBLE_EQUAL_THRESHOLD));
        }
        return Matchers.arrayContaining(matchers);
    }

    protected static Matcher<Double[]> ratioListCloseTo(Double[] list) {
        List<Matcher<? super Double>> matchers = new ArrayList<>();
        for(double d : list) {
            matchers.add(Matchers.closeTo(d, RATIO_EQUAL_THRESHOLD));
        }
        return Matchers.arrayContaining(matchers);
    }

    protected void moveRelativeToImageView(FxRobot robot, Point2D startPointRatios, Point2D endPointRatios) {
        Point2D startPoint = getScreenPointFromImageViewRatios(startPointRatios);
        Point2D endPoint = getScreenPointFromImageViewRatios(endPointRatios);

        robot.moveTo(startPoint)
             .press(MouseButton.PRIMARY)
             .moveTo(endPoint)
             .release(MouseButton.PRIMARY);
    }

    protected void moveAndClickRelativeToImageView(FxRobot robot, MouseButton mousebutton, Point2D... points) {
        for(Point2D point : points) {
            robot.moveTo(getScreenPointFromImageViewRatios(point)).clickOn(mousebutton);
        }
    }

    protected void enterNewCategory(FxRobot robot, String categoryName) {
        timeOutClickOn(robot, "#category-input-field");
        WaitForAsyncUtils.waitForFxEvents();

        if(categoryName != null) {
            robot.write(categoryName);
            WaitForAsyncUtils.waitForFxEvents();
        }

        timeOutClickOn(robot, "#add-button");
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected void waitUntilCurrentImageIsLoaded() {
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> mainView.isWorkspaceVisible()),
                                      "WorkspaceSplitPane not visible within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils
                                              .waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS, () -> mainView.getCurrentImage() != null),
                                      "Image not found within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      mainView.getCurrentImage().progressProperty()
                                                                              .isEqualTo(1)),
                                      "Image not fully loaded within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        WaitForAsyncUtils.waitForFxEvents();
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
        FxToolkit.cleanupStages();
        FxToolkit.hideStage();
    }

    protected Point2D getScreenPointFromImageViewRatios(Point2D ratios) {
        final ImageView imageView = mainView.getEditorImageView();
        final Bounds imageViewScreenBounds = imageView.localToScreen(imageView.getBoundsInLocal());

        return PointQueryUtils.atPositionFactors(imageViewScreenBounds, ratios);
    }

    protected Point2D getParentPointFromImageViewRatios(Point2D ratios) {
        final ImageView imageView = mainView.getEditorImageView();
        final Bounds imageViewParentBounds = imageView.getBoundsInParent();

        return PointQueryUtils.atPositionFactors(imageViewParentBounds, ratios);
    }

    protected Point2D getLocalPointFromImageViewRatios(Point2D ratios) {
        final ImageView imageView = mainView.getEditorImageView();
        final Bounds imageViewParentBounds = imageView.getBoundsInLocal();

        return PointQueryUtils.atPositionFactors(imageViewParentBounds, ratios);
    }

    protected void timeOutLookUpInStage(FxRobot robot, Stage stage, String id) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> robot.targetWindow(stage).lookup(id)
                                                                                 .tryQuery().isPresent()),
                                      "Element with id = " + id + " was not found in scene " +
                                              stage.getTitle() + " within " + TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    protected void timeOutLookUpFrom(FxRobot robot, Node node, String id) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> robot.from(node).lookup(id).tryQuery()
                                                                                 .isPresent()),
                                      "Element with id = " + id + " was not found within " + TIMEOUT_DURATION_IN_SEC +
                                              " sec.");
    }

    protected void timeOutClickOnFrom(FxRobot robot, Node node, String id) {
        timeOutLookUpFrom(robot, node, id);
        robot.clickOn((Node) robot.from(node).lookup(id).query());
    }

    protected void timeOutLookUpInStageAndClickOn(FxRobot robot, Stage stage, String id) {
        timeOutLookUpInStage(robot, stage, id);
        robot.targetWindow(stage).clickOn(id);
    }

    protected void timeOutLookUp(FxRobot robot, String id) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> robot.lookup(id).tryQuery().isPresent()),
                                      "Element with id = " + id + " was not found within " + TIMEOUT_DURATION_IN_SEC +
                                              " sec.");
    }

    protected void timeOutLookUpNth(FxRobot robot, String id, int n) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> robot.lookup(id).nth(n).tryQuery()
                                                                                 .isPresent()),
                                      "Element with id = " + id + " and index " + n + " was not found within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    protected void timeOutClickOn(FxRobot robot, String id) {
        timeOutLookUp(robot, id);
        robot.clickOn(id);
    }

    protected void timeOutClickOnNth(FxRobot robot, String id, int n) {
        timeOutLookUpNth(robot, id, n);
        robot.clickOn((Node) robot.lookup(id).nth(n).query());
    }

    protected void timeOutMoveTo(FxRobot robot, String id) {
        timeOutLookUp(robot, id);
        robot.moveTo(id);
    }

    protected <T extends Node> void timeOutLookupAs(FxRobot robot, String id, Class<T> clazz) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> robot.lookup(id).tryQueryAs(clazz)
                                                                                 .isPresent()),
                                      "Element with id = " + id + " and class " + clazz + " was not found within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    protected <T extends Node> T timeOutQueryAs(FxRobot robot, String id, Class<T> clazz) {
        timeOutLookupAs(robot, id, clazz);
        return robot.lookup(id).queryAs(clazz);
    }

    protected void loadImageFolderAndClickDialogOption(FxRobot robot, String imageFolderPath, String optionText) {
        Platform.runLater(() -> controller
                .initiateImageFolderLoading(new File(getClass().getResource(imageFolderPath).getFile())));
        WaitForAsyncUtils.waitForFxEvents();

        Stage saveAnnotationsDialogStage = timeOutAssertDialogOpenedAndGetStage(robot, "Open image folder",
                                                                                "Opening a new image folder will remove any existing annotation data. " +
                                                                                        "Do you want to save the currently existing annotation data?");

        timeOutLookUpInStageAndClickOn(robot, saveAnnotationsDialogStage, optionText);
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected void loadImageFolderAndClickKeepCategoriesAndSaveAnnotationOptions(FxRobot robot, String imageFolderPath,
                                                                                 String keepCategoriesOption,
                                                                                 String saveAnnotationsOption) {
        Platform.runLater(() -> controller
                .initiateImageFolderLoading(new File(getClass().getResource(imageFolderPath).getFile())));
        WaitForAsyncUtils.waitForFxEvents();

        Stage keepExistingCategoriesDialogStage = timeOutAssertDialogOpenedAndGetStage(robot,
                                                                                       "Open image folder",
                                                                                       "Keep existing categories?");

        timeOutLookUpInStageAndClickOn(robot, keepExistingCategoriesDialogStage, keepCategoriesOption);
        WaitForAsyncUtils.waitForFxEvents();

        Stage saveAnnotationsDialogStage = timeOutAssertDialogOpenedAndGetStage(robot, "Open image folder",
                                                                                "Opening a new image folder will remove any existing annotation data. " +
                                                                                        "Do you want to save the currently existing annotation data?");

        timeOutLookUpInStageAndClickOn(robot, saveAnnotationsDialogStage, saveAnnotationsOption);
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected Stage timeOutAssertDialogOpenedAndGetStage(FxRobot robot, String title, String content) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> {
                                                                          Stage topModalStage =
                                                                                  getTopModalStage(robot, title);
                                                                          return topModalStage != null &&
                                                                                  topModalStage.getScene()
                                                                                               .getRoot() instanceof DialogPane &&
                                                                                  Objects.equals(
                                                                                          ((DialogPane) topModalStage
                                                                                                  .getScene().getRoot())
                                                                                                  .getContentText(),
                                                                                          content);
                                                                      }),
                                      "Expected info dialog with title \"" + title + "\" and content \"" +
                                              content + "\" did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        return getTopModalStage(robot, title);
    }

    private Scene createSceneFromParent(final Parent parent) {
        final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return new Scene(parent, INITIAL_WINDOW_SCALE * screenBounds.getWidth(),
                         INITIAL_WINDOW_SCALE * screenBounds.getHeight());
    }
}
