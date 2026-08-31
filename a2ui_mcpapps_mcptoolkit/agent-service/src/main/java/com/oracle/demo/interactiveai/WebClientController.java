package com.oracle.demo.interactiveai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Files;
import java.nio.file.Path;

@Controller
final class WebClientController {
    private final Path webRoot;

    WebClientController(@Value("${web.root:../web-client}") String webRoot) {
        this.webRoot = Path.of(webRoot).toAbsolutePath().normalize();
    }

    @GetMapping({"/", "/{*path}"})
    ResponseEntity<Resource> staticFile(
            @PathVariable(name = "path", required = false) String requestPath) {
        String filename = requestPath == null || requestPath.equals("/")
                ? "index.html"
                : requestPath.replaceFirst("^/", "");
        if (!filename.matches("[A-Za-z0-9._/-]+") || filename.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        Path file = webRoot.resolve(filename).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = filename.endsWith(".js")
                ? MediaType.valueOf("text/javascript")
                : filename.endsWith(".css")
                        ? MediaType.valueOf("text/css")
                        : MediaType.TEXT_HTML;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(file));
    }
}
