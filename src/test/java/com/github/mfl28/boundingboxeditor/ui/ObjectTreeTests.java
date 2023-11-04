/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.geometry.Point2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.hamcrest.io.FileMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.ComboBoxMatchers;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.mockito.Mockito.verify;
import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class ObjectTreeTests extends BoundingBoxEditorTestBase {

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onBoundingBoxesDrawnAndInteractedWith_ShouldCorrectlyDisplayTreeItems(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        // Enter new category
        enterNewCategory(robot, "Test", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getTagInputField().isDisabled(), Matchers.is(true), saveScreenshot(testinfo));

        /* ----Drawing---- */
        // Draw first bounding-box.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.5, 0.5));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getTagInputField().isDisabled(), Matchers.is(false), saveScreenshot(testinfo));

        final List<TreeItem<Object>> topLevelTreeItems = mainView.getObjectTree().getRoot().getChildren();

        verifyThat(topLevelTreeItems.size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(topLevelTreeItems.get(0), Matchers.instanceOf(ObjectCategoryTreeItem.class),
                   saveScreenshot(testinfo));

        final ObjectCategoryTreeItem testCategoryTreeItem = (ObjectCategoryTreeItem) topLevelTreeItems.get(0);
        verifyThat(mainView.getObjectTree().getRow(testCategoryTreeItem), Matchers.equalTo(0),
                   saveScreenshot(testinfo));
        verifyThat(testCategoryTreeItem.getObjectCategory().getName(), Matchers.equalTo("Test"),
                   saveScreenshot(testinfo));
        verifyThat(testCategoryTreeItem.getChildren().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(testCategoryTreeItem.getChildren().get(0), Matchers.instanceOf(BoundingBoxTreeItem.class),
                   saveScreenshot(testinfo));

        final BoundingBoxTreeItem firstTestChildTreeItem =
                (BoundingBoxTreeItem) testCategoryTreeItem.getChildren().get(0);

        verifyThat(((BoundingBoxView) firstTestChildTreeItem.getValue()).getObjectCategory(),
                   Matchers.equalTo(testCategoryTreeItem.getObjectCategory()));
        verifyThat(firstTestChildTreeItem.getId(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(((BoundingBoxView) firstTestChildTreeItem.getValue()).isSelected(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));

        // Draw second bounding-box.
        moveRelativeToImageView(robot, new Point2D(0.6, 0.25), new Point2D(0.85, 0.5));
        WaitForAsyncUtils.waitForFxEvents();
        // Still there should be only one category...
        verifyThat(topLevelTreeItems.size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(topLevelTreeItems.get(0), Matchers.equalTo(testCategoryTreeItem), saveScreenshot(testinfo));

        // ...but now there should be two child tree-items corresponding to the two drawn bounding-boxes.
        verifyThat(testCategoryTreeItem.getChildren().size(), Matchers.equalTo(2), saveScreenshot(testinfo));

        verifyThat(testCategoryTreeItem.getChildren().get(0), Matchers.equalTo(firstTestChildTreeItem),
                   saveScreenshot(testinfo));
        verifyThat(((BoundingBoxView) firstTestChildTreeItem.getValue()).isSelected(), Matchers.equalTo(false),
                   saveScreenshot(testinfo));

        final BoundingBoxTreeItem secondTestChildTreeItem =
                (BoundingBoxTreeItem) testCategoryTreeItem.getChildren().get(1);

        verifyThat(((BoundingBoxView) secondTestChildTreeItem.getValue()).getObjectCategory(),
                   Matchers.equalTo(testCategoryTreeItem.getObjectCategory()));
        verifyThat(secondTestChildTreeItem.getId(), Matchers.equalTo(2), saveScreenshot(testinfo));
        verifyThat(((BoundingBoxView) secondTestChildTreeItem.getValue()).isSelected(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));

        /* ----Hiding And Showing---- */
        // Hide first bounding-box by right-clicking.
        robot.rightClickOn("Test 1");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Hide", testinfo);

        verifyThat(firstTestChildTreeItem.isIconToggledOn(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat((BoundingBoxView) firstTestChildTreeItem.getValue(), NodeMatchers.isInvisible(),
                   saveScreenshot(testinfo));

        // Hide second bounding-box by clicking on its hide toggle.
        verifyThat(secondTestChildTreeItem.toggleIcon, Matchers.instanceOf(ToggleSquare.class),
                   saveScreenshot(testinfo));
        robot.clickOn((ToggleSquare) secondTestChildTreeItem.toggleIcon);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(secondTestChildTreeItem.isIconToggledOn(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat((BoundingBoxView) secondTestChildTreeItem.getValue(), NodeMatchers.isInvisible(),
                   saveScreenshot(testinfo));

        // Now the parent-category-item's square-icon should be toggled off (because all children are toggled-off.
        verifyThat(testCategoryTreeItem.isIconToggledOn(), Matchers.equalTo(false), saveScreenshot(testinfo));

        // Now toggle the category-item's icon to on.
        robot.clickOn(testCategoryTreeItem.getGraphic());
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(testCategoryTreeItem.isIconToggledOn(), Matchers.equalTo(true), saveScreenshot(testinfo));
        // This should toggle on all child-items.
        verifyThat(firstTestChildTreeItem.isIconToggledOn(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat((BoundingBoxView) firstTestChildTreeItem.getValue(), NodeMatchers.isVisible(),
                   saveScreenshot(testinfo));
        verifyThat(secondTestChildTreeItem.isIconToggledOn(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat((BoundingBoxView) secondTestChildTreeItem.getValue(), NodeMatchers.isVisible(),
                   saveScreenshot(testinfo));

        // Hide all except Test 1
        robot.rightClickOn("Test 1");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Hide others", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(firstTestChildTreeItem.isIconToggledOn(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(secondTestChildTreeItem.isIconToggledOn(), Matchers.is(false), saveScreenshot(testinfo));

        // Show all via context menu
        robot.rightClickOn("Test 1");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Show all", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(firstTestChildTreeItem.isIconToggledOn(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(secondTestChildTreeItem.isIconToggledOn(), Matchers.is(true), saveScreenshot(testinfo));

        // Hide all via context menu
        robot.rightClickOn("Test 2");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Hide all", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(firstTestChildTreeItem.isIconToggledOn(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(secondTestChildTreeItem.isIconToggledOn(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(testCategoryTreeItem.isIconToggledOn(), Matchers.is(false), saveScreenshot(testinfo));

        // Show via context menu
        robot.rightClickOn("Test 1");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Show", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(firstTestChildTreeItem.isIconToggledOn(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(secondTestChildTreeItem.isIconToggledOn(), Matchers.is(false), saveScreenshot(testinfo));

        robot.rightClickOn("Test 2");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Show", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(firstTestChildTreeItem.isIconToggledOn(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(secondTestChildTreeItem.isIconToggledOn(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(testCategoryTreeItem.isIconToggledOn(), Matchers.is(true), saveScreenshot(testinfo));

        /* ----Nesting---- */
        // Draw another bounding-box belonging to the Test-category.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.6), new Point2D(0.5, 0.85));
        WaitForAsyncUtils.waitForFxEvents();

        final BoundingBoxTreeItem thirdTestChildTreeItem =
                (BoundingBoxTreeItem) testCategoryTreeItem.getChildren().get(2);

        // Enter new category
        enterNewCategory(robot, "Dummy", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("Dummy"),
                   saveScreenshot(testinfo));

        // Draw a bounding-box belonging to the Dummy-category
        moveRelativeToImageView(robot, new Point2D(0.6, 0.6), new Point2D(0.85, 0.85));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(topLevelTreeItems.size(), Matchers.equalTo(2), saveScreenshot(testinfo));
        verifyThat(topLevelTreeItems.get(0), Matchers.equalTo(testCategoryTreeItem), saveScreenshot(testinfo));
        verifyThat(topLevelTreeItems.get(1), Matchers.instanceOf(ObjectCategoryTreeItem.class),
                   saveScreenshot(testinfo));

        final ObjectCategoryTreeItem dummyCategoryTreeItem = (ObjectCategoryTreeItem) topLevelTreeItems.get(1);
        verifyThat(dummyCategoryTreeItem.getObjectCategory().getName(), Matchers.equalTo("Dummy"),
                   saveScreenshot(testinfo));
        verifyThat(dummyCategoryTreeItem.getChildren().size(), Matchers.equalTo(1), saveScreenshot(testinfo));

        final BoundingBoxTreeItem firstDummyChildTreeItem =
                (BoundingBoxTreeItem) dummyCategoryTreeItem.getChildren().get(0);
        verifyThat(firstDummyChildTreeItem.getId(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(((BoundingBoxView) firstDummyChildTreeItem.getValue()).isSelected(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));

        // Make the third child of the Test-category a nested part of the first item of the Dummy-category.
        robot.moveTo("Test 3").press(MouseButton.PRIMARY).moveTo("Dummy 1").release(MouseButton.PRIMARY);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(testCategoryTreeItem.getChildren().size(), Matchers.equalTo(2), saveScreenshot(testinfo));
        verifyThat(dummyCategoryTreeItem.getChildren().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(firstDummyChildTreeItem.getChildren().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(firstDummyChildTreeItem.getChildren().get(0), Matchers.instanceOf(ObjectCategoryTreeItem.class),
                   saveScreenshot(testinfo));
        // The dragged item should be automatically selected after the completion of a successful drag.
        verifyThat(((BoundingBoxView) thirdTestChildTreeItem.getValue()).isSelected(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));

        final ObjectCategoryTreeItem nestedTestCategoryTreeItem =
                (ObjectCategoryTreeItem) firstDummyChildTreeItem.getChildren().get(0);
        verifyThat(nestedTestCategoryTreeItem.getObjectCategory(),
                   Matchers.equalTo(testCategoryTreeItem.getObjectCategory()));
        verifyThat(nestedTestCategoryTreeItem.getChildren().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(nestedTestCategoryTreeItem.getChildren().get(0), Matchers.equalTo(thirdTestChildTreeItem),
                   saveScreenshot(testinfo));

        /* ----Reloading On Image Change---- */
        // Switch to the next image.
        timeOutClickOn(robot, "#next-button", testinfo);

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        // Now the tree-should be empty, as no bounding-boxes have been created for the current image.
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(0),
                   saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0), saveScreenshot(testinfo));

        // Switch back to the previous image.
        timeOutClickOn(robot, "#previous-button", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        // The old tree should have been exactly reconstructed.
        final List<TreeItem<Object>> newTopLevelTreeItems = mainView.getObjectTree().getRoot().getChildren();
        verifyThat(newTopLevelTreeItems, Matchers.equalTo(topLevelTreeItems), saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        /* ----Deleting---- */
        final ObjectCategoryTreeItem newTestCategoryTreeItem = (ObjectCategoryTreeItem) newTopLevelTreeItems.get(0);
        final ObjectCategoryTreeItem newDummyCategoryTreeItem = (ObjectCategoryTreeItem) newTopLevelTreeItems.get(1);
        final BoundingBoxTreeItem newFirstTestChildTreeItem =
                (BoundingBoxTreeItem) newTestCategoryTreeItem.getChildren().get(0);
        final BoundingBoxTreeItem newSecondTestChildTreeItem =
                (BoundingBoxTreeItem) newTestCategoryTreeItem.getChildren().get(1);

        // Delete first Test-bounding-box via context-menu on the tree-cell.
        robot.rightClickOn("Test 1");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Delete", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        // There should still be two categories...
        verifyThat(newTopLevelTreeItems.size(), Matchers.equalTo(2), saveScreenshot(testinfo));
        // ...but one less Test-children.
        verifyThat(newTestCategoryTreeItem.getChildren().size(), Matchers.equalTo(1), saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes(),
                   Matchers.not(Matchers.hasItem((BoundingBoxView) newFirstTestChildTreeItem.getValue())));
        // After the first bounding-box and its tree-item was deleted, the (formerly) second tree-item's id should have been updated.
        verifyThat(newSecondTestChildTreeItem.getId(), Matchers.equalTo(1), saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        // Delete second Test-bounding-box via the context-menu on the element itself.
        robot.rightClickOn((BoundingBoxView) newSecondTestChildTreeItem.getValue());
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Delete", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        // Now just the Dummy-category item should be left.
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(1),
                   saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes(),
                   Matchers.not(Matchers.hasItem((BoundingBoxView) newSecondTestChildTreeItem.getValue())));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        // Delete Dummy-category-item. This should delete all children recursively.
        robot.rightClickOn(newDummyCategoryTreeItem.getGraphic());
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(robot.lookup("Change Category").tryQuery().isEmpty(), Matchers.is(true),
                saveScreenshot(testinfo));
        verifyThat(robot.lookup("Add Vertices").tryQuery().isEmpty(), Matchers.is(true),
                saveScreenshot(testinfo));
        verifyThat(robot.lookup("Remove Vertices").tryQuery().isEmpty(), Matchers.is(true),
                saveScreenshot(testinfo));
        timeOutClickOn(robot, "Delete", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        // Now the tree-view should be empty (besides the invisible root-item).
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(0),
                   saveScreenshot(testinfo));
        // There should be no remaining bounding-boxes.
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0), saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));
        // The tag input field should be disabled if not bounding shape element is currently selected.
        verifyThat(mainView.getTagInputField().isDisabled(), Matchers.is(true), saveScreenshot(testinfo));

        /* ---- Object category change ---- */
        moveRelativeToImageView(robot, new Point2D(0.25, 0.6), new Point2D(0.5, 0.85));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(1),
                   saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().get(0).getViewData().getObjectCategory().getName(),
                   Matchers.equalTo("Dummy"));

        robot.rightClickOn("Dummy 1");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Change Category", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        final Stage changeCategoryStage = timeOutGetTopModalStage(robot, "Change Category", testinfo);
        verifyThat(changeCategoryStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final DialogPane changeCategoryDialog = (DialogPane) changeCategoryStage.getScene().getRoot();
        verifyThat(changeCategoryDialog.getHeaderText(), Matchers.equalTo("Select new category (current: \"Dummy\")"),
                   saveScreenshot(testinfo));
        verifyThat(changeCategoryDialog.getContentText(), Matchers.equalTo("New category:"), saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(2), saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories().stream().map(ObjectCategory::getName).collect(Collectors.toList()),
                   Matchers.containsInRelativeOrder("Test", "Dummy"));

        ObjectCategory testCategory = model.getObjectCategories().get(0);
        ObjectCategory dummyCategory = model.getObjectCategories().get(1);

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> robot.from(changeCategoryDialog)
                                                                                 .lookup(".combo-box").tryQuery()
                                                                                 .isPresent()),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected combo-box not found within " +
                                                                                   TIMEOUT_DURATION_IN_SEC + " sec."));
        WaitForAsyncUtils.waitForFxEvents();

        ComboBox<ObjectCategory> comboBox = robot.from(changeCategoryDialog).lookup(".combo-box").queryComboBox();

        verifyThat(comboBox, ComboBoxMatchers.containsExactlyItemsInOrder(testCategory, dummyCategory),
                   saveScreenshot(testinfo));
        verifyThat(comboBox, ComboBoxMatchers.hasSelectedItem(dummyCategory), saveScreenshot(testinfo));

        robot.interact(() -> comboBox.getSelectionModel().select(testCategory));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(comboBox, ComboBoxMatchers.hasSelectedItem(testCategory), saveScreenshot(testinfo));

        timeOutLookUpInStageAndClickOn(robot, changeCategoryStage, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Change Category", testinfo);

        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(1),
                   saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().get(0).getViewData().getObjectCategory(),
                   Matchers.equalTo(testCategory));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get("Test"), Matchers.equalTo(1),
                   saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get("Dummy"), Matchers.equalTo(0),
                   saveScreenshot(testinfo));

        final String testTagName = "TestTag";

        verifyThat(mainView.getTagInputField().isDisabled(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(mainView.getTagInputField().getPromptText(), Matchers.equalTo("New Tag"), saveScreenshot(testinfo));

        robot.clickOn("New Tag").write(testTagName).press(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        final BoundingShapeViewData currentBoundingShape = mainView.getCurrentBoundingShapes().get(0).getViewData();

        verifyThat(currentBoundingShape.getTags(), Matchers.contains(testTagName), saveScreenshot(testinfo));

        verifyThat(robot.lookup("#tag").queryAll(), Matchers.hasSize(1), saveScreenshot(testinfo));

        verifyThat("#tag", NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat("#tag", NodeMatchers.hasChild("#delete-button"), saveScreenshot(testinfo));
        verifyThat("#tag", NodeMatchers.hasChild("#tag-label"), saveScreenshot(testinfo));

        verifyThat("#tag-label", NodeMatchers.isVisible(), saveScreenshot(testinfo));

        verifyThat("#tag-label", LabeledMatchers.hasText(testTagName), saveScreenshot(testinfo));

        robot.clickOn("#tag #delete-button");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(robot.lookup("#tag").queryAll(), Matchers.empty(), saveScreenshot(testinfo));
        verifyThat(currentBoundingShape.getTags(), Matchers.empty(), saveScreenshot(testinfo));
        verifyThat(mainView.getTagInputField().isDisabled(), Matchers.is(false), saveScreenshot(testinfo));
    }

    @Test
    void onBoundingShapeSaveAsImageRequested_ShouldWriteImageFile(FxRobot robot, TestInfo testinfo, @TempDir Path tempDirectory) {
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        enterNewCategory(robot, "Dummy", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.5, 0.5));
        WaitForAsyncUtils.waitForFxEvents();
        final String expectedFilename = "austin-neill-685084-unsplash_Dummy_1.png";

        final AtomicReference<MockedConstruction<FileChooser>> mockedFileChooser = createMockedFileChooser(
                tempDirectory.resolve(expectedFilename).toFile());

        verifyThat(mockedFileChooser.get(), Matchers.notNullValue());

        try {
            robot.rightClickOn("Dummy 1");
            WaitForAsyncUtils.waitForFxEvents();
            timeOutClickOn(robot, "Save as Image...", testinfo);
            WaitForAsyncUtils.waitForFxEvents();

            verifyThat(mockedFileChooser.get().constructed().size(), Matchers.equalTo(1));
            verify(mockedFileChooser.get().constructed().get(0)).setInitialFileName(expectedFilename);

            verifyThat(tempDirectory.resolve(expectedFilename).toFile(), FileMatchers.anExistingFile());
        } finally {
            mockedFileChooser.get().close();
        }

        robot.rightClickOn("Dummy 1");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Delete", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0));

        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.25, 0.25));
        WaitForAsyncUtils.waitForFxEvents();

        robot.rightClickOn("Dummy 1");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Save as Image...", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertDialogOpenedAndGetStage(robot, "Image Saving Error", "Bounding shape region is too small.",
                testinfo);
    }
}
