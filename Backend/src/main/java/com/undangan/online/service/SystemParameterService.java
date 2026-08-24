package com.undangan.online.service;

import com.undangan.online.dto.CreateSystemParameterRequest;
import com.undangan.online.dto.SystemParameterDto;

import java.util.List;
import java.util.Map;

public interface SystemParameterService {

    List<SystemParameterDto> listAll(String groupCode, String status, String search);

    SystemParameterDto getById(Long id);

    List<SystemParameterDto> getByGroup(String groupCode);

    SystemParameterDto getValue(String groupCode, String code);

    SystemParameterDto create(CreateSystemParameterRequest request);

    SystemParameterDto update(Long id, CreateSystemParameterRequest request);

    SystemParameterDto updateStatus(Long id, String status);

    void delete(Long id);

    Map<String, Object> batchUpdateStatus(List<Map<String, Object>> updates);

    Map<String, Object> seedDefaultParameters();
}
