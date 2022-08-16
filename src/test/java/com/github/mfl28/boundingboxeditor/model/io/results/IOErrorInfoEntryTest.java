package com.github.mfl28.boundingboxeditor.model.io.results;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class IOErrorInfoEntryTest {
    @Test
    void checkEqualsContract() {
        EqualsVerifier.simple().forClass(IOErrorInfoEntry.class)
                .withNonnullFields("sourceName", "errorDescription")
                .verify();
    }
}