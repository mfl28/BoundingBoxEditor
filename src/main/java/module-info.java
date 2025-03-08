/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
    requires org.controlsfx.controls;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires com.github.benmanes.caffeine;
    requires org.apache.commons.lang3;
    requires java.prefs;
    requires com.google.gson;
    requires jersey.client;
    requires jersey.common;
    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires jakarta.activation;
    requires javafx.swing;
    requires jersey.media.multipart;
    requires org.jvnet.mimepull;
    requires org.locationtech.jts;
    requires metadata.extractor;
    requires static lombok;
    requires com.fasterxml.jackson.dataformat.csv;
    requires com.fasterxml.jackson.databind;
    requires org.checkerframework.checker.qual;

    opens com.github.mfl28.boundingboxeditor.model to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.data to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.io to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.io.results to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.io.restclients to javafx.base, com.google.gson;
    opens com.github.mfl28.boundingboxeditor.model.io.data to com.fasterxml.jackson.databind;
    exports com.github.mfl28.boundingboxeditor.model.io.restclients to org.glassfish.hk2.locator;
    exports com.github.mfl28.boundingboxeditor to javafx.graphics;
}