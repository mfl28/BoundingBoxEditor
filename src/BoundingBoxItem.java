import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class BoundingBoxItem {
    private static final String DEFAULT_NAME = "Default";

    private final StringProperty name;
    private final ObjectProperty<Color> color;

    public BoundingBoxItem() {
        this.name = new SimpleStringProperty(DEFAULT_NAME);
        this.color = new SimpleObjectProperty<>(Color.ORANGE);
    }

    public BoundingBoxItem(String name, Color color) {
        this.name = new SimpleStringProperty(name);
        this.color = new SimpleObjectProperty<>(color);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }
}
