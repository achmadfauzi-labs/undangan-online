package com.undangan.online.service;

import com.undangan.online.entity.Role;
import com.undangan.online.repository.RoleRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientInitService {

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
                    role.setCreatedAt(java.time.OffsetDateTime.now());
                    roleRepository.save(role);
                }
            }
        } catch (Exception ex) {
            System.err.println("Gagal init master data: " + ex.getMessage());
        }
    }
}
