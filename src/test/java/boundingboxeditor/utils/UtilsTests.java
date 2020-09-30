/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package boundingboxeditor.utils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Tag("unit")
class UtilsTests {
    @Test
    void onCreateMathUtils_ShouldThrowException() throws Exception {
        Constructor<MathUtils> mathUtilsConstructor = MathUtils.class.getDeclaredConstructor();
        mathUtilsConstructor.setAccessible(true);

        try {
            mathUtilsConstructor.newInstance();
            Assertions.fail("Expected an InvocationTargetException to be thrown");
        } catch(InvocationTargetException e) {
            MatcherAssert.assertThat(e.getCause(), Matchers.instanceOf(IllegalStateException.class));
            Assertions.assertEquals("MathUtils class", e.getCause().getMessage());
        }
    }

    @Test
    void onCreateColorUtils_ShouldThrowException() throws Exception {
        Constructor<ColorUtils> colorUtilsConstructor = ColorUtils.class.getDeclaredConstructor();
        colorUtilsConstructor.setAccessible(true);

        try {
            colorUtilsConstructor.newInstance();
            Assertions.fail("Expected an InvocationTargetException to be thrown");
        } catch(InvocationTargetException e) {
            MatcherAssert.assertThat(e.getCause(), Matchers.instanceOf(IllegalStateException.class));
            Assertions.assertEquals("ColorUtils class", e.getCause().getMessage());
        }
    }

    @Test
    void onCreateUiUtils_ShouldThrowException() throws Exception {
        Constructor<UiUtils> uiUtilsConstructor = UiUtils.class.getDeclaredConstructor();
        uiUtilsConstructor.setAccessible(true);

        try {
            uiUtilsConstructor.newInstance();
            Assertions.fail("Expected an InvocationTargetException to be thrown");
        } catch(InvocationTargetException e) {
            MatcherAssert.assertThat(e.getCause(), Matchers.instanceOf(IllegalStateException.class));
            Assertions.assertEquals("UiUtils class", e.getCause().getMessage());
        }
    }

    @Test
    void isWithin0And1_onOutOfIntervalArgument_ShouldReturnFalse() {
        Assertions.assertFalse(MathUtils.isWithin(-0.00001, 0.0, 1.0));
        Assertions.assertFalse(MathUtils.isWithin(1.00001, 0.0, 1.0));
    }

    @Test
    void isWithin0And1_onWithinIntervalArgument_ShouldReturnTrue() {
        Assertions.assertTrue(MathUtils.isWithin(0.00001, 0.0, 1.0));
        Assertions.assertTrue(MathUtils.isWithin(0.99999, 0.0, 1.0));
    }
}
