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
package com.github.mfl28.boundingboxeditor.model.io;

import java.util.Arrays;

/**
 * The compressed string form of COCO run-length counts, as written and read by pycocotools
 * ({@code rleToString}/{@code rleFrString} in maskApi.c): each count (from the third on as the difference to the
 * count two places before it) is stored in 5-bit groups, lowest first, as characters from '0' (48) on; bit 0x20
 * marks that another group follows, and bit 0x10 of the last group is the sign.
 */
final class COCORunLengthEncoding {
    private static final int CHARACTER_OFFSET = 48;

    private COCORunLengthEncoding() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Encodes run-length counts as a string.
     *
     * @param counts the counts
     * @return the string
     */
    static String encode(long[] counts) {
        final StringBuilder result = new StringBuilder();

        for(int i = 0; i < counts.length; ++i) {
            long value = counts[i];

            if(i > 2) {
                value -= counts[i - 2];
            }

            boolean more = true;

            while(more) {
                long group = value & 0x1f;
                // Arithmetic shift: negative differences keep their sign.
                value >>= 5;
                more = (group & 0x10) != 0 ? value != -1 : value != 0;

                if(more) {
                    group |= 0x20;
                }

                result.append((char) (group + CHARACTER_OFFSET));
            }
        }

        return result.toString();
    }

    /**
     * Decodes run-length counts from a string.
     *
     * @param encoded the string
     * @return the counts
     * @throws IllegalArgumentException if the string is not a valid encoding
     */
    static long[] decode(String encoded) {
        long[] counts = new long[16];
        int nrCounts = 0;
        int position = 0;

        while(position < encoded.length()) {
            long value = 0;
            int shift = 0;
            boolean more = true;

            while(more) {
                if(position >= encoded.length()) {
                    throw new IllegalArgumentException("The run-length encoding ends in the middle of a count.");
                }

                final int group = encoded.charAt(position++) - CHARACTER_OFFSET;

                if(group < 0 || group > 0x3f || shift > 60) {
                    throw new IllegalArgumentException("Invalid character in the run-length encoding at position "
                                                               + position + ".");
                }

                value |= (long) (group & 0x1f) << shift;
                more = (group & 0x20) != 0;
                shift += 5;

                if(!more && (group & 0x10) != 0) {
                    value |= -1L << shift;
                }
            }

            if(nrCounts > 2) {
                value += counts[nrCounts - 2];
            }

            if(nrCounts == counts.length) {
                counts = Arrays.copyOf(counts, counts.length * 2);
            }

            counts[nrCounts++] = value;
        }

        return Arrays.copyOf(counts, nrCounts);
    }
}
