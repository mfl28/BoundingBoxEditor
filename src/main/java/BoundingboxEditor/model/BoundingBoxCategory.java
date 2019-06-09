package BoundingboxEditor.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

import java.util.Objects;


/**
 * Represents the category (e.g. "Person", "Car" etc.) a bounding-box belongs to.
 */
public class BoundingBoxCategory {
    private final StringProperty name;
    private final ObjectProperty<Color> color;

    /**
     * Creates a new bounding-box category.
     *
     * @param name  the name of the bounding-box category.
     * @param color the color used for the visual representation of
     *              the bounding-box category in the program.
     */
    public BoundingBoxCategory(String name, Color color) {
        this.name = new SimpleStringProperty(name);
        this.color = new SimpleObjectProperty<>(color);
    }

    /**
     * Returns the category-name property.
     *
     * @return the string-property of the name
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the category-color-property .
     *
     * @return the color-property
     */
    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.get(), color.get());
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

    /**
     * Returns the name of the bounding-box category.
     *
     * @return the name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the name of the bounding-box category.
     *
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * Returns the color used for the visual representation of the bounding-box category.
     *
     * @return the color
     */
    public Color getColor() {
        return color.get();
    }

    /**
     * Sets the color used for the visual representation of the bounding-box category.
     *
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color.set(color);
    }
}
