package com.supermarket.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneySerializer extends StdSerializer<BigDecimal> {
    public MoneySerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator generator,
                          SerializerProvider provider) throws IOException {
        generator.writeString(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }
}
