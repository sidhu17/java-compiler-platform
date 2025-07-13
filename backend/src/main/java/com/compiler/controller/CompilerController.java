package com.compiler.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class CompilerController {

    @PostMapping("/run")
    public ResponseEntity<String> runCode(@RequestBody String code) {
        try {
            Path javaFile = Paths.get("Main.java");
            Files.writeString(javaFile, code);

            ProcessBuilder build = new ProcessBuilder("docker", "build", "-t", "javacode", ".");
            build.redirectErrorStream(true);
            Process p1 = build.start();
            p1.waitFor();

            ProcessBuilder run = new ProcessBuilder("docker", "run", "--rm", "javacode");
            run.redirectErrorStream(true);
            Process p2 = run.start();
            String output = new String(p2.getInputStream().readAllBytes());
            p2.waitFor();

            return ResponseEntity.ok(output);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}