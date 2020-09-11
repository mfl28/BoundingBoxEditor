package boundingboxeditor.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class UtilsTests {
    @Test
    void onCreateMathUtils_ShouldThrowException() throws Exception {
        Constructor<MathUtils> mathUtilsConstructor = MathUtils.class.getDeclaredConstructor();
        mathUtilsConstructor.setAccessible(true);

        Throwable throwable = Assertions.assertThrows(IllegalStateException.class, () -> {
            try {
                mathUtilsConstructor.newInstance();
            } catch(InvocationTargetException e) {
                throw e.getCause();
            }
        });

        Assertions.assertEquals(throwable.getMessage(), "MathUtils class");
    }

    @Test
    void onCreateColorUtils_ShouldThrowException() throws Exception {
        Constructor<ColorUtils> colorUtilsConstructor = ColorUtils.class.getDeclaredConstructor();
        colorUtilsConstructor.setAccessible(true);

        Throwable throwable = Assertions.assertThrows(IllegalStateException.class, () -> {
            try {
                colorUtilsConstructor.newInstance();
            } catch(InvocationTargetException e) {
                throw e.getCause();
            }
        });

        Assertions.assertEquals(throwable.getMessage(), "ColorUtils class");
    }

    @Test
    void onCreateUiUtils_ShouldThrowException() throws Exception {
        Constructor<UiUtils> uiUtilsConstructor = UiUtils.class.getDeclaredConstructor();
        uiUtilsConstructor.setAccessible(true);

        Throwable throwable = Assertions.assertThrows(IllegalStateException.class, () -> {
            try {
                uiUtilsConstructor.newInstance();
            } catch(InvocationTargetException e) {
                throw e.getCause();
            }
        });

        Assertions.assertEquals(throwable.getMessage(), "UiUtils class");
    }
}
