package com.gojek.beast.converter;

import com.gojek.beast.converter.fields.ByteField;
import com.gojek.beast.converter.fields.DefaultProtoField;
import com.gojek.beast.converter.fields.EnumField;
import com.gojek.beast.converter.fields.NestedField;
import com.gojek.beast.converter.fields.ProtoField;
import com.gojek.beast.converter.fields.StructField;
import com.gojek.beast.converter.fields.TimestampField;
import com.gojek.beast.converter.fields.DateField;
import com.google.protobuf.Descriptors;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FieldFactory {

    public static ProtoField getField(Descriptors.FieldDescriptor descriptor, Object fieldValue) {
        List<ProtoField> protoFields = Arrays.asList(
                new TimestampField(descriptor, fieldValue),
                new DateField(descriptor, fieldValue),
                new EnumField(descriptor, fieldValue),
                new ByteField(descriptor, fieldValue),
                new StructField(descriptor, fieldValue),
                new NestedField(descriptor, fieldValue)
        );
        Optional<ProtoField> first = protoFields
                .stream()
                .filter(ProtoField::matches)
                .findFirst();
        return first.orElseGet(() -> new DefaultProtoField(descriptor, fieldValue));
    }

}
