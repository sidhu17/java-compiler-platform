package com.compiler.controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CompilerController {

    @Value("${compiler.rapidapi.key}")
    private String rapidApiKey;

    @Value("${compiler.rapidapi.host}")
    private String rapidApiHost;

    private static final String SUBMISSION_URL = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=true&wait=false";
    private static final String RESULT_URL = "https://judge0-ce.p.rapidapi.com/submissions/";

    private final RestTemplate restTemplate = new RestTemplate();

    private String encode(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    @PostMapping("/run")
    public ResponseEntity<String> runJavaCode(@RequestBody Map<String, String> payload) {
        try {
            String code = payload.get("source_code");
            String stdin = payload.getOrDefault("input", "");

            System.out.println("=== Java Code ===\n" + code);
            System.out.println("=== Input ===\n" + stdin);

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Java code cannot be empty.");
            }

            // Prepare base64-encoded request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("language_id", 62); // Java (OpenJDK 17)
            requestBody.put("source_code", encode(code));
            requestBody.put("stdin", encode(stdin));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-rapidapi-key", rapidApiKey);
            headers.set("x-rapidapi-host", rapidApiHost);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> submission = restTemplate.postForEntity(SUBMISSION_URL, request, Map.class);

            if (!submission.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(500).body("Submission failed.");
            }

            String token = (String) submission.getBody().get("token");
            if (token == null) {
                return ResponseEntity.status(500).body("No token returned.");
            }

            // Poll for result
            Map<?, ?> result = null;
            for (int i = 0; i < 10; i++) {
                ResponseEntity<Map> poll = restTemplate.exchange(
                    RESULT_URL + token, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
                result = poll.getBody();
                if (result == null) break;
                Object status = ((Map<?, ?>) result.get("status")).get("description");
                if (status != null && !status.toString().matches("(?i)In Queue|Processing")) break;
                Thread.sleep(1000);
            }

            if (result == null) return ResponseEntity.status(500).body("No result found.");

            String stdout = Objects.toString(result.get("stdout"), "");
            String stderr = Objects.toString(result.get("stderr"), "");
            String compileOutput = Objects.toString(result.get("compile_output"), "");

            if (!stdout.isBlank()) return ResponseEntity.ok(stdout);
            if (!compileOutput.isBlank()) return ResponseEntity.ok("Compilation Error:\n" + compileOutput);
            if (!stderr.isBlank()) return ResponseEntity.ok("Runtime Error:\n" + stderr);

            return ResponseEntity.ok("No output.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }
}