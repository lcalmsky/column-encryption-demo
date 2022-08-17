package io.lcalmsky.demo;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Account {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Convert(converter = Aes256Converter.class)
  private String phoneNumber;

  private Account(String name, String phoneNumber) {
    this.name = name;
    this.phoneNumber = phoneNumber;
  }

  public static Account of(String name, String phoneNumber) {
    return new Account(name, phoneNumber);
  }
}
