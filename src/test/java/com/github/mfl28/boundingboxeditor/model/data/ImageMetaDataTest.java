package com.github.mfl28.boundingboxeditor.model.data;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


@Tag("unit")
class ImageMetaDataTest {
    @Test
    void checkEqualsContract() {
        EqualsVerifier.simple().forClass(ImageMetaData.class).verify();
    }

    @Test
    void onGetDimensionsString_WhenNoDetailsPresent_ShouldReturnCorrectRepresentation() {
        final ImageMetaData imageMetaData = new ImageMetaData("test");
        Assertions.assertEquals("[]", imageMetaData.getDimensionsString());
    }
}
