package BoundingboxEditor;

import javafx.scene.control.TreeItem;

class SelectionRectangleTreeItem extends TreeItem<SelectionRectangle> {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 9.0;
    private final ToggleRectangleIcon toggleIcon = new ToggleRectangleIcon(TOGGLE_ICON_SIDE_LENGTH, TOGGLE_ICON_SIDE_LENGTH);
    private int id = 0;

    public SelectionRectangleTreeItem(SelectionRectangle selectionRectangle) {
        super(selectionRectangle);
        // TODO: CategoryItem needs to be updated (currently toggling category items in treeview does not make rectangles invisible)
        setGraphic(toggleIcon);
        setUpInternalListeners();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isIconToggledOn() {
        return toggleIcon.isToggledOn();
    }

    public void setIconToggledOn(boolean toggled) {
        toggleIcon.setToggledOn(toggled);
    }

    private void setUpInternalListeners() {
        toggleIcon.fillProperty().bind(getValue().getBoundingBoxCategory().colorProperty());
        toggleIcon.toggledOnProperty().addListener(((observable, oldValue, newValue) -> getValue().setVisible(newValue)));
    }
}
