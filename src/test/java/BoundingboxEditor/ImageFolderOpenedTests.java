package BoundingboxEditor;

import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;

public class ImageFolderOpenedTests extends BoundingBoxAppTestBase {
    private static String TESTIMAGE_FOLDER_PATH = "/TestImages/MediumSizedImages";

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFilesFromDirectory(new File(getClass().getResource(TESTIMAGE_FOLDER_PATH).toString().replace("file:", "")));
    }

    @Test
    void verifyProgramStateAfterImageFolderOpened(FxRobot robot) {
        verifyNodeVisibilities();
        verifyMenuBarFunctionality(robot);
        verifyCategorySelectorState(robot);
        verifyCategorySelectorEnterNewCategoryFunctionality(robot);
    }

    private void verifyNodeVisibilities() {
        verifyThat("#main-menu-bar", NodeMatchers.isVisible());
        verifyThat("#work-space", NodeMatchers.isVisible());
        verifyThat("#status-panel", NodeMatchers.isVisible());
    }

    private void verifyMenuBarFunctionality(FxRobot robot) {
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

        CheckMenuItem fitWindowItem = (CheckMenuItem) getSubMenuItem(robot, "View", "Fit Window");
        assertTrue(fitWindowItem.isVisible());
        assertFalse(fitWindowItem.isDisable());
        assertTrue(fitWindowItem.isSelected());

        CheckMenuItem imageExplorerItem = (CheckMenuItem) getSubMenuItem(robot, "View", "Image Explorer");
        assertTrue(imageExplorerItem.isVisible());
        assertFalse(imageExplorerItem.isDisable());
        assertTrue(imageExplorerItem.isSelected());
    }

    private void verifyCategorySelectorState(FxRobot robot) {
        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0));
    }

    private void verifyCategorySelectorEnterNewCategoryFunctionality(FxRobot robot) {
        // Enter category without valid name
        enterNewCategory(robot, null);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(getTopModalStage(robot, "Category Creation Error"), CoreMatchers.notNullValue());

        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0));

        // Enter valid category name
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(1));
        verifyThat("#category-selector", TableViewMatchers.hasTableCell("Test"));

        // Enter duplicate category name
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(getTopModalStage(robot, "Category Creation Error"), CoreMatchers.notNullValue());

        robot.clickOn("OK");
        WaitForAsyncUtils.waitForFxEvents();
        // Flush text-field manually
        TextField textField = robot.lookup("#category-input-field").query();
        textField.setText("");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(1));

        // Renaming a category
        robot.clickOn("Test").write("Dummy").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasTableCell("Dummy"));

        // Entering a category with a name that previously exited but is not currently in the category-selector
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();
        // There should be no error message
        verifyThat(getTopModalStage(robot, "Category Creation Error"), CoreMatchers.nullValue());

        // Renaming a category to a name that already exits
        robot.clickOn("Test").write("Dummy").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(getTopModalStage(robot, "Category Creation Error"), CoreMatchers.notNullValue());

        robot.clickOn("OK");

        // Deleting remaining categories
        robot.clickOn("#delete-button").clickOn("#delete-button");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0));
    }

}
