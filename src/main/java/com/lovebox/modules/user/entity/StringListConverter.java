package com.lovebox.modules.user.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/** List<String> ↔ "a,b,c" (giá trị là nhãn chuẩn trong Vocab, không chứa dấu phẩy). */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    @Override
    public String convertToDatabaseColumn(List<String> list) {
        return list == null ? "" : String.join(",", list);
    }

    @Override
    public List<String> convertToEntityAttribute(String s) {
        return s == null || s.isBlank() ? List.of() : Arrays.asList(s.split(","));
    }
}
