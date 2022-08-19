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