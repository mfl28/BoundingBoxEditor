package BoundingboxEditor;

import BoundingboxEditor.views.MainView;
import BoundingboxEditor.views.SelectionRectangle;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.PointQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.testfx.api.FxAssert.verifyThat;

@ExtendWith(ApplicationExtension.class)
public class ControllerTest extends ApplicationTest {
    private static final double WINDOW_WIDTH = 1500;
    private static final double WINDOW_HEIGHT = 900;

    private Controller controller;
    private MainView mainView;


    @BeforeAll
    static void config() throws Exception {
        // Does not seem to work with injected robots, extend ApplicationTest instead and
        // use the inherited robot.
        //System.getProperties().put("testfx.robot", "awt");
    }

    @AfterEach
    void tearDown() throws Exception {
        FxToolkit.hideStage();
        release(new KeyCode[]{});
        release(new MouseButton[]{});
    }


    @Start
    void onStart(Stage stage) {
        controller = new Controller(stage);
        mainView = controller.getView();
        final Scene scene = new Scene(mainView, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/stylesheets/styles.css").toExternalForm());
        // Add TestImages programmatically, as System Dialogue Windows (such as Directory Chooser Dialogue) cannot be interacted with in a consistent way.
        controller.updateViewFromDirectory(new File(getClass().getResource("/TestImages").toString().replace("file:", "")));
        stage.setScene(scene);
        stage.show();
    }


    @Test
    void onAddButtonClicked_WhenNoCategoryNameEntered_ShouldShowErrorDialogue(FxRobot robot) {
        robot.clickOn(mainView.getProjectSidePanel().getAddButton());
        final Stage topModalStage = getTopModalStage(robot);

        verifyThat(topModalStage, CoreMatchers.notNullValue());
        verifyThat(topModalStage.getTitle(), CoreMatchers.equalTo("Category Input Error"));
    }

    @Test
    void onClickThroughImages_WhenEndsReached_ShouldProperlySetButtonDisabledProperty(FxRobot robot) throws Exception {
        waitUntilCurrentImageIsLoaded();

        final Button nextButton = mainView.getNextButton();
        final Button previousButton = mainView.getPreviousButton();
        // forward
        verifyThat(nextButton, NodeMatchers.isEnabled());
        verifyThat(previousButton, NodeMatchers.isDisabled());

        for(int i = 0; i != 3; ++i) {
            robot.clickOn(nextButton);

            waitUntilCurrentImageIsLoaded();

            verifyThat(nextButton, NodeMatchers.isEnabled());
            verifyThat(previousButton, NodeMatchers.isEnabled());
        }

        robot.clickOn(nextButton);

        waitUntilCurrentImageIsLoaded();

        verifyThat(nextButton, NodeMatchers.isDisabled());
        verifyThat(previousButton, NodeMatchers.isEnabled());
        // end forward

        // backward
        for(int i = 0; i != 3; ++i) {
            robot.clickOn(previousButton);

            waitUntilCurrentImageIsLoaded();
            verifyThat(nextButton, NodeMatchers.isEnabled());
            verifyThat(previousButton, NodeMatchers.isEnabled());
        }

        robot.clickOn(previousButton);

        waitUntilCurrentImageIsLoaded();
        verifyThat(nextButton, NodeMatchers.isEnabled());
        verifyThat(previousButton, NodeMatchers.isDisabled());
        // end backward
    }

    @Test
    void onOpenFolderClicked_WhenNoFolderLoaded_ShouldLoadSelectedFolder(FxRobot robot) throws Exception {
        waitUntilCurrentImageIsLoaded();

        robot.clickOn(mainView.getNextButton());

        waitUntilCurrentImageIsLoaded();

        final String imagePath = mainView.getCurrentImage().getUrl();
        final String imageFileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);

        verifyThat(imageFileName, CoreMatchers.equalTo("david-barajas-733625-unsplash.jpg"));

        WaitForAsyncUtils.waitForFxEvents();

        final Point2D startPoint = getScreenPointFromImageViewRatios(0.4, 0.4);
        final Point2D intermediatePoint1 = getScreenPointFromImageViewRatios(0.8, 0.7);
        final Point2D intermediatePoint2 = getScreenPointFromImageViewRatios(0.1, 0.8);
        final Point2D intermediatePoint3 = getScreenPointFromImageViewRatios(0.1, 0.1);
        final Point2D endPoint = getScreenPointFromImageViewRatios(0.8, 0.1);

        final Bounds expectedBounds = new BoundingBox(startPoint.getX(), endPoint.getY(),
                Math.abs(endPoint.getX() - startPoint.getX()),
                Math.abs(endPoint.getY() - startPoint.getY()));

        robot.moveTo(startPoint)
                .press(MouseButton.PRIMARY)
                .moveTo(intermediatePoint1)
                .moveTo(intermediatePoint2)
                .moveTo(intermediatePoint3)
                .moveTo(endPoint)
                .release(MouseButton.PRIMARY);

        WaitForAsyncUtils.waitForFxEvents();

        // There should now be exactly one rectangle in the list.
        verifyThat(mainView.getSelectionRectangleList().size(), CoreMatchers.equalTo(1));

        final SelectionRectangle selectionRectangle = mainView.getSelectionRectangleList().get(0);
        // The selectionRectangle's category should be the same as the currently selected category in the category selector.
        verifyThat(selectionRectangle.getBoundingBoxCategory(), CoreMatchers.equalTo(mainView.getBoundingBoxItemTableView().getSelectionModel().getSelectedItem()));

        final Bounds selectionRectangleBounds = selectionRectangle.localToScreen(selectionRectangle.getBoundsInLocal());

        // Very crude tests for coordinate equality with expected values
        verifyThat(pixelApproximatelyEqual(selectionRectangle.getWidth(), expectedBounds.getWidth()), CoreMatchers.is(true));
        verifyThat(pixelApproximatelyEqual(selectionRectangle.getHeight(), expectedBounds.getHeight()), CoreMatchers.is(true));

        verifyThat(pixelApproximatelyEqual(selectionRectangleBounds.getMinX(), expectedBounds.getMinX()), CoreMatchers.is(true));
        verifyThat(pixelApproximatelyEqual(selectionRectangleBounds.getMinY(), expectedBounds.getMinY()), CoreMatchers.is(true));
    }

    @Test
    void onAddNewBoundingBoxCategory_WhenFolderLoaded_ShouldDisplayAndSelectCategoryInTableView(FxRobot robot) {
        final String testName = "Dummy";
        enterNewCategory(robot, testName);
        verifyThat(controller.getView().getBoundingBoxItemTableView().getSelectionModel().getSelectedItem().getName(), CoreMatchers.equalTo(testName));
    }

    @Test
    void onAddNewBoundingBoxCategory_WhenCategoryAlreadyExitsInTableView_ShouldDisplayErrorDialogue(FxRobot robot) {
        robot.clickOn(".bounding-box-name-text-field");

        String testName = "Dummy";
        robot.write(testName);
        robot.clickOn("#add-button");

        robot.clickOn(".bounding-box-name-text-field");

        robot.write(testName);
        robot.clickOn("#add-button");

        Stage topModalStage = getTopModalStage(robot);
        //then
        verifyThat(topModalStage, CoreMatchers.notNullValue());
        verifyThat(topModalStage.getTitle(), CoreMatchers.equalTo("Category Input Error"));
        verifyThat(controller.getView().getBoundingBoxItemTableView().getSelectionModel().getSelectedItem().getName(), CoreMatchers.equalTo(testName));
    }

    @Test
    void onDeleteSelectionRectangleInExplorer_WhenFolderLoaded_ShouldRemoveSelectionRectangleFromStoredList(FxRobot robot) throws Exception {
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        drawSelectionRectangleOnImageView(robot, new Point2D(0.1, 0.1), new Point2D(0.4, 0.4));

        WaitForAsyncUtils.waitForFxEvents();

        final List<TreeItem<SelectionRectangle>> categoryList = mainView.getProjectSidePanel().getExplorerView().getRoot().getChildren();
        // One category item in treeview
        verifyThat(categoryList.size(), CoreMatchers.equalTo(1));

        verifyThat(controller.getView().getSelectionRectangleList().size(), CoreMatchers.equalTo(1));

        final List<TreeItem<SelectionRectangle>> leafList = categoryList.get(0).getChildren();

        verifyThat(leafList.size(), CoreMatchers.equalTo(1));

        verifyThat(leafList.get(0).getValue(), CoreMatchers.equalTo(controller.getView().getSelectionRectangleList().get(0)));

        // Now delete the rectangle
        // expand the only existing category
        robot.clickOn(".tree-cell .arrow")
                .sleep(1000)
                .rightClickOn("Default1")
                .sleep(1000)
                .clickOn("Delete");

        WaitForAsyncUtils.waitForFxEvents();

        // there should be no elements in the treeview (besides the hidden root)
        verifyThat(categoryList.size(), CoreMatchers.equalTo(0));
        // The selection rectangle list should be empty
        verifyThat(controller.getView().getSelectionRectangleList().isEmpty(), CoreMatchers.equalTo(true));
    }

    @Test
    void onClickingThroughImages_WhenSelectionRectanglesCreated_ShouldCorrectlyLoadExistingSelectionRectangles(FxRobot robot) throws Exception {
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        enterNewCategory(robot, "Cat1");
        drawSelectionRectangleOnImageView(robot, new Point2D(0.1, 0.1), new Point2D(0.4, 0.4));

        final List<SelectionRectangle> startChildRectangles = mainView.getImagePaneView().getChildren().stream()
                .filter(item -> item instanceof SelectionRectangle)
                .map(item -> (SelectionRectangle) item)
                .filter(item -> item.getBoundingBoxCategory() != null)
                .collect(Collectors.toList());

        verifyThat(startChildRectangles.size(), CoreMatchers.equalTo(1));

        final SelectionRectangle expectedRectangle = startChildRectangles.get(0);

        robot.clickOn(mainView.getNextButton());
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        enterNewCategory(robot, "Cat2");
        drawSelectionRectangleOnImageView(robot, new Point2D(0.1, 0.1), new Point2D(0.4, 0.4));

        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(mainView.getNextButton());
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        enterNewCategory(robot, "Cat3");
        drawSelectionRectangleOnImageView(robot, new Point2D(0.1, 0.1), new Point2D(0.4, 0.4));

        WaitForAsyncUtils.waitForFxEvents();

        final Button previousButton = mainView.getPreviousButton();
        robot.clickOn(previousButton);
        waitUntilCurrentImageIsLoaded();

        robot.clickOn(previousButton);
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final List<TreeItem<SelectionRectangle>> categoryList = mainView.getProjectSidePanel().getExplorerView().getRoot().getChildren();
        // One category item in treeview
        verifyThat(categoryList.size(), CoreMatchers.equalTo(1));

        verifyThat(controller.getView().getSelectionRectangleList().size(), CoreMatchers.equalTo(1));

        final List<TreeItem<SelectionRectangle>> leafList = categoryList.get(0).getChildren();

        verifyThat(leafList.size(), CoreMatchers.equalTo(1));

        verifyThat(leafList.get(0).getValue().getBoundingBoxCategory().getName(), CoreMatchers.equalTo("Cat1"));

        verifyThat(mainView.getSelectionRectangleList().size(), CoreMatchers.equalTo(1));
        verifyThat(mainView.getSelectionRectangleList().get(0).getBoundingBoxCategory().getName(), CoreMatchers.equalTo("Cat1"));

        final List<SelectionRectangle> childRectangles = mainView.getImagePaneView().getChildren().stream()
                .filter(item -> item instanceof SelectionRectangle)
                .map(item -> (SelectionRectangle) item)
                .filter(item -> item.getBoundingBoxCategory() != null)
                .collect(Collectors.toList());

        verifyThat(childRectangles.size(), CoreMatchers.equalTo(1));

        final SelectionRectangle actualRectangle = childRectangles.get(0);

        verifyThat(actualRectangle, CoreMatchers.equalTo(expectedRectangle));
        verifyThat(actualRectangle, NodeMatchers.isVisible());

        // Check if rectangle is fully loaded
        verifyThat(actualRectangle.getX(), CoreMatchers.not(Double.NaN));
        verifyThat(actualRectangle.getY(), CoreMatchers.not(Double.NaN));
        verifyThat(actualRectangle.getWidth(), CoreMatchers.not(Double.NaN));
        verifyThat(actualRectangle.getHeight(), CoreMatchers.not(Double.NaN));
    }


    private boolean pixelApproximatelyEqual(double first, double second) {
        // Due to discrepancies between testfx awt/glass robot mouse positioning and the reported
        // coordinates of javafx mouse-events we'll use a crude equality check.
        return Math.abs(first - second) <= 3.0;
    }


    // source: https://stackoverflow.com/questions/48565782/testfx-how-to-test-validation-dialogs-with-no-ids
    private Stage getTopModalStage(FxRobot robot) {
        // Get a list of windows but ordered from top[0] to bottom[n] ones.
        // It is needed to get the first found modal window.
        return (Stage) robot.listWindows()
                .stream()
                .filter(window -> window instanceof Stage)
                .filter(window -> ((Stage) window).getModality() == Modality.APPLICATION_MODAL)
                .findFirst()
                .orElse(null);
    }

    private void waitUntilCurrentImageIsLoaded() throws TimeoutException {
        final Image image = mainView.getCurrentImage();

        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> image != null);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, image.progressProperty().isEqualTo(1));
    }

    private Point2D getScreenPointFromImageViewRatios(double xRatio, double yRatio) {
        final ImageView imageView = mainView.getImageView();
        final Bounds imageViewScreenBounds = imageView.localToScreen(imageView.getBoundsInLocal());

        return PointQueryUtils.atPositionFactors(imageViewScreenBounds, new Point2D(xRatio, yRatio));
    }

    private Point2D getScreenPointFromImageViewRatios(Point2D ratios) {
        final ImageView imageView = mainView.getImageView();
        final Bounds imageViewScreenBounds = imageView.localToScreen(imageView.getBoundsInLocal());

        return PointQueryUtils.atPositionFactors(imageViewScreenBounds, ratios);
    }

    private void drawSelectionRectangleOnImageView(FxRobot robot, Point2D startPointRatios, Point2D endPointRatios) {
        Point2D startPoint = getScreenPointFromImageViewRatios(startPointRatios);
        Point2D endPoint = getScreenPointFromImageViewRatios(endPointRatios);

        robot.moveTo(startPoint)
                .press(MouseButton.PRIMARY)
                .moveTo(endPoint)
                .release(MouseButton.PRIMARY);
    }

    private void enterNewCategory(FxRobot robot, String categoryName) {
        robot.clickOn(mainView.getProjectSidePanel().getCategoryInputField())
                .write(categoryName)
                .clickOn(mainView.getProjectSidePanel().getAddButton());
    }
}
