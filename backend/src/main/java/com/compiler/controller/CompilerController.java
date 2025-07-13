package com.compiler.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Or use your frontend origin
public class CompilerController {

    @PostMapping("/run")
    public ResponseEntity<String> runCode(@RequestBody String code) {
        try {
            // Split input into multiple files
            Map<String, String> files = splitCodeByFiles(code);

            // Create a temp directory
            Path tempDir = Files.createTempDirectory("javacode");
            for (Map.Entry<String, String> entry : files.entrySet()) {
                Path filePath = tempDir.resolve(entry.getKey());
                Files.writeString(filePath, entry.getValue());
            }

            // Compile
            ProcessBuilder compile = new ProcessBuilder("javac", "*.java");
            compile.directory(tempDir.toFile());
            compile.redirectErrorStream(true);
            Process p1 = compile.start();
            String compileOutput = new String(p1.getInputStream().readAllBytes());
            int compileStatus = p1.waitFor();

            if (compileStatus != 0) {
                return ResponseEntity.ok("Compilation Error:\n" + compileOutput);
            }

            // Run Main
            ProcessBuilder run = new ProcessBuilder("java", "Main");
            run.directory(tempDir.toFile());
            run.redirectErrorStream(true);
            Process p2 = run.start();
            String runOutput = new String(p2.getInputStream().readAllBytes());
            p2.waitFor();

            return ResponseEntity.ok(runOutput);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private Map<String, String> splitCodeByFiles(String input) {
        Map<String, String> files = new LinkedHashMap<>();
        String[] parts = input.split("// File: ");
        for (String part : parts) {
            if (part.trim().isEmpty()) continue;
            int newline = part.indexOf("\n");
            String filename = part.substring(0, newline).trim();
            String content = part.substring(newline + 1);
            files.put(filename, content);
        }
        return files;
    }
}