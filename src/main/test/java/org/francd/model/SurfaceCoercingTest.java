package org.francd.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SurfaceCoercingTest {

    private final SurfaceCoercing coercing = new SurfaceCoercing();

    @Test
    void serializeDouble() {
        //Given
        double value = 12345.123;

        //When
        String serialized = coercing.serialize(value,null,null); //should be mocked not null

        //Then
        assertThat(serialized).isEqualToIgnoringWhitespace("12345.123 km²");
    }

    @Test
    void serializeSurface() {
        //Given
        Surface surface = new Surface( 12345.123, Surface.Unit.M2);

        //When
        String serialized = coercing.serialize(surface,null,null); //should be mocked not null

        //Then
        assertThat(serialized).isEqualToIgnoringWhitespace(" 12345.123 m²");
    }

    @Test
    void parseValue() {
        //Given
        String serialized = "12345.123 m²";

        //When
        Surface parsed = coercing.parseValue(serialized, null, null);

        //Then
        assertThat(parsed).isEqualTo(new Surface(12345.123, Surface.Unit.M2));
    }
}