package com.lutetiae.controller;

import com.lutetiae.service.BooksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 书籍处理 controller
 */
@Controller
public class BooksController {

    private static final Logger logger = LoggerFactory.getLogger(BooksController.class);

    private final BooksService booksService;

    private final ResourceLoader resourceLoader;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.meta-data-name}")
    private String metaDataName;

    public BooksController(BooksService booksService, ResourceLoader resourceLoader) {
        this.booksService = booksService;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 首页
     * @return 首页 HTML
     */
    @GetMapping("/")
    public String showUploadForm() {
        return "upload";
    }

    /**
     * 文件上传
     *
     * @param file 待上传文件信息
     * @return 文件上传 HTML
     */
    @PostMapping("/upload")
    public String fileUpload(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            logger.warn("请至少选择一个文件上传");
            return "upload";
        }
        try {
            booksService.uploadFile(file);
        } catch (IOException e) {
            logger.error("文件上传失败: " + e.getMessage());
        }
        return "upload";
    }

    /**
     * 图书列表
     *
     * @param model 数据信息
     * @return 文件列表 HTML
     * @throws IOException 异常
     */
    @GetMapping("/books")
    public String listFiles(Model model) throws IOException {
        Map<String, Map<String, Object>> books = booksService.getFilesMetadata();
        if (books.isEmpty()) {
            logger.info("当前文件列表为空！");
        }
        model.addAttribute("books", books);
        return "list";
    }

    /**
     * 下载文件
     *
     * @param id json id
     * @return 响应信息
     */
    @GetMapping("/download/{id:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) {
        return booksService.downloadFile(id);
    }

    /**
     * 删除文件
     *
     * @param id 文件 id
     * @return 重定向到图书列表页面
     */
    @GetMapping("/delete/{id}")
    public String deleteFile(@PathVariable String id) {
        try {
            booksService.deleteFile(id);
        } catch (IOException e) {
            logger.error("删除文件失败：" + e.getMessage());
        }
        return "redirect:/books";
    }

    /**
     * 访问元数据
     *
     * @return 元数据信息
     * @throws Exception 异常
     */
    @GetMapping(value = "/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getMetadata() throws Exception {
        Resource resource = resourceLoader.getResource("file:" + uploadDir + metaDataName);
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(content);
    }
}
