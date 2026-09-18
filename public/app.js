// ParkEasy Real-Time Web Application Logic (Firebase Cloud Firestore v10)
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { 
  getFirestore, 
  collection, 
  onSnapshot, 
  doc, 
  setDoc,
  updateDoc 
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";

// Firebase Configuration from google-services.json
const firebaseConfig = {
  apiKey: "AIzaSyAOfp5nHZhRg4csd5O0pGDunTCNDq49jD0",
  authDomain: "parkeasy-20d83.firebaseapp.com",
  projectId: "parkeasy-20d83",
  storageBucket: "parkeasy-20d83.firebasestorage.app",
  messagingSenderId: "437275182001",
  appId: "1:437275182001:web:e1f69b3bfe57556d777390"
};

// Initialize Firebase & Firestore
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// ==========================================
// APPLICATION STATE
// ==========================================
let allSpaces = [];
let allBookings = [];
let currentView = "home"; // 'home', 'explore', 'bookings', 'host', 'profile'
let activeCategoryFilter = "All";
let selectedVehicleCategory = "All";
let selectedCityFilter = "Bengaluru";
let currentSearchQuery = "";
let selectedSpotForDetail = null;
let selectedSpotForBooking = null;
let activePassTimerInterval = null;

// Filter Sheet Detailed State
let filterSheetState = {
  vehicle: "All",
  priceTier: "Any",
  sortBy: "recommended",
  isCovered: false,
  hasCctv: false,
  hasGuard: false,
  hasEv: false
};

// User Profile & Saved Vehicles (persisted in localStorage)
const defaultVehicles = [
  { id: 1, type: "EV", reg: "KA-01-AB-1234", model: "Tata Nexon EV Max" },
  { id: 2, type: "Two Wheeler", reg: "KA-05-EX-9988", model: "Ather 450X" },
  { id: 3, type: "Sedan", reg: "DL-03-CC-5678", model: "Honda City Hybrid" }
];

let savedVehicles = JSON.parse(localStorage.getItem("parkeasy_saved_vehicles")) || defaultVehicles;
let userLanguage = localStorage.getItem("parkeasy_lang") || "en";

// Leaflet Map & Markers Storage
let map = null;
let mapMarkers = [];

// ==========================================
// MAP INITIALIZATION
// ==========================================
function initMap() {
  const defaultLat = 12.9716;
  const defaultLng = 77.5946;

  const mapEl = document.getElementById("leaflet-map");
  if (!mapEl) return;

  map = L.map("leaflet-map").setView([defaultLat, defaultLng], 13);

  L.tileLayer("https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png", {
    attribution: '&copy; <a href="https://carto.com/">CARTO</a>',
    subdomains: 'abcd',
    maxZoom: 19
  }).addTo(map);
}

// ==========================================
// REAL-TIME FIRESTORE LISTENERS
// ==========================================
function setupRealtimeListeners() {
  // 1. Stream Parking Spaces
  onSnapshot(collection(db, "parking_spaces"), (snapshot) => {
    allSpaces = [];
    snapshot.forEach((docSnap) => {
      const data = docSnap.data();
      const id = data.id || docSnap.id;
      allSpaces.push({
        id: id,
        docId: docSnap.id,
        title: data.title || "Parking Spot",
        address: data.address || "",
        area: data.area || "Indiranagar",
        city: data.city || "Bengaluru",
        state: data.state || "Karnataka",
        hourlyPrice: data.hourlyPrice || 40,
        vehicleCapacity: data.vehicleCapacity || 4,
        parkingType: data.parkingType || "Covered",
        rating: data.rating || 4.9,
        reviewsCount: data.reviewsCount || 12,
        isCovered: data.isCovered ?? true,
        hasCctv: data.hasCctv ?? true,
        hasSecurityGuard: data.hasSecurityGuard ?? false,
        hasEvCharging: data.hasEvCharging ?? false,
        has24x7Access: data.has24x7Access ?? true,
        latitude: data.latitude || 12.9716,
        longitude: data.longitude || 77.5946,
        parkingPhoto: data.parkingPhoto || "https://images.unsplash.com/photo-1590674899484-d5640e854abe?w=400&q=80",
        verificationStatus: data.verificationStatus || "Verified",
        status: data.status || "Active",
        isOnline: data.isOnline ?? true
      });
    });

    console.log(`ðŸ”¥ Real-Time Firestore Sync: ${allSpaces.length} parking spaces loaded.`);
    renderHomeFeatured();
    renderExploreSpots();
    renderMapMarkers();
    updateHostDashboard();
  }, (error) => {
    console.error("Firestore spaces error:", error);
  });

  // 2. Stream Bookings
  onSnapshot(collection(db, "bookings"), (snapshot) => {
    allBookings = [];
    snapshot.forEach((docSnap) => {
      allBookings.push({ docId: docSnap.id, ...docSnap.data() });
    });

    console.log(`ðŸ”¥ Real-Time Firestore Sync: ${allBookings.length} bookings loaded.`);
    updateActivePassesBadge();
    renderBookings();
    renderHomeActivePassBanner();
    updateHostDashboard();
  }, (error) => {
    console.error("Firestore bookings error:", error);
  });
}

// ==========================================
// VIEW SWITCHER (5 CORE TABS)
// ==========================================
function switchView(viewName) {
  currentView = viewName;

  const views = {
    home: document.getElementById("view-home"),
    explore: document.getElementById("view-explore"),
    bookings: document.getElementById("view-bookings"),
    host: document.getElementById("view-host"),
    profile: document.getElementById("view-profile")
  };

  // Hide all views
  Object.values(views).forEach(v => {
    if (v) {
      v.classList.add("hidden");
      v.classList.remove("flex", "grid");
    }
  });

  // Show target view
  const target = views[viewName];
  if (target) {
    target.classList.remove("hidden");
    if (viewName === "explore") {
      target.classList.add("flex");
      setTimeout(() => {
        if (map) map.invalidateSize();
      }, 200);
    }
  }

  // Update Desktop Nav Active States
  document.querySelectorAll(".nav-tab-btn").forEach(btn => {
    btn.classList.remove("active");
  });
  const desktopBtn = document.getElementById(`nav-tab-${viewName}`);
  if (desktopBtn) desktopBtn.classList.add("active");

  // Update Mobile Bottom Nav Active States
  document.querySelectorAll(".bottom-nav-btn").forEach(btn => {
    if (btn.getAttribute("data-view") === viewName) {
      btn.classList.add("active");
    } else {
      btn.classList.remove("active");
    }
  });

  // Refresh view contents
  if (viewName === "home") renderHomeFeatured();
  if (viewName === "explore") {
    renderExploreSpots();
    renderMapMarkers();
  }
  if (viewName === "bookings") renderBookings();
  if (viewName === "host") updateHostDashboard();
  if (viewName === "profile") renderProfileVehicles();

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ==========================================
// SMART GOOGLE MAPS NAVIGATION URL RESOLVER
// ==========================================
function getSmartMapsUrl(space) {
  if (!space) return "https://www.google.com/maps";
  const titleUpper = (space.title || "").toUpperCase() + " " + (space.address || "").toUpperCase() + " " + (space.area || "").toUpperCase();
  
  if (titleUpper.includes("SDGI") || titleUpper.includes("SUNDER DEEP")) {
    return "https://www.google.com/maps/dir/?api=1&destination=28.6738,77.4912";
  }
  if (titleUpper.includes("IMS")) {
    return "https://www.google.com/maps/dir/?api=1&destination=28.6472,77.4526";
  }
  
  const isDefaultCoords = !space.latitude || 
    (Math.abs(space.latitude - 12.9716) < 0.05 && (titleUpper.includes("GHAZIABAD") || titleUpper.includes("DELHI") || titleUpper.includes("NOIDA") || titleUpper.includes("NCR")));
    
  if (isDefaultCoords) {
    const searchQuery = encodeURIComponent(`${space.title} ${space.address || space.area || ''} ${space.city || ''}`.trim());
    return `https://www.google.com/maps/dir/?api=1&destination=${searchQuery}`;
  }
  
  return `https://www.google.com/maps/dir/?api=1&destination=${space.latitude},${space.longitude}`;
}

// ==========================================
// FILTERING ENGINE
// ==========================================
function getFilteredSpaces() {
  return allSpaces.filter(space => {
    // 1. Search Query
    if (currentSearchQuery.trim() !== "") {
      const q = currentSearchQuery.toLowerCase();
      const matchesSearch = (space.title || "").toLowerCase().includes(q) ||
        (space.area || "").toLowerCase().includes(q) ||
        (space.city || "").toLowerCase().includes(q) ||
        (space.address || "").toLowerCase().includes(q);
      if (!matchesSearch) return false;
    } else if (selectedCityFilter !== "All Cities") {
      if ((space.city || "").toLowerCase() !== selectedCityFilter.toLowerCase() && 
          (space.area || "").toLowerCase() !== selectedCityFilter.toLowerCase()) {
        return false;
      }
    }

    // 2. Vehicle Category
    if (selectedVehicleCategory === "Two Wheeler" && space.parkingType === "Commercial Only") return false;
    if (selectedVehicleCategory === "EV" && !space.hasEvCharging) return false;

    // 3. Quick Filter Pills
    if (activeCategoryFilter === "Covered" && !space.isCovered) return false;
    if (activeCategoryFilter === "EV Charging" && !space.hasEvCharging) return false;
    if (activeCategoryFilter === "Under â‚¹50" && space.hourlyPrice > 50) return false;
    if (activeCategoryFilter === "24/7" && !space.has24x7Access) return false;
    if (activeCategoryFilter === "CCTV" && !space.hasCctv) return false;

    // 4. Detailed Filter Sheet Options
    if (filterSheetState.vehicle !== "All") {
      if (filterSheetState.vehicle === "EV" && !space.hasEvCharging) return false;
    }

    if (filterSheetState.priceTier !== "Any") {
      if (filterSheetState.priceTier === "Under 30" && space.hourlyPrice >= 30) return false;
      if (filterSheetState.priceTier === "30-60" && (space.hourlyPrice < 30 || space.hourlyPrice > 60)) return false;
      if (filterSheetState.priceTier === "Above 60" && space.hourlyPrice <= 60) return false;
    }

    if (filterSheetState.isCovered && !space.isCovered) return false;
    if (filterSheetState.hasCctv && !space.hasCctv) return false;
    if (filterSheetState.hasGuard && !space.hasSecurityGuard) return false;
    if (filterSheetState.hasEv && !space.hasEvCharging) return false;

    return true;
  }).sort((a, b) => {
    if (filterSheetState.sortBy === "price-low") return a.hourlyPrice - b.hourlyPrice;
    if (filterSheetState.sortBy === "price-high") return b.hourlyPrice - a.hourlyPrice;
    if (filterSheetState.sortBy === "rating") return (b.rating || 0) - (a.rating || 0);
    return (b.rating || 0) - (a.rating || 0); // default recommended
  });
}

// ==========================================
// RENDER HOME DASHBOARD SPOTLIGHTS
// ==========================================
function renderHomeFeatured() {
  const container = document.getElementById("home-featured-spots");
  if (!container) return;

  const spots = getFilteredSpaces().slice(0, 6);
  if (spots.length === 0) {
    container.innerHTML = `
      <div class="col-span-full glass-card rounded-3xl p-8 text-center text-slate-400">
        <i class="fa-solid fa-magnifying-glass text-3xl mb-3 text-blue-400"></i>
        <h4 class="text-white font-extrabold text-base mb-1">No spaces found for current filter</h4>
        <p class="text-xs text-slate-400">Try selecting "All Vehicles" or clearing search query.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = spots.map(space => {
    const mapsUrl = getSmartMapsUrl(space);
    const freeSlots = Math.max(1, (space.vehicleCapacity || 4) - 1);

    return `
      <div class="glass-card glass-card-hover rounded-3xl p-4.5 flex flex-col justify-between group cursor-pointer transition-all duration-300 border border-slate-800" onclick="window.openSpotDetailModal('${space.id}')">
        <div>
          <!-- Photo Banner with Badges -->
          <div class="h-40 rounded-2xl overflow-hidden bg-slate-900 relative border border-white/10 shadow-lg">
            <img src="${space.parkingPhoto}" alt="${space.title}" class="w-full h-full object-cover group-hover:scale-106 transition-transform duration-500">
            
            <div class="absolute top-2.5 left-2.5 flex flex-wrap gap-1.5">
              <span class="px-2.5 py-0.5 rounded-full bg-slate-950/80 text-emerald-400 text-[10px] font-extrabold backdrop-blur-md border border-emerald-500/30">
                <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 inline-block mr-1 animate-pulse"></span>
                ${freeSlots} Free Slots
              </span>
              ${space.hasEvCharging ? `
                <span class="px-2 py-0.5 rounded-full bg-emerald-500/90 text-white text-[10px] font-black backdrop-blur-md shadow-md">
                  âš¡ EV
                </span>
              ` : ''}
            </div>

            <span class="absolute bottom-2.5 left-2.5 px-2.5 py-0.5 rounded-full bg-slate-950/85 text-amber-400 text-[11px] font-black backdrop-blur-md border border-amber-500/30">
              â˜… ${space.rating || 4.9} (${space.reviewsCount || 14})
            </span>
          </div>

          <!-- Spot Title & Location -->
          <div class="mt-3.5 space-y-1">
            <h3 class="font-extrabold text-white text-base group-hover:text-blue-400 transition-colors line-clamp-1">
              ${space.title}
            </h3>
            <p class="text-xs text-slate-400 flex items-center gap-1.5">
              <i class="fa-solid fa-location-dot text-blue-400 text-[11px]"></i>
              <span class="line-clamp-1">${space.area}, ${space.city}</span>
            </p>
          </div>

          <!-- Amenities Icons -->
          <div class="flex items-center gap-2 mt-3 text-[11px] text-slate-300">
            <span class="px-2 py-0.5 rounded-lg bg-slate-900/80 border border-slate-800">${space.isCovered ? 'ðŸš˜ Covered' : 'â˜€ï¸ Open'}</span>
            ${space.hasCctv ? '<span class="px-2 py-0.5 rounded-lg bg-slate-900/80 border border-slate-800">ðŸ“¹ CCTV</span>' : ''}
            ${space.hasSecurityGuard ? '<span class="px-2 py-0.5 rounded-lg bg-slate-900/80 border border-slate-800">ðŸ‘® Guard</span>' : ''}
          </div>
        </div>

        <!-- Price & Booking CTA -->
        <div class="flex items-center justify-between mt-4 pt-3 border-t border-slate-800/80">
          <div>
            <span class="text-xl font-black text-white">â‚¹${space.hourlyPrice}</span>
            <span class="text-xs text-slate-400 font-medium">/hr</span>
          </div>
          <div class="flex items-center gap-2" onclick="event.stopPropagation()">
            <a href="${mapsUrl}" target="_blank" class="w-9 h-9 rounded-xl bg-slate-900 hover:bg-slate-800 text-blue-400 flex items-center justify-center border border-slate-800 transition" title="Maps Navigation">
              <i class="fa-solid fa-location-arrow text-xs"></i>
            </a>
            <button class="px-4 py-2 rounded-xl btn-stitch-primary text-xs font-black shadow-lg" onclick="window.openBookingModalById('${space.id}')">
              Reserve
            </button>
          </div>
        </div>
      </div>
    `;
  }).join("");
}

// ==========================================
// RENDER EXPLORE VIEW (LIST + MAP)
// ==========================================
function renderExploreSpots() {
  const container = document.getElementById("spots-list-container");
  const countEl = document.getElementById("spots-count");
  if (!container) return;

  const filtered = getFilteredSpaces();
  if (countEl) countEl.textContent = `${filtered.length} verified parking spaces available`;

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="glass-card rounded-3xl p-8 text-center text-slate-400">
        <i class="fa-solid fa-magnifying-glass text-3xl mb-3 text-blue-400"></i>
        <h4 class="text-white font-extrabold text-base mb-1">No parking spaces found</h4>
        <p class="text-xs text-slate-400">Try selecting "All Cities" or resetting active filters.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(space => {
    const mapsUrl = getSmartMapsUrl(space);
    
    return `
      <div class="glass-card glass-card-hover rounded-3xl p-4 transition-all duration-300 group cursor-pointer border border-slate-800" onclick="window.openSpotDetailModal('${space.id}')">
        <div class="flex gap-4">
          <div class="w-28 h-28 rounded-2xl overflow-hidden bg-slate-900 shrink-0 relative border border-white/10 shadow-lg">
            <img src="${space.parkingPhoto}" alt="${space.title}" class="w-full h-full object-cover group-hover:scale-108 transition-transform duration-500">
            ${space.hasEvCharging ? `
              <span class="absolute top-2 left-2 px-2 py-0.5 rounded-full bg-emerald-500/90 text-white text-[10px] font-black tracking-wide shadow-md">
                âš¡ EV
              </span>
            ` : ''}
            <span class="absolute bottom-2 left-2 px-2 py-0.5 rounded-full bg-slate-950/80 text-amber-400 text-[10px] font-bold border border-amber-500/30">
              â˜… ${space.rating || 4.9}
            </span>
          </div>

          <div class="flex-1 flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between gap-2">
                <h4 class="font-extrabold text-white text-sm group-hover:text-blue-400 transition-colors line-clamp-1">
                  ${space.title}
                </h4>
                <span class="shrink-0 inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 text-[10px] font-bold border border-emerald-500/30">
                  <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                  Verified
                </span>
              </div>
              <p class="text-xs text-slate-400 mt-1 flex items-center gap-1.5">
                <i class="fa-solid fa-location-dot text-blue-400 text-[11px]"></i>
                <span>${space.area}, ${space.city}</span>
              </p>
            </div>

            <div class="flex items-center gap-2 text-[11px] text-slate-300 mt-2">
              <span class="px-2 py-0.5 rounded-lg bg-slate-900/80 border border-slate-800 font-medium">
                ${space.isCovered ? 'ðŸš˜ Covered' : 'â˜€ï¸ Open Surface'}
              </span>
              ${space.hasCctv ? '<span class="px-2 py-0.5 rounded-lg bg-slate-900/80 border border-slate-800">ðŸ“¹ CCTV</span>' : ''}
            </div>

            <div class="flex items-center justify-between mt-3 pt-2.5 border-t border-slate-800/80" onclick="event.stopPropagation()">
              <div>
                <span class="text-xl font-black text-white">â‚¹${space.hourlyPrice}</span>
                <span class="text-xs text-slate-400 font-semibold">/hr</span>
              </div>
              <div class="flex items-center gap-2">
                <a href="${mapsUrl}" target="_blank" class="w-9 h-9 rounded-xl bg-slate-900 hover:bg-slate-800 text-blue-400 flex items-center justify-center border border-slate-800 transition" title="Maps Navigation">
                  <i class="fa-solid fa-location-arrow text-xs"></i>
                </a>
                <button class="px-4 py-2 rounded-xl btn-stitch-primary text-xs font-black shadow-lg" onclick="window.openBookingModalById('${space.id}')">
                  Reserve
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }).join("");
}

// ==========================================
// RENDER LEAFLET MAP MARKERS
// ==========================================
function renderMapMarkers() {
  if (!map) return;
  mapMarkers.forEach(m => map.removeLayer(m));
  mapMarkers = [];

  const filtered = getFilteredSpaces();
  filtered.forEach(space => {
    const mapsUrl = getSmartMapsUrl(space);
    const pinHtml = `<div class="custom-map-pin"><span>â‚¹${space.hourlyPrice}</span></div>`;
    const icon = L.divIcon({
      html: pinHtml,
      className: '',
      iconSize: [56, 24],
      iconAnchor: [28, 12]
    });

    const marker = L.marker([space.latitude, space.longitude], { icon: icon }).addTo(map);
    
    const popupContent = `
      <div class="p-3 text-xs space-y-2">
        <div class="font-extrabold text-white text-sm line-clamp-1">${space.title}</div>
        <div class="text-slate-400 flex items-center gap-1">
          <i class="fa-solid fa-location-dot text-blue-400"></i> ${space.area}, ${space.city}
        </div>
        <div class="flex items-center justify-between pt-1">
          <span class="text-emerald-400 font-black text-sm">â‚¹${space.hourlyPrice} <span class="text-[10px] text-slate-400 font-normal">/hr</span></span>
          <span class="text-amber-400 font-bold">â˜… ${space.rating || 4.9}</span>
        </div>
        <a href="${mapsUrl}" target="_blank" class="block w-full py-1.5 px-3 bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs rounded-xl text-center shadow-md transition">
          ðŸ§­ Navigate Google Maps
        </a>
      </div>
    `;
    marker.bindPopup(popupContent);
    mapMarkers.push(marker);
  });

  if (filtered.length > 0) {
    const group = L.featureGroup(mapMarkers);
    map.fitBounds(group.getBounds().pad(0.15));
  }
}

// ==========================================
// SPOT DETAIL MODAL LOGIC
// ==========================================
window.openSpotDetailModal = function(spotId) {
  const spot = allSpaces.find(s => String(s.id) === String(spotId));
  if (!spot) return;

  selectedSpotForDetail = spot;
  const mapsUrl = getSmartMapsUrl(spot);

  document.getElementById("detail-photo").src = spot.parkingPhoto;
  document.getElementById("detail-title").textContent = spot.title;
  document.getElementById("detail-price").innerHTML = `â‚¹${spot.hourlyPrice}<span class="text-xs text-slate-400 font-normal">/hr</span>`;
  document.getElementById("detail-address").innerHTML = `<i class="fa-solid fa-location-dot text-blue-400"></i> ${spot.address || spot.area}, ${spot.city}`;
  document.getElementById("detail-rating").textContent = `â˜… ${spot.rating || 4.9} (${spot.reviewsCount || 14} reviews)`;
  
  const cap = spot.vehicleCapacity || 6;
  const freeSlots = Math.max(1, cap - 2);
  document.getElementById("detail-occupancy").textContent = `${freeSlots} of ${cap} Slots Free`;
  document.getElementById("detail-occupancy-bar").style.width = `${Math.round((freeSlots / cap) * 100)}%`;

  const amenitiesTagsEl = document.getElementById("detail-amenities-tags");
  amenitiesTagsEl.innerHTML = `
    <span class="px-2.5 py-1 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300">${spot.isCovered ? 'ðŸš˜ Covered Roof' : 'â˜€ï¸ Open Surface'}</span>
    ${spot.hasCctv ? '<span class="px-2.5 py-1 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300">ðŸ“¹ 24/7 CCTV Camera</span>' : ''}
    ${spot.hasSecurityGuard ? '<span class="px-2.5 py-1 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300">ðŸ‘® Security Guard</span>' : ''}
    ${spot.hasEvCharging ? '<span class="px-2.5 py-1 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">âš¡ Fast EV Charger</span>' : ''}
    ${spot.has24x7Access ? '<span class="px-2.5 py-1 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300">ðŸ•’ 24/7 Access</span>' : ''}
  `;

  document.getElementById("detail-directions-btn").href = mapsUrl;
  
  const bookBtn = document.getElementById("detail-book-btn");
  bookBtn.onclick = () => {
    document.getElementById("modal-spot-detail").classList.add("hidden");
    openBookingModal(spot);
  };

  document.getElementById("modal-spot-detail").classList.remove("hidden");
};

// ==========================================
// MY BOOKINGS & PASSES (Active, Completed, Session Finish, Rate)
// ==========================================
let currentBookingTab = "active";

function updateActivePassesBadge() {
  const activeBookings = allBookings.filter(b => b.status === "Confirmed");
  const count = activeBookings.length;

  const deskBadge = document.getElementById("nav-passes-badge");
  const mobBadge = document.getElementById("mobile-passes-badge");
  const tabActive = document.getElementById("tab-bk-active");

  if (deskBadge) {
    deskBadge.textContent = count;
    deskBadge.classList.toggle("hidden", count === 0);
  }
  if (mobBadge) {
    mobBadge.textContent = count;
    mobBadge.classList.toggle("hidden", count === 0);
  }
  if (tabActive) {
    tabActive.textContent = `Active (${count})`;
  }
}

function renderHomeActivePassBanner() {
  const banner = document.getElementById("home-active-pass-banner");
  if (!banner) return;

  const active = allBookings.find(b => b.status === "Confirmed");
  if (!active) {
    banner.classList.add("hidden");
    return;
  }

  banner.classList.remove("hidden");
  document.getElementById("home-pass-spot-name").textContent = active.parkingTitle || "Reserved Parking Spot";
  document.getElementById("home-pass-vehicle").textContent = `Vehicle: ${active.vehicleRegNumber || 'KA-01-AB-1234'} â€¢ Pass #${active.bookingCode}`;
  
  const spot = allSpaces.find(s => s.id === active.parkingSpaceId);
  const mapsUrl = getSmartMapsUrl(spot || { title: active.parkingTitle, address: active.parkingAddress, city: active.parkingCity });
  document.getElementById("home-pass-maps-btn").href = mapsUrl;

  document.getElementById("home-pass-view-btn").onclick = () => {
    showDigitalPassModal(active);
  };
}

function renderBookings() {
  const container = document.getElementById("bookings-list-container");
  if (!container) return;

  const filtered = allBookings.filter(b => {
    if (currentBookingTab === "active") return b.status === "Confirmed";
    if (currentBookingTab === "completed") return b.status === "Completed";
    return b.status === "Cancelled";
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="glass-card rounded-3xl p-10 text-center text-slate-400 space-y-3">
        <div class="w-14 h-14 rounded-3xl bg-slate-900 border border-slate-800 flex items-center justify-center mx-auto text-2xl text-blue-400">
          <i class="fa-solid fa-ticket"></i>
        </div>
        <h3 class="text-lg font-black text-white">No ${currentBookingTab} passes found</h3>
        <p class="text-xs text-slate-400 max-w-sm mx-auto">You don't have any ${currentBookingTab} parking reservations yet.</p>
        <button class="px-5 py-2.5 rounded-xl btn-stitch-primary text-xs font-bold shadow-lg" onclick="window.switchView('explore')">
          Find & Reserve Spot
        </button>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(b => {
    const spot = allSpaces.find(s => s.id === b.parkingSpaceId);
    const mapsUrl = getSmartMapsUrl(spot || { title: b.parkingTitle, address: b.parkingAddress, city: b.parkingCity });

    return `
      <div class="glass-card rounded-3xl p-5 border ${b.status === 'Confirmed' ? 'border-emerald-500/40 bg-gradient-to-r from-emerald-950/20 to-slate-900/80' : 'border-slate-800'} shadow-xl space-y-4">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center gap-3.5">
            <div class="w-11 h-11 rounded-2xl ${b.status === 'Confirmed' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40' : 'bg-slate-800 text-slate-300'} flex items-center justify-center text-lg font-black shrink-0">
              <i class="fa-solid ${b.status === 'Confirmed' ? 'fa-qrcode' : 'fa-check'}"></i>
            </div>
            <div>
              <div class="flex items-center gap-2">
                <span class="text-sm font-black text-white">${b.parkingTitle || 'Parking Space'}</span>
                <span class="px-2 py-0.5 rounded-full ${b.status === 'Confirmed' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'bg-slate-800 text-slate-400'} text-[10px] font-extrabold uppercase">
                  ${b.status}
                </span>
              </div>
              <p class="text-xs text-slate-400 mt-0.5">${b.parkingAddress || b.parkingCity || 'Verified Address'}</p>
            </div>
          </div>
          <div class="text-left sm:text-right">
            <div class="text-lg font-black text-emerald-400">â‚¹${b.totalAmount || 80}</div>
            <div class="text-[11px] text-slate-500 font-mono">Pass #${b.bookingCode}</div>
          </div>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs text-slate-300 p-3 rounded-2xl bg-slate-950/60 border border-slate-800/80">
          <div>
            <span class="text-[10px] text-slate-500 block uppercase">Vehicle</span>
            <span class="font-mono font-bold text-white">${b.vehicleRegNumber || 'KA-01-AB-1234'}</span>
          </div>
          <div>
            <span class="text-[10px] text-slate-500 block uppercase">Duration</span>
            <span class="font-bold text-white">${b.durationHours || 2} Hours</span>
          </div>
          <div>
            <span class="text-[10px] text-slate-500 block uppercase">Payment</span>
            <span class="font-bold text-emerald-400">${b.paymentMethod || 'Google Pay'}</span>
          </div>
          <div>
            <span class="text-[10px] text-slate-500 block uppercase">Booked On</span>
            <span class="font-medium text-slate-300">${new Date(b.createdAt || Date.now()).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</span>
          </div>
        </div>

        <!-- Action Row -->
        <div class="flex flex-wrap items-center justify-between gap-2.5 pt-1">
          ${b.status === 'Confirmed' ? `
            <div class="flex items-center gap-2 w-full sm:w-auto">
              <button class="flex-1 sm:flex-none px-4 py-2.5 rounded-xl btn-stitch-emerald text-xs font-black shadow-lg" onclick="window.showPassModalByCode('${b.bookingCode}')">
                <i class="fa-solid fa-qrcode mr-1.5"></i> View QR Pass
              </button>
              <a href="${mapsUrl}" target="_blank" class="px-3.5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 transition">
                <i class="fa-solid fa-location-arrow text-blue-400 mr-1"></i> Directions
              </a>
            </div>
            <button class="w-full sm:w-auto px-4 py-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold border border-rose-500/30 transition" onclick="window.completeParkingSession('${b.bookingCode}', '${b.parkingTitle}', '${b.parkingSpaceId}')">
              <i class="fa-solid fa-flag-checkered mr-1.5"></i> Finish Parking
            </button>
          ` : `
            <div class="flex items-center gap-2 w-full sm:w-auto">
              <button class="px-4 py-2 rounded-xl bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 text-xs font-bold transition" onclick="window.openRatingModal('${b.parkingTitle}', '${b.parkingSpaceId}')">
                â˜… Rate & Review
              </button>
              <button class="px-4 py-2 rounded-xl bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/30 text-xs font-bold transition" onclick="window.switchView('explore')">
                Re-Book
              </button>
            </div>
          `}
        </div>
      </div>
    `;
  }).join("");
}

// Complete Parking Session Flow
window.completeParkingSession = async function(bookingCode, spotTitle, spaceId) {
  if (!confirm(`Are you sure you want to finish your parking session for '${spotTitle}'?`)) return;

  try {
    const docRef = doc(db, "bookings", bookingCode);
    await updateDoc(docRef, {
      status: "Completed",
      completedAt: Date.now()
    });

    // Prompt Rating Dialog
    window.openRatingModal(spotTitle, spaceId);
  } catch (err) {
    console.error("Error completing session:", err);
    alert("Failed to update status: " + err.message);
  }
};

// ==========================================
// DIGITAL PASS MODAL
// ==========================================
function showDigitalPassModal(booking) {
  const modalPass = document.getElementById("modal-pass");
  if (!modalPass || !booking) return;

  document.getElementById("pass-code").textContent = `Pass #${booking.bookingCode}`;
  document.getElementById("pass-spot-name").textContent = booking.parkingTitle || "Parking Spot";
  document.getElementById("pass-time").textContent = `Valid Today â€¢ ${booking.durationHours || 2} Hours â€¢ ${booking.paymentMethod || 'UPI'}`;
  document.getElementById("pass-reg").textContent = `Vehicle: ${(booking.vehicleRegNumber || 'KA-01-AB-1234').toUpperCase()}`;

  const spot = allSpaces.find(s => s.id === booking.parkingSpaceId);
  const mapsUrl = getSmartMapsUrl(spot || { title: booking.parkingTitle, address: booking.parkingAddress, city: booking.parkingCity });
  const passMapsBtn = document.getElementById("pass-maps-btn");
  if (passMapsBtn) passMapsBtn.href = mapsUrl;

  // Render QR Code
  const qrContainer = document.getElementById("qrcode-container");
  qrContainer.innerHTML = "";
  new QRCode(qrContainer, {
    text: `PARKEASY:${booking.bookingCode}:${booking.vehicleRegNumber}`,
    width: 140,
    height: 140,
    colorDark: "#000000",
    colorLight: "#ffffff",
    correctLevel: QRCode.CorrectLevel.H
  });

  modalPass.classList.remove("hidden");
}

window.showPassModalByCode = function(code) {
  const b = allBookings.find(x => x.bookingCode === code);
  if (b) showDigitalPassModal(b);
};

// ==========================================
// RATING & REVIEW MODAL
// ==========================================
let currentRatingVal = 5;
let currentReviewSpaceId = null;

window.openRatingModal = function(spotTitle, spaceId) {
  currentReviewSpaceId = spaceId;
  currentRatingVal = 5;

  document.getElementById("rating-spot-title").textContent = spotTitle || "Parking Spot";
  document.getElementById("rating-comment").value = "";
  updateStarVisuals(5);

  document.getElementById("modal-rating").classList.remove("hidden");
};

function updateStarVisuals(rating) {
  currentRatingVal = rating;
  const stars = document.querySelectorAll("#star-rating-stars .star-btn");
  stars.forEach(s => {
    const val = parseInt(s.getAttribute("data-rating"));
    if (val <= rating) {
      s.classList.add("active");
    } else {
      s.classList.remove("active");
    }
  });

  const ratingLabels = {
    1: "Poor (1 Star)",
    2: "Fair (2 Stars)",
    3: "Good (3 Stars)",
    4: "Very Good (4 Stars)",
    5: "Excellent (5 Stars)"
  };
  document.getElementById("star-rating-text").textContent = ratingLabels[rating] || "5 Stars";
}

// ==========================================
// DRIVER BOOKING FLOW MODAL
// ==========================================
window.openBookingModalById = function(spotId) {
  const spot = allSpaces.find(s => String(s.id) === String(spotId));
  if (spot) openBookingModal(spot);
};

function openBookingModal(spot) {
  selectedSpotForBooking = spot;
  const mapsUrl = getSmartMapsUrl(spot);

  document.getElementById("bk-title").textContent = spot.title;
  document.getElementById("bk-address").textContent = `${spot.area}, ${spot.city}`;
  document.getElementById("bk-price").textContent = `â‚¹${spot.hourlyPrice} / hour`;
  
  const btnDirections = document.getElementById("bk-directions-btn");
  if (btnDirections) btnDirections.href = mapsUrl;

  // Render Saved Vehicle Presets
  const vehicleContainer = document.getElementById("bk-vehicle-preset-container");
  if (vehicleContainer) {
    vehicleContainer.innerHTML = savedVehicles.map(v => `
      <button type="button" class="p-2 rounded-xl bg-slate-900 border border-slate-800 text-left hover:border-blue-500 transition text-[11px]" onclick="document.getElementById('bk-reg-no').value = '${v.reg}'">
        <div class="font-bold text-white">${v.model}</div>
        <div class="text-slate-400 font-mono">${v.reg}</div>
      </button>
    `).join("");
  }

  updateBookingTotal();
  document.getElementById("modal-booking").classList.remove("hidden");
}

function updateBookingTotal() {
  if (!selectedSpotForBooking) return;
  const hours = parseInt(document.getElementById("bk-hours").value);
  const evAddon = document.getElementById("bk-addon-ev")?.checked ? 99 : 0;
  const total = (selectedSpotForBooking.hourlyPrice * hours) + evAddon + 10;
  document.getElementById("bk-total-amount").textContent = `â‚¹${total}`;
}

async function confirmBooking() {
  if (!selectedSpotForBooking) return;
  const regNo = document.getElementById("bk-reg-no").value.trim();
  if (!regNo) {
    alert("Please enter your vehicle registration plate number.");
    return;
  }

  const hours = parseInt(document.getElementById("bk-hours").value);
  const upiApp = document.getElementById("bk-upi").value;
  const evAddon = document.getElementById("bk-addon-ev")?.checked ? 99 : 0;
  const total = (selectedSpotForBooking.hourlyPrice * hours) + evAddon + 10;
  const bookingCode = `PS-${Math.floor(10000 + Math.random() * 90000)}`;

  const bookingData = {
    id: Date.now(),
    bookingCode: bookingCode,
    parkingSpaceId: selectedSpotForBooking.id,
    parkingTitle: selectedSpotForBooking.title,
    parkingAddress: selectedSpotForBooking.address || selectedSpotForBooking.area,
    parkingCity: selectedSpotForBooking.city,
    vehicleRegNumber: regNo,
    durationHours: hours,
    totalAmount: total,
    paymentMethod: upiApp,
    status: "Confirmed",
    createdAt: Date.now()
  };

  try {
    await setDoc(doc(db, "bookings", bookingCode), bookingData);
    document.getElementById("modal-booking").classList.add("hidden");
    
    // Automatically open digital pass modal
    showDigitalPassModal(bookingData);
    updateActivePassesBadge();
  } catch (err) {
    console.error("Booking error:", err);
    alert("Booking failed: " + err.message);
  }
}

// ==========================================
// HOST HUB / PROVIDER CONSOLE LOGIC
// ==========================================
function updateHostDashboard() {
  const hostListingsCount = document.getElementById("host-listings-count");
  const hostBookingsCount = document.getElementById("host-bookings-count");
  const providerContainer = document.getElementById("provider-listings-list");
  const recentBookingsContainer = document.getElementById("host-recent-bookings");

  if (hostListingsCount) hostListingsCount.textContent = `${allSpaces.length} Listings`;
  if (hostBookingsCount) hostBookingsCount.textContent = `${allBookings.length} Passes`;

  if (providerContainer) {
    if (allSpaces.length === 0) {
      providerContainer.innerHTML = `<p class="text-slate-400 text-xs">No spaces listed yet.</p>`;
    } else {
      providerContainer.innerHTML = allSpaces.map(s => `
        <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 gap-3 text-xs">
          <div class="flex items-center gap-3">
            <img src="${s.parkingPhoto}" class="w-12 h-12 rounded-xl object-cover shrink-0 border border-slate-700">
            <div>
              <div class="font-extrabold text-white text-sm">${s.title}</div>
              <div class="text-slate-400">${s.area}, ${s.city} â€¢ â‚¹${s.hourlyPrice}/hr â€¢ Capacity: ${s.vehicleCapacity || 4} cars</div>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <span class="px-2.5 py-1 rounded-full ${s.isOnline !== false ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/10 text-rose-400 border border-rose-500/30'} font-bold text-[11px]">
              ${s.isOnline !== false ? 'ðŸŸ¢ Live Online' : 'ðŸ”´ Offline'}
            </span>
            <button class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold transition" onclick="window.toggleSpotOnline('${s.docId || s.id}', ${s.isOnline !== false})">
              Toggle
            </button>
          </div>
        </div>
      `).join("");
    }
  }

  if (recentBookingsContainer) {
    if (allBookings.length === 0) {
      recentBookingsContainer.innerHTML = `<p class="text-slate-400 text-xs">No reservations received yet.</p>`;
    } else {
      recentBookingsContainer.innerHTML = allBookings.slice(0, 5).map(b => `
        <div class="flex items-center justify-between p-3 rounded-xl bg-slate-900/60 border border-slate-800/80 text-xs">
          <div>
            <span class="font-bold text-white">${b.parkingTitle || 'Parking Spot'}</span>
            <span class="text-slate-400 ml-2 font-mono">${b.vehicleRegNumber || 'KA-01-AB-1234'}</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="font-extrabold text-emerald-400">â‚¹${b.totalAmount || 80}</span>
            <span class="px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-bold text-[10px]">${b.status}</span>
          </div>
        </div>
      `).join("");
    }
  }
}

window.toggleSpotOnline = async function(docId, currentOnline) {
  try {
    const docRef = doc(db, "parking_spaces", String(docId));
    await updateDoc(docRef, { isOnline: !currentOnline });
  } catch (e) {
    console.error("Toggle online status failed:", e);
    alert("Could not toggle status: " + e.message);
  }
};

// ==========================================
// PROFILE & SAVED VEHICLES LOGIC
// ==========================================
function renderProfileVehicles() {
  const container = document.getElementById("saved-vehicles-list");
  if (!container) return;

  if (savedVehicles.length === 0) {
    container.innerHTML = `<p class="text-slate-400 text-xs">No vehicles saved yet. Add your car or bike for faster bookings.</p>`;
    return;
  }

  const icons = {
    "EV": "âš¡",
    "Two Wheeler": "ðŸ›µ",
    "Hatchback": "ðŸš—",
    "Sedan": "ðŸš˜",
    "SUV": "ðŸš™"
  };

  container.innerHTML = savedVehicles.map(v => `
    <div class="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center justify-between text-xs">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-blue-600/20 text-blue-400 flex items-center justify-center text-lg">
          ${icons[v.type] || 'ðŸš—'}
        </div>
        <div>
          <div class="font-bold text-white text-sm">${v.model}</div>
          <div class="text-slate-400 font-mono">${v.reg} â€¢ ${v.type}</div>
        </div>
      </div>
      <button class="w-8 h-8 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 flex items-center justify-center transition" onclick="window.deleteVehicle(${v.id})" title="Delete Vehicle">
        <i class="fa-solid fa-trash text-xs"></i>
      </button>
    </div>
  `).join("");
}

window.deleteVehicle = function(id) {
  savedVehicles = savedVehicles.filter(v => v.id !== id);
  localStorage.setItem("parkeasy_saved_vehicles", JSON.stringify(savedVehicles));
  renderProfileVehicles();
};

function handleAddVehicle(e) {
  e.preventDefault();
  const type = document.getElementById("v-type").value;
  const reg = document.getElementById("v-reg").value.trim().toUpperCase();
  const model = document.getElementById("v-model").value.trim() || `${type} Vehicle`;

  if (!reg) {
    alert("Please enter a registration plate number.");
    return;
  }

  savedVehicles.push({
    id: Date.now(),
    type: type,
    reg: reg,
    model: model
  });

  localStorage.setItem("parkeasy_saved_vehicles", JSON.stringify(savedVehicles));
  renderProfileVehicles();
  document.getElementById("modal-add-vehicle").classList.add("hidden");
  document.getElementById("form-add-vehicle").reset();
}

// ==========================================
// INDIAN STATES & DEPENDENT CITIES DATASET
// ==========================================
const indianLocationData = {
  "Uttar Pradesh": ["Noida", "Ghaziabad", "Greater Noida", "Lucknow", "Kanpur", "Agra", "Varanasi", "Prayagraj", "Meerut", "Aligarh", "Bareilly", "Moradabad", "Gorakhpur", "Jhansi", "Mathura", "Ayodhya"],
  "Karnataka": ["Bengaluru", "Mysuru", "Mangaluru", "Hubballi-Dharwad", "Belagavi", "Kalaburagi", "Davanagere", "Ballari", "Tumakuru", "Shivamogga", "Udupi", "Hassan"],
  "Delhi (NCT)": ["New Delhi", "Central Delhi", "North Delhi", "South Delhi", "East Delhi", "West Delhi", "Dwarka", "Rohini", "Connaught Place", "Saket"],
  "Maharashtra": ["Mumbai", "Pune", "Nagpur", "Thane", "Nashik", "Chhatrapati Sambhajinagar", "Navi Mumbai", "Kalyan-Dombivli"],
  "Haryana": ["Gurugram", "Faridabad", "Panipat", "Ambala", "Karnal", "Sonipat", "Panchkula"],
  "Tamil Nadu": ["Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem"],
  "Telangana": ["Hyderabad", "Warangal", "Nizamabad", "Karimnagar"],
  "Gujarat": ["Ahmedabad", "Surat", "Vadodara", "Rajkot", "Gandhinagar"],
  "Rajasthan": ["Jaipur", "Jodhpur", "Kota", "Bikaner", "Udaipur"],
  "West Bengal": ["Kolkata", "Howrah", "Durgapur", "Siliguri"]
};

function initLocationSelectors() {
  const stateSelect = document.getElementById("space-state");
  const citySelectModal = document.getElementById("space-city");
  if (!stateSelect || !citySelectModal) return;

  stateSelect.innerHTML = Object.keys(indianLocationData).map(st => 
    `<option value="${st}" ${st === "Karnataka" ? "selected" : ""}>ðŸ“ ${st}</option>`
  ).join("");

  function updateCitiesForState() {
    const selectedState = stateSelect.value;
    const cities = indianLocationData[selectedState] || ["Bengaluru"];
    citySelectModal.innerHTML = cities.map(c => 
      `<option value="${c}">ðŸ¢ ${c}</option>`
    ).join("");
  }

  stateSelect.addEventListener("change", updateCitiesForState);
  updateCitiesForState();
}

async function publishNewSpace(e) {
  e.preventDefault();
  
  const title = document.getElementById("space-title").value;
  const state = document.getElementById("space-state") ? document.getElementById("space-state").value : "Karnataka";
  const city = document.getElementById("space-city").value;
  const area = document.getElementById("space-area").value;
  const pincode = document.getElementById("space-pincode") ? document.getElementById("space-pincode").value : "";
  const address = document.getElementById("space-address").value;
  const price = parseFloat(document.getElementById("space-price").value);
  const capacity = parseInt(document.getElementById("space-capacity").value);
  const type = document.getElementById("space-type").value;
  
  const isCovered = document.getElementById("chk-covered").checked;
  const hasCctv = document.getElementById("chk-cctv").checked;
  const hasGuard = document.getElementById("chk-guard").checked;
  const hasEv = document.getElementById("chk-ev").checked;

  const newId = Date.now();
  const docRef = doc(db, "parking_spaces", String(newId));

  const titleUpper = (title + " " + area + " " + city).toUpperCase();
  let defaultLat = 12.9716 + (Math.random() - 0.5) * 0.05;
  let defaultLng = 77.5946 + (Math.random() - 0.5) * 0.05;

  if (titleUpper.includes("SDGI") || titleUpper.includes("SUNDER DEEP")) {
    defaultLat = 28.6738;
    defaultLng = 77.4912;
  } else if (titleUpper.includes("IMS")) {
    defaultLat = 28.6472;
    defaultLng = 77.4526;
  }

  const spaceData = {
    id: newId,
    title: title,
    state: state,
    area: area,
    city: city,
    pincode: pincode,
    address: address,
    hourlyPrice: price,
    vehicleCapacity: capacity,
    parkingType: type,
    isCovered: isCovered,
    hasCctv: hasCctv,
    hasSecurityGuard: hasGuard,
    hasEvCharging: hasEv,
    has24x7Access: true,
    rating: 5.0,
    reviewsCount: 1,
    status: "Active",
    isOnline: true,
    verificationStatus: "Verified",
    latitude: defaultLat,
    longitude: defaultLng,
    parkingPhoto: "https://images.unsplash.com/photo-1590674899484-d5640e854abe?w=400&q=80",
    createdAt: Date.now()
  };

  try {
    await setDoc(docRef, spaceData);
    alert(`ðŸŽ‰ Success! '${title}' in ${city} is now live on the platform!`);
    document.getElementById("modal-list-space").classList.add("hidden");
    document.getElementById("form-list-space").reset();
  } catch (err) {
    console.error("Failed publishing to Firestore:", err);
    alert("Error publishing listing: " + err.message);
  }
}

// ==========================================
// ATTACH GLOBAL EVENT LISTENERS
// ==========================================
function setupEventListeners() {
  // Navigation Tabs (Desktop)
  document.getElementById("nav-tab-home")?.addEventListener("click", () => switchView("home"));
  document.getElementById("nav-tab-explore")?.addEventListener("click", () => switchView("explore"));
  document.getElementById("nav-tab-bookings")?.addEventListener("click", () => switchView("bookings"));
  document.getElementById("nav-tab-host")?.addEventListener("click", () => switchView("host"));
  document.getElementById("nav-tab-profile")?.addEventListener("click", () => switchView("profile"));

  // Navigation (Mobile Bottom Bar)
  document.querySelectorAll(".bottom-nav-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {
      const view = e.currentTarget.getAttribute("data-view");
      if (view) switchView(view);
    });
  });

  // Brand Logo Click -> Home
  document.getElementById("brand-logo-btn")?.addEventListener("click", (e) => {
    e.preventDefault();
    switchView("home");
  });

  // Hero Search in Home
  document.getElementById("btn-hero-search-go")?.addEventListener("click", () => {
    const query = document.getElementById("hero-search-input").value;
    currentSearchQuery = query;
    const searchInput = document.getElementById("search-input");
    if (searchInput) searchInput.value = query;
    switchView("explore");
  });

  document.getElementById("btn-see-all-explore")?.addEventListener("click", () => switchView("explore"));
  document.getElementById("btn-home-list-cta")?.addEventListener("click", () => {
    document.getElementById("modal-list-space").classList.remove("hidden");
  });

  // Vehicle Category Chips on Home Screen
  document.querySelectorAll(".vehicle-chip").forEach(chip => {
    chip.addEventListener("click", (e) => {
      document.querySelectorAll(".vehicle-chip").forEach(c => c.classList.remove("selected"));
      const target = e.currentTarget;
      target.classList.add("selected");
      selectedVehicleCategory = target.getAttribute("data-vehicle");
      renderHomeFeatured();
    });
  });

  // Explore Search Input
  const searchInput = document.getElementById("search-input");
  const btnClearSearch = document.getElementById("btn-clear-search");
  if (searchInput) {
    searchInput.addEventListener("input", (e) => {
      currentSearchQuery = e.target.value;
      if (btnClearSearch) btnClearSearch.classList.toggle("hidden", currentSearchQuery === "");
      renderExploreSpots();
      renderMapMarkers();
    });
  }

  if (btnClearSearch) {
    btnClearSearch.addEventListener("click", () => {
      searchInput.value = "";
      currentSearchQuery = "";
      btnClearSearch.classList.add("hidden");
      renderExploreSpots();
      renderMapMarkers();
    });
  }

  // City Selector
  const citySelect = document.getElementById("city-select");
  if (citySelect) {
    citySelect.addEventListener("change", (e) => {
      selectedCityFilter = e.target.value;
      const qCity = document.getElementById("quick-city-text");
      if (qCity) qCity.textContent = selectedCityFilter === "All Cities" ? "All India" : selectedCityFilter;
      renderHomeFeatured();
      renderExploreSpots();
      renderMapMarkers();
    });
  }

  // Explore Quick Pills
  document.querySelectorAll(".filter-pill").forEach(pill => {
    pill.addEventListener("click", (e) => {
      document.querySelectorAll(".filter-pill").forEach(p => p.classList.remove("active", "bg-blue-600", "text-white"));
      e.target.classList.add("active", "bg-blue-600", "text-white");
      activeCategoryFilter = e.target.getAttribute("data-filter");
      renderExploreSpots();
      renderMapMarkers();
    });
  });

  // GPS Auto Pinning
  const btnNearMe = document.getElementById("btn-near-me");
  const btnHeroGps = document.getElementById("btn-hero-gps");
  const handleGps = () => {
    if (navigator.geolocation) {
      if (btnNearMe) btnNearMe.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Locating...`;
      navigator.geolocation.getCurrentPosition((pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        switchView("explore");
        if (map) {
          map.setView([lat, lng], 14);
          L.circle([lat, lng], { radius: 1000, color: '#059669', fillColor: '#059669', fillOpacity: 0.15 }).addTo(map);
        }
        if (btnNearMe) btnNearMe.innerHTML = `<i class="fa-solid fa-location-crosshairs text-emerald-400"></i><span>GPS Pin</span>`;
        alert(`ðŸ“ GPS Position Locked: ${lat.toFixed(4)}, ${lng.toFixed(4)}`);
      }, (err) => {
        if (btnNearMe) btnNearMe.innerHTML = `<i class="fa-solid fa-location-crosshairs text-emerald-400"></i><span>GPS Pin</span>`;
        alert("Geolocation error: " + err.message);
      });
    }
  };
  btnNearMe?.addEventListener("click", handleGps);
  btnHeroGps?.addEventListener("click", handleGps);

  // Filter Sheet Modal Logic
  document.getElementById("btn-open-filter-sheet")?.addEventListener("click", () => {
    document.getElementById("modal-filter-sheet").classList.remove("hidden");
  });

  document.querySelectorAll("#filter-vehicle-options .filter-opt-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {
      document.querySelectorAll("#filter-vehicle-options .filter-opt-btn").forEach(b => {
        b.classList.remove("active", "bg-blue-600", "text-white");
        b.classList.add("bg-slate-900/80", "text-slate-300");
      });
      e.target.classList.add("active", "bg-blue-600", "text-white");
      filterSheetState.vehicle = e.target.getAttribute("data-val");
    });
  });

  document.querySelectorAll("#filter-price-options .filter-opt-btn").forEach(btn => {
    btn.addEventListener("click", (e) => {
      document.querySelectorAll("#filter-price-options .filter-opt-btn").forEach(b => {
        b.classList.remove("active", "bg-blue-600", "text-white");
        b.classList.add("bg-slate-900/80", "text-slate-300");
      });
      e.target.classList.add("active", "bg-blue-600", "text-white");
      filterSheetState.priceTier = e.target.getAttribute("data-val");
    });
  });

  document.getElementById("btn-apply-filters")?.addEventListener("click", () => {
    filterSheetState.sortBy = document.getElementById("filter-sort-select").value;
    filterSheetState.isCovered = document.getElementById("f-chk-covered").checked;
    filterSheetState.hasCctv = document.getElementById("f-chk-cctv").checked;
    filterSheetState.hasGuard = document.getElementById("f-chk-guard").checked;
    filterSheetState.hasEv = document.getElementById("f-chk-ev").checked;

    document.getElementById("modal-filter-sheet").classList.add("hidden");
    renderExploreSpots();
    renderMapMarkers();
  });

  document.getElementById("btn-reset-filters")?.addEventListener("click", () => {
    filterSheetState = {
      vehicle: "All",
      priceTier: "Any",
      sortBy: "recommended",
      isCovered: false,
      hasCctv: false,
      hasGuard: false,
      hasEv: false
    };
    document.getElementById("modal-filter-sheet").classList.add("hidden");
    renderExploreSpots();
    renderMapMarkers();
  });

  // Bookings Tab Switcher
  document.getElementById("tab-bk-active")?.addEventListener("click", () => {
    currentBookingTab = "active";
    document.getElementById("tab-bk-active").className = "px-4 py-2 rounded-xl bg-blue-600 text-white shadow-md transition";
    document.getElementById("tab-bk-completed").className = "px-4 py-2 rounded-xl text-slate-400 hover:text-white transition";
    document.getElementById("tab-bk-cancelled").className = "px-4 py-2 rounded-xl text-slate-400 hover:text-white transition";
    renderBookings();
  });

  document.getElementById("tab-bk-completed")?.addEventListener("click", () => {
    currentBookingTab = "completed";
    document.getElementById("tab-bk-completed").className = "px-4 py-2 rounded-xl bg-blue-600 text-white shadow-md transition";
    document.getElementById("tab-bk-active").className = "px-4 py-2 rounded-xl text-slate-400 hover:text-white transition";
    document.getElementById("tab-bk-cancelled").className = "px-4 py-2 rounded-xl text-slate-400 hover:text-white transition";
    renderBookings();
  });

  document.getElementById("tab-bk-cancelled")?.addEventListener("click", () => {
    currentBookingTab = "cancelled";
    document.getElementById("tab-bk-cancelled").className = "px-4 py-2 rounded-xl bg-blue-600 text-white shadow-md transition";
    document.getElementById("tab-bk-active").className = "px-4 py-2 rounded-xl text-slate-400 hover:text-white transition";
    document.getElementById("tab-bk-completed").className = "px-4 py-2 rounded-xl text-slate-400 hover:text-white transition";
    renderBookings();
  });

  // Star Rating Click Handlers
  document.querySelectorAll("#star-rating-stars .star-btn").forEach(star => {
    star.addEventListener("click", (e) => {
      const r = parseInt(e.currentTarget.getAttribute("data-rating"));
      updateStarVisuals(r);
    });
  });

  document.getElementById("btn-submit-review")?.addEventListener("click", () => {
    alert(`â­ Thank you! Your ${currentRatingVal}-star rating and review have been submitted.`);
    document.getElementById("modal-rating").classList.add("hidden");
  });

  // Modals Open & Close
  document.getElementById("btn-open-list-modal")?.addEventListener("click", () => {
    document.getElementById("modal-list-space").classList.remove("hidden");
  });
  document.getElementById("btn-host-add-space")?.addEventListener("click", () => {
    document.getElementById("modal-list-space").classList.remove("hidden");
  });
  document.getElementById("btn-open-emergency")?.addEventListener("click", () => {
    document.getElementById("modal-emergency").classList.remove("hidden");
  });
  document.getElementById("card-pref-emergency")?.addEventListener("click", () => {
    document.getElementById("modal-emergency").classList.remove("hidden");
  });
  document.getElementById("btn-open-notifications")?.addEventListener("click", () => {
    document.getElementById("modal-notifications").classList.remove("hidden");
  });
  document.getElementById("btn-open-language")?.addEventListener("click", () => {
    document.getElementById("modal-language").classList.remove("hidden");
  });
  document.getElementById("card-pref-lang")?.addEventListener("click", () => {
    document.getElementById("modal-language").classList.remove("hidden");
  });
  document.getElementById("btn-open-add-vehicle")?.addEventListener("click", () => {
    document.getElementById("modal-add-vehicle").classList.remove("hidden");
  });

  document.querySelectorAll(".btn-close-modal").forEach(b => {
    b.addEventListener("click", () => {
      document.querySelectorAll("[id^='modal-']").forEach(m => m.classList.add("hidden"));
    });
  });

  // Forms
  document.getElementById("form-list-space")?.addEventListener("submit", publishNewSpace);
  document.getElementById("form-add-vehicle")?.addEventListener("submit", handleAddVehicle);
  document.getElementById("bk-hours")?.addEventListener("change", updateBookingTotal);
  document.getElementById("bk-addon-ev")?.addEventListener("change", updateBookingTotal);
  document.getElementById("btn-confirm-booking")?.addEventListener("click", confirmBooking);

  // Refresh Sync Buttons
  document.getElementById("btn-refresh-home")?.addEventListener("click", () => renderHomeFeatured());
  document.getElementById("btn-refresh-sync")?.addEventListener("click", () => {
    renderExploreSpots();
    renderMapMarkers();
  });
}

// ==========================================
// APP INITIALIZATION
// ==========================================
window.addEventListener("DOMContentLoaded", () => {
  initMap();
  initLocationSelectors();
  setupRealtimeListeners();
  setupEventListeners();
  renderProfileVehicles();
  switchView("home");
});