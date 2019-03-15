package BoundingboxEditor.views;

import BoundingboxEditor.BoundingBoxCategory;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;

class CategoryTreeItem extends TreeItem<SelectionRectangle> {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 10.0;

    private final ToggleRectangleIcon toggleIcon = new ToggleRectangleIcon(TOGGLE_ICON_SIDE_LENGTH, TOGGLE_ICON_SIDE_LENGTH);
    private final IntegerProperty numToggledOnChildren = new SimpleIntegerProperty(0);
    private final BoundingBoxCategory boundingBoxCategory;

    CategoryTreeItem(BoundingBoxCategory category) {
        //FIXME: Currently this is a workaround, because cells that are not associated with a main.java.BoundingboxEditor.views.SelectionRectangle
        //       are considered empty, which leads to problems when deleting items.
        super(SelectionRectangle.getDummy());
        boundingBoxCategory = category;

        setGraphic(toggleIcon);

        setUpInternalListeners();
    }

    BoundingBoxCategory getBoundingBoxCategory() {
        return boundingBoxCategory;
    }

    void setIconToggledOn(boolean toggled) {
        toggleIcon.setToggledOn(toggled);
    }

    void incrementNumToggledOnChildren() {
        numToggledOnChildren.set(numToggledOnChildren.get() + 1);
    }

    void decrementNumToggledOnChildren() {
        numToggledOnChildren.set(numToggledOnChildren.get() - 1);
    }

    private void setUpInternalListeners() {
        toggleIcon.fillProperty().bind(boundingBoxCategory.colorProperty());

        toggleIcon.toggledOnProperty().addListener(((observable, oldValue, newValue) ->
                getChildren().stream()
                        .map(childItem -> (SelectionRectangleTreeItem) childItem)
                        .forEach(childItem -> childItem.setIconToggledOn(newValue))
        ));

        getChildren().addListener((ListChangeListener<TreeItem<SelectionRectangle>>) c -> {
            while(c.next()) {
                int numToggledChildrenToAdd = c.getAddedSize();
                int numToggledChildrenToRemove = (int) c.getRemoved().stream()
                        .map(item -> (SelectionRectangleTreeItem) item)
                        .filter(SelectionRectangleTreeItem::isIconToggledOn)
                        .count();

                numToggledOnChildren.setValue(numToggledOnChildren.get() + numToggledChildrenToAdd - numToggledChildrenToRemove);
            }
        });

        numToggledOnChildren.addListener((observable, oldValue, newValue) -> {
            if(!getChildren().isEmpty() && newValue.intValue() == getChildren().size()) {
                toggleIcon.setToggledOn(true);
            } else if(newValue.intValue() == 0) {
                toggleIcon.setToggledOn(false);
            }
        });
    }
}
