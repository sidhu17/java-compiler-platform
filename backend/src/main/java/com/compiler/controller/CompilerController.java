package com.compiler.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    private static final String RAPIDAPI_KEY = "YOUR_RAPIDAPI_KEY";
    private static final String RAPIDAPI_HOST = "judge0-ce.p.rapidapi.com";
    private static final String SUBMISSION_URL = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=false&wait=false";
    private static final String RESULT_URL = "https://judge0-ce.p.rapidapi.com/submissions/";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/run")
    public ResponseEntity<String> runJavaCode(@RequestBody Map<String, String> payload) {
        try {
            String code = payload.get("source_code");
            String input = payload.getOrDefault("input", "");

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Java code cannot be empty.");
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("language_id", 62); // Java 17
            requestBody.put("source_code", code);
            requestBody.put("stdin", input);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-rapidapi-key", RAPIDAPI_KEY);
            headers.set("x-rapidapi-host", RAPIDAPI_HOST);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> submission = restTemplate.postForEntity(SUBMISSION_URL, request, Map.class);

            String token = (String) submission.getBody().get("token");
            Map<?, ?> result = null;

            for (int i = 0; i < 10; i++) {
                ResponseEntity<Map> poll = restTemplate.exchange(
                        RESULT_URL + token,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class);
                result = poll.getBody();
                String status = ((Map<?, ?>) result.get("status")).get("description").toString();
                if (!status.equalsIgnoreCase("In Queue") && !status.equalsIgnoreCase("Processing")) break;
                Thread.sleep(1000);
            }

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