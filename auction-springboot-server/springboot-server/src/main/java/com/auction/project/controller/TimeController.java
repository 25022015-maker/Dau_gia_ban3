package com.auction.project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TimeController {

    @GetMapping("/time")
    public ResponseEntity<Map<String, String>> getServerTime() {
        return ResponseEntity.ok(Map.of("serverTime", LocalDateTime.now().toString()));
    }
}