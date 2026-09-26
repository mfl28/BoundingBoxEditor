/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.io.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Reads a whole number of pixels that may be written with decimals (e.g. "10.4", as some tools export
 * coordinates), rounding it to the nearest integer.
 */
public class RoundingIntDeserializer extends StdDeserializer<Integer> {
    private static final long serialVersionUID = 1L;

    public RoundingIntDeserializer() {
        super(Integer.class);
    }

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        final String text = parser.getValueAsString();

        try {
            final double value = Double.parseDouble(text.strip());

            if(Double.isFinite(value) && Math.abs(value) <= Integer.MAX_VALUE) {
                return (int) Math.round(value);
            }
        } catch(NumberFormatException | NullPointerException _) {
            // Reported below.
        }

        return (Integer) context.handleWeirdStringValue(Integer.class, text, "not a valid number");
    }
}
