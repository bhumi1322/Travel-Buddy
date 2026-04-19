package com.example.travel_planner.controller;

import com.example.travel_planner.model.RequestDTO;
import com.example.travel_planner.service.AIService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/travel")
@CrossOrigin(origins = "*")
public class TravelController {

    @Autowired
    private AIService aiService;

    private final String API_KEY = "9806cdb9ce5e438ebedec188989d57a9";

    @PostMapping("/plan")
    public ResponseEntity<?> generatePlan(@RequestBody RequestDTO request) {

        List<String> cities = request.getDestinations();

        if (cities == null || cities.size() < 2) {
            return ResponseEntity.badRequest().body("At least 2 cities required");
        }

        Map<String, Object> matrix = getTravelMatrix(cities);

        List<List<Map<String, Object>>> matrixData = null;
        if (matrix != null && matrix.containsKey("sources_to_targets")) {
            matrixData = (List<List<Map<String, Object>>>) matrix.get("sources_to_targets");
        }

        // 🔥 STEP 1: OPTIMIZE ROUTE
        List<String> optimizedCities = (matrixData != null)
                ? optimizeRoute(cities, matrixData)
                : cities;

        // 🔥 STEP 2: BUILD TRAVEL DATA
        List<Map<String, Object>> travelList = new ArrayList<>();

        if (matrixData != null) {
            for (int i = 0; i < optimizedCities.size() - 1; i++) {

                int fromIndex = cities.indexOf(optimizedCities.get(i));
                int toIndex = cities.indexOf(optimizedCities.get(i + 1));

                if (fromIndex < 0 || toIndex < 0) continue;

                List<Map<String, Object>> row = matrixData.get(fromIndex);
                if (row == null || toIndex >= row.size()) continue;

                Map<String, Object> cell = row.get(toIndex);
                if (cell == null) continue;

                double distance = ((Number) cell.get("distance")).doubleValue();
                double time = ((Number) cell.get("time")).doubleValue();

                Map<String, Object> travel = new HashMap<>();
                travel.put("from", optimizedCities.get(i));
                travel.put("to", optimizedCities.get(i + 1));
                travel.put("distance", Math.round(distance / 1000));
                travel.put("time", Math.round(time / 3600.0 * 10) / 10.0);
                travel.put("cost", Math.round((distance / 1000) * 6));

                travelList.add(travel);
            }
        }

        // 🔥 STEP 3: AI (DAY-WISE PLAN)
        String aiResult = "";
        try {
            aiResult = aiService.generatePlan(buildPrompt(optimizedCities, request));
            aiResult = aiResult.replace("```json", "").replace("```", "").trim();
        } catch (Exception e) {
            aiResult = "{\"days\":[]}";
        }

        // 🔥 RESPONSE
        Map<String, Object> response = new HashMap<>();
        response.put("route", optimizedCities);
        response.put("travel", travelList);
        response.put("plan", aiResult);

        return ResponseEntity.ok(response);
    }

    // ================= ROUTE OPTIMIZATION =================
    private List<String> optimizeRoute(List<String> cities, List<List<Map<String, Object>>> matrixData) {

        List<String> route = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        int current = 0;
        route.add(cities.get(current));
        visited.add(current);

        while (route.size() < cities.size()) {

            int next = -1;
            double min = Double.MAX_VALUE;

            for (int i = 0; i < cities.size(); i++) {

                if (!visited.contains(i)) {

                    List<Map<String, Object>> row = matrixData.get(current);
                    if (row == null || i >= row.size()) continue;

                    Map<String, Object> cell = row.get(i);
                    if (cell == null) continue;

                    double dist = ((Number) cell.get("distance")).doubleValue();

                    if (dist < min) {
                        min = dist;
                        next = i;
                    }
                }
            }

            if (next == -1) break;

            route.add(cities.get(next));
            visited.add(next);
            current = next;
        }

        return route;
    }

    // ================= GEOAPIFY =================
    private Map<String, Object> getTravelMatrix(List<String> cities) {

        RestTemplate rest = new RestTemplate();
        List<double[]> coords = new ArrayList<>();

        for (String city : cities) {
            try {
                String geoUrl = "https://api.geoapify.com/v1/geocode/search?text="
                        + city + "&apiKey=" + API_KEY;

                Map<String, Object> geoRes = rest.getForObject(geoUrl, Map.class);

                if (geoRes == null || geoRes.get("features") == null) continue;

                List<Map<String, Object>> features =
                        (List<Map<String, Object>>) geoRes.get("features");

                if (features.isEmpty()) continue;

                Map<String, Object> props =
                        (Map<String, Object>) features.get(0).get("properties");

                double lat = ((Number) props.get("lat")).doubleValue();
                double lon = ((Number) props.get("lon")).doubleValue();

                coords.add(new double[]{lon, lat});

            } catch (Exception e) {
                System.out.println("Geo failed: " + city);
            }
        }

        if (coords.size() < 2) return null;

        try {
            String url = "https://api.geoapify.com/v1/routematrix?apiKey=" + API_KEY;

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

            return rest.postForObject(url, body, Map.class);

        } catch (Exception e) {
            return null;
        }
    }


    // ========================= PROMPT =========================
    private String buildPrompt(List<String> optimizedCities, RequestDTO request) {

        return "You are a travel planner.\n\n" +

                "For each destination, provide:\n" +
                "- 5 activities\n" +
                "- At least 2 must be adventure activities\n" +
                "- Include time (in hours)\n\n" +

                "Route: " + String.join(" -> ", request.getDestinations()) + "\n" +
                "Preference: " + request.getPreference() + "\n\n" +

                "Rules:\n" +

                "- STRICTLY follow preference\n" +
                "- Include REAL activities only\n" +
                "- Include adventure like rafting, trekking, paragliding\n" +
                "- Avoid only temples\n\n" +
                "- First city is starting point, DO NOT generate itinerary for it\n" +
                "- Each city should have balanced activities (2 main + 2 light)\n" +
                "- Do NOT overload more than 8 hours per day\n" +
                "- Include 1–2 adventure activities where possible\n"+
                "- Keep activities geographically logical\n"+

                "Return JSON:\n" +
                "{ \"cities\": [ { \"name\": \"City\", \"places\": [ {\"name\":\"Activity\",\"time\":2} ] } ] }";
    }
}