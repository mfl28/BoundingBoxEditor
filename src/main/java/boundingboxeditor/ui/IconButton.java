/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package boundingboxeditor.ui;

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
