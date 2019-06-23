package BoundingBoxEditor.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.Region;

/**
 * A button that only displays a clickable icon.
 */
class IconButton extends Button {
    /**
     * Creates a new icon-button.
     *
     * @param iconCssId the css-id-string of the button (in case of {@link IconType}.BACKGROUND) or the
     *                  id of the {@link Region} containing the icon in case of {@link IconType}.GRAPHIC)
     * @param iconType  indicates the way the icon should be embedded in the button:
     *                  <ul>
     *                  <li>{@link IconType}.BACKGROUND: the icon will be directly set as the shape of the
     *                  button-background</li>
     *                  <li>{@link IconType}.GRAPHIC: the icon will be embedded within a {@link Region} which
     *                  then is set as the graphic of the button</li>
     *                  </ul>
     */
    IconButton(String iconCssId, IconType iconType) {
        if(iconType == IconType.GRAPHIC) {
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
