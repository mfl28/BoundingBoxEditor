package boundingboxeditor.ui;

import boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();

        MenuItem openFolderItem = getSubMenuItem(robot, "File", "Open Folder...");
        assertTrue(openFolderItem.isVisible());
        assertFalse(openFolderItem.isDisable());

        timeOutClickOn(robot, "Open Folder...");
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem exportItem = getSubMenuItem(robot, "File", "Export Annotations");
        assertTrue(exportItem.isVisible());
        assertFalse(exportItem.isDisable());

        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Export Annotations");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Pascal-VOC format...");
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Save Error") != null),
                "Expected save error dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Stage errorDialogStage = getTopModalStage(robot, "Save Error");
        verifyThat(errorDialogStage, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, errorDialogStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Export Annotations");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "Pascal-VOC format...");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "YOLO format...");
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Save Error") != null),
                "Expected save error dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Stage errorDialogStage2 = getTopModalStage(robot, "Save Error");
        verifyThat(errorDialogStage, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, errorDialogStage2, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        MenuItem exitItem = getSubMenuItem(robot, "File", "Exit");
        assertTrue(exitItem.isVisible());
        assertFalse(exitItem.isDisable());

        timeOutClickOn(robot, "View");

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

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Category Creation Error") != null),
                "Expected category creation error dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Stage categoryCreationErrorStage = getTopModalStage(robot, "Category Creation Error");
        verifyThat(categoryCreationErrorStage, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage, "OK");
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

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Category Creation Error") != null),
                "Expected category creation error dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Stage categoryCreationErrorStage2 = getTopModalStage(robot, "Category Creation Error");
        verifyThat(categoryCreationErrorStage2, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage2, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        // Flush text-field manually
        TextField textField = robot.lookup("#category-input-field").query();
        textField.setText("");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(1));

        // Renaming a category
        timeOutClickOn(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();
        robot.write("Dummy").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasTableCell("Dummy"));

        // Entering a category with a name that previously exited but is not currently in the category-selector
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();
        // There should be no error message
        verifyThat(getTopModalStage(robot, "Category Creation Error"), Matchers.nullValue());

        // Renaming a category to a name that already exits
        timeOutClickOn(robot, "Test");
        robot.write("Dummy").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Category Creation Error") != null),
                "Expected category creation error dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Stage categoryCreationErrorStage3 = getTopModalStage(robot, "Category Creation Error");
        verifyThat(categoryCreationErrorStage3, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage3, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        // Deleting remaining categories
        timeOutClickOn(robot, "#delete-button");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#delete-button");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getObjectCategoryTable(), TableViewMatchers.hasNumRows(0));
    }
}
