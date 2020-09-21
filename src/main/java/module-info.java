module boundingboxeditor {
    requires javafx.controls;
    requires java.desktop;
    requires org.controlsfx.controls;
    requires org.apache.commons.collections4;
    requires com.github.benmanes.caffeine;
    requires java.xml;
    requires org.apache.commons.lang3;
    requires java.prefs;
    requires java.logging;
    requires com.google.gson;

    opens boundingboxeditor.model to javafx.base, com.google.gson;
    opens boundingboxeditor.model.data to javafx.base, com.google.gson;
    opens boundingboxeditor.model.io to javafx.base, com.google.gson;
    opens boundingboxeditor.model.io.results to javafx.base, com.google.gson;
    exports boundingboxeditor to javafx.graphics;
}