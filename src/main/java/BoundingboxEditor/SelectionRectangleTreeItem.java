package BoundingboxEditor;

import javafx.scene.control.TreeItem;
import javafx.scene.shape.Rectangle;

class SelectionRectangleTreeItem extends TreeItem<SelectionRectangle> {
    private int id = 0;
    private final Rectangle toggleVisibilityIcon = new Rectangle(0, 0, 9, 9);

    public SelectionRectangleTreeItem(SelectionRectangle selectionRectangle) {
        super(selectionRectangle);
        setGraphic(toggleVisibilityIcon);

        toggleVisibilityIcon.fillProperty().bind(selectionRectangle.getBoundingBoxCategory().colorProperty());
        selectionRectangle.visibleProperty().bind(toggleVisibilityIcon.opacityProperty().greaterThan(0.5));

        toggleVisibilityIcon.setOnMousePressed(event -> toggleVisibilityIcon.setOpacity(toggleVisibilityIcon.getOpacity() > 0.5 ? 0.3 : 1.0));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
