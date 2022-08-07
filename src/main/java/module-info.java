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
module com.github.mfl28.boundingboxeditor {
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
    requires jersey.client;
    requires jersey.common;
    requires java.ws.rs;
    requires javafx.swing;
    requires jersey.media.multipart;
    requires org.jvnet.mimepull;

    opens com.github.mfl28.boundingboxeditor.model to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.data to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.io to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.io.results to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.io.restclients to javafx.base, com.google.gson;
    exports com.github.mfl28.boundingboxeditor.model.io.restclients to hk2.locator;
    exports com.github.mfl28.boundingboxeditor to javafx.graphics;
}