package BoundingboxEditor;

import javafx.scene.control.TreeItem;

class CategoryTreeItem extends TreeItem<SelectionRectangle> {
    //private final Rectangle toggleVisibilityIcon = new Rectangle(0, 0, 10, 10);
    private static final double TOGGLE_ICON_SIDE_LENGTH = 10.0;
    private final ToggleRectangleIcon toggleIcon = new ToggleRectangleIcon(TOGGLE_ICON_SIDE_LENGTH, TOGGLE_ICON_SIDE_LENGTH);
    private final BoundingBoxCategory boundingBoxCategory;

    public CategoryTreeItem(final BoundingBoxCategory category) {
        //FIXME: Currently this is a workaround, because cells that are not associated with a main.java.BoundingboxEditor.SelectionRectangle
        //       are considered empty, which leads to problems when deleting items.
        super(SelectionRectangle.getDummy());
        boundingBoxCategory = category;
        setGraphic(toggleIcon);

        setUpInternalListeners();
    }

    public BoundingBoxCategory getBoundingBoxCategory() {
        return boundingBoxCategory;
    }

    public boolean isIconToggledOn() {
        return toggleIcon.isToggledOn();
    }

    public void setIconToggledOn(boolean toggled) {
        toggleIcon.setToggledOn(toggled);
    }

    private void setUpInternalListeners() {
        toggleIcon.fillProperty().bind(boundingBoxCategory.colorProperty());

        toggleIcon.toggledOnProperty().addListener(((observable, oldValue, newValue) ->
                getChildren().stream()
                        .map(childItem -> (SelectionRectangleTreeItem) childItem)
                        .forEach(childItem -> childItem.setIconToggledOn(newValue))
        ));
    }
}
