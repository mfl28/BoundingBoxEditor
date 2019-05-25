package BoundingboxEditor.ui;

import BoundingboxEditor.model.BoundingBoxCategory;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;

import java.util.List;

class CategoryTreeItem extends TreeItem<BoundingBoxView> {
    private static final double TOGGLE_ICON_SIDE_LENGTH = 9.5;

    private final ToggleRectangleIcon toggleIcon = new ToggleRectangleIcon(TOGGLE_ICON_SIDE_LENGTH, TOGGLE_ICON_SIDE_LENGTH);
    private final IntegerProperty numToggledOnChildren = new SimpleIntegerProperty(0);
    private final BoundingBoxCategory boundingBoxCategory;

    CategoryTreeItem(BoundingBoxCategory category) {
        // TreeItems require a non-null value-item for them not to be considered 'empty':
        super(BoundingBoxView.getDummy());

        boundingBoxCategory = category;
        setGraphic(toggleIcon);

        setUpInternalListeners();
    }

    BoundingBoxCategory getBoundingBoxCategory() {
        return boundingBoxCategory;
    }

    void detachChildId(int id) {
        List<TreeItem<BoundingBoxView>> children = getChildren();
        for(int i = id; i < children.size(); ++i) {
            ((BoundingBoxTreeItem) children.get(i)).setId(i);
        }
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
                        .map(childItem -> (BoundingBoxTreeItem) childItem)
                        .forEach(childItem -> childItem.setIconToggledOn(newValue))
        ));

        getChildren().addListener((ListChangeListener<TreeItem<BoundingBoxView>>) c -> {
            while(c.next()) {
                int numToggledChildrenToAdd = c.getAddedSize();
                int numToggledChildrenToRemove = (int) c.getRemoved().stream()
                        .map(item -> (BoundingBoxTreeItem) item)
                        .filter(BoundingBoxTreeItem::isIconToggledOn)
                        .count();

                numToggledOnChildren.setValue(numToggledOnChildren.get() + numToggledChildrenToAdd - numToggledChildrenToRemove);
            }
        });

        numToggledOnChildren.addListener((observable, oldValue, newValue) -> {
            if(newValue.intValue() == 0) {
                toggleIcon.setToggledOn(false);
            } else if(newValue.intValue() == getChildren().size()) {
                toggleIcon.setToggledOn(true);
            }
        });
    }
}
