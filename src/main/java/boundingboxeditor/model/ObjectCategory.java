package boundingboxeditor.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

import java.util.Objects;


/**
 * Represents the category (e.g. "Person", "Car" etc.) an annotated object belongs to.
 */
public class ObjectCategory {
    private final StringProperty name;
    private final ObjectProperty<Color> color;

    /**
     * Creates a new object category.
     *
     * @param name  the name of the object category.
     * @param color the color used for the visual representation of
     *              the category in the program.
     */
    public ObjectCategory(String name, Color color) {
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

        if(!(other instanceof ObjectCategory)) {
            return false;
        }

        final ObjectCategory otherCategory = (ObjectCategory) other;
        return otherCategory.getName().equals(this.getName()) && otherCategory.getColor().equals(this.getColor());
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the name of the object category.
     *
     * @return the name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the name of the object category.
     *
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * Returns the color used for the visual representation of the object category.
     *
     * @return the color
     */
    public Color getColor() {
        return color.get();
    }

    /**
     * Sets the color used for the visual representation of the object category.
     *
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color.set(color);
    }
}
