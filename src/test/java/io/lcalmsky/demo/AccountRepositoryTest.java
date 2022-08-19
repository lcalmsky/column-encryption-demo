package io.lcalmsky.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.NoSuchElementException;
import javax.persistence.EntityManager;
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
  AccountRepository accountRepository;
  @Autowired
  EntityManager entityManager;

  Aes256Utils aes256Utils = new Aes256Utils();

  @BeforeEach
  void setup() {
    accountRepository.save(Account.of(NAME, PHONE_NUMBER));
  }

  @AfterEach
  void teardown() {
    accountRepository.deleteAll();
  }

  @Test
  @DisplayName("쿼리 메서드를 이용해 암호화 컬럼을 평문으로 조회시 정상 동작")
  void test() {
    Account account = accountRepository.findByPhoneNumber(PHONE_NUMBER).orElseThrow();
    assertEquals(NAME, account.getName());
  }

  @Test
  @DisplayName("쿼리 메서드를 이용해 암호화 컬럼을 암호화 된 값으로 조회시 실패")
  void test2() {
    String encryptedPhoneNumber = aes256Utils.encrypt(PHONE_NUMBER);
    assertThrows(NoSuchElementException.class,
        () -> accountRepository.findByPhoneNumber(encryptedPhoneNumber).orElseThrow());
  }

  @Test
  @DisplayName("JPQL을 이용해 암호화 컬럼을 평문으로 조회시 정상 동작")
  void test3() {
    Account account = accountRepository.findAccountByPhoneNumber(PHONE_NUMBER).orElseThrow();
    assertEquals(NAME, account.getName());
  }

  @Test
  @DisplayName("JPQL을 이용해 암호화 컬럼을 암호화 된 값으로 조회시 실패")
  void test4() {
    String encryptedPhoneNumber = aes256Utils.encrypt(PHONE_NUMBER);
    assertThrows(NoSuchElementException.class,
        () -> accountRepository.findByPhoneNumber(encryptedPhoneNumber).orElseThrow());
  }

  @Test
  @DisplayName("querydsl을 이용해 암호화 컬럼을 평문으로 조회시 정상 동작")
  void test5() {
    QAccount account = QAccount.account;
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
    Account found = queryFactory.selectFrom(account)
        .where(account.phoneNumber.eq(PHONE_NUMBER))
        .fetchOne();
    assertNotNull(found);
    assertEquals(NAME, found.getName());
  }

  @Test
  @DisplayName("querydsl을 이용해 암호화 컬럼을 암호화 된 값으로 조회시 실패")
  void test6() {
    String encryptedPhoneNumber = aes256Utils.encrypt(PHONE_NUMBER);
    QAccount account = QAccount.account;
    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
    Account found = queryFactory.selectFrom(account)
        .where(account.phoneNumber.eq(encryptedPhoneNumber))
        .fetchOne();
    assertNull(found);
  }
}