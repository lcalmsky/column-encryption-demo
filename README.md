![](https://img.shields.io/badge/spring--boot-2.7.2-red) ![](https://img.shields.io/badge/gradle-7.5-brightgreen) ![](https://img.shields.io/badge/java-17-blue)

## Overview

개인정보 보호 등을 위해 컬럼을 암호화하는 경우가 있습니다.

`JPA` 사용이 너무나도 당연해진 요즘, 암호화 필드를 검색하기 위해서는 where절에 평문을 비교해야 할까요? 아니면 암호화 된 값을 비교해야 할까요?

뭔가 당연히 이거 겠지! 싶었던 건데 막상 답을 하려니 헷갈리시죠?

확인하기 위해 테스트를 해보았습니다.

> AES256 등의 알고리즘을 이용해 개인을 특정할 수 있는 항목을 암호화하게 되는데, salt를 사용하면 암호가 계속 바뀌게 되기 때문에 검색이 어려워집니다.  
> 따라서 여기서는 고정된 IV를 사용해 암호화를 하더라도 항상 같은 값을 이용하도록 설정하였습니다.

## Implementation

앞서 언급했듯이 JPA를 이용해야하기 때문에 spring boot 프로젝트를 생성하였습니다.

`JPA`를 이용한다는 것은 `EntityManager`를 이용한다는 것이고, 이는 `Querydsl` 또한 동잃하기 떄문에 두 가지를 모두 테스트 할 수 있는 환경을 구축하였습니다.

프로젝트 생성 자체를 설명하는 것이 이번 포스팅의 목적이 아니기 떄문에 자세한 설명은 생략합니다. 

### build.gradle

> 맨 위에 뱃지에 사용한 버전이 모두 나와있습니다.

```groovy
buildscript {
	ext {
		queryDslVersion = "5.0.0"
	}
}

plugins {
	id 'org.springframework.boot' version '2.7.2'
	id 'io.spring.dependency-management' version '1.0.12.RELEASE'
	id 'com.ewerk.gradle.plugins.querydsl' version '1.0.10'
	id 'java'
}

group = 'io.lcalmsky'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	compileOnly 'org.projectlombok:lombok'
	runtimeOnly 'com.h2database:h2'
	implementation "com.querydsl:querydsl-jpa:${queryDslVersion}"
	implementation "com.querydsl:querydsl-apt:${queryDslVersion}"
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
	useJUnitPlatform()
}

def querydslDir = "$buildDir/generated/querydsl"

querydsl {
	jpa = true
	querydslSourcesDir = querydslDir
}

sourceSets {
	main.java.srcDir querydslDir
}

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
	querydsl.extendsFrom compileClasspath
}

compileQuerydsl {
	options.annotationProcessorPath = configurations.querydsl
}
```

### Util 및 Converter 구현

**Aes256Utils.java**

```java
package io.lcalmsky.demo;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;

public class Aes256Utils {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";
  static final Decoder DECODER = Base64.getDecoder();
  static final Encoder ENCODER = Base64.getEncoder();
  private final SecretKeySpec keySpec;
  private static final byte[] IV = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  private final String encodedIv;

  public Aes256Utils() {
    byte[] key = DECODER.decode("01234567890123456789012345678901");
    keySpec = new SecretKeySpec(key, ALGORITHM);
    this.encodedIv = ENCODER.encodeToString(IV);
  }

  @SneakyThrows
  String encrypt(String plainText) {
    Cipher cipher = getCipher(encodedIv, Cipher.ENCRYPT_MODE);
    byte[] encrypted = cipher.doFinal(plainText.getBytes());
    return ENCODER.encodeToString(encrypted);
  }

  @SneakyThrows
  String decrypt(String cipherText) {
    Cipher cipher = getCipher(encodedIv, Cipher.DECRYPT_MODE);
    byte[] encrypted = DECODER.decode(cipherText);
    byte[] decrypted = cipher.doFinal(encrypted);
    return new String(decrypted);
  }

  private Cipher getCipher(String encodedIv, int decryptMode)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    IvParameterSpec ivSpec = new IvParameterSpec(DECODER.decode(encodedIv));
    cipher.init(decryptMode, keySpec, ivSpec);
    return cipher;
  }
}
```

단순 암복호화를 지원하는 유틸 클래스 입니다.

**Aes256Converter.java**
```java
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
```

데이터가 없는 경우를 제외하고는 암호화 실패시 에러를 반환하도록 하였고, 복호화 실패시에는 기존 DB 데이터를 반환하도록 예외처리를 추가하였습니다.

이렇게하면 암호화 되기 전 데이터와의 하위호환이 가능합니다.

### Entity, Repository 구현

**Account.java**
```java
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
```

전화번호 필드만 암호화하도록 컨버터를 추가하였습니다.

**AccountRepository.java**
```java
package io.lcalmsky.demo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByPhoneNumber(String phoneNumber);

  @Query("select a from Account a where a.phoneNumber = :phoneNumber")
  Optional<Account> findAccountByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
```

쿼리 메서드, JPQL 두 가지 방식을 모두 이용할 수 있도록 메서드를 추가하였습니다.

## Test

빌드를 실행하여 Querydsl이 Q클래스를 생성하도록 한 뒤 테스트를 작성하였습니다.

각 테스트는 쿼리 메서드, JPQL, Querydsl을 사용하여 암호화 컬럼을 where절로 사용하는 쿼리에 평문과 암호문을 각각 전달하도록 하였습니다.

```java
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
```

결과는

![](https://raw.githubusercontent.com/lcalmsky/column-encryption-demo/master/test.png)

모두 성공입니다!

## 동작 원리

평문만 전달했는데 어떤 원리로 암호화 필드를 검색한 것일까요?

직접 디버깅하면서 순서를 따라가 보았습니다.

1. 실행한 테스트로 진입합니다. 
2. 프록시를 호출하여 메서드를 실행시킵니다.
3. 등록된 다양한 인터셉터들을 실행시킵니다. 각 인터셉터는 `proceed`라는 메서드를 호출하여 자기 역할을 수행하고 다음으로 등록된 인터셉터를 호출합니다.
   1. `ExposeInvocationInterceptor`: 현재 `MethodInvocation`을 스레드 로컬 객체로 노출시키는 인터셉터
   2. `ReflectiveMethodInvocation`: `Reflection`을 사용해 대상 객체를 호출하는 인터셉터
   3. `CrudMethodMetadataPostProcessor`: 호출된 메서드에서 메타데이터 정보를 읽기 위해 인터셉터를 설정하는 프로세서(인터셉터 어드바이스 역할)
   4. `PersistenceExceptionTranslationInterceptor`: 예외가 발생하면 Spring의 `DataAccessException`(RuntimeException 형태)으로 변환해주는 인터셉터
   5. `TransactionInterceptor`: Spring 트랜잭션 관리를 위해 메서드를 올바른 순서로 호출해주는 인터셉터
   6. `DefaultMethodInvokingMethodInterceptor`: 리파지토리 프록시에서 기본 메서드를 호출하는 인터셉터
   7. `QueryExecutorMethodInterceptor`: 사용자 정의 구현 메서드 호출을 위한 인터셉터
4. 인터셉터가 순차적으로 호출된 뒤에 `RepositoryMethodInvoker`를 호출해 리파지토리 메서드를 호출합니다.
5. `AbstractJpaQuery`에서 `execute`가 호출되면 구현체(자식클래스)인 `JpaQueryExecution`의 `execute`가 호출됩니다.
6. 이미 메서드를 해석하여 한 개만 가져오는 것인지 리스트로 가져오는 것인지 알고있기 때문에 이 테스트에선 `SingleEntityExecution`의 `doExecute`가 호출되면서 쿼리를 생성합니다.
7. 다양한 생성 과정과 `flush` 설정 등을 거쳐 Action(`EntityInsertAction`)이 수행됩니다.
8. 값을 전달하면서 값의 타입을 검사하고 `AttributeConverterSqlTypeDescriptorAdapter`가 `getBinder`를 호출해 `AttributeConverter`를 실행시킵니다.
9. `AttributeConverterBean`에서 등록한 컨버터를 불러와 `convertToDatabaseColumn`를 수행합니다.

실제로는 훨씬 복잡하지만 간단하게 정리해보았습니다.

## 결론

`JPA`나 `Querydsl`을 사용하는 경우 조건절에서 비교할 컬럼이 암호화 되어있는 경우, 실제로는 평문을 전달하여도 `@Converter`가 등록되어 있는 경우 `AttributeConverter` 구현체의 `convertToDatabaseColumn`가 호출되어 암호화 한 값으로 비교를 진행하게 됩니다. 

