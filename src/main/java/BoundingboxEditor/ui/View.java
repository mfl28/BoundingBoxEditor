package BoundingboxEditor.ui;

import BoundingboxEditor.controller.Controller;

public interface View {
    default void connectToController(final Controller controller) {
    }
}
