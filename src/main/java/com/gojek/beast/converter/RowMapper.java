package com.gojek.beast.converter;

import com.gojek.beast.config.ColumnMapping;
import com.gojek.beast.converter.fields.DefaultProtoField;
import com.gojek.beast.converter.fields.EnumField;
import com.gojek.beast.converter.fields.ProtoField;
import com.gojek.beast.converter.fields.TimestampField;
import com.gojek.beast.models.ConfigurationException;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class RowMapper {

    private final ColumnMapping mapping;

    public Map<String, Object> map(DynamicMessage message) {
        if (mapping == null) {
            throw new ConfigurationException("BQ_PROTO_COLUMN_MAPPING is not configured");
        }
        List<Descriptors.FieldDescriptor> messageDescriptors = message.getDescriptorForType().getFields();

        Map<String, Object> row = new HashMap<>(mapping.size());
        mapping.forEach((key, value) -> {
            String columnName = value.toString();
            Integer protoIndex = Integer.valueOf(key.toString()) - 1;
            if (messageDescriptors.size() <= protoIndex) {
                throw new ConfigurationException("Invalid ProtoColumn Mapping Index: " + protoIndex);
            }
            Descriptors.FieldDescriptor fieldDesc = messageDescriptors.get(protoIndex);
            if (fieldDesc != null) {
                Object field = message.getField(fieldDesc);
                Object fieldValue = getField(fieldDesc, field).getValue();
                row.put(columnName, fieldValue);
            }
        });
        return row;
    }

    private ProtoField getField(Descriptors.FieldDescriptor descriptor, Object fieldValue) {
        List<ProtoField> protoFields = Arrays.asList(
                new TimestampField(descriptor, fieldValue),
                new EnumField(descriptor, fieldValue)
        );
        Optional<ProtoField> first = protoFields
                .stream()
                .filter(ProtoField::matches)
                .findFirst();
        return first.orElseGet(() -> new DefaultProtoField(descriptor, fieldValue));
    }

}
