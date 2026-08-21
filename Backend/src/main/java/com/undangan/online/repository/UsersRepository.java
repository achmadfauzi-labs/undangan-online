package com.undangan.online.repository;

import com.undangan.online.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);

    Optional<Users> findByEmail(String email);

    List<Users> findByClientId(Long clientId);
}
