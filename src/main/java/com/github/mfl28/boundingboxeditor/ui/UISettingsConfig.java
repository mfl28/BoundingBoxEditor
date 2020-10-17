package com.github.mfl28.boundingboxeditor.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class UISettingsConfig {
    private final BooleanProperty showObjectPopover = new SimpleBooleanProperty(true);

    public boolean isShowObjectPopover() {
        return showObjectPopover.get();
    }

    public void setShowObjectPopover(boolean showObjectPopover) {
        this.showObjectPopover.set(showObjectPopover);
    }

    public BooleanProperty showObjectPopoverProperty() {
        return showObjectPopover;
    }
}
