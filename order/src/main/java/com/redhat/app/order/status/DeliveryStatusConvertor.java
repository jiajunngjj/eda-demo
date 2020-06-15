package com.redhat.app.order.status;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class DeliveryStatusConvertor implements AttributeConverter<DeliveryStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(DeliveryStatus status) {
        if (status == null ) {
            return null;
        }


        return status.label;
    }

    @Override
    public DeliveryStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(DeliveryStatus.values())
        .filter(s -> s.label.equals(dbData))
        .findFirst()
        .orElseThrow(IllegalArgumentException::new);
    }
}