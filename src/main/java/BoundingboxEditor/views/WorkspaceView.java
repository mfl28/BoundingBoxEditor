package BoundingboxEditor.views;

import BoundingboxEditor.Controller;
import javafx.scene.control.SplitPane;

public class WorkspaceView extends SplitPane implements View {
    private static final double DEFAULT_FIRST_DIVIDER_RATIO = 0.15;
    private static final double DEFAULT_SECOND_DIVIDER_RATIO = 0.85;

    private final ProjectSidePanelView projectSidePanel = new ProjectSidePanelView();
    private final ImageShowerView imageShower = new ImageShowerView();
    private final ImageExplorerPanelView imageExplorer = new ImageExplorerPanelView();

    WorkspaceView() {
        getItems().addAll(projectSidePanel, imageShower, imageExplorer);

        SplitPane.setResizableWithParent(projectSidePanel, false);
        SplitPane.setResizableWithParent(imageExplorer, false);
        SplitPane.setResizableWithParent(imageShower, true);
        setDividerPositions(DEFAULT_FIRST_DIVIDER_RATIO, DEFAULT_SECOND_DIVIDER_RATIO);
        setVisible(false);

        setUpInternalListeners();
    }

    @Override
    public void connectToController(Controller controller) {
        projectSidePanel.connectToController(controller);
        imageShower.connectToController(controller);
    }

    ProjectSidePanelView getProjectSidePanel() {
        return projectSidePanel;
    }

    ImageShowerView getImageShower() {
        return imageShower;
    }

    ImageExplorerPanelView getImageExplorer() {
        return imageExplorer;
    }

    private void setUpInternalListeners() {
        managedProperty().bind(visibleProperty());
    }
}
