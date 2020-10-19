package com.github.mfl28.boundingboxeditor.controller;

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.ui.settings.InferenceSettingsView;
import com.github.mfl28.boundingboxeditor.ui.settings.SettingsDialogView;
import com.github.mfl28.boundingboxeditor.ui.settings.UISettingsView;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
public class SettingsTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onUISettingsChanged_ShouldCorrectlyApplyChanges(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-settings-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        final Stage settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.is(true), saveScreenshot(testinfo));

        final SettingsDialogView settingsDialogView = mainView.getSettingsDialog();
        verifyThat(settingsDialogView.getDialogPane().getContent(), Matchers.instanceOf(SplitPane.class),
                   saveScreenshot(testinfo));

        verifyThat(settingsDialogView.getDialogPane().lookupButton(ButtonType.APPLY).isDisable(),
                   Matchers.is(true), saveScreenshot(testinfo));

        final SplitPane settingsSplitPane = (SplitPane) settingsDialogView.getDialogPane().getContent();
        verifyThat(settingsSplitPane.isVisible(), Matchers.is(true), saveScreenshot(testinfo));

        verifyThat(settingsSplitPane.getItems().get(0), Matchers.instanceOf(ListView.class), saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final ListView<String> categoriesListView =
                (ListView<String>) settingsSplitPane.getItems().get(0);
        verifyThat(categoriesListView.getSelectionModel().getSelectedItem(), Matchers.equalTo("Inference"),
                   saveScreenshot(testinfo));

        timeOutLookUpInStageAndClickOn(robot, settingsStage, "UI", testinfo);
        verifyThat(categoriesListView.getSelectionModel().getSelectedItem(), Matchers.equalTo("UI"),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getUiSettingsConfig().isShowObjectPopover(), Matchers.is(true), saveScreenshot(testinfo));

        final UISettingsView uiSettingsView = settingsDialogView.getUiSettings();
        verifyThat(uiSettingsView.isVisible(), Matchers.is(true), saveScreenshot(testinfo));

        final CheckBox showObjectPopoverControl = uiSettingsView.getShowObjectPopoverControl();
        verifyThat(showObjectPopoverControl.isVisible(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(showObjectPopoverControl.isSelected(), Matchers.is(true), saveScreenshot(testinfo));

        robot.clickOn(uiSettingsView.getShowObjectPopoverControl());
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(showObjectPopoverControl.isSelected(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(settingsDialogView.getDialogPane().lookupButton(ButtonType.APPLY).isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));

        robot.clickOn(settingsDialogView.getDialogPane().lookupButton(ButtonType.APPLY));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(settingsStage.isShowing(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(settingsDialogView.getDialogPane().lookupButton(ButtonType.APPLY).isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));

        clickOnButtonInDialogStage(robot, settingsStage, ButtonType.OK, testinfo);

        timeOutAssertNoTopModelStage(robot, testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.is(false));

        verifyThat(mainView.getUiSettingsConfig().isShowObjectPopover(), Matchers.is(false));
    }

    @Test
    void onInferenceSettingsChanged_ShouldCorrectlyApplyChanges(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-settings-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        final Stage settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.is(true), saveScreenshot(testinfo));

        final SettingsDialogView settingsDialogView = mainView.getSettingsDialog();
        verifyThat(settingsDialogView.getDialogPane().getContent(), Matchers.instanceOf(SplitPane.class),
                   saveScreenshot(testinfo));

        verifyThat(settingsDialogView.getDialogPane().lookupButton(ButtonType.APPLY).isDisable(),
                   Matchers.is(true), saveScreenshot(testinfo));

        final SplitPane settingsSplitPane = (SplitPane) settingsDialogView.getDialogPane().getContent();
        verifyThat(settingsSplitPane.isVisible(), Matchers.is(true), saveScreenshot(testinfo));

        verifyThat(settingsSplitPane.getItems().get(0), Matchers.instanceOf(ListView.class), saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final ListView<String> categoriesListView =
                (ListView<String>) settingsSplitPane.getItems().get(0);
        verifyThat(categoriesListView.getSelectionModel().getSelectedItem(), Matchers.equalTo("Inference"),
                   saveScreenshot(testinfo));

        final InferenceSettingsView inferenceSettingsView = settingsDialogView.getInferenceSettings();

        verifyThat(inferenceSettingsView.isVisible(), Matchers.is(true), saveScreenshot(testinfo));

        // Verify enabled/disabled state
        verifyDefaultInferenceSettingsStates(testinfo);

        robot.clickOn(inferenceSettingsView.getInferenceEnabledControl());
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(settingsDialogView.getDialogPane().lookupButton(ButtonType.APPLY).isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));

        verifyThat(inferenceSettingsView.getInferenceEnabledControl().isSelected(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getInferenceEnabledControl().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getInferenceAddressField().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getInferencePortField().isDisabled(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getManagementAddressField().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getManagementPortField().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getSelectedModelLabel().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getSelectModelButton().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMinimumScoreControl().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMergeCategoriesControl().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getResizeImagesControl().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMaxImageWidthField().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMaxImageHeightField().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getKeepImageRatioControl().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));

        verifyThat(inferenceSettingsView.getInferenceAddressField().getText(), Matchers.equalTo("http://localhost"),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getInferencePortField().getText(), Matchers.equalTo("8080"),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getManagementAddressField().getText(), Matchers.equalTo("http://localhost"),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getManagementPortField().getText(), Matchers.equalTo("8081"),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getSelectedModelLabel().getText(), Matchers.equalTo("None"),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMinimumScoreControl().getValue(), Matchers.equalTo(0.5),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMergeCategoriesControl().isSelected(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getResizeImagesControl().isSelected(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMaxImageWidthField().getText(), Matchers.equalTo("600"),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMaxImageHeightField().getText(), Matchers.equalTo("600"),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getKeepImageRatioControl().isSelected(), Matchers.is(true),
                   saveScreenshot(testinfo));


        robot.doubleClickOn(inferenceSettingsView.getMaxImageWidthField()).eraseText(1).write("777");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(inferenceSettingsView.getMaxImageWidthField().getText(), Matchers.equalTo("777"),
                   saveScreenshot(testinfo));

        robot.clickOn(settingsDialogView.getDialogPane().lookupButton(ButtonType.CANCEL));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertNoTopModelStage(robot, testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.is(false));

        final BoundingBoxPredictorClientConfig clientConfig = model.getBoundingBoxPredictorClientConfig();

        verifyThat(clientConfig.getInferenceUrl(), Matchers.equalTo("http://localhost"),
                   saveScreenshot(testinfo));
        verifyThat(clientConfig.getInferencePort(), Matchers.equalTo(8080),
                   saveScreenshot(testinfo));
        verifyThat(clientConfig.getManagementUrl(), Matchers.equalTo("http://localhost"),
                   saveScreenshot(testinfo));
        verifyThat(clientConfig.getManagementPort(), Matchers.equalTo(8081),
                   saveScreenshot(testinfo));
        verifyThat(clientConfig.getInferenceModelName(), Matchers.nullValue(), saveScreenshot(testinfo));

        final BoundingBoxPredictorConfig predictorConfig = model.getBoundingBoxPredictorConfig();

        verifyThat(predictorConfig.getMinimumScore(), Matchers.equalTo(0.5),
                   saveScreenshot(testinfo));
        verifyThat(predictorConfig.isMergeCategories(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(predictorConfig.isResizeImages(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(predictorConfig.getMaxImageWidth(), Matchers.equalTo(600),
                   saveScreenshot(testinfo));
        verifyThat(predictorConfig.getMaxImageHeight(), Matchers.equalTo(600),
                   saveScreenshot(testinfo));
        verifyThat(predictorConfig.isKeepImageRatio(), Matchers.is(true),
                   saveScreenshot(testinfo));

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-settings-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyDefaultInferenceSettingsStates(testinfo);

        robot.clickOn(inferenceSettingsView.getInferenceEnabledControl());
        WaitForAsyncUtils.waitForFxEvents();

        robot.doubleClickOn(inferenceSettingsView.getManagementPortField()).eraseText(1);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(inferenceSettingsView.getSelectModelButton().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));

        robot.doubleClickOn(inferenceSettingsView.getManagementPortField()).write("1234");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(inferenceSettingsView.getManagementPortField().getText(), Matchers.equalTo("1234"),
                   saveScreenshot(testinfo));

        verifyThat(inferenceSettingsView.getSelectModelButton().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));

        robot.clickOn(inferenceSettingsView.getResizeImagesControl());
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(inferenceSettingsView.getResizeImagesControl().isSelected(), Matchers.equalTo(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMaxImageWidthField().getParent().isVisible(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getKeepImageRatioControl().getParent().isVisible(), Matchers.is(false),
                   saveScreenshot(testinfo));

        clickOnButtonInDialogStage(robot, settingsStage, ButtonType.OK, testinfo);

        final Stage errorStage = timeOutGetTopModalStage(robot, "Settings Application Error", testinfo);
        clickOnButtonInDialogStage(robot, errorStage, ButtonType.OK, testinfo);

        robot.clickOn(inferenceSettingsView.getInferenceEnabledControl());
        WaitForAsyncUtils.waitForFxEvents();

        clickOnButtonInDialogStage(robot, settingsStage, ButtonType.OK, testinfo);

        timeOutAssertNoTopModelStage(robot, testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.is(false));

        verifyThat(clientConfig.getManagementPort(), Matchers.equalTo(1234));
        verifyThat(predictorConfig.isResizeImages(), Matchers.is(false));
    }


    private void verifyDefaultInferenceSettingsStates(TestInfo testinfo) {
        final InferenceSettingsView inferenceSettingsView = mainView.getSettingsDialog().getInferenceSettings();

        verifyThat(inferenceSettingsView.getInferenceEnabledControl().isSelected(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getInferenceEnabledControl().isDisable(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getInferenceAddressField().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getInferencePortField().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getManagementAddressField().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getManagementPortField().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getSelectedModelLabel().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getSelectModelButton().isDisabled(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMinimumScoreControl().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMergeCategoriesControl().isDisable(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getResizeImagesControl().isDisabled(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMaxImageWidthField().isDisabled(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getMaxImageHeightField().isDisabled(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(inferenceSettingsView.getKeepImageRatioControl().isDisabled(), Matchers.is(true),
                   saveScreenshot(testinfo));
    }

}
