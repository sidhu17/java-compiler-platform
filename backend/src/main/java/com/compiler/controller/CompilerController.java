package com.compiler.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
      requestBody.put("language_id", 62);
      requestBody.put("source_code", code);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("x-rapidapi-key", RAPIDAPI_KEY);
      headers.set("x-rapidapi-host", RAPIDAPI_HOST);

      ResponseEntity<Map> submission = restTemplate.postForEntity(SUBMISSION_URL,
        new HttpEntity<>(requestBody, headers), Map.class);

      if (!submission.getStatusCode().is2xxSuccessful()) {
        return ResponseEntity.status(500)
          .body("Submission failed: HTTP " + submission.getStatusCode());
      }

      String token = (String) submission.getBody().get("token");
      if (token == null) {
        return ResponseEntity.status(500).body("No token returned.");
      }

      Map<?, ?> result = null;
      for (int i = 0; i < 10; i++) {
        ResponseEntity<Map> r = restTemplate.exchange(
          RESULT_URL + token, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        result = r.getBody();
        if (result == null) break;

        Object status = ((Map<?, ?>)result.get("status")).get("description");
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
      e.printStackTrace();
      return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
    }
  }

  private Map<String, String> splitCodeByFiles(String input) {
    Map<String, String> files = new LinkedHashMap<>();
    for (String part : input.split("// File: ")) {
      if (part.isBlank()) continue;
      int idx = part.indexOf("\n");
      String filename = part.substring(0, idx).trim();
      String content = part.substring(idx + 1);
      files.put(filename, content);
    }
    return files;
  }
}