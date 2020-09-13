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
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.nio.file.Files;

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
        waitUntilCurrentImageIsLoaded();
        verifyNodeVisibilities();
        verifyMenuBarFunctionality(robot);
        verifyCategorySelectorState();
        verifyCategorySelectorEnterNewCategoryFunctionality(robot);
        verifyImageSidePanelSearchFunctionality(robot);
        verifyCategorySearchFunctionality(robot);
    }

    @Test
    void onImageFolderOpened_WhenImageFileChanges_ShouldForceReloadFolder(FxRobot robot, @TempDir File tempDir) {
        waitUntilCurrentImageIsLoaded();

        final File sourceImage = model.getCurrentImageFile();

        Assertions.assertDoesNotThrow(() -> Files.copy(sourceImage.toPath(), tempDir.toPath().resolve("foo.jpg")),
                                      "Could not " +
                                              "copy image file to temporary " +
                                              "directory.");

        verifyThat(Files.isRegularFile(tempDir.toPath().resolve("foo.jpg")), Matchers.is(true));

        loadImageFolder(tempDir);

        waitUntilCurrentImageIsLoaded();

        verifyThat(model.getCurrentImageFile(), Matchers.equalTo(tempDir.toPath().resolve("foo.jpg").toFile()));

        // Verify that exactly one Image file change watcher thread is running:
        timeOutAssertThreadCount(robot, "ImageFileChangeWatcher", 1);

        // Rename the loaded image file:
        Assertions.assertDoesNotThrow(() -> Files.move(tempDir.toPath().resolve("foo.jpg"),
                                                       tempDir.toPath().resolve("bar.jpg")),
                                      "Could not rename image file in temporary directory.");

        final Stage alert = timeOutGetTopModalStage(robot, "Image files changed");
        timeOutLookUpInStageAndClickOn(robot, alert, "OK");
        timeOutAssertTopModalStageClosed(robot, "Image files changed");

        WaitForAsyncUtils.waitForFxEvents();
        waitUntilCurrentImageIsLoaded();

        verifyThat(model.getCurrentImageFile(), Matchers.equalTo(tempDir.toPath().resolve("bar.jpg").toFile()));
        timeOutAssertThreadCount(robot, "ImageFileChangeWatcher", 1);

        // Delete the loaded image file:
        Assertions.assertDoesNotThrow(() -> Files.delete(tempDir.toPath().resolve("bar.jpg")),
                                      "Could not delete image in temporary directory.");

        final Stage alert2 = timeOutGetTopModalStage(robot, "Image files changed");
        timeOutLookUpInStageAndClickOn(robot, alert2, "OK");

        timeOutAssertTopModalStageClosed(robot, "Image files changed");

        WaitForAsyncUtils.waitForFxEvents();

        final Stage errorAlert = timeOutGetTopModalStage(robot, "Error loading image folder");
        timeOutLookUpInStageAndClickOn(robot, errorAlert, "OK");

        timeOutAssertTopModalStageClosed(robot, "Error loading image folder");

        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertThreadCount(robot, "ImageFileChangeWatcher", 0);

        verifyThat(model.containsImageFiles(), Matchers.is(false));
        verifyThat(mainView.isWorkspaceVisible(), Matchers.is(false));

        loadImageFolder(tempDir);

        final Stage errorAlert1 = timeOutGetTopModalStage(robot, "Error loading image folder");
        timeOutLookUpInStageAndClickOn(robot, errorAlert1, "OK");

        timeOutAssertTopModalStageClosed(robot, "Error loading image folder");

        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertThreadCount(robot, "ImageFileChangeWatcher", 0);
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

        Stage errorDialogStage = timeOutGetTopModalStage(robot, "Save Error");
        verifyThat(errorDialogStage, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, errorDialogStage, "OK");
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

        Stage errorDialogStage2 = timeOutGetTopModalStage(robot, "Save Error");
        verifyThat(errorDialogStage2, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, errorDialogStage2, "OK");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Save Error");

        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Export Annotations");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "Pascal-VOC format...");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "JSON format...");
        WaitForAsyncUtils.waitForFxEvents();

        Stage errorDialogStage3 = timeOutGetTopModalStage(robot, "Save Error");
        verifyThat(errorDialogStage3, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, errorDialogStage3, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Save Error");

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

        Stage categoryCreationErrorStage = timeOutGetTopModalStage(robot, "Category Creation Error");
        verifyThat(categoryCreationErrorStage, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Category Creation Error");

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0));

        // Enter valid category name
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(1));
        verifyThat("#category-selector", TableViewMatchers.hasTableCell("Test"));

        // Enter duplicate category name
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage2 = timeOutGetTopModalStage(robot, "Category Creation Error");
        verifyThat(categoryCreationErrorStage2, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage2, "OK");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Category Creation Error");

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

        // Entering a category with a name that previously existed but is not currently in the category-selector
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();
        // There should be no error message
        verifyThat(getTopModalStage(robot, "Category Creation Error"), Matchers.nullValue());

        // Renaming a category to a name that already exists
        timeOutClickOn(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();
        robot.write("Dummy").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage3 = timeOutGetTopModalStage(robot, "Category Creation Error");
        verifyThat(categoryCreationErrorStage3, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage3, "OK");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Category Creation Error");

        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("Test"));

        // Renaming a category to a blank string
        timeOutClickOn(robot, "Dummy");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("Dummy"));

        timeOutClickOn(robot, "Dummy");
        robot.write("    ").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage4 = timeOutGetTopModalStage(robot, "Category Creation Error");
        verifyThat(categoryCreationErrorStage4, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage4, "OK");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Category Creation Error");

        // Deleting remaining categories
        timeOutClickOnNth(robot, "#delete-button", 1);
        WaitForAsyncUtils.waitForFxEvents();

        robot.rightClickOn();
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getObjectCategoryTable().getRowContextMenu().isShowing(), Matchers.equalTo(false));

        timeOutClickOn(robot, "#delete-button");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getObjectCategoryTable(), TableViewMatchers.hasNumRows(0));
    }

    private void verifyImageSidePanelSearchFunctionality(FxRobot robot) {
        final TextField fileSearchField = mainView.getImageFileSearchField();

        verifyThat(fileSearchField.getPromptText(), Matchers.equalTo("Search File"));
        verifyThat(fileSearchField, NodeMatchers.isNotFocused());

        robot.clickOn(fileSearchField);
        verifyThat(fileSearchField, NodeMatchers.isFocused());

        robot.write("nico");
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo("nico-bhlr-1067059-unsplash.jpg"));
        verifyThat(mainView.getCurrentImage().getUrl(), Matchers.endsWith("nico-bhlr-1067059-unsplash.jpg"));
    }

    private void verifyCategorySearchFunctionality(FxRobot robot) {
        enterNewCategory(robot, "AAA");
        enterNewCategory(robot, "ABB");
        enterNewCategory(robot, "ABC");

        final TextField categorySearchField = mainView.getCategorySearchField();
        verifyThat(categorySearchField.getPromptText(), Matchers.equalTo("Search Category"));

        robot.clickOn(categorySearchField).write("A");
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("AAA"));

        robot.write("B");
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("ABB"));

        robot.write("C");
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("ABC"));
    }
}
