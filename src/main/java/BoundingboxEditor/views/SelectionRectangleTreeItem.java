package BoundingboxEditor.views;

import javafx.scene.control.TreeItem;

class SelectionRectangleTreeItem extends TreeItem<SelectionRectangle> {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 9.0;

    private final ToggleRectangleIcon toggleIcon = new ToggleRectangleIcon(TOGGLE_ICON_SIDE_LENGTH, TOGGLE_ICON_SIDE_LENGTH);
    private int id = 0;

    SelectionRectangleTreeItem(SelectionRectangle selectionRectangle) {
        super(selectionRectangle);
        setGraphic(toggleIcon);

        setUpInternalListeners();
    }

    boolean isIconToggledOn() {
        return toggleIcon.isToggledOn();
    }

    void setIconToggledOn(boolean toggled) {
        toggleIcon.setToggledOn(toggled);
    }

    int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    private void setUpInternalListeners() {
        toggleIcon.fillProperty().bind(getValue().getBoundingBoxCategory().colorProperty());
        toggleIcon.toggledOnProperty().addListener(((observable, oldValue, newValue) -> {
            getValue().setVisible(newValue);

            if(newValue) {
                ((CategoryTreeItem) getParent()).incrementNumToggledOnChildren();
            } else {
                ((CategoryTreeItem) getParent()).decrementNumToggledOnChildren();
            }
        }));

    }
}
