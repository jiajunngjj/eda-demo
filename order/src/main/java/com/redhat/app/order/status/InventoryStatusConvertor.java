package com.redhat.app.order.status;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import java.util.stream.Stream;


@Converter(autoApply = true)
public class InventoryStatusConvertor implements AttributeConverter<InventoryStatus, String>{
  

    @Override
    public String convertToDatabaseColumn(InventoryStatus status) {
        if (status == null ) {
            return null;
        }


        return status.label;
    }

    @Override
    public InventoryStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
            return Stream.of(InventoryStatus.values())
            .filter(s -> s.label.equals(dbData))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
        }
}

    
    
