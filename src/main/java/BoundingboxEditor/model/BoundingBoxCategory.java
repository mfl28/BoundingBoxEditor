package BoundingboxEditor.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

import java.util.Objects;


/**
 * A class representing the category (e.g. "Car", "Bus" etc.) of a bounding-box.
 */
public class BoundingBoxCategory {
    private final StringProperty name;
    private final ObjectProperty<Color> color;

    /**
     * @param name  The name of the bounding-box category.
     * @param color The color of the visual representation of
     *              the bounding-box in the program.
     */
    public BoundingBoxCategory(final String name, final Color color) {
        this.name = new SimpleStringProperty(name);
        this.color = new SimpleObjectProperty<>(color);
    }

    /**
     * @return The name of this bounding-box category.
     */
    public String getName() {
        return name.get();
    }

    /**
     * @param name The desired name of this bounding-box category.
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * @return The color of the visual representation bounding-boxes belonging
     * to this category.
     */
    public Color getColor() {
        return color.get();
    }

    /**
     * @param color The desired color of the visual representation bounding-boxes belonging
     *              to this category.
     */
    public void setColor(Color color) {
        this.color.set(color);
    }

    /**
     * Allows to bind or add listeners to the name of this bounding-box category.
     *
     * @return The string-property of the name of this bounding-box category.
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Allows to bind or add listeners to the color of this bounding-box category.
     *
     * @return The object-property of the color of this bounding-box category.
     */
    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    @Override
    public boolean equals(Object other) {
        if(this == other) {
            return true;
        }

        if(!(other instanceof BoundingBoxCategory)) {
            return false;
        }

        final BoundingBoxCategory otherCategory = (BoundingBoxCategory) other;
        return otherCategory.getName().equals(this.getName()) && otherCategory.getColor().equals(this.getColor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.get(), color.get());
    }
}
