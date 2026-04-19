package com.example.travel_planner.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AIService {
    public Map<String, Object> getTravelData(List<String> cities) {

        String apiKey = "YOUR_GEOAPIFY_API_KEY";

        List<double[]> coords = new ArrayList<>();

        RestTemplate rest = new RestTemplate();

        // 🔹 Step 1: Get coordinates
        for (String city : cities) {

            String geoUrl = "https://api.geoapify.com/v1/geocode/search?text="
                    + city + "&apiKey=" + apiKey;

            Map geoRes = rest.getForObject(geoUrl, Map.class);

            List features = (List) geoRes.get("features");
            if (features == null || features.isEmpty()) continue;

            Map prop = (Map) ((Map) features.get(0)).get("properties");

            double lat = ((Number) prop.get("lat")).doubleValue();
            double lon = ((Number) prop.get("lon")).doubleValue();

            coords.add(new double[]{lon, lat});
        }

        // 🔹 Step 2: Route matrix
        String url = "https://api.geoapify.com/v1/routematrix?apiKey=" + apiKey;

        Map<String, Object> body = new HashMap<>();
        body.put("mode", "drive");

        List<Map<String, Object>> sources = new ArrayList<>();
        List<Map<String, Object>> targets = new ArrayList<>();

        for (double[] c : coords) {
            Map<String, Object> obj = new HashMap<>();
            obj.put("location", c);
            sources.add(obj);
            targets.add(obj);
        }

        body.put("sources", sources);
        body.put("targets", targets);

        Map matrix = rest.postForObject(url, body, Map.class);

        return matrix;
    }

    private final String API_KEY = "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"; // your openrouter key
    private final String URL = "https://openrouter.ai/api/v1/chat/completions";

    public String generatePlan(String prompt) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "openai/gpt-3.5-turbo");
        body.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(URL, request, Map.class);

            List choices = (List) response.getBody().get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map messageMap = (Map) firstChoice.get("message");

            return messageMap.get("content").toString().replace("```json", "").replace("```", "");

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}