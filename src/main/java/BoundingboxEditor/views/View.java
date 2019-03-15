package BoundingboxEditor.views;

import BoundingboxEditor.Controller;

public interface View {
    default void connectToController(final Controller controller) {
    }
}
