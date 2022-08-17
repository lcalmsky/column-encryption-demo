package io.lcalmsky.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class AccountRepositoryTest {

  public static final String PHONE_NUMBER = "01012341234";
  public static final String NAME = "jaime";
  @Autowired
  UserRepository userRepository;

  Aes256Utils aes256Utils = new Aes256Utils();

  @BeforeEach
  void beforeEach() {
    userRepository.save(Account.of(NAME, PHONE_NUMBER));
  }

  @AfterEach
  void afterEach() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("암호화 컬럼을 plaintext로 조회시 정상 동작")
  void test() {
    Account account = userRepository.findByPhoneNumber(PHONE_NUMBER).orElseThrow();
    assertEquals(NAME, account.getName());
  }

  @Test
  @DisplayName("암호화 컬럼을 cipher로 조회시 정상 동작")
  void test2() {
    String encryptedPhoneNumber = aes256Utils.encrypt(PHONE_NUMBER);
    Account account = userRepository.findByPhoneNumber(encryptedPhoneNumber).orElseThrow();
    assertEquals(NAME, account.getName());
  }
}