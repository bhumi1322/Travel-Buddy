# Travel-Buddy
🚀 Voyager AI – Smart Travel Planner
🌍 Overview

Voyager AI is an intelligent travel planning system that helps users efficiently organize multi-destination trips. Unlike traditional route planners, it combines AI-generated itineraries, real-time travel data, and user preferences to create optimized and personalized travel plans.

✨ Key Features
🧭 Route Optimization
Computes the most efficient path between multiple destinations
Uses distance matrix (Geoapify API)
Implements a greedy nearest-neighbor algorithm for shortest route
🚗 Travel Insights
Distance between cities (km)
Estimated travel time (hours)
Travel cost estimation (₹/km based)
🤖 AI-Powered Itinerary
Generates day-wise travel plans
Includes:
Popular attractions
Adventure activities
Cultural experiences
Ensures balanced schedules (time-constrained)
🎯 Preference-Based Planning
Customizes trips based on user selection:
⚡ Fastest
💰 Cheapest
⚖️ Balanced
Backend logic + prompt engineering used for control
🛡️ Robust Backend Handling
API failure handling (Geoapify + AI)
CORS configuration for frontend-backend communication
Safe parsing of AI responses


🏗️ Tech Stack

Frontend

HTML

CSS

JavaScript

Backend

Java (Spring Boot)

APIs

Geoapify (Geocoding + Route Matrix)

AI Model (HuggingFace / LLM API)

⚙️ System Architecture


Frontend (UI)

     ↓

Spring Boot Backend

     ↓


     
 ┌───────────────┬───────────────┐
 | Geoapify API  | AI Service    |
 | (distance)    | (itinerary)   |
 └───────────────┴───────────────┘
     ↓


     
Processed Response → Frontend Display
🔄 Workflow
User enters:
Destinations
Budget
Preference
Backend:
Fetches coordinates
Computes distance matrix
Optimizes route
Calls AI for itinerary
System:
Combines travel data + AI output
Applies filtering logic
Output:
Optimized route
Travel breakdown
Day-wise plan




📊 Example Output
Route: Delhi → Rishikesh → Haridwar

Travel:
Delhi → Rishikesh → 238 km | 3.3 hrs | ₹1430

Day 1: Rishikesh
- River Rafting (4 hrs)
- Bungee Jumping (3 hrs)
- Beatles Ashram (2 hrs)

Day 2: Haridwar
- Ganga Aarti
- Mansa Devi Temple
- Local Market Exploration
🚧 Challenges Faced
Handling inconsistent AI responses
Implementing CORS between frontend & backend
Preventing backend crashes due to API failures
Ensuring realistic itinerary generation
🔮 Future Improvements
Strict backend-based preference filtering
Integration with hotel & booking APIs
Real-time pricing models
Map visualization (Leaflet / Google Maps)
User authentication & saved trips
💡 Key Learnings
Full-stack integration (Frontend ↔ Backend ↔ APIs)
Debugging real-world issues (CORS, 500 errors, API failures)
Prompt engineering for controlled AI outputs
Designing scalable system architecture
📌 Conclusion

Voyager AI demonstrates how AI can enhance traditional travel planning by combining optimization algorithms, real-time APIs, and user-driven customization into a single intelligent system.

👤 Author

Bhumija verma
