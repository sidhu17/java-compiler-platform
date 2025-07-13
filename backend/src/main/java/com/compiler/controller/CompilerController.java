package com.compiler.controller;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CompilerController {

    static class CodeRequest {
        public String code;
        public String input;
    }

    @PostMapping("/run")
    public ResponseEntity<String> runCode(@RequestBody CodeRequest request) {
        try {
            Map<String, String> files = splitCodeByFiles(request.code);

            Path tempDir = Files.createTempDirectory("javacode");
            for (Map.Entry<String, String> entry : files.entrySet()) {
                Files.writeString(tempDir.resolve(entry.getKey()), entry.getValue());
            }

            Path dockerfile = tempDir.resolve("Dockerfile");
            StringBuilder dockerContent = new StringBuilder();
            dockerContent.append("FROM openjdk:17\n")
                         .append("WORKDIR /usr/src/app\n");

            for (String filename : files.keySet()) {
                dockerContent.append("COPY ").append(filename).append(" .\n");
            }

            dockerContent.append("RUN javac Main.java\n")
                         .append("CMD [\"java\", \"Main\"]\n");
            Files.writeString(dockerfile, dockerContent);

            ProcessBuilder build = new ProcessBuilder("docker", "build", "-t", "javacode", ".");
            build.directory(tempDir.toFile());
            build.redirectErrorStream(true);
            Process p1 = build.start();
            String buildLog = new String(p1.getInputStream().readAllBytes());
            int buildStatus = p1.waitFor();
            if (buildStatus != 0) return ResponseEntity.ok("Build Error:\n" + buildLog);

            List<String> command = new ArrayList<>(Arrays.asList(
                "docker", "run", "--rm", "-i", "javacode"
            ));
            ProcessBuilder run = new ProcessBuilder(command);
            run.redirectErrorStream(true);
            Process p2 = run.start();

            if (request.input != null && !request.input.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p2.getOutputStream()))) {
                    writer.write(request.input);
                    writer.flush();
                }
            }

            String output = new String(p2.getInputStream().readAllBytes());
            p2.waitFor();

            return ResponseEntity.ok(output);

        } catch (Exception e) {
            e.printStackTrace(); // <-- this will help you see detailed logs
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