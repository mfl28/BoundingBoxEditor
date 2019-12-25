package boundingboxeditor.ui;

import boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.geometry.Point2D;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.testfx.api.FxAssert.verifyThat;

class BoundingBoxTreeTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onBoundingBoxesDrawnAndInteractedWith_ShouldCorrectlyDisplayTreeItems(FxRobot robot) throws TimeoutException {
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        // Enter new category
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();

        /* ----Drawing---- */
        // Draw first bounding-box.
        drawBoundingBox(robot, new Point2D(0.25, 0.25), new Point2D(0.5, 0.5));
        WaitForAsyncUtils.waitForFxEvents();

        final List<TreeItem<BoundingBoxView>> topLevelTreeItems = mainView.getBoundingBoxTree().getRoot().getChildren();

        verifyThat(topLevelTreeItems.size(), CoreMatchers.equalTo(1));
        verifyThat(topLevelTreeItems.get(0), CoreMatchers.instanceOf(BoundingBoxCategoryTreeItem.class));

        final BoundingBoxCategoryTreeItem testCategoryTreeItem = (BoundingBoxCategoryTreeItem) topLevelTreeItems.get(0);
        verifyThat(mainView.getBoundingBoxTree().getRow(testCategoryTreeItem), CoreMatchers.equalTo(0));
        verifyThat(testCategoryTreeItem.getBoundingBoxCategory().getName(), CoreMatchers.equalTo("Test"));
        verifyThat(testCategoryTreeItem.getChildren().size(), CoreMatchers.equalTo(1));
        verifyThat(testCategoryTreeItem.getChildren().get(0), CoreMatchers.instanceOf(BoundingBoxTreeItem.class));

        final BoundingBoxTreeItem firstTestChildTreeItem = (BoundingBoxTreeItem) testCategoryTreeItem.getChildren().get(0);

        verifyThat(firstTestChildTreeItem.getValue().getBoundingBoxCategory(),
                CoreMatchers.equalTo(testCategoryTreeItem.getBoundingBoxCategory()));
        verifyThat(firstTestChildTreeItem.getId(), CoreMatchers.equalTo(1));
        verifyThat(firstTestChildTreeItem.getValue().isSelected(), CoreMatchers.equalTo(true));

        // Draw second bounding-box.
        drawBoundingBox(robot, new Point2D(0.6, 0.25), new Point2D(0.85, 0.5));
        WaitForAsyncUtils.waitForFxEvents();
        // Still there should be only one category...
        verifyThat(topLevelTreeItems.size(), CoreMatchers.equalTo(1));
        verifyThat(topLevelTreeItems.get(0), CoreMatchers.equalTo(testCategoryTreeItem));

        // ...but now there should be two child tree-items corresponding to the two drawn bounding-boxes.
        verifyThat(testCategoryTreeItem.getChildren().size(), CoreMatchers.equalTo(2));

        verifyThat(testCategoryTreeItem.getChildren().get(0), CoreMatchers.equalTo(firstTestChildTreeItem));
        verifyThat(firstTestChildTreeItem.getValue().isSelected(), CoreMatchers.equalTo(false));

        final BoundingBoxTreeItem secondTestChildTreeItem = (BoundingBoxTreeItem) testCategoryTreeItem.getChildren().get(1);

        verifyThat(secondTestChildTreeItem.getValue().getBoundingBoxCategory(),
                CoreMatchers.equalTo(testCategoryTreeItem.getBoundingBoxCategory()));
        verifyThat(secondTestChildTreeItem.getId(), CoreMatchers.equalTo(2));
        verifyThat(secondTestChildTreeItem.getValue().isSelected(), CoreMatchers.equalTo(true));

        /* ----Hiding And Showing---- */
        // Hide first bounding-box by right-clicking.
        robot.rightClickOn("Test 1").clickOn("Hide");

        verifyThat(firstTestChildTreeItem.isIconToggledOn(), CoreMatchers.equalTo(false));
        verifyThat(firstTestChildTreeItem.getValue(), NodeMatchers.isInvisible());

        // Hide second bounding-box by right-clicking.
        robot.rightClickOn("Test 2").clickOn("Hide");

        verifyThat(secondTestChildTreeItem.isIconToggledOn(), CoreMatchers.equalTo(false));
        verifyThat(secondTestChildTreeItem.getValue(), NodeMatchers.isInvisible());

        // Now the parent-category-item's square-icon should be toggled off (because all children are toggled-off.
        verifyThat(testCategoryTreeItem.isIconToggledOn(), CoreMatchers.equalTo(false));

        // Now toggle the category-item's icon to on.
        robot.clickOn(testCategoryTreeItem.getGraphic());
        verifyThat(testCategoryTreeItem.isIconToggledOn(), CoreMatchers.equalTo(true));
        // This should toggle on all child-items.
        verifyThat(firstTestChildTreeItem.isIconToggledOn(), CoreMatchers.equalTo(true));
        verifyThat(firstTestChildTreeItem.getValue(), NodeMatchers.isVisible());
        verifyThat(secondTestChildTreeItem.isIconToggledOn(), CoreMatchers.equalTo(true));
        verifyThat(secondTestChildTreeItem.getValue(), NodeMatchers.isVisible());

        /* ----Nesting---- */
        // Draw another bounding-box belonging to the Test-category.
        drawBoundingBox(robot, new Point2D(0.25, 0.6), new Point2D(0.5, 0.85));
        WaitForAsyncUtils.waitForFxEvents();

        final BoundingBoxTreeItem thirdTestChildTreeItem = (BoundingBoxTreeItem) testCategoryTreeItem.getChildren().get(2);

        // Enter new category
        enterNewCategory(robot, "Dummy");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getBoundingBoxCategoryTable().getSelectedCategory().getName(), CoreMatchers.equalTo("Dummy"));

        // Draw a bounding-box belonging to the Dummy-category
        drawBoundingBox(robot, new Point2D(0.6, 0.6), new Point2D(0.85, 0.85));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(topLevelTreeItems.size(), CoreMatchers.equalTo(2));
        verifyThat(topLevelTreeItems.get(0), CoreMatchers.equalTo(testCategoryTreeItem));
        verifyThat(topLevelTreeItems.get(1), CoreMatchers.instanceOf(BoundingBoxCategoryTreeItem.class));

        final BoundingBoxCategoryTreeItem dummyCategoryTreeItem = (BoundingBoxCategoryTreeItem) topLevelTreeItems.get(1);
        verifyThat(dummyCategoryTreeItem.getBoundingBoxCategory().getName(), CoreMatchers.equalTo("Dummy"));
        verifyThat(dummyCategoryTreeItem.getChildren().size(), CoreMatchers.equalTo(1));

        final BoundingBoxTreeItem firstDummyChildTreeItem = (BoundingBoxTreeItem) dummyCategoryTreeItem.getChildren().get(0);
        verifyThat(firstDummyChildTreeItem.getId(), CoreMatchers.equalTo(1));
        verifyThat(firstDummyChildTreeItem.getValue().isSelected(), CoreMatchers.equalTo(true));

        // Make the third child of the Test-category a nested part of the first item of the Dummy-category.
        robot.moveTo("Test 3").press(MouseButton.PRIMARY).moveTo("Dummy 1").release(MouseButton.PRIMARY);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(testCategoryTreeItem.getChildren().size(), CoreMatchers.equalTo(2));
        verifyThat(dummyCategoryTreeItem.getChildren().size(), CoreMatchers.equalTo(1));
        verifyThat(firstDummyChildTreeItem.getChildren().size(), CoreMatchers.equalTo(1));
        verifyThat(firstDummyChildTreeItem.getChildren().get(0), CoreMatchers.instanceOf(BoundingBoxCategoryTreeItem.class));
        // The dragged item should be automatically selected after the completion of a successful drag.
        verifyThat(thirdTestChildTreeItem.getValue().isSelected(), CoreMatchers.equalTo(true));

        final BoundingBoxCategoryTreeItem nestedTestCategoryTreeItem = (BoundingBoxCategoryTreeItem) firstDummyChildTreeItem.getChildren().get(0);
        verifyThat(nestedTestCategoryTreeItem.getBoundingBoxCategory(), CoreMatchers.equalTo(testCategoryTreeItem.getBoundingBoxCategory()));
        verifyThat(nestedTestCategoryTreeItem.getChildren().size(), CoreMatchers.equalTo(1));
        verifyThat(nestedTestCategoryTreeItem.getChildren().get(0), CoreMatchers.equalTo(thirdTestChildTreeItem));

        /* ----Reloading On Image Change---- */
        // Switch to the next image.
        robot.clickOn("#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();
        // Now the tree-should be empty, as no bounding-boxes have been created for the current image.
        verifyThat(mainView.getBoundingBoxTree().getRoot().getChildren().size(), CoreMatchers.equalTo(0));
        verifyThat(mainView.getCurrentBoundingBoxes().size(), CoreMatchers.equalTo(0));

        // Switch back to the previous image.
        robot.clickOn("#previous-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();
        // The old tree should have been exactly reconstructed.
        final List<TreeItem<BoundingBoxView>> newTopLevelTreeItems = mainView.getBoundingBoxTree().getRoot().getChildren();
        verifyThat(newTopLevelTreeItems, CoreMatchers.equalTo(topLevelTreeItems));

        /* ----Deleting---- */
        final BoundingBoxCategoryTreeItem newTestCategoryTreeItem = (BoundingBoxCategoryTreeItem) newTopLevelTreeItems.get(0);
        final BoundingBoxCategoryTreeItem newDummyCategoryTreeItem = (BoundingBoxCategoryTreeItem) newTopLevelTreeItems.get(1);
        final BoundingBoxTreeItem newFirstTestChildTreeItem = (BoundingBoxTreeItem) newTestCategoryTreeItem.getChildren().get(0);
        final BoundingBoxTreeItem newSecondTestChildTreeItem = (BoundingBoxTreeItem) newTestCategoryTreeItem.getChildren().get(1);

        // Delete first Test-bounding-box via context-menu on the tree-cell.
        robot.rightClickOn("Test 1").clickOn("Delete");

        // There should still be two categories...
        verifyThat(newTopLevelTreeItems.size(), CoreMatchers.equalTo(2));
        // ...but one less Test-children.
        verifyThat(newTestCategoryTreeItem.getChildren().size(), CoreMatchers.equalTo(1));

        verifyThat(mainView.getCurrentBoundingBoxes(), CoreMatchers.not(CoreMatchers.hasItem(newFirstTestChildTreeItem.getValue())));
        // After the first bounding-box and its tree-item was deleted, the (formerly) second tree-item's id should have been updated.
        verifyThat(newSecondTestChildTreeItem.getId(), CoreMatchers.equalTo(1));

        // Delete second Test-bounding-box via the context-menu on the element itself.
        robot.rightClickOn(newSecondTestChildTreeItem.getValue()).clickOn("Delete");
        WaitForAsyncUtils.waitForFxEvents();
        // Now just the Dummy-category item should be left.
        verifyThat(mainView.getBoundingBoxTree().getRoot().getChildren().size(), CoreMatchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingBoxes(), CoreMatchers.not(CoreMatchers.hasItem(newSecondTestChildTreeItem.getValue())));

        // Delete Dummy-category-item. This should delete all children recursively.
        robot.rightClickOn(newDummyCategoryTreeItem.getGraphic()).clickOn("Delete");
        WaitForAsyncUtils.waitForFxEvents();
        // Now the tree-view should be empty (besides the invisible root-item).
        verifyThat(mainView.getBoundingBoxTree().getRoot().getChildren().size(), CoreMatchers.equalTo(0));
        // There should be no remaining bounding-boxes.
        verifyThat(mainView.getCurrentBoundingBoxes().size(), CoreMatchers.equalTo(0));
    }
}
