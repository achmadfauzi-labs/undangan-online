package com.undangan.online.service.impl;

import com.undangan.online.dto.ClientDto;
import com.undangan.online.dto.CreateClientRequest;
import com.undangan.online.dto.UpdateClientRequest;
import com.undangan.online.entity.Client;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.ClientRepository;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.repository.UsersRepository;
import com.undangan.online.service.ClientService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientServiceImpl.class);

    private final ClientRepository clientRepository;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvitationRepository invitationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ClientServiceImpl(ClientRepository clientRepository,
                             UsersRepository usersRepository,
                             PasswordEncoder passwordEncoder,
                             InvitationRepository invitationRepository) {
        this.clientRepository = clientRepository;
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.invitationRepository = invitationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDto> list(String status, String search, Pageable pageable) {
        List<Client> all = clientRepository.findAll();

        List<Client> filtered = all.stream()
                .filter(c -> {
                    if (status == null || status.isBlank()) return true;
                    if ("expired".equals(status)) {
                        return c.getExpiresAt().isBefore(LocalDate.now());
                    }
                    return status.equals(c.getStatus());
                })
                .filter(c -> search == null || search.isBlank() ||
                        (c.getName() != null && c.getName().toLowerCase().contains(search.toLowerCase())) ||
                        (c.getCode() != null && c.getCode().toLowerCase().contains(search.toLowerCase())))
                .sorted(Comparator.comparing(Client::getCreatedAt).reversed())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<ClientDto> content = new ArrayList<>();
        for (int i = start; i < end; i++) {
            content.add(toDto(filtered.get(i)));
        }

        return new PageImpl<>(content, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Client tidak ditemukan"));

        ClientDto dto = toDto(client);
        dto.setUserCount((long) usersRepository.findByClientId(id).size());

        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(i) FROM Invitation i WHERE i.clientId = :clientId", Long.class);
        query.setParameter("clientId", id);
        dto.setInvitationCount(query.getSingleResult());

        return dto;
    }

    @Override
    @Transactional
    public ClientDto create(CreateClientRequest request) {
        if (clientRepository.findByCode(request.getCode()).isPresent()) {
            log.warn("Create client failed: code '{}' already exists", request.getCode());
            throw new AuthException("CONFLICT", "Code client sudah digunakan", 409);
        }

        Client client = new Client();
        client.setCode(request.getCode());
        client.setName(request.getName());
        client.setPhone(request.getPhone());
        client.setCompany(request.getCompany());
        client.setAddress(request.getAddress());
        client.setNotes(request.getNotes());
        client.setDurationMonths(request.getDurationMonths());
        client.setStatus("active");
        client.setActivatedAt(LocalDate.now());
        client.setExpiresAt(LocalDate.now().plusMonths(request.getDurationMonths()));
        client.setCreatedAt(OffsetDateTime.now());
        client.setUpdatedAt(OffsetDateTime.now());

        clientRepository.save(client);

        Users defaultUser = new Users();
        defaultUser.setUsername(request.getCode() + "_admin");
        defaultUser.setEmail(request.getCode() + "@placeholder.local");
        defaultUser.setPasswordHash(passwordEncoder.encode("changeme123"));
        defaultUser.setName(request.getName() + " Admin");
        defaultUser.setPhone(request.getPhone());
        defaultUser.setRoleCode("USER");
        defaultUser.setClientId(client.getId());
        defaultUser.setStatus("active");
        defaultUser.setCreatedAt(OffsetDateTime.now());
        defaultUser.setUpdatedAt(OffsetDateTime.now());

        usersRepository.save(defaultUser);

        log.info("Client {} created by admin action", request.getCode());
        return toDto(client);
    }

    @Override
    @Transactional
    public ClientDto update(Long id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Client tidak ditemukan"));

        if (request.getName() != null) {
            client.setName(request.getName());
        }
        if (request.getPhone() != null) {
            client.setPhone(request.getPhone());
        }
        if (request.getCompany() != null) {
            client.setCompany(request.getCompany());
        }
        if (request.getAddress() != null) {
            client.setAddress(request.getAddress());
        }
        if (request.getNotes() != null) {
            client.setNotes(request.getNotes());
        }
        if (request.getDurationMonths() != null && request.getDurationMonths() > 0) {
            client.setDurationMonths(request.getDurationMonths());
            client.setExpiresAt(client.getActivatedAt().plusMonths(request.getDurationMonths()));
        }

        client.setUpdatedAt(OffsetDateTime.now());
        clientRepository.save(client);

        log.info("Client {} updated by admin action", id);
        return toDto(client);
    }

    @Override
    @Transactional
    public ClientDto updateStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new AuthException("VALIDATION_ERROR", "Status harus 'active' atau 'inactive'", 400);
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Client tidak ditemukan"));

        client.setStatus(status);
        client.setUpdatedAt(OffsetDateTime.now());
        clientRepository.save(client);

        log.info("Client {} status changed to {} by admin action", id, status);
        return toDto(client);
    }

    @Override
    @Transactional
    public ClientDto extendExpiry(Long id, Integer additionalMonths) {
        if (additionalMonths == null || additionalMonths <= 0) {
            throw new AuthException("VALIDATION_ERROR", "additionalMonths harus lebih dari 0", 400);
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Client tidak ditemukan"));

        client.setExpiresAt(client.getExpiresAt().plusMonths(additionalMonths));
        client.setDurationMonths((short) (client.getDurationMonths() + additionalMonths));
        client.setUpdatedAt(OffsetDateTime.now());
        clientRepository.save(client);

        log.info("Client {} expiry extended by {} months by admin action", id, additionalMonths);
        return toDto(client);
    }

    private ClientDto toDto(Client client) {
        ClientDto dto = new ClientDto();
        dto.setId(client.getId());
        dto.setCode(client.getCode());
        dto.setName(client.getName());
        dto.setPhone(client.getPhone());
        dto.setCompany(client.getCompany());
        dto.setAddress(client.getAddress());
        dto.setActivatedAt(client.getActivatedAt());
        dto.setExpiresAt(client.getExpiresAt());
        dto.setStatus(client.getStatus());
        dto.setDurationMonths(client.getDurationMonths());
        dto.setNotes(client.getNotes());
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());
        return dto;
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Client tidak ditemukan"));

        client.setStatus("inactive");
        client.setUpdatedAt(OffsetDateTime.now());
        clientRepository.save(client);

        log.warn("Client {} soft-deleted by admin action", id);
    }

}
