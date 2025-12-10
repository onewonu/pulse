package com.pulse.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Pulse API");
        response.put("timestamp", LocalDateTime.now());
        response.put("protocol", request.getScheme());
        response.put("secure", request.isSecure());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("received", payload);
        response.put("timestamp", LocalDateTime.now());
        response.put("protocol", request.getScheme());
        response.put("secure", request.isSecure());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/headers")
    public ResponseEntity<Map<String, Object>> headers(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> headers = new HashMap<>();

        request.getHeaderNames().asIterator()
                .forEachRemaining(name -> headers.put(name, request.getHeader(name)));

        response.put("headers", headers);
        response.put("protocol", request.getScheme());
        response.put("secure", request.isSecure());
        response.put("remoteAddr", request.getRemoteAddr());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
