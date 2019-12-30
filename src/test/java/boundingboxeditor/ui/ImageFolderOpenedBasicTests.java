package boundingboxeditor.ui;

import boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

class ImageFolderOpenedBasicTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onImageFolderOpened_UIElementsShouldHaveCorrectState(FxRobot robot) {
        verifyNodeVisibilities();
        verifyMenuBarFunctionality(robot);
        verifyCategorySelectorState();
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

        MenuItem saveItem = getSubMenuItem(robot, "File", "Save Annotations...");
        assertTrue(saveItem.isVisible());
        assertFalse(saveItem.isDisable());

        robot.clickOn("File").clickOn("Save Annotations...");

        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(getTopModalStage(robot, "Save Error"), Matchers.notNullValue());

        robot.clickOn("OK");

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem exitItem = getSubMenuItem(robot, "File", "Exit");
        assertTrue(exitItem.isVisible());
        assertFalse(exitItem.isDisable());

        robot.clickOn("View");

        WaitForAsyncUtils.waitForFxEvents();

        CheckMenuItem fitWindowItem = (CheckMenuItem) getSubMenuItem(robot, "View", "Maximize Images");
        assertTrue(fitWindowItem.isVisible());
        assertFalse(fitWindowItem.isDisable());
        assertTrue(fitWindowItem.isSelected());

        CheckMenuItem imageExplorerItem = (CheckMenuItem) getSubMenuItem(robot, "View", "Show Images Panel");
        assertTrue(imageExplorerItem.isVisible());
        assertFalse(imageExplorerItem.isDisable());
        assertTrue(imageExplorerItem.isSelected());
    }

    private void verifyCategorySelectorState() {
        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0));
    }

    private void verifyCategorySelectorEnterNewCategoryFunctionality(FxRobot robot) {
        // Enter category without valid name
        enterNewCategory(robot, null);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(getTopModalStage(robot, "Category Creation Error"), Matchers.notNullValue());

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

        verifyThat(getTopModalStage(robot, "Category Creation Error"), Matchers.notNullValue());

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
        verifyThat(getTopModalStage(robot, "Category Creation Error"), Matchers.nullValue());

        // Renaming a category to a name that already exits
        robot.clickOn("Test").write("Dummy").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(getTopModalStage(robot, "Category Creation Error"), Matchers.notNullValue());

        robot.clickOn("OK");

        // Deleting remaining categories
        robot.clickOn("#delete-button").clickOn("#delete-button");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getBoundingBoxCategoryTable(), TableViewMatchers.hasNumRows(0));
    }
}
