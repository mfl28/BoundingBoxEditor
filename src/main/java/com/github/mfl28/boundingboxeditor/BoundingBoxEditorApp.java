/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor;

import com.github.mfl28.boundingboxeditor.controller.Controller;
import com.github.mfl28.boundingboxeditor.ui.MainView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The main app-class and entry point of the application.
 * This is an application that allows a user to load images from an image-folder which can then be
 * annotated with bounding-shapes. It is furthermore possible to import and save image-annotation data.
 * Bounding-shapes drawn or imported by the user can be tagged (e.g.: difficult, truncated, pose: frontal,
 * action: walking etc.) and nested (meaning part-whole relations can be created by dragging items of a
 * tree-like UI-element onto each other).
 */
public class BoundingBoxEditorApp extends Application {
    private static final double INITIAL_WINDOW_SCALE = 0.75;
    private static final String STYLESHEET_PATH = "/stylesheets/css/styles.css";

    /**
     * Launches the application.
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        launch(BoundingBoxEditorApp.class);
    }

    @Override
    public void start(Stage primaryStage) {
        final MainView view = new MainView();
        final Scene scene = createSceneFromParent(view);

        scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
        primaryStage.setScene(scene);

        final Controller controller = new Controller(primaryStage, view, getHostServices());

        scene.setOnKeyPressed(controller::onRegisterSceneKeyPressed);
        scene.setOnKeyReleased(controller::onRegisterSceneKeyReleased);

        primaryStage.show();
    }

    private Scene createSceneFromParent(Parent parent) {
        final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return new Scene(parent, INITIAL_WINDOW_SCALE * screenBounds.getWidth(),
                         INITIAL_WINDOW_SCALE * screenBounds.getHeight());
    }
}
