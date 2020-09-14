package boundingboxeditor.ui;

import boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

class NoImageFolderOpenedBasicTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
    }

    @Test
    void onMenuItemsClicked_ShouldCorrectlyApplyVisibilityAndShowDialogueWindows(FxRobot robot) {
        verifyNodeVisibilities();
        verifyMenuBarFunctionality(robot);
    }

    private void verifyMenuBarFunctionality(FxRobot robot) {
        timeOutClickOn(robot, "File");

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem openFolderItem = getSubMenuItem(robot, "File", "Open Folder...");
        assertTrue(openFolderItem.isVisible());
        assertFalse(openFolderItem.isDisable());

        timeOutClickOn(robot, "Open Folder...");
        robot.push(KeyCode.ESCAPE);

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem saveItem = getSubMenuItem(robot, "File", "Export Annotations");
        assertTrue(saveItem.isVisible());
        assertFalse(saveItem.isDisable());

        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Export Annotations");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Pascal-VOC format...");
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage = timeOutGetTopModalStage(robot, "Save Error");
        verifyThat(categoryCreationErrorStage, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Save Error");

        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Export Annotations");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "Pascal-VOC format...");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "YOLO format...");
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage2 = timeOutGetTopModalStage(robot, "Save Error");
        verifyThat(categoryCreationErrorStage2, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage2, "OK");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Save Error");

        MenuItem exitItem = getSubMenuItem(robot, "File", "Exit");
        assertTrue(exitItem.isVisible());
        assertFalse(exitItem.isDisable());

        timeOutClickOn(robot, "View");

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem fitWindowItem = getSubMenuItem(robot, "View", "Maximize Images");
        assertTrue(fitWindowItem.isVisible());
        assertTrue(fitWindowItem.isDisable());

        MenuItem imageExplorerItem = getSubMenuItem(robot, "View", "Show Images Panel");
        assertTrue(imageExplorerItem.isVisible());
        assertTrue(imageExplorerItem.isDisable());
    }

    private void verifyNodeVisibilities() {
        verifyThat("#main-menu-bar", NodeMatchers.isVisible());
        verifyThat("#work-space", NodeMatchers.isInvisible());
        verifyThat("#status-panel", NodeMatchers.isInvisible());
    }

}
