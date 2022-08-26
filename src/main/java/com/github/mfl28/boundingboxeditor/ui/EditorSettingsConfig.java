/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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