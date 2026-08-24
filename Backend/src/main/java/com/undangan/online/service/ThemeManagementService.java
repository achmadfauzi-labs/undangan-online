package com.undangan.online.service;

import com.undangan.online.dto.CreateThemeRequest;
import com.undangan.online.dto.ThemeDto;
import com.undangan.online.dto.ThemeListDto;
import com.undangan.online.dto.UpdateThemeRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ThemeManagementService {

    List<ThemeListDto> listAll(String status, String category, String search);

    ThemeDto getById(Long id);

    ThemeDto create(CreateThemeRequest request, MultipartFile zipFile) throws IOException;

    ThemeDto update(Long id, UpdateThemeRequest request, MultipartFile zipFile) throws IOException;

    ThemeDto updateStatus(Long id, String status);

    void delete(Long id);

    List<String> listCategories();

    ThemeDto uploadZip(Long id, MultipartFile zipFile) throws IOException;

    List<ThemeListDto> listActive();

    ThemeDto getActiveById(Long id);

    void assignToMyInvitation(Long clientId, Long themeId);

    ThemeDto getCurrentTheme(Long clientId);

    void removeThemeFromInvitation(Long clientId);
}
