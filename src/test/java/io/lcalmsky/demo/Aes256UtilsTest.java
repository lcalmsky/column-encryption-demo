package io.lcalmsky.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Aes256UtilsTest {

  public static final String PLAINTEXT = "plaintext";
  private final String CIPHER = "k/eAC8IaNzEYIQul+73ppw==";

  @Test
  @DisplayName("암호화 테스트")
  void test() {
    Aes256Utils aes256Utils = new Aes256Utils();
    String cipher = aes256Utils.encrypt(PLAINTEXT);
    assertEquals(CIPHER, cipher);
  }

  @Test
  @DisplayName("IV가 동일하기 때문에 여러 번 반복해도 같은 결과를 얻음")
  void test2() {
    Aes256Utils aes256Utils = new Aes256Utils();
    String cipher = aes256Utils.encrypt(PLAINTEXT);
    assertEquals(CIPHER, cipher);
    String cipher2 = aes256Utils.encrypt(PLAINTEXT);
    assertEquals(CIPHER, cipher2);
  }

  @Test
  @DisplayName("복호화 테스트")
  void test3() {
    Aes256Utils aes256Utils = new Aes256Utils();
    String plainText = aes256Utils.decrypt(CIPHER);
    assertEquals(PLAINTEXT, plainText);
  }
}