/*
 * Copyright (C) 2021 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.model.Model;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import com.github.mfl28.boundingboxeditor.ui.MainView;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Worker;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.DebugUtils;
import org.testfx.util.NodeQueryUtils;
import org.testfx.util.PointQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.testfx.api.FxAssert.verifyThat;

@ExtendWith(ApplicationExtension.class)
public class BoundingBoxEditorTestBase {
    protected static final double RATIO_EQUAL_THRESHOLD = 1e-2;
    private static final double INITIAL_WINDOW_SCALE = 0.75;
    private static final String STYLESHEET_PATH = "/stylesheets/css/styles.css";
    private static final Path SCREENSHOT_PATH = Paths.get("").toAbsolutePath().resolve(
            "build/test-screenshots/");
    private static final String FULL_SCREEN_TESTS_SYSTEM_PROPERTY_NAME = "fullScreenTests";
    protected static int TIMEOUT_DURATION_IN_SEC = 30;
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
        return robot.listWindows()
                    .stream()
                    .filter(window -> window instanceof Stage)
                    .map(window -> (Stage) window)
                    .filter(stage -> stage.getModality() == Modality.APPLICATION_MODAL)
                    .filter(stage -> stage.getTitle().equals(title))
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

    protected void enterNewCategory(FxRobot robot, String categoryName, TestInfo testinfo) {
        timeOutClickOn(robot, "#category-input-field", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        if(categoryName != null) {
            robot.write(categoryName);
            WaitForAsyncUtils.waitForFxEvents();
        }

        timeOutClickOn(robot, "#add-button", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected void waitUntilCurrentImageIsLoaded(TestInfo testinfo) {
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> mainView.isWorkspaceVisible()),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "WorkspaceSplitPane not visible within" +
                                                                                   " " + TIMEOUT_DURATION_IN_SEC +
                                                                                   " sec" +
                                                                                   "."));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils
                                              .waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS, () -> mainView.getCurrentImage() != null),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Image not found within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      mainView.getCurrentImage().progressProperty()
                                                                              .isEqualTo(1)),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Image not fully loaded within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));

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
        stage.setMaximized(Boolean.getBoolean(FULL_SCREEN_TESTS_SYSTEM_PROPERTY_NAME));

        final Scene scene = createSceneFromParent(mainView);
        scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

        stage.setScene(scene);
        stage.show();
        // To allow for correct testing of dialog windows (which should be in front of the main stage)
        // switch the top-positioning of the main stage off after it is shown.
        stage.setAlwaysOnTop(false);

        // Set up image screenshot directory:
        final File screenShotDirectory = SCREENSHOT_PATH.toFile();

        if(!screenShotDirectory.isDirectory()) {
            if(!screenShotDirectory.mkdir()) {
                throw new RuntimeException("Could not create test-screenshot directory.");
            }
        }
    }

    @AfterEach
    protected void tearDown() throws TimeoutException {
        FxToolkit.cleanupStages();
        FxToolkit.hideStage();
        // Make sure FileChangeWatcher is interrupted.
        Thread[] list = new Thread[Thread.activeCount()];
        Thread.currentThread().getThreadGroup().enumerate(list);
        Arrays.stream(list).filter(thread -> thread != null && thread.getName().equals("ImageFileChangeWatcher"))
              .forEach(Thread::interrupt);
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

    protected void timeOutLookUpInStage(FxRobot robot, Stage stage, String id, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> nodePresentAndVisibleInStage(robot, stage
                                                                              , id)),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Element with id = " + id + " was not " +
                                                                                   "found " +
                                                                                   "in scene " +
                                                                                   stage.getTitle() + " within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    protected void timeOutLookUpInStageAndClickOn(FxRobot robot, Stage stage, String id, TestInfo testinfo) {
        timeOutLookUpInStage(robot, stage, id, testinfo);
        robot.targetWindow(stage).clickOn(id);
    }

    protected void timeOutClickOnButtonInDialogStage(FxRobot robot, Stage stage, ButtonType buttonType,
                                                     TestInfo testinfo) {
        final Node buttonNode = timeOutLookUpAndGetButtonInDialogStage(stage, buttonType, testinfo);
        verifyThat(buttonNode, Matchers.notNullValue());
        verifyThat(buttonNode, NodeMatchers.isVisible());

        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn(buttonNode);
    }

    protected Node timeOutLookUpAndGetButtonInDialogStage(Stage stage, ButtonType buttonType,
                                                          TestInfo testinfo) {
        verifyThat(stage.getScene().getRoot(), Matchers.instanceOf(DialogPane.class), saveScreenshot(testinfo));

        final DialogPane dialogPane = (DialogPane) stage.getScene().getRoot();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> dialogPane.lookupButton(buttonType) !=
                                                                              null &&
                                                                              dialogPane.lookupButton(buttonType)
                                                                                        .isVisible()),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Button was not visible " +
                                                                                   " within " +
                                                                                   TIMEOUT_DURATION_IN_SEC +
                                                                                   " sec."));

        return dialogPane.lookupButton(buttonType);
    }

    protected void timeOutLookUp(FxRobot robot, String id, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> nodePresentAndVisible(robot, id)),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Element with id = " + id + " was not " +
                                                                                   "found within " +
                                                                                   TIMEOUT_DURATION_IN_SEC +
                                                                                   " sec."));
    }

    protected void timeOutLookUpNth(FxRobot robot, String id, int n, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> nodePresentAndVisibleNth(robot, id, n)),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Element with id = " + id + " and " +
                                                                                   "index " + n +
                                                                                   " was not found within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    protected void timeOutClickOn(FxRobot robot, String id, TestInfo testinfo) {
        timeOutLookUp(robot, id, testinfo);
        robot.clickOn(id);
    }

    protected void timeOutClickOnNth(FxRobot robot, String id, int n, TestInfo testinfo) {
        timeOutLookUpNth(robot, id, n, testinfo);
        robot.clickOn((Node) robot.lookup(id).nth(n).query());
    }

    protected void timeOutMoveTo(FxRobot robot, String id, TestInfo testinfo) {
        timeOutLookUp(robot, id, testinfo);
        robot.moveTo(id);
    }

    protected <T extends Node> void timeOutLookupAs(FxRobot robot, String id, Class<T> clazz, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> nodePresentAndVisibleAs(robot, id, clazz)),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Element with id = " + id + " and " +
                                                                                   "class " + clazz +
                                                                                   " was not found within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    protected <T extends Node> T timeOutQueryAs(FxRobot robot, String id, Class<T> clazz, TestInfo testinfo) {
        timeOutLookupAs(robot, id, clazz, testinfo);
        return robot.lookup(id).queryAs(clazz);
    }

    protected void loadImageFolder(String imageFolderPath) {
        Platform.runLater(() -> controller
                .initiateImageFolderLoading(new File(getClass().getResource(imageFolderPath).getFile())));
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected void loadImageFolder(File imageFolderFile) {
        Platform.runLater(() -> controller
                .initiateImageFolderLoading(imageFolderFile));
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected void loadImageFolderAndClickKeepCategoriesAndSaveAnnotationOptions(FxRobot robot, String imageFolderPath,
                                                                                 String keepCategoriesOption,
                                                                                 String saveAnnotationsOption,
                                                                                 TestInfo testinfo) {
        loadImageFolder(imageFolderPath);

        Stage keepExistingCategoriesDialogStage = timeOutAssertDialogOpenedAndGetStage(robot,
                                                                                       "Open Image Folder",
                                                                                       "Keep existing categories?",
                                                                                       testinfo);

        timeOutLookUpInStageAndClickOn(robot, keepExistingCategoriesDialogStage, keepCategoriesOption, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Stage saveAnnotationsDialogStage = timeOutAssertDialogOpenedAndGetStage(robot, "Open Image Folder",
                                                                                "Opening a new image folder will remove any existing annotation data. " +
                                                                                        "Do you want to save the " +
                                                                                        "currently existing " +
                                                                                        "annotation data?", testinfo);

        timeOutLookUpInStageAndClickOn(robot, saveAnnotationsDialogStage, saveAnnotationsOption, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected Stage timeOutAssertDialogOpenedAndGetStage(FxRobot robot, String title, String content,
                                                         TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> {
                                                                          Stage topModalStage =
                                                                                  getTopModalStage(robot, title);
                                                                          return topModalStage != null &&
                                                                                  topModalStage.isShowing() &&
                                                                                  topModalStage.getScene()
                                                                                               .getRoot() instanceof DialogPane &&
                                                                                  Objects.equals(
                                                                                          ((DialogPane) topModalStage
                                                                                                  .getScene().getRoot())
                                                                                                  .getContentText(),
                                                                                          content);
                                                                      }),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected info dialog with title \"" +
                                                                                   title +
                                                                                   "\" and content \"" +
                                                                                   content + "\" did not open within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));

        return getTopModalStage(robot, title);
    }

    protected Stage timeOutGetTopModalStage(FxRobot robot, String stageTitle, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> {
                                                                          final Stage stage =
                                                                                  getTopModalStage(robot, stageTitle);
                                                                          return stage != null && stage.isShowing();
                                                                      }),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected top modal stage with title " +
                                                                                   stageTitle + " did not open within "
                                                                                   + TIMEOUT_DURATION_IN_SEC +
                                                                                   " sec."));
        Stage stage = getTopModalStage(robot, stageTitle);
        verifyThat(stage, Matchers.notNullValue());
        verifyThat(stage.isShowing(), Matchers.is(true));

        return stage;
    }

    protected void timeOutAssertTopModalStageClosed(FxRobot robot, String stageTitle, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> getTopModalStage(robot, stageTitle) ==
                                                                              null),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected top modal stage with title " +
                                                                                   stageTitle + " did not close within "
                                                                                   + TIMEOUT_DURATION_IN_SEC +
                                                                                   " sec."));
    }

    protected void timeOutAssertNoTopModelStage(FxRobot robot, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> !isTopModalStagePresent(robot)),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Could find top modal stage within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + "  sec."));
    }

    protected void timeOutAssertThreadCount(String threadName, int count, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> threadCount(threadName) == count),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected count for thread " + threadName +
                                                                                   " was not reached within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    protected void timeOutAssertServiceSucceeded(Service<? extends IOResult> service,
                                                 TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> WaitForAsyncUtils.asyncFx(
                                                                              () -> service.getState()
                                                                                           .equals(
                                                                                                   Worker.State.SUCCEEDED))
                                                                                             .get()),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Service did not succeed within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    protected long threadCount(String threadName) {
        final Thread[] currentThreads = new Thread[Thread.activeCount()];
        Thread.currentThread().getThreadGroup().enumerate(currentThreads);
        return Arrays.stream(currentThreads).filter(thread -> thread.getName().equals(threadName)).count();
    }

    protected Function<StringBuilder, StringBuilder> saveScreenshot(TestInfo testInfo) {
        return DebugUtils
                .saveScreenshot(SCREENSHOT_PATH.resolve(testInfo.getTestMethod().orElseThrow().getName()).toString());
    }

    protected String saveScreenshotAndReturnMessage(TestInfo testinfo, String message) {
        return DebugUtils.saveScreenshot(SCREENSHOT_PATH.resolve(testinfo.getTestMethod()
                                                                         .orElseThrow()
                                                                         .getName()).toString())
                         .apply(new StringBuilder(message)).toString();
    }

    protected List<IOErrorInfoEntry> timeOutGetErrorInfoEntriesFromStage(Stage errorReportStage, TestInfo testinfo) {
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class),
                   saveScreenshot(testinfo));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0),
                   Matchers.instanceOf(TableView.class), saveScreenshot(testinfo));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class),
                   saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        return errorInfoTable.getItems();
    }

    private boolean nodePresentAndVisibleInStage(FxRobot robot, Stage stage, String id) {
        Optional<Node> queryResult = robot.targetWindow(stage).lookup(id).tryQuery();

        return queryResult.isPresent() && NodeQueryUtils.isVisible().test(queryResult.get());
    }

    private boolean nodePresentAndVisible(FxRobot robot, String id) {
        Optional<Node> queryResult = robot.lookup(id).tryQuery();

        return queryResult.isPresent() && NodeQueryUtils.isVisible().test(queryResult.get());
    }

    private boolean nodePresentAndVisibleNth(FxRobot robot, String id, int n) {
        Optional<Node> queryResult = robot.lookup(id).nth(n).tryQuery();

        return queryResult.isPresent() && NodeQueryUtils.isVisible().test(queryResult.get());
    }

    private <T extends Node> boolean nodePresentAndVisibleAs(FxRobot robot, String id, Class<T> clazz) {
        Optional<T> queryResult = robot.lookup(id).tryQueryAs(clazz);

        return queryResult.isPresent() && NodeQueryUtils.isVisible().test(queryResult.get());
    }

    private boolean isTopModalStagePresent(FxRobot robot) {
        return robot.listWindows()
                    .stream()
                    .filter(window -> window instanceof Stage)
                    .map(window -> (Stage) window)
                    .anyMatch(stage -> stage.getModality() == Modality.APPLICATION_MODAL);
    }

    private Scene createSceneFromParent(final Parent parent) {
        final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return new Scene(parent, INITIAL_WINDOW_SCALE * screenBounds.getWidth(),
                         INITIAL_WINDOW_SCALE * screenBounds.getHeight());
    }
}
