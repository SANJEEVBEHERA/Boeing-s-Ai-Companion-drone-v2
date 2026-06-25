package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- Data Models ---
data class UserProfile(
    val email: String = "alex.johnson@email.com",
    val displayName: String = "Alex Johnson",
    val photoUrl: String = "",
    val flightsCount: Int = 28,
    val totalDistanceKm: Double = 154.6,
    val totalTimeHrs: Int = 23,
    val totalTimeMins: Int = 47,
    val achievements: Int = 12,
    val isPremium: Boolean = true
)

data class FlightLog(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val durationMin: String = "",
    val distanceKm: String = "",
    val photosCount: Int = 0,
    val videosCount: Int = 0,
    val maxAltitude: String = "",
    val date: String = "",
    val mood: String = "Adventure",
    val storyText: String = ""
)

data class PreflightCheckItem(
    val label: String,
    val statusText: String,
    val isCompleted: Boolean,
    val isSuccess: Boolean
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface MissionStep {
    object Idle : MissionStep
    object PreflightCheck : MissionStep
    object MissionReady : MissionStep
    object ActiveFlight : MissionStep
    object MissionComplete : MissionStep
    object StoryView : MissionStep
}

class DroneViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DroneViewModel"

    // Dynamic State Observables
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isDroneConnected = MutableStateFlow(false)
    val isDroneConnected: StateFlow<Boolean> = _isDroneConnected.asStateFlow()

    private val _missionStep = MutableStateFlow<MissionStep>(MissionStep.Idle)
    val missionStep: StateFlow<MissionStep> = _missionStep.asStateFlow()

    private val _selectedMode = MutableStateFlow("Travel")
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    // Preflight check list status
    private val _preflightChecks = MutableStateFlow<List<PreflightCheckItem>>(emptyList())
    val preflightChecks: StateFlow<List<PreflightCheckItem>> = _preflightChecks.asStateFlow()

    private val _preflightCheckProgress = MutableStateFlow(0f)
    val preflightCheckProgress: StateFlow<Float> = _preflightCheckProgress.asStateFlow()

    // Active flight telemetry
    private val _altitude = MutableStateFlow(60f)
    val altitude: StateFlow<Float> = _altitude.asStateFlow()

    private val _speed = MutableStateFlow(0)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    private val _flightTime = MutableStateFlow("00:00")
    val flightTime: StateFlow<String> = _flightTime.asStateFlow()

    private val _flightDistance = MutableStateFlow("0.0 km")
    val flightDistance: StateFlow<String> = _flightDistance.asStateFlow()

    private val _batteryLevel = MutableStateFlow(92)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _gpsSatellites = MutableStateFlow(18)
    val gpsSatellites: StateFlow<Int> = _gpsSatellites.asStateFlow()

    private val _connectionStrength = MutableStateFlow("Strong")
    val connectionStrength: StateFlow<String> = _connectionStrength.asStateFlow()

    // Voice assistant / chat log
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("How can I help with your flight? Speak naturally or select a quick command below.", false)
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _assistantIsListening = MutableStateFlow(false)
    val assistantIsListening: StateFlow<Boolean> = _assistantIsListening.asStateFlow()

    private val _assistantIsThinking = MutableStateFlow(false)
    val assistantIsThinking: StateFlow<Boolean> = _assistantIsThinking.asStateFlow()

    // AI suggestion events during active flight
    private val _aiSuggestions = MutableStateFlow<List<String>>(emptyList())
    val aiSuggestions: StateFlow<List<String>> = _aiSuggestions.asStateFlow()

    // AI story results
    private val _currentStoryText = MutableStateFlow("")
    val currentStoryText: StateFlow<String> = _currentStoryText.asStateFlow()

    private val _currentStoryMood = MutableStateFlow("Adventure")
    val currentStoryMood: StateFlow<String> = _currentStoryMood.asStateFlow()

    private val _isStoryLoading = MutableStateFlow(false)
    val isStoryLoading: StateFlow<Boolean> = _isStoryLoading.asStateFlow()

    // Wallpaper generator settings
    private val _wallpaperPrompt = MutableStateFlow("")
    val wallpaperPrompt: StateFlow<String> = _wallpaperPrompt.asStateFlow()

    private val _wallpaperSize = MutableStateFlow("1K") // 1K, 2K, 4K
    val wallpaperSize: StateFlow<String> = _wallpaperSize.asStateFlow()

    private val _generatedWallpaperStatus = MutableStateFlow("") // loading, success, empty
    val generatedWallpaperStatus: StateFlow<String> = _generatedWallpaperStatus.asStateFlow()

    // Drone settings parameters
    private val _returnHomeAltitude = MutableStateFlow(120)
    val returnHomeAltitude: StateFlow<Int> = _returnHomeAltitude.asStateFlow()

    private val _obstacleAvoidance = MutableStateFlow(true)
    val obstacleAvoidance: StateFlow<Boolean> = _obstacleAvoidance.asStateFlow()

    private val _maxFlightSpeedSetting = MutableStateFlow(32)
    val maxFlightSpeedSetting: StateFlow<Int> = _maxFlightSpeedSetting.asStateFlow()

    private val _cameraResolution = MutableStateFlow("4K (3840×2160)")
    val cameraResolution: StateFlow<String> = _cameraResolution.asStateFlow()

    private val _videoFormatSetting = MutableStateFlow("MP4 (H.265)")
    val videoFormatSetting: StateFlow<String> = _videoFormatSetting.asStateFlow()

    private val _unitsMetric = MutableStateFlow(true)
    val unitsMetric: StateFlow<Boolean> = _unitsMetric.asStateFlow()

    // Past histories
    private val _recentActivities = MutableStateFlow<List<FlightLog>>(listOf(
        FlightLog("1", "Beautiful Sunset", "Cinematic Video • May 12, 2024", "18:47", "3.2", 126, 8, "120 m", "May 12, 2024", "Adventure", "A spectacular sunset captured over mountain lakes. AeroGuard One completed automated wide sweep maneuvers."),
        FlightLog("2", "Group Photo", "Photo • May 12, 2024", "15:20", "2.1", 12, 1, "45 m", "May 12, 2024", "Happy", "Friends reunited at the cabin, wide angle camera captured deep laughters and sharp details of everyone together."),
        FlightLog("3", "Mountain Panorama", "Panorama • May 12, 2024", "25:40", "4.8", 1, 0, "150 m", "May 12, 2024", "Epic", "Full ultra-wide panoramic stitching of the jagged peaks at sunrise.")
    ))
    val recentActivities: StateFlow<List<FlightLog>> = _recentActivities.asStateFlow()

    // Real Firebase Auth & Firestore Helpers
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null
    private var isFirebaseAvailable = false

    init {
        checkFirebaseAvailability()
    }

    private fun checkFirebaseAvailability() {
        try {
            if (FirebaseApp.getApps(getApplication<Application>()).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                firebaseFirestore = FirebaseFirestore.getInstance()
                isFirebaseAvailable = true
                Log.d(TAG, "Firebase initialized successfully in AeroGuard Drone App.")
                listenToAuthState()
            } else {
                Log.w(TAG, "Firebase not initialized. Operating in local mode.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Firebase availability: ", e)
        }
    }

    private fun listenToAuthState() {
        firebaseAuth?.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                _userProfile.update {
                    it.copy(
                        email = user.email ?: "alex.johnson@email.com",
                        displayName = user.displayName ?: "Alex Johnson"
                    )
                }
                syncHistoryFromFirestore()
            }
        }
    }

    // --- Core Operations ---

    fun setDroneConnected(connected: Boolean) {
        _isDroneConnected.value = connected
    }

    fun setSelectedMode(mode: String) {
        _selectedMode.value = mode
    }

    fun setStoryMood(mood: String) {
        _currentStoryMood.value = mood
        generateStoryForCurrentMood()
    }

    fun setWallpaperPrompt(prompt: String) {
        _wallpaperPrompt.value = prompt
    }

    fun setWallpaperSize(size: String) {
        _wallpaperSize.value = size
    }

    fun toggleObstacleAvoidance() {
        _obstacleAvoidance.value = !_obstacleAvoidance.value
    }

    fun setReturnHomeAltitude(value: Int) {
        _returnHomeAltitude.value = value
    }

    fun setMaxFlightSpeedSetting(value: Int) {
        _maxFlightSpeedSetting.value = value
    }

    fun setUnitsMetric(metric: Boolean) {
        _unitsMetric.value = metric
    }

    // --- Preflight Operations ---

    fun startPreflightCheck() {
        _missionStep.value = MissionStep.PreflightCheck
        _preflightCheckProgress.value = 0f
        _preflightChecks.value = listOf(
            PreflightCheckItem("Weather Conditions", "Analyzing atmosphere...", false, false),
            PreflightCheckItem("Wind Conditions", "Measuring velocity...", false, false),
            PreflightCheckItem("Battery Calibration", "Checking cells...", false, false),
            PreflightCheckItem("GPS Signal Lock", "Acquiring satellites...", false, false),
            PreflightCheckItem("Flight Zone Authority", "Checking local airspace rules...", false, false),
            PreflightCheckItem("Obstacle Detection", "Calibrating LiDAR sensors...", false, false),
            PreflightCheckItem("Safe Landing Zone", "Evaluating launch pad surface...", false, false)
        )

        viewModelScope.launch {
            delay(800)
            updateCheckItem(0, "Clear skies, 24°C", true)
            _preflightCheckProgress.value = 0.15f

            delay(1000)
            updateCheckItem(1, "8 km/h • Light breeze", true)
            _preflightCheckProgress.value = 0.30f

            delay(800)
            updateCheckItem(2, "92% charged • Optimal health", true)
            _preflightCheckProgress.value = 0.45f

            delay(1200)
            updateCheckItem(3, "Locked • 19 Satellites", true)
            _preflightCheckProgress.value = 0.60f

            delay(900)
            updateCheckItem(4, "In authorized flight zone", true)
            _preflightCheckProgress.value = 0.75f

            delay(1000)
            updateCheckItem(5, "Sensors calibrated • Clear path", true)
            _preflightCheckProgress.value = 0.90f

            delay(800)
            updateCheckItem(6, "Launch area clear • Ready to takeoff", true)
            _preflightCheckProgress.value = 1.0f

            delay(500)
            _missionStep.value = MissionStep.MissionReady
        }
    }

    private fun updateCheckItem(index: Int, statusText: String, success: Boolean) {
        val currentList = _preflightChecks.value.toMutableList()
        if (index < currentList.size) {
            currentList[index] = PreflightCheckItem(
                currentList[index].label,
                statusText,
                isCompleted = true,
                isSuccess = success
            )
            _preflightChecks.value = currentList
        }
    }

    // --- Active Flight Operations ---

    fun launchDroneMission() {
        _missionStep.value = MissionStep.ActiveFlight
        _altitude.value = 10f
        _speed.value = 0
        _flightTime.value = "00:00"
        _flightDistance.value = "0.0 km"
        _aiSuggestions.value = emptyList()

        viewModelScope.launch {
            // Simulated drone takeoff ascent
            for (alt in 10..60 step 5) {
                _altitude.value = alt.toFloat()
                delay(150)
            }
            _speed.value = 24

            // Simulate flight timeline telemetry and AI moments triggered autonomously!
            launch {
                var seconds = 0
                var distance = 0.0
                while (_missionStep.value == MissionStep.ActiveFlight) {
                    delay(1000)
                    seconds++
                    distance += 0.005 * (_speed.value / 10f)

                    val m = seconds / 60
                    val s = seconds % 60
                    _flightTime.value = String.format("%02d:%02d", m, s)
                    _flightDistance.value = String.format("%.2f km", distance)

                    // battery decreases slowly
                    if (seconds % 10 == 0) {
                        _batteryLevel.update { if (it > 5) it - 1 else it }
                    }

                    // Trigger AI highlights along the flight as suggestions!
                    when (seconds) {
                        4 -> triggerAiSuggestion("🌄 Beautiful Sunset Detected - Cinematic Video Saved Automatically")
                        10 -> triggerAiSuggestion("👥 Group Moment captured - Wide-angle photo optimized")
                        16 -> triggerAiSuggestion("🏔️ Mountain Panorama stitched together successfully")
                        22 -> triggerAiSuggestion("🦌 Wildlife (Deer) detected in valley - 4K Zoom-In photo completed")
                    }
                }
            }
        }
    }

    private fun triggerAiSuggestion(text: String) {
        val current = _aiSuggestions.value.toMutableList()
        current.add(0, text)
        _aiSuggestions.value = current
    }

    fun pauseFlight() {
        _speed.value = if (_speed.value > 0) 0 else 24
    }

    fun endFlightMission() {
        _missionStep.value = MissionStep.MissionComplete
        _speed.value = 0

        // Save flight log locally and sync to Firebase Firestore!
        val newLog = FlightLog(
            id = System.currentTimeMillis().toString(),
            title = "${_selectedMode.value} Mode Mission",
            subtitle = "Completed successfully • Just Now",
            durationMin = _flightTime.value,
            distanceKm = _flightDistance.value.replace(" km", ""),
            photosCount = 126,
            videosCount = 8,
            maxAltitude = "${_altitude.value.toInt()} m",
            date = "Today",
            mood = _currentStoryMood.value,
            storyText = ""
        )

        val updatedList = _recentActivities.value.toMutableList()
        updatedList.add(0, newLog)
        _recentActivities.value = updatedList

        // Increment count in user Profile
        _userProfile.update {
            it.copy(
                flightsCount = it.flightsCount + 1,
                totalDistanceKm = String.format("%.1f", it.totalDistanceKm + (newLog.distanceKm.toDoubleOrNull() ?: 0.0)).toDouble(),
                achievements = it.achievements + 1
            )
        }

        saveToFirestore(newLog)
        generateStoryForCurrentMood()
    }

    fun returnToDashboard() {
        _missionStep.value = MissionStep.Idle
    }

    // --- AI Story Generation ---

    private fun generateStoryForCurrentMood() {
        _isStoryLoading.value = true
        _currentStoryText.value = ""

        viewModelScope.launch {
            // Fetch dynamically from Gemini using our helper!
            val distance = _flightDistance.value.replace(" km", "")
            val time = _flightTime.value
            val response = GeminiApiHelper.generateAiStory(_currentStoryMood.value, time, distance)
            _currentStoryText.value = response
            _isStoryLoading.value = false
        }
    }

    // --- Voice Assistant Chat Actions ---

    fun sendVoiceCommand(commandText: String) {
        if (commandText.isEmpty()) return

        // Add user query
        val list = _chatMessages.value.toMutableList()
        list.add(ChatMessage(commandText, true))
        _chatMessages.value = list

        _assistantIsThinking.value = true

        viewModelScope.launch {
            // Call Gemini 3.1-flash-lite low-latency response!
            val responseText = GeminiApiHelper.generateVoiceResponse(commandText)
            delay(400) // add slight natural rhythm pause
            _assistantIsThinking.value = false

            val updatedList = _chatMessages.value.toMutableList()
            updatedList.add(ChatMessage(responseText, false))
            _chatMessages.value = updatedList
        }
    }

    // --- Creative Image Generation (Wallpaper Affordance) ---

    fun generateWallpaper() {
        val prompt = _wallpaperPrompt.value
        if (prompt.isEmpty()) return

        _generatedWallpaperStatus.value = "loading"

        viewModelScope.launch {
            val status = GeminiApiHelper.generateCustomDroneWallpaper(prompt, _wallpaperSize.value)
            _generatedWallpaperStatus.value = status
        }
    }

    fun clearWallpaper() {
        _generatedWallpaperStatus.value = ""
        _wallpaperPrompt.value = ""
    }

    // --- Firebase Auth & Google Sign-In Simulation/Actual Integration ---

    fun authenticateWithGoogle(email: String, displayName: String) {
        viewModelScope.launch {
            // Real FirebaseAuth call simulation or execution
            if (isFirebaseAvailable && firebaseAuth != null) {
                try {
                    // Let's sign in locally if possible or perform simulated success to Firestore
                    Log.d(TAG, "FirebaseAuth active. Simulating sync with real console credentials.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Firebase Auth flow: ", e)
                }
            }

            delay(1000)
            _userProfile.update {
                it.copy(
                    email = email,
                    displayName = displayName
                )
            }
            syncHistoryFromFirestore()
        }
    }

    fun logout() {
        viewModelScope.launch {
            if (isFirebaseAvailable && firebaseAuth != null) {
                firebaseAuth?.signOut()
            }
            _userProfile.update {
                UserProfile(
                    email = "guest@aeroguard.com",
                    displayName = "Guest Pilot",
                    flightsCount = 0,
                    totalDistanceKm = 0.0,
                    totalTimeHrs = 0,
                    totalTimeMins = 0,
                    achievements = 0,
                    isPremium = false
                )
            }
            _recentActivities.value = emptyList()
        }
    }

    private fun saveToFirestore(log: FlightLog) {
        if (!isFirebaseAvailable || firebaseFirestore == null) {
            Log.w(TAG, "Firestore not available. Retaining flight log locally.")
            return
        }

        viewModelScope.launch {
            try {
                val flightMap = hashMapOf(
                    "id" to log.id,
                    "title" to log.title,
                    "subtitle" to log.subtitle,
                    "durationMin" to log.durationMin,
                    "distanceKm" to log.distanceKm,
                    "photosCount" to log.photosCount,
                    "videosCount" to log.videosCount,
                    "maxAltitude" to log.maxAltitude,
                    "date" to log.date,
                    "mood" to log.mood,
                    "storyText" to log.storyText
                )

                val userEmail = _userProfile.value.email
                firebaseFirestore?.collection("users")
                    ?.document(userEmail)
                    ?.collection("flights")
                    ?.document(log.id)
                    ?.set(flightMap)
                    ?.addOnSuccessListener {
                        Log.d(TAG, "Flight record successfully saved to Firestore.")
                    }
                    ?.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to save flight record to Firestore: ", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore exception: ", e)
            }
        }
    }

    private fun syncHistoryFromFirestore() {
        if (!isFirebaseAvailable || firebaseFirestore == null) return

        val userEmail = _userProfile.value.email
        firebaseFirestore?.collection("users")
            ?.document(userEmail)
            ?.collection("flights")
            ?.get()
            ?.addOnSuccessListener { querySnapshot ->
                val logs = mutableListOf<FlightLog>()
                for (doc in querySnapshot.documents) {
                    val log = FlightLog(
                        id = doc.getString("id") ?: "",
                        title = doc.getString("title") ?: "",
                        subtitle = doc.getString("subtitle") ?: "",
                        durationMin = doc.getString("durationMin") ?: "",
                        distanceKm = doc.getString("distanceKm") ?: "",
                        photosCount = doc.getLong("photosCount")?.toInt() ?: 0,
                        videosCount = doc.getLong("videosCount")?.toInt() ?: 0,
                        maxAltitude = doc.getString("maxAltitude") ?: "",
                        date = doc.getString("date") ?: "",
                        mood = doc.getString("mood") ?: "Adventure",
                        storyText = doc.getString("storyText") ?: ""
                    )
                    logs.add(log)
                }
                if (logs.isNotEmpty()) {
                    _recentActivities.value = logs.sortedByDescending { it.id }
                    // Update profile stats from Firestore
                    val totalDistance = logs.sumOf { it.distanceKm.toDoubleOrNull() ?: 0.0 }
                    _userProfile.update {
                        it.copy(
                            flightsCount = logs.size,
                            totalDistanceKm = String.format("%.1f", totalDistance).toDouble(),
                            achievements = logs.size * 2 + 5
                        )
                    }
                }
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Error syncing from Firestore: ", e)
            }
    }
}
