package com.example.travel_planner.model;


import lombok.Data;
import java.util.List;

public class RequestDTO {
    private List<String> destinations;
    private String preference;
    private int budget;

    public List<String> getDestinations() { return destinations; }
    public String getPreference() { return preference; }
    public int getBudget() { return budget; }

    public void setDestinations(List<String> destinations) { this.destinations = destinations; }
    public void setPreference(String preference) { this.preference = preference; }
    public void setBudget(int budget) { this.budget = budget; }
}