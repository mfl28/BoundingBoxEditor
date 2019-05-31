package BoundingboxEditor.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.Region;

class IconButton extends Button {
    IconButton(String iconCssId, IconType iconType) {
        if(iconType.equals(IconType.GRAPHIC)) {
            Region icon = new Region();
            icon.setId(iconCssId);
            icon.setPickOnBounds(true);
            setGraphic(icon);
        } else {
            setId(iconCssId);
        }

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setFocusTraversable(false);
        setPickOnBounds(true);
    }

    enum IconType {BACKGROUND, GRAPHIC}
}
