package com.example.board.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class StorageController {

    private static final Logger logger = LoggerFactory.getLogger(StorageController.class);
    
    // PHP/Laravel 시절의 storage 구조를 유지하기 위한 매핑
    @GetMapping("/storage/{*filePath}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String filePath, HttpServletRequest request) {
        try {
            logger.info("Storage 요청 수신: {}", filePath);
            
            // 파일 경로에서 불필요한 슬래시 등 정리
            String decodedFilePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8);
            if (decodedFilePath.startsWith("/")) {
                decodedFilePath = decodedFilePath.substring(1);
            }
            
            // DB에 'attachments/'가 포함되어 있으므로, 베이스 경로 설정 시 주의
            // 기본 베이스: /var/www/html/storage/app/public/
            Path baseDir = Paths.get("/var/www/html/storage/app/public");
            Path file = baseDir.resolve(decodedFilePath).normalize();
            
            logger.info("파일 실제 탐색 경로: {}", file.toString());
            
            if (!Files.exists(file)) {
                logger.warn("파일을 찾을 수 없음: {}", file.toString());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("리소스를 읽을 수 없음: {}", file.toString());
                return ResponseEntity.notFound().build();
            }
            
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName().toString() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("파일 다운로드 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
