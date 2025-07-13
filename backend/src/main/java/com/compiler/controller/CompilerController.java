package com.compiler.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private static final String RAPIDAPI_KEY = "e004417eb7msh105ea63cbc83000p15f4a5jsn620d1c0e206a";
    private static final String RAPIDAPI_HOST = "judge0-ce.p.rapidapi.com";
    private static final String SUBMISSION_URL = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=false&wait=false";
    private static final String RESULT_URL = "https://judge0-ce.p.rapidapi.com/submissions/";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/run")
    public ResponseEntity<String> runJavaCode(@RequestBody String combinedCode) {
        try {
            Map<String, String> files = splitCodeByFiles(combinedCode);
            String code = files.getOrDefault("Main.java", files.values().iterator().next());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("language_id", 62); // Java (OpenJDK 17)
            requestBody.put("source_code", code);
            requestBody.put("stdin", ""); // Optional input

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-rapidapi-key", RAPIDAPI_KEY);
            headers.set("x-rapidapi-host", RAPIDAPI_HOST);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(SUBMISSION_URL, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(500).body("Error creating submission.");
            }

            String token = (String) response.getBody().get("token");

            Map result = null;
            for (int i = 0; i < 10; i++) {
                ResponseEntity<Map> poll = restTemplate.exchange(
                        RESULT_URL + token,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class
                );

                result = poll.getBody();
                String status = ((Map<String, Object>) result.get("status")).get("description").toString();

                if (!status.equalsIgnoreCase("In Queue") && !status.equalsIgnoreCase("Processing")) {
                    break;
                }

                Thread.sleep(1000);
            }

            String output = (String) result.get("stdout");
            String stderr = (String) result.get("stderr");
            String compileOutput = (String) result.get("compile_output");

            if (output != null) return ResponseEntity.ok(output);
            else if (compileOutput != null) return ResponseEntity.ok("Compilation Error:\n" + compileOutput);
            else if (stderr != null) return ResponseEntity.ok("Runtime Error:\n" + stderr);
            else return ResponseEntity.ok("Unknown Error.");

        } catch (Exception e) {
            e.printStackTrace();
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