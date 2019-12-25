module BoundingBoxEditor {
    requires javafx.controls;
    requires java.desktop;
    requires org.controlsfx.controls;
    requires org.apache.commons.collections4;
    requires com.github.benmanes.caffeine;
    requires java.xml;
    requires org.apache.commons.lang3;
    requires java.prefs;
    requires java.logging;

    opens BoundingBoxEditor.model to javafx.base;
    opens BoundingBoxEditor.model.io to javafx.base;
    exports BoundingBoxEditor to javafx.graphics;
}