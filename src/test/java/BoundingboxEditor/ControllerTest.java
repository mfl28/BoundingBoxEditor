package BoundingboxEditor;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.ListViewMatchers;
import org.testfx.util.PointQueryUtils;
import org.testfx.util.WaitForAsyncUtils;


import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@ExtendWith(ApplicationExtension.class)
public class ControllerTest extends ApplicationTest {
    private Controller controller;


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
        final Scene scene = new Scene(controller.getView(), 1500, 900);
        scene.getStylesheets().add(getClass().getResource("/stylesheets/styles.css").toExternalForm());
        // Add TestImages programmatically, as System Dialogue Windows (such as Directory Chooser Dialogue) cannot be interacted with in a consistent way.
        controller.updateViewFromDirectory(new File(getClass().getResource("/TestImages").toString().replace("file:", "")));
        stage.setScene(scene);
        stage.show();
    }


    @Test
    void onAddButtonClicked_WhenNoCategorynameEntered_ShouldShowErrorDialogue(FxRobot robot) {
        //when
        robot.clickOn("#add-button");

        Stage topModalStage = getTopModalStage(robot);
        //then
        verifyThat(topModalStage, CoreMatchers.notNullValue());
        verifyThat(topModalStage.getTitle(), CoreMatchers.equalTo("Category Input Error"));
    }

    @ParameterizedTest(name = "Waiting {0} milliseconds between clicks")
    @ValueSource(ints = {0, 1000})
    void onClickThroughTestImages_WhenFolderLoaded_ShouldProperlySetButtonDisabledProperty(int waitMilliseconds, FxRobot robot) throws Exception{
        final String nextButtonId = "#" + TopPanelView.NEXT_BUTTON_ID;
        final String previousButtonId = "#" + TopPanelView.PREVIOUS_BUTTON_ID;

        ImageView imageView = controller.getView().getImageView();
        verifyThat(imageView, CoreMatchers.notNullValue());

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, imageView.imageProperty().isNotNull());

        // forward
        verifyThat(nextButtonId, NodeMatchers.isEnabled());
        verifyThat(previousButtonId, NodeMatchers.isDisabled());

        for(int i = 0; i != 3; ++i){
            robot.clickOn(nextButtonId).sleep(waitMilliseconds);

            verifyThat(nextButtonId, NodeMatchers.isEnabled());
            verifyThat(previousButtonId, NodeMatchers.isEnabled());
        }

        robot.clickOn(nextButtonId).sleep(waitMilliseconds);

        verifyThat(nextButtonId, NodeMatchers.isDisabled());
        verifyThat(previousButtonId, NodeMatchers.isEnabled());
        // end forward

        // backward
        for(int i = 0; i != 3; ++i){
            robot.clickOn(previousButtonId).sleep(waitMilliseconds);

            verifyThat(nextButtonId, NodeMatchers.isEnabled());
            verifyThat(previousButtonId, NodeMatchers.isEnabled());
        }

        robot.clickOn(previousButtonId).sleep(waitMilliseconds);

        verifyThat(nextButtonId, NodeMatchers.isEnabled());
        verifyThat(previousButtonId, NodeMatchers.isDisabled());
        // end backward
    }

    @Test
    void onOpenFolderClicked_WhenNoFolderLoaded_ShouldLoadSelectedFolder(FxRobot robot) throws Exception{
        verifyThat("#image-pane", CoreMatchers.notNullValue());

        ImageView imageView = controller.getView().getImageView();

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, imageView.imageProperty().isNotNull());

        Image image = imageView.getImage();

        verifyThat(image, CoreMatchers.notNullValue());

        robot.clickOn("#next-button");

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, imageView.imageProperty().isNotEqualTo(image));

        String imagePath = imageView.getImage().getUrl();
        String imageFileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);

        verifyThat(imageFileName, CoreMatchers.equalTo("david-barajas-733625-unsplash.jpg"));

        WaitForAsyncUtils.waitForFxEvents();
        Bounds testBound = imageView.localToScreen(imageView.getBoundsInLocal());

        Point2D startPoint = PointQueryUtils.atPositionFactors(testBound, new Point2D(0.4, 0.4));
        Point2D intermediatePoint1 = PointQueryUtils.atPositionFactors(testBound, new Point2D(0.8, 0.7));
        Point2D intermediatePoint2 = PointQueryUtils.atPositionFactors(testBound, new Point2D(0.1, 0.8));
        Point2D intermediatePoint3 = PointQueryUtils.atPositionFactors(testBound, new Point2D(0.1, 0.1));
        Point2D endPoint = PointQueryUtils.atPositionFactors(testBound, new Point2D(0.8, 0.1));

        Bounds expectedBounds = new BoundingBox(startPoint.getX(), endPoint.getY(),
                Math.abs(endPoint.getX() - startPoint.getX()), Math.abs(endPoint.getY() - startPoint.getY()));


        robot.moveTo(startPoint).press(MouseButton.PRIMARY)
                .moveTo(intermediatePoint1)
                .moveTo(intermediatePoint2)
                .moveTo(intermediatePoint3)
                .moveTo(endPoint)
                .release(MouseButton.PRIMARY);

        WaitForAsyncUtils.waitForFxEvents();

        // There should be exactly one rectangle in the list.
        verifyThat(controller.getView().getSelectionRectangleList().size(), CoreMatchers.equalTo(1));

        SelectionRectangle selectionRectangle = controller.getView().getSelectionRectangleList().get(0);

        verifyThat(selectionRectangle.getBoundingBoxCategory(), CoreMatchers.equalTo(controller.getView().getBoundingBoxItemTableView().getSelectionModel().getSelectedItem()));

        Bounds selectionRectangleBounds = selectionRectangle.localToScreen(selectionRectangle.getBoundsInLocal());

        verifyThat(selectionRectangle, CoreMatchers.notNullValue());

        // very crude tests
        verifyThat(pixelApproximatelyEqual(selectionRectangle.getWidth(), expectedBounds.getWidth()), CoreMatchers.is(true));
        verifyThat(pixelApproximatelyEqual(selectionRectangle.getHeight(), expectedBounds.getHeight()), CoreMatchers.is(true));

        verifyThat(pixelApproximatelyEqual(selectionRectangleBounds.getMinX(), expectedBounds.getMinX()), CoreMatchers.is(true));
        verifyThat(pixelApproximatelyEqual(selectionRectangleBounds.getMinY(), expectedBounds.getMinY()), CoreMatchers.is(true));


        // verify that the rectangle exits in the treeview
    }

    @Test
    void onAddNewBoundingBoxCategory_WhenFolderLoaded_ShouldDisplayAndSelectCategoryInTableView(FxRobot robot){
        robot.clickOn(".bounding-box-name-text-field");

        String testName = "Dummy";
        robot.write(testName);
        robot.clickOn("#add-button");

        verifyThat(controller.getView().getBoundingBoxItemTableView().getSelectionModel().getSelectedItem().getName(), CoreMatchers.equalTo(testName));
    }

    @Test
    void onAddNewBoundingBoxCategory_WhenCategoryAlreadyExitsInTableView_ShouldDisplayErrorDialogue(FxRobot robot){
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
    void onDeleteSelectionRectangleInExplorer_WhenFolderLoaded_ShouldRemoveSelectionRectangleFromStoredList(FxRobot robot) throws Exception{
        ImageView imageView = controller.getView().getImageView();

        verifyThat(imageView, CoreMatchers.notNullValue());

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, imageView.imageProperty().isNotNull());

        WaitForAsyncUtils.waitForFxEvents();
        Bounds testBound = imageView.localToScreen(imageView.getBoundsInLocal());

        Point2D startPoint = PointQueryUtils.atPositionFactors(testBound, new Point2D(0.1, 0.1));

        Point2D endPoint = PointQueryUtils.atPositionFactors(testBound, new Point2D(0.4, 0.4));

        robot.moveTo(startPoint).press(MouseButton.PRIMARY)
                .moveTo(endPoint)
                .release(MouseButton.PRIMARY);

        WaitForAsyncUtils.waitForFxEvents();

        TreeView<SelectionRectangle> explorer = controller.getView().getProjectSidePanel().getExplorerView();

        List<TreeItem<SelectionRectangle>> categoryList = explorer.getRoot().getChildren();

        // One category item in treeview
        verifyThat(categoryList.size(), CoreMatchers.equalTo(1));

        verifyThat(controller.getView().getSelectionRectangleList().size(), CoreMatchers.equalTo(1));

        List<TreeItem<SelectionRectangle>> leafList = categoryList.get(0).getChildren();

        verifyThat(leafList.size(), CoreMatchers.equalTo(1));

        verifyThat(leafList.get(0).getValue(), CoreMatchers.equalTo(controller.getView().getSelectionRectangleList().get(0)));

        // Now delete the rectangle
        // expand the only existing category
        robot.clickOn(".tree-cell > .tree-disclosure-node > .arrow");
        robot.sleep(1000);
        robot.rightClickOn("Default1");
        robot.sleep(1000);
        robot.clickOn("Delete");

        WaitForAsyncUtils.waitForFxEvents();

        // there should be no elements in the treeview (besides the hidden root)
        verifyThat(categoryList.size(), CoreMatchers.equalTo(0));
        // The selection rectangle list should be empty
        verifyThat(controller.getView().getSelectionRectangleList().isEmpty(), CoreMatchers.equalTo(true));

    }


    private boolean pixelApproximatelyEqual(double first, double second){
        // Due to discrepancies between testfx awt/glass robot mouse positioning and the reported
        // coordinates of javafx mouse-events we'll use a crude equality check.
        return Math.abs(first - second) <= 2.0;
    }


    // source: https://stackoverflow.com/questions/48565782/testfx-how-to-test-validation-dialogs-with-no-ids
    private Stage getTopModalStage(FxRobot robot) {
        // Get a list of windows but ordered from top[0] to bottom[n] ones.
        // It is needed to get the first found modal window.
        final List<Window> allWindows = robot.listWindows();

        return (Stage) allWindows
                .stream()
                .filter(window -> window instanceof Stage)
                .filter(window -> ((Stage) window).getModality() == Modality.APPLICATION_MODAL)
                .findFirst()
                .orElse(null);
    }
}
