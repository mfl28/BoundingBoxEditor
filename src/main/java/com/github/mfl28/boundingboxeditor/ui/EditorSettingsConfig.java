package com.github.mfl28.boundingboxeditor.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class EditorSettingsConfig {
    private final BooleanProperty autoSimplifyPolygons = new SimpleBooleanProperty(true);

    private final DoubleProperty simplifyRelativeDistanceTolerance = new SimpleDoubleProperty(0.1);


    public double getSimplifyRelativeDistanceTolerance() {
        return simplifyRelativeDistanceTolerance.get();
    }

    public void setSimplifyRelativeDistanceTolerance(double simplifyRelativeDistanceTolerance) {
        this.simplifyRelativeDistanceTolerance.set(simplifyRelativeDistanceTolerance);
    }

    public DoubleProperty simplifyRelativeDistanceToleranceProperty() {
        return simplifyRelativeDistanceTolerance;
    }

    public boolean isAutoSimplifyPolygons() {
        return autoSimplifyPolygons.get();
    }

    public void setAutoSimplifyPolygons(boolean autoSimplifyPolygons) {
        this.autoSimplifyPolygons.set(autoSimplifyPolygons);
    }

    public BooleanProperty autoSimplifyPolygonsProperty() {
        return autoSimplifyPolygons;
    }
}