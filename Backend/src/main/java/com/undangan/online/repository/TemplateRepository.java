package com.undangan.online.repository;

import com.undangan.online.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByStatus(String status);

    Optional<Template> findByCode(String code);
}
