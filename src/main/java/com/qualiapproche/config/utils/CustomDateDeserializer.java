package com.qualiapproche.config.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CustomDateDeserializer extends JsonDeserializer<LocalDate> {
  @Override
  public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String dateStr = ((TextNode) p.getCodec().readTree(p)).textValue();
    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM"));
  }
}

