package BoundingboxEditor;

import javafx.scene.control.TreeItem;
import javafx.scene.shape.Rectangle;

class CategoryTreeItem extends TreeItem<SelectionRectangle> {
    private final Rectangle toggleVisibilityIcon = new Rectangle(0, 0, 10, 10);
    private final BoundingBoxCategory boundingBoxCategory;

    public CategoryTreeItem(final BoundingBoxCategory category) {
        //FIXME: Currently this is a workaround, because cells that are not associated with a main.java.BoundingboxEditor.SelectionRectangle
        //       are considered empty, which leads to problems when deleting items.
        super(SelectionRectangle.getDummy());
        boundingBoxCategory = category;
        setGraphic(toggleVisibilityIcon);

        toggleVisibilityIcon.fillProperty().bind(category.colorProperty());

        toggleVisibilityIcon.setOnMousePressed(event -> {
            for(TreeItem<SelectionRectangle> childItem : getChildren()) {
                childItem.getGraphic().setOpacity(toggleVisibilityIcon.getOpacity() > 0.5 ? 0.3 : 1.0);
            }
            toggleVisibilityIcon.setOpacity(toggleVisibilityIcon.getOpacity() > 0.5 ? 0.3 : 1.0);
        });
    }

    public BoundingBoxCategory getBoundingBoxCategory() {
        return boundingBoxCategory;
    }
}
