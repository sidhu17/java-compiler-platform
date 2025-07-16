package com.compiler.controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
import org.springframework.web.client.RestClientException;
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
    private static final String RESULT_URL_TEMPLATE = "https://judge0-ce.p.rapidapi.com/submissions/%s?base64_encoded=true&fields=stdout,stderr,compile_output,status";

    private final RestTemplate restTemplate = new RestTemplate();

    private String encode(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    private String decode(Object input) {
        if (input == null) return "";
        return new String(Base64.getDecoder().decode(input.toString()));
    }

    @PostMapping("/run")
    public ResponseEntity<?> runJavaCode(@RequestBody Map<String, String> payload) {
        try {
            String code = payload.get("source_code");
            String stdin = payload.getOrDefault("input", "");

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Java code cannot be empty.");
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("language_id", 62); // Java (OpenJDK 17)
            requestBody.put("source_code", encode(code));
            requestBody.put("stdin", encode(stdin));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-rapidapi-key", rapidApiKey);
            headers.set("x-rapidapi-host", rapidApiHost);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Step 1: Submit code
            ResponseEntity<Map<String, Object>> submission = restTemplate.exchange(
                SUBMISSION_URL,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> submissionBody = submission.getBody();
            if (submissionBody == null || !submissionBody.containsKey("token")) {
                return ResponseEntity.status(500).body("No token returned from Judge0.");
            }

            String token = submissionBody.get("token").toString();

            // Step 2: Poll result
            Map<String, Object> result = null;
            for (int i = 0; i < 10; i++) {
                String resultUrl = String.format(RESULT_URL_TEMPLATE, token);
                ResponseEntity<Map<String, Object>> poll = restTemplate.exchange(
                    resultUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
                );

                result = poll.getBody();
                if (result == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> statusMap = (Map<String, Object>) result.get("status");
                String status = statusMap != null ? Objects.toString(statusMap.get("description"), "") : "";

                if (!status.equalsIgnoreCase("In Queue") && !status.equalsIgnoreCase("Processing")) {
                    break;
                }

                Thread.sleep(1000); // wait before next poll
            }

            if (result == null) {
                return ResponseEntity.status(500).body("No result received.");
            }

            // Decode and return output
            String stdout = decode(result.get("stdout"));
            String stderr = decode(result.get("stderr"));
            String compileOutput = decode(result.get("compile_output"));

            Map<String, String> response = new HashMap<>();
            response.put("output", stdout);
            response.put("compile_output", compileOutput);
            response.put("stderr", stderr);

            return ResponseEntity.ok(response);

        } catch (InterruptedException | RestClientException e) {
            return ResponseEntity.status(500).body("Error occurred: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }
}