package io.lcalmsky.demo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByPhoneNumber(String phoneNumber);
}