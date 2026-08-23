package com.undangan.online.service;

import com.undangan.online.entity.Role;
import com.undangan.online.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ClientInitService {

    private static final Logger log = LoggerFactory.getLogger(ClientInitService.class);

    private final RoleRepository roleRepository;

    public ClientInitService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initMasterData() {
        try {
            List<String> defaultRoles = List.of("ADMIN", "STAFF", "USER");
            for (String code : defaultRoles) {
                if (roleRepository.findById(code).isEmpty()) {
                    Role role = new Role();
                    role.setCode(code);
                    role.setName(code);
                    role.setDescription("Role default: " + code);
                    role.setIsSystem(true);
                    role.setCreatedAt(OffsetDateTime.now());
                    roleRepository.save(role);
                    log.info("Default role '{}' created during startup", code);
                }
            }
        } catch (Exception ex) {
            log.error("Gagal init master data: {}", ex.getMessage(), ex);
        }
    }
}
