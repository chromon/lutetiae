package com.lutetiae.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 书籍处理 service
 */
@Service
public class BooksService {

    // 文件上传目录
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 元数据文件名
    @Value("${file.meta-data-name}")
    private String metaDataName;

    private final Map<String, Map<String, Object>> fileMetadata = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(BooksService.class);

    /**
     * 处理文件上传
     *
     * @param file 待上传文件信息
     * @throws IOException 异常
     */
    public void uploadFile(MultipartFile file) throws IOException {
        // 创建上传目录（如果不存在）
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            boolean mkdirs = directory.mkdirs();
            if (!mkdirs) {
                logger.error("创建上传目录失败！");
                return;
            }
        }

        // 检查文件是否已存在
        if (isFileExist(file)) {
            return;
        }

        // 保存文件
        String fileName = file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.write(filePath, file.getBytes());
        logger.info("上传文件成功：" + fileName);

        //    {
        //        id: {
        //            name: "",
        //            type: "",
        //            size: "",
        //        }
        //    }
        // 创建元数据
        Map<String, Object> metadata = new HashMap<>();
        // 生成唯一 id
        DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(DEFAULT_FORMATTER);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String id = String.format("%s_%s", timestamp, uniqueId);

        metadata.put("name", fileName);
        metadata.put("type", file.getContentType());
        metadata.put("size", file.getSize());
        fileMetadata.put(id, metadata);

        // 保存元数据为JSON文件
        saveMetadata();
        logger.info("添加元数据信息成功：" + fileName);
    }

    /**
     * 判断待上传文件是否已存在
     *
     * @param file 待上传文件信息
     * @return 文件是否已存在
     */
    private boolean isFileExist(MultipartFile file) {
        Map<String, Map<String, Object>> existingMetadata = getFilesMetadata();

        String value = file.getOriginalFilename();
        long size = file.getSize();

        boolean nameExists = existingMetadata.values().stream()
                .anyMatch(innerMap -> {
                    assert value != null;
                    return value.equals(innerMap.get("name"));
                });
        if (nameExists) {
            boolean sizeExists = existingMetadata.values().stream()
                    .anyMatch(innerMap -> {
                        long existingSize = Long.parseLong(innerMap.get("size").toString());
                        return (size == existingSize);
                    });
            if (!sizeExists) {
                logger.info("已存在的同名文件，文件大小不同：" + file.getOriginalFilename());
            } else {
                logger.info("已存在相同文件：" + file.getOriginalFilename());
            }
            return true;
        }
        return false;
    }

    /**
     * 保存文件元数据信息
     *
     * @throws IOException 异常
     */
    private void saveMetadata() throws IOException {
        Path jsonFilePath = Paths.get(uploadDir, metaDataName);
        ObjectMapper objectMapper = new ObjectMapper();

        // 如果 JSON 文件已存在，读取并合并元数据信息
        if (Files.exists(jsonFilePath)) {
            JsonNode existingData = objectMapper.readTree(jsonFilePath.toFile());
            ObjectNode newData = objectMapper.valueToTree(fileMetadata);
            newData.setAll((ObjectNode) existingData);
            objectMapper.writeValue(jsonFilePath.toFile(), newData);
        } else {
            // 如果 JSON 文件不存在，直接写入元数据信息
            objectMapper.writeValue(jsonFilePath.toFile(), fileMetadata);
        }
    }

    /**
     * 获取全部文件的元数据信息
     *
     * @return 元数据信息 map
     */
    public Map<String, Map<String, Object>> getFilesMetadata() {
        Path jsonFilePath = Paths.get(uploadDir, metaDataName);

        try {
            // 如果文件不存在，创建一个空的 JSON 文件
            if (!Files.exists(jsonFilePath)) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(jsonFilePath.toFile(), new HashMap<>());
                logger.info("元数据信息文件未初始化，重新创建新文件");
                return new HashMap<>();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonFilePath.toFile());

            Map<String, Map<String, Object>> metadata = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String fileName = entry.getKey();
                JsonNode fileMetadata = entry.getValue();

                Map<String, Object> map = new HashMap<>();
                Iterator<String> fieldIterator = fileMetadata.fieldNames();
                while (fieldIterator.hasNext()) {
                    String field = fieldIterator.next();
                    map.put(field, fileMetadata.get(field).asText());
                }

                metadata.put(fileName, map);
            }
            return metadata;
        } catch (IOException e) {
            logger.error("获取元数据信息失败: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 下载文件
     *
     * @param id 文件名
     * @return 响应信息
     */
    public ResponseEntity<Resource> downloadFile(String id) {
        Map<String, Map<String, Object>> metadata = getFilesMetadata();
        String fileName = (String) metadata.get(id).get("name");

        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String encodedFileName = URLEncoder
                        .encode(Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20");
                String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;

                logger.info("下载文件成功：" + fileName);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除文件
     *
     * @param id 文件 id
     * @return 是否删除成功
     * @throws IOException 异常
     */
    public boolean deleteFile(String id) throws IOException {
        Map<String, Map<String, Object>> metadata = getFilesMetadata();
        String fileName = (String) metadata.get(id).get("name");

        Path filePath = Paths.get(uploadDir, fileName);
        Path jsonFilePath = Paths.get(uploadDir, metaDataName);

        // 删除源文件
        boolean isFileDeleted = Files.deleteIfExists(filePath);

        // 从 JSON 文件中删除元数据
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonFilePath.toFile());

        if (rootNode.has(id)) {
            ((ObjectNode) rootNode).remove(id);
            objectMapper.writeValue(jsonFilePath.toFile(), rootNode);
            logger.info("删除源文件以及元数据信息成功：" + fileName);
            return isFileDeleted;
        }

        return false;
    }
}
