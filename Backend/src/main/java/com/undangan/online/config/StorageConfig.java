package com.undangan.online.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {

    @Value("${app.storage.themes:/app/storage/themes}")
    private String themesPath;

    @Value("${app.storage.musics:/app/storage/musics}")
    private String musicsPath;

    @Value("${app.storage.images:/app/storage/images}")
    private String imagesPath;

    @Value("${MAX_MUSIC_SIZE_MB:10}")
    private long maxMusicSizeMb;

    @Value("${MAX_IMAGE_SIZE_MB:5}")
    private long maxImageSizeMb;

    @Bean
    public Path themesStoragePath() {
        return initPath(Paths.get(themesPath), "STORAGE_THEMES_PATH");
    }

    @Bean
    public Path musicsStoragePath() {
        return initPath(Paths.get(musicsPath), "STORAGE_MUSICS_PATH");
    }

    @Bean
    public Path imagesStoragePath() {
        return initPath(Paths.get(imagesPath), "STORAGE_IMAGES_PATH");
    }

    private Path initPath(Path path, String envName) {
        if (!Files.exists(path)) {
            throw new IllegalStateException(envName + " tidak ditemukan: " + path);
        }
        return path;
    }

    public long getMaxMusicSizeMb() {
        return maxMusicSizeMb;
    }

    public long getMaxImageSizeMb() {
        return maxImageSizeMb;
    }
}
