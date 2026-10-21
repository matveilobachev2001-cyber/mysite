package com.example.mysite;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Static_handler implements HttpHandler {

    private final Path root;

    public Static_handler(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        Path file = root.resolve(path.substring(1)).normalize();
        if (!file.startsWith(root) || !Files.exists(file) || Files.isDirectory(file)) {
            ex.sendResponseHeaders(404, -1);
            return;
        }

        String mime = Files.probeContentType(file);
        if (mime == null) mime = "application/octet-stream";
        if (mime.startsWith("text/") || mime.equals("application/javascript")) {
            mime += "; charset=utf-8";
        }

        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, Files.size(file));
        try (OutputStream out = ex.getResponseBody()) {
            Files.copy(file, out);
        }
    }
}
