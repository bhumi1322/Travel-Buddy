// ================= STATE =================
let stops = ["", ""];
let modes = ["flight", "train"];
let pref = "fastest";
let map;

const apiKey = "9806cdb9ce5e438ebedec188989d57a9";

// ================= UI =================
function renderStops() {
  const div = document.getElementById("stops");
  div.innerHTML = "";

  stops.forEach((s,i)=>{
    div.innerHTML += `
      <div class="stop">
        <input value="${s}" onchange="updateStop(${i}, this.value)" placeholder="Destination ${i+1}">
        <button onclick="removeStop(${i})">X</button>
      </div>
    `;
  });
}

function addStop() {
  stops.push("");
  renderStops();
}

function removeStop(i) {
  stops.splice(i,1);
  renderStops();
}

function updateStop(i,val){
  stops[i]=val;
}

// ================= MODES =================
const MODES = ["flight","train","bus","car"];

function renderModes() {
  const div = document.getElementById("modes");
  div.innerHTML = "";

  MODES.forEach(m=>{
    div.innerHTML += `
      <span class="tag ${modes.includes(m) ? "active":""}" 
        onclick="toggleMode('${m}')">
        ${formatMode(m)}
      </span>
    `;
  });
}

function toggleMode(m){
  if(modes.includes(m)){
    modes = modes.filter(x=>x!==m);
  } else {
    modes.push(m);
  }
  renderModes();
}

function formatMode(m){
  if(m==="flight") return "✈ Flight";
  if(m==="train") return "🚄 Train";
  if(m==="bus") return "🚌 Bus";
  return "🚗 Car";
}

// ================= PREF =================
const PREFS = ["cheapest","fastest","balanced"];

function renderPrefs() {
  const div = document.getElementById("prefs");
  div.innerHTML = "";

  PREFS.forEach(p=>{
    div.innerHTML += `
      <span class="tag ${pref===p?"active":""}" 
        onclick="setPref('${p}')">
        ${p === "cheapest" ? "💰 Cheapest" :
          p === "fastest" ? "⚡ Fastest" :
          "⚖️ Balanced"}
      </span>
    `;
  });
}

function setPref(p){
  pref = p;
  renderPrefs();
}

// ================= BUDGET =================
document.getElementById("budget").oninput = function(){
  document.getElementById("budgetVal").innerText = this.value;
};

// ================= API =================
async function getCoords(city){
  const res = await fetch(`https://api.geoapify.com/v1/geocode/search?text=${city}&apiKey=${apiKey}`);
  const data = await res.json();
  const {lat,lon} = data.features[0].properties;
  return {lat,lon,name:city};
}

async function getPlaces(city) {
  try {
    const geo = await fetch(`https://api.geoapify.com/v1/geocode/search?text=${city}&apiKey=${apiKey}`);
    const geoData = await geo.json();
    const { lat, lon } = geoData.features[0].properties;

    const res = await fetch(
      `https://api.geoapify.com/v2/places?categories=tourism.sights&filter=circle:${lon},${lat},5000&limit=5&apiKey=${apiKey}`
    );

    const data = await res.json();
    return data.features.map(p => p.properties.name || "Place");

  } catch {
    return ["Explore city"];
  }
}

async function getHotels(city) {
  try {
    const geo = await fetch(`https://api.geoapify.com/v1/geocode/search?text=${city}&apiKey=${apiKey}`);
    const geoData = await geo.json();
    const { lat, lon } = geoData.features[0].properties;

    const res = await fetch(
      `https://api.geoapify.com/v2/places?categories=accommodation.hotel&filter=circle:${lon},${lat},5000&limit=3&apiKey=${apiKey}`
    );

    const data = await res.json();
    return data.features.map(h => h.properties.name || "Hotel");

  } catch {
    return ["Standard Hotel"];
  }
}

// ================= DISTANCE =================
async function getDistanceMatrix(cities) {

  let coords = [];

  for (let city of cities) {
    const geo = await fetch(`https://api.geoapify.com/v1/geocode/search?text=${city}&apiKey=${apiKey}`);
    const data = await geo.json();
    const { lat, lon } = data.features[0].properties;
    coords.push([lon, lat]);
  }

  const res = await fetch(
    `https://api.geoapify.com/v1/routematrix?apiKey=${apiKey}`,
    {
      method: "POST",
      headers: {"Content-Type":"application/json"},
      body: JSON.stringify({
        mode:"drive",
        sources: coords.map(c=>({location:c})),
        targets: coords.map(c=>({location:c}))
      })
    }
  );

  const matrix = await res.json();
  return matrix.sources_to_targets;
}

// ================= OPTIMIZATION =================
function optimizeRoute(cities, matrix) {
  let visited = new Set();
  let route = [];

  let current = 0;
  route.push(cities[current]);
  visited.add(current);

  while(route.length < cities.length){
    let next = -1;
    let min = Infinity;

    for(let i=0;i<cities.length;i++){
      if(!visited.has(i)){
        const dist = matrix[current][i].distance;
        if(dist < min){
          min = dist;
          next = i;
        }
      }
    }

    route.push(cities[next]);
    visited.add(next);
    current = next;
  }

  return route;
}

// ================= COST MODEL =================
function calculateTravelDetails(distance){
  const km = distance/1000;

  return {
    car:{ time:(km/60).toFixed(1), cost:(km*8).toFixed(0) },
    train:{ time:(km/70).toFixed(1), cost:(km*1.5).toFixed(0) },
    bus:{ time:(km/50).toFixed(1), cost:(km*2).toFixed(0) },
    flight:{ time:(km/600+2).toFixed(1), cost:(km*5+2000).toFixed(0) }
  };
}

function calculateDailyExpenses(budget){
  if(budget < 1000) return {hotel:500, food:200, activity:100};
  if(budget < 3000) return {hotel:1200, food:400, activity:300};
  return {hotel:2500, food:800, activity:600};
}

// ================= MAP =================
function showMap(coords){
  if(map) map.remove();

  map = L.map('map').setView([20,78],4);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

  const latlngs = coords.map(c=>[c.lat,c.lon]);

  coords.forEach(c=>{
    L.marker([c.lat,c.lon]).addTo(map).bindPopup(c.name);
  });

  L.polyline(latlngs,{color:'blue'}).addTo(map);
  map.fitBounds(latlngs);
}

// ================= GENERATE =================
async function generate() {

  const res = await fetch("http://localhost:8080/api/travel/plan", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      destinations: stops,
      budget: document.getElementById("budget").value,
      preference: pref
    })
  });

  const data = await res.json();

  renderResults(data);
}
// ================= RENDER =================
function renderResults(data) {

  const div = document.getElementById("results");

  let html = "<h2>Your Travel Plan</h2>";

  // 🔹 Travel
  data.travel.forEach(t => {
    html += `
      <div class="travel-card">
        <h4>🚗 ${t.from} → ${t.to}</h4>
        <p>📏 ${t.distance} km</p>
        <p>⏱ ${t.time} hrs</p>
        <p>💰 ₹${t.cost}</p>
      </div>
    `;
  });

  // 🔹 AI (IMPORTANT FIX)
  try {
    const ai = JSON.parse(data.plan);

    ai.cities.forEach(city => {
      html += `<h3>📍 ${city.name}</h3>`;  // ✅ += NOT =

      city.places.forEach(p => {
        html += `<p>📌 ${p.name} — ⏱ ${p.time} hrs</p>`;
      });
    });

  } catch (e) {
    console.log("AI parse error");
  }

  div.innerHTML = html;
}
// ================= INIT =================
document.addEventListener("DOMContentLoaded", () => {

  renderStops();
  renderModes();
  renderPrefs();

  const budget = document.getElementById("budget");
  const budgetVal = document.getElementById("budgetVal");

  if (budget) {
    budget.oninput = function () {
      budgetVal.innerText = this.value;
    };
  }

});