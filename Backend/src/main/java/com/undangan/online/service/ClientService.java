package com.undangan.online.service;

import com.undangan.online.dto.ClientDto;
import com.undangan.online.dto.CreateClientRequest;
import com.undangan.online.dto.UpdateClientRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {
    Page<ClientDto> list(String status, String search, Pageable pageable);
    ClientDto getById(Long id);
    ClientDto create(CreateClientRequest request);
    ClientDto update(Long id, UpdateClientRequest request);
    ClientDto updateStatus(Long id, String status);
    ClientDto extendExpiry(Long id, Integer additionalMonths);
    void softDelete(Long id);
}
