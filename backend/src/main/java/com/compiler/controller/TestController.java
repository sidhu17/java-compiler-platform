package com.compiler.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {

    @GetMapping("/ping")
    public String ping() {
        return "✅ Backend is working!";
    }
}
