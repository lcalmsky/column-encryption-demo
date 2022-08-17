package io.lcalmsky.demo;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import org.springframework.util.StringUtils;

@Converter
public class Aes256Converter implements AttributeConverter<String, String> {

  private final Aes256Utils aes256Utils;

  public Aes256Converter() {
    this.aes256Utils = new Aes256Utils();
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (!StringUtils.hasText(attribute)) {
      return attribute;
    }
    try {
      return aes256Utils.encrypt(attribute);
    } catch (Exception e) {
      throw new RuntimeException("failed to encrypt data", e);
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    try {
      return aes256Utils.decrypt(dbData);
    } catch (Exception e) {
      return dbData;
    }
  }
}