package BoundingboxEditor;

import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;

public class NoImageFolderOpenedTests extends BoundingBoxAppTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
    }

    @Test
    void onMenuItemsClicked_ShouldCorrectlyApplyVisibilityAndShowDialogueWindows(FxRobot robot) {
        verifyNodeVisiblitities();

        robot.clickOn("File");

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem openFolderItem = getSubMenuItem(robot, "File", "Open Folder...");
        assertTrue(openFolderItem.isVisible());
        assertFalse(openFolderItem.isDisable());

        robot.clickOn("Open Folder...").push(KeyCode.ESCAPE);

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem saveItem = getSubMenuItem(robot, "File", "Save...");
        assertTrue(saveItem.isVisible());
        assertFalse(saveItem.isDisable());

        robot.clickOn("File").clickOn("Save...");

        WaitForAsyncUtils.waitForFxEvents();

        Stage errorAlertStage = getTopModalStage(robot, "Save Error");
        assertNotNull(errorAlertStage);

        robot.clickOn("OK");

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem exitItem = getSubMenuItem(robot, "File", "Exit");
        assertTrue(exitItem.isVisible());
        assertFalse(exitItem.isDisable());

        robot.clickOn("View");

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem fitWindowItem = getSubMenuItem(robot, "View", "Fit Window");
        assertTrue(fitWindowItem.isVisible());
        assertTrue(fitWindowItem.isDisable());

        MenuItem imageExplorerItem = getSubMenuItem(robot, "View", "Image Explorer");
        assertTrue(imageExplorerItem.isVisible());
        assertTrue(imageExplorerItem.isDisable());
    }

    private void verifyNodeVisiblitities(){
        verifyThat("#main-menu-bar", NodeMatchers.isVisible());
        verifyThat("#work-space", NodeMatchers.isInvisible());
        verifyThat("#status-panel", NodeMatchers.isVisible());
    }

}
