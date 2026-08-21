package com.undangan.online.repository;

import com.undangan.online.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemParameterRepository extends JpaRepository<SystemParameter, Long> {
    Optional<SystemParameter> findByGroupCodeAndCode(String groupCode, String code);
}
