module BoundingboxEditor {
    requires javafx.controls;
    requires java.desktop;
    requires org.controlsfx.controls;
    requires org.apache.commons.collections4;
    requires com.github.benmanes.caffeine;
    requires java.xml;
    requires org.apache.commons.lang3;
    requires java.prefs;
    requires slf4j.api;
    opens BoundingboxEditor.model to javafx.base;
    opens BoundingboxEditor.model.io to javafx.base;
    exports BoundingboxEditor;
}