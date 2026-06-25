@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.*
import kotlin.math.sin

// --- Frosted Glass Theme Helper Shadow Constructors ---
fun Color(colorValue: Long): Color {
    return when (colorValue) {
        0xFF0B1528L, 0xFF0B1528 -> Color(0x991C2028) // Translucent CardBackground
        0xFF1E293BL, 0xFF1E293B -> Color(0x0EFFFFFF) // Translucent glass white border
        0xFF0F172AL, 0xFF0F172A -> Color(0x801C2028) // Translucent secondary glass card
        0xFF070D1EL, 0xFF070D1E -> Color(0xFF16191E) // Bottom Nav Bar background
        0xFF030712L, 0xFF030712 -> Color(0xFF0B0D11) // Deep body background
        0xFF0052FFL, 0xFF0052FF -> Color(0xFF0039A6) // Primary rich blue brand accent
        else -> androidx.compose.ui.graphics.Color(colorValue)
    }
}

fun Color(colorValue: Int): Color {
    return when (colorValue.toLong() and 0xFFFFFFFFL) {
        0xFF0B1528L -> Color(0x991C2028)
        0xFF1E293BL -> Color(0x0EFFFFFF)
        0xFF0F172AL -> Color(0x801C2028)
        0xFF070D1EL -> Color(0xFF16191E)
        0xFF030712L -> Color(0xFF0B0D11)
        0xFF0052FFL -> Color(0xFF0039A6)
        else -> androidx.compose.ui.graphics.Color(colorValue)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneApp(viewModel: DroneViewModel) {
    var currentTab by remember { mutableStateOf("Home") }

    val missionStep by viewModel.missionStep.collectAsState()
    val isConnected by viewModel.isDroneConnected.collectAsState()

    // Full-screen scaffold supporting safe system drawing
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Render standard navigation bar ONLY when drone is not in active flight/checks to maximize screen focus
            if (missionStep == MissionStep.Idle || missionStep == MissionStep.MissionComplete || missionStep == MissionStep.StoryView) {
                NavigationBar(
                    containerColor = Color(0xFF070D1E),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .testTag("bottom_nav_bar")
                        .drawBehind {
                            // Subtle glass-top border
                            drawLine(
                                color = Color(0x0EFFFFFF),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    val tabs = listOf(
                        Triple("Home", Icons.Default.Home, Icons.Outlined.Home),
                        Triple("Gallery", Icons.Default.PhotoLibrary, Icons.Outlined.PhotoLibrary),
                        Triple("AI Assistant", Icons.Default.SmartButton, Icons.Outlined.SmartButton),
                        Triple("Settings", Icons.Default.Settings, Icons.Outlined.Settings),
                        Triple("Profile", Icons.Default.Person, Icons.Outlined.Person)
                    )

                    tabs.forEach { (tabName, filledIcon, outlinedIcon) ->
                        val isSelected = currentTab == tabName
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tabName },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) filledIcon else outlinedIcon,
                                    contentDescription = tabName,
                                    tint = if (isSelected) Color(0xFFD1E4FF) else Color(0x66E2E2E6)
                                )
                            },
                            label = {
                                Text(
                                    text = tabName,
                                    color = if (isSelected) Color(0xFFD1E4FF) else Color(0x66E2E2E6),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0x330039A6)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tabName.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Direct Screen Routing
            when (currentTab) {
                "Home" -> {
                    when (missionStep) {
                        is MissionStep.Idle -> HomeDashboardScreen(viewModel)
                        is MissionStep.PreflightCheck -> PreflightCheckScreen(viewModel)
                        is MissionStep.MissionReady -> MissionReadyScreen(viewModel)
                        is MissionStep.ActiveFlight -> ActiveFlightScreen(viewModel)
                        is MissionStep.MissionComplete -> MissionCompleteScreen(viewModel)
                        is MissionStep.StoryView -> StoryViewScreen(viewModel)
                    }
                }
                "Gallery" -> GalleryScreen(viewModel)
                "AI Assistant" -> VoiceAssistantScreen(viewModel)
                "Settings" -> SettingsScreen(viewModel)
                "Profile" -> ProfileScreen(viewModel)
            }
        }
    }
}

// ==========================================
// SCREEN 1: HOME DASHBOARD
// ==========================================
@Composable
fun HomeDashboardScreen(viewModel: DroneViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isConnected by viewModel.isDroneConnected.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val satellites by viewModel.gpsSatellites.collectAsState()
    val connection by viewModel.connectionStrength.collectAsState()

    var showActivitySelection by remember { mutableStateOf(false) }

    if (showActivitySelection) {
        ChooseActivityScreen(
            viewModel = viewModel,
            onBack = { showActivitySelection = false },
            onStartMission = { mode ->
                viewModel.setSelectedMode(mode)
                showActivitySelection = false
                viewModel.startPreflightCheck()
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Header greeting
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Good Morning,",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                        Text(
                            text = "${userProfile.displayName}! 👋",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00D4FF)
                        )
                        Text(
                            text = "Ready for your next adventure?",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = { /* Notification action */ },
                        modifier = Modifier
                            .background(Color(0xFF0F172A), CircleShape)
                            .border(1.dp, Color(0xFF1E293B), CircleShape)
                    ) {
                        BadgedBox(badge = { Badge(containerColor = Color.Red) }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = Color.White)
                        }
                    }
                }
            }

            // Central Drone Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drone_status_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isConnected) "Drone Ready" else "Disconnected",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "AeroGuard One",
                                color = Color(0xFF00D4FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Premium Hand-Drawn Drone Vector Canvas in Compose
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DroneGraphic(animateProps = isConnected)
                        }

                        // Dynamic telemetry stats row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatusItem(icon = Icons.Default.BatteryChargingFull, label = "$batteryLevel%", subLabel = "Battery", color = Color(0xFF10B981))
                            Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFF1E293B)))
                            StatusItem(icon = Icons.Default.GpsFixed, label = "$satellites", subLabel = "GPS Satellites", color = Color(0xFF00D4FF))
                            Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFF1E293B)))
                            StatusItem(icon = Icons.Default.Wifi, label = connection, subLabel = "Connection", color = Color(0xFF00FFCC))
                        }

                        // Large Action Button
                        Button(
                            onClick = { viewModel.setDroneConnected(!isConnected) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("connect_drone_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) Color(0xFF1E293B) else Color(0xFF0052FF)
                            )
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.LinkOff else Icons.Default.Link,
                                contentDescription = "Connect Link"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) "Disconnect Drone" else "Connect to Drone",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Launcher Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Mission Mode",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = { showActivitySelection = true }) {
                        Text("View Details", color = Color(0xFF00D4FF))
                    }
                }
            }

            // Mode Shortcuts Row (Horizontal LazyRow)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val modes = listOf(
                        QuadMode("Travel", "Capture scenic places", Icons.Default.CardTravel, Color(0xFF0052FF)),
                        QuadMode("Creator", "Stunning cinematic shots", Icons.Default.VideoCameraBack, Color(0xFF8B5CF6)),
                        QuadMode("Adventure", "Track sports motion", Icons.Default.DirectionsBike, Color(0xFF10B981)),
                        QuadMode("Explore", "Search hidden gems", Icons.Default.TravelExplore, Color(0xFF00D4FF))
                    )
                    items(modes) { item ->
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .height(160.dp)
                                .clickable {
                                    if (isConnected) {
                                        viewModel.setSelectedMode(item.name)
                                        viewModel.startPreflightCheck()
                                    } else {
                                        // Auto connect first for smoother prototyping
                                        viewModel.setDroneConnected(true)
                                        viewModel.setSelectedMode(item.name)
                                        viewModel.startPreflightCheck()
                                    }
                                }
                                .testTag("mode_card_${item.name.lowercase()}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(item.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(item.icon, contentDescription = item.name, tint = item.color)
                                }
                                Column {
                                    Text(
                                        text = item.name + " Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = item.desc,
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Panel Grid
            item {
                Text(
                    text = "Quick Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val actions = listOf(
                        Pair("Return Home", Icons.Default.HomeWork),
                        Pair("Take Off", Icons.Default.FlightTakeoff),
                        Pair("AI Check", Icons.Default.Shield),
                        Pair("Find Drone", Icons.Default.MyLocation)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        actions.take(2).forEach { (name, icon) ->
                            ActionCard(name, icon, modifier = Modifier.weight(1f)) {
                                if (isConnected) {
                                    if (name == "Take Off" || name == "AI Check") {
                                        viewModel.startPreflightCheck()
                                    }
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        actions.drop(2).forEach { (name, icon) ->
                            ActionCard(name, icon, modifier = Modifier.weight(1f)) {
                                if (name == "AI Check") {
                                    viewModel.startPreflightCheck()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class QuadMode(val name: String, val desc: String, val icon: ImageVector, val color: Color)

@Composable
fun ActionCard(name: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1E293B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = name, tint = Color(0xFF00D4FF), modifier = Modifier.size(20.dp))
            }
            Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Draw a beautiful responsive drone using custom Compose Canvas
@Composable
fun DroneGraphic(animateProps: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.size(140.dp)) {
        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        val radius = 18f

        // Draw structural arms
        val strokeWidth = 8f
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(centerOffset.x - 45f, centerOffset.y - 45f),
            end = Offset(centerOffset.x + 45f, centerOffset.y + 45f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(centerOffset.x + 45f, centerOffset.y - 45f),
            end = Offset(centerOffset.x - 45f, centerOffset.y + 45f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw central core fuselage
        drawCircle(
            color = Color(0xFF0052FF),
            radius = radius + 6f,
            center = centerOffset
        )
        drawCircle(
            color = Color(0xFF00D4FF),
            radius = radius - 2f,
            center = centerOffset
        )

        // Camera lens glow indicator
        drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(centerOffset.x - 4f, centerOffset.y - 4f)
        )

        // Draw propellers / rotors
        val propPositions = listOf(
            Offset(centerOffset.x - 45f, centerOffset.y - 45f),
            Offset(centerOffset.x + 45f, centerOffset.y - 45f),
            Offset(centerOffset.x - 45f, centerOffset.y + 45f),
            Offset(centerOffset.x + 45f, centerOffset.y + 45f)
        )

        propPositions.forEach { pos ->
            drawCircle(color = Color(0xFF1E293B), radius = 12f, center = pos)
            drawCircle(color = Color(0xFF00D4FF), radius = 4f, center = pos)

            // Spin animation if connected
            if (animateProps) {
                // Draw propeller line sweep
                val angleRad = Math.toRadians((rotationAngle).toDouble())
                val endX = pos.x + 20 * Math.cos(angleRad).toFloat()
                val endY = pos.y + 20 * Math.sin(angleRad).toFloat()
                val startX = pos.x - 20 * Math.cos(angleRad).toFloat()
                val startY = pos.y - 20 * Math.sin(angleRad).toFloat()

                drawLine(
                    color = Color.White,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            } else {
                // Idle static propellers
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(pos.x - 16f, pos.y),
                    end = Offset(pos.x + 16f, pos.y),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun StatusItem(icon: ImageVector, label: String, subLabel: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = subLabel, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        Text(text = subLabel, fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}

// Helper to provide standard M3 Icon size
fun Modifier.size(size: Int) = this.size(size.dp)

// ==========================================
// SCREEN 2: CHOOSE ACTIVITY SCREEN
// ==========================================
@Composable
fun ChooseActivityScreen(
    viewModel: DroneViewModel,
    onBack: () -> Unit,
    onStartMission: (String) -> Unit
) {
    val modes = listOf(
        ActivityModeItem("Travel", "Capture scenic mountain ridges, rivers, and canyons automatically. Perfect route planning included.", "20 - 30 min", listOf("Scenic Routing", "Auto Highlights", "Cinematic Panoramas"), Color(0xFF0052FF)),
        ActivityModeItem("Creator", "Cinematic dynamic motion, precise object tracking, and social media formatted recording.", "15 - 25 min", listOf("Subject Tracking", "Smart Framing", "TikTok/Reels format"), Color(0xFF8B5CF6)),
        ActivityModeItem("Adventure", "Follow action paths, cycling trails, high velocity descent, and custom action shots.", "25 - 40 min", listOf("Follow Me", "High G-Force lock", "3D Obstacle Sweep"), Color(0xFF10B981)),
        ActivityModeItem("Explore", "Search hidden local points of interest using maps and autonomously snap panoramas.", "20 - 35 min", listOf("Point of Interest", "360° Panorama", "Historical Scan"), Color(0xFF00D4FF))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Choose Activity", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF030712))
            )
        },
        containerColor = Color(0xFF030712)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "What would you like AeroGuard to do today?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            // AI Recommendation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Recommend", tint = Color(0xFF00FFCC))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Recommendation", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Adventure Mode looks perfect for your location & the current clear breeze.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = { onStartMission("Adventure") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052FF))
                        ) {
                            Text("Go", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Activity Cards
            items(modes) { mode ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartMission(mode.name) }
                        .testTag("mode_config_card_${mode.name.lowercase()}"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column {
                        // Dynamic Abstract Backdrop drawn with Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(mode.color.copy(alpha = 0.8f), Color(0xFF070D1E))
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = mode.name + " Mode",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = mode.description, color = Color(0xFF94A3B8), fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Time", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    Text("Flight Time: ${mode.estTime}", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Features Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                mode.features.forEach { feat ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(feat, fontSize = 10.sp, color = Color.White) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF1E293B))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onStartMission(mode.name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = mode.color)
                            ) {
                                Text("Select and Pre-flight Check", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ActivityModeItem(
    val name: String,
    val description: String,
    val estTime: String,
    val features: List<String>,
    val color: Color
)

// ==========================================
// SCREEN 3: PRE-FLIGHT CHECKLIST
// ==========================================
@Composable
fun PreflightCheckScreen(viewModel: DroneViewModel) {
    val selectedMode by viewModel.selectedMode.collectAsState()
    val checks by viewModel.preflightChecks.collectAsState()
    val progress by viewModel.preflightCheckProgress.collectAsState()

    Scaffold(
        containerColor = Color(0xFF030712)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI PRE-FLIGHT CHECK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4FF),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AI is preparing your flight",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Analyzing local atmospheric data points for $selectedMode Mode.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF00D4FF),
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Checklist Items Column
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    items(checks) { check ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (check.isCompleted) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF00D4FF),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    Column {
                                        Text(check.label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                        Text(check.statusText, fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    }
                                }

                                Text(
                                    text = if (check.isCompleted) "Clear" else "Checking",
                                    color = if (check.isCompleted) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Takeoff Trigger
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { /* Go to ready screen, auto transitioned */ },
                    enabled = progress >= 1.0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("launch_drone_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = "Rocket Launch")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (progress >= 1.0f) "Continue to Mission Launch" else "Systems Calibrating...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Autonomous flight paths checked with localized geofence safety locks.", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

// ==========================================
// SCREEN 4: READY TO LAUNCH
// ==========================================
@Composable
fun MissionReadyScreen(viewModel: DroneViewModel) {
    val mode by viewModel.selectedMode.collectAsState()

    Scaffold(
        containerColor = Color(0xFF030712)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AEROGUARD MISSION READY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4FF),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Systems Green",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Airspace cleared for autonomous $mode flight plan.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsing 3D-effect Drone Illustration
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color(0xFF0B1528), CircleShape)
                        .border(1.dp, Color(0xFF00D4FF), CircleShape)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DroneGraphic(animateProps = true)
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Config Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, contentDescription = "Route", tint = Color(0xFF00D4FF))
                            Column {
                                Text("Autonomous Route", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("3.2 km dynamic trajectory plan", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                        }
                        Divider(color = Color(0xFF1E293B))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudQueue, contentDescription = "Weather", tint = Color(0xFF10B981))
                            Column {
                                Text("Atmosphere Safety Lock", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("GPS Signal Active • Locked on 19 Satellites", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // Big Blue LAUNCH Button
            Button(
                onClick = { viewModel.launchDroneMission() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("launch_trigger_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052FF))
            ) {
                Icon(Icons.Default.FlightTakeoff, contentDescription = "Takeoff", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("LAUNCH AUTONOMOUS FLIGHT", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

// ==========================================
// SCREEN 5: ACTIVE FLIGHT (FULL SCREEN CAMERA OVERLAY)
// ==========================================
@Composable
fun ActiveFlightScreen(viewModel: DroneViewModel) {
    val alt by viewModel.altitude.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val time by viewModel.flightTime.collectAsState()
    val dist by viewModel.flightDistance.collectAsState()
    val battery by viewModel.batteryLevel.collectAsState()
    val suggestions by viewModel.aiSuggestions.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full screen camera simulated live view backdrop (Sunset mountain scan lines)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Abstract warm horizon landscape
            val brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B1528), Color(0xFF5B21B6), Color(0xFFF43F5E), Color(0xFFFBBF24))
            )
            drawRect(brush = brush)

            // Dynamic scope lines / crosshairs
            val crosshairColor = Color(0xFF00D4FF).copy(alpha = 0.4f)
            drawLine(crosshairColor, androidx.compose.ui.geometry.Offset(width / 2f - 40f, height / 2f), androidx.compose.ui.geometry.Offset(width / 2f + 40f, height / 2f), strokeWidth = 3f)
            drawLine(crosshairColor, androidx.compose.ui.geometry.Offset(width / 2f, height / 2f - 40f), androidx.compose.ui.geometry.Offset(width / 2f, height / 2f + 40f), strokeWidth = 3f)

            // Horizon angle guide line
            val angleOffset = sin(System.currentTimeMillis() / 1500.0).toFloat() * 15f
            drawLine(
                color = Color(0xFF00FFCC).copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(width / 2f - 120f, height / 2f + angleOffset),
                end = androidx.compose.ui.geometry.Offset(width / 2f + 120f, height / 2f - angleOffset),
                strokeWidth = 2f
            )
        }

        // Overlay: Top Telemetry Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Battery", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("$battery%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duration", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(time, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Distance", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(dist, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Speed", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("$speed km/h", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4FF))
                }
            }
        }

        // Overlay: Live AI Capture Notifications (Bottom Sheet-like floating panel)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live AI captured highlights list
            if (suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070D1E).copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF00D4FF).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Captures", tint = Color(0xFF00D4FF), modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AERO COPILOT CAPTURED MOMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC))
                            Text(suggestions.first(), fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Command Control center (Pause, return, end mission)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: height status slider mock
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Altitude", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text("${alt.toInt()} m", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4FF))
                    }

                    // Middle/Right controls
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(
                            onClick = { viewModel.pauseFlight() },
                            modifier = Modifier.background(Color(0xFF1E293B), CircleShape).size(48.dp)
                        ) {
                            Icon(if (speed == 0) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                        }

                        Button(
                            onClick = { viewModel.endFlightMission() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp).testTag("end_mission_button")
                        ) {
                            Icon(Icons.Default.FlightLand, contentDescription = "Land")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("End Mission", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 8: MISSION COMPLETE CELEBRATION
// ==========================================
@Composable
fun MissionCompleteScreen(viewModel: DroneViewModel) {
    val recent by viewModel.recentActivities.collectAsState()
    val latest = recent.firstOrNull() ?: FlightLog()

    Scaffold(
        containerColor = Color(0xFF030712)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Celebration Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Success Verification",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Amazing Flight!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Boeing AeroGuard returned and landed safely.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Dashboard Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("MISSION SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4FF), letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Flight Time", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(latest.durationMin + " min", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Distance", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(latest.distanceKm + " km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Captured Shots", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("${latest.photosCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // AI Highlights slider title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Highlights Captured", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text("Saved to cloud", fontSize = 12.sp, color = Color(0xFF10B981))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slider of scenic cards
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val highlights = listOf(
                        Pair("Beautiful Sunset", "Cinematic Video"),
                        Pair("Group Moment", "Photo"),
                        Pair("Epic Landscape", "Panorama"),
                        Pair("Action Shot", "Slow Motion")
                    )
                    items(highlights) { highlight ->
                        Card(
                            modifier = Modifier
                                .width(130.dp)
                                .height(110.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(Icons.Default.Image, contentDescription = highlight.first, tint = Color(0xFF00D4FF))
                                Column {
                                    Text(highlight.first, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(highlight.second, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }
            }

            // CTA Action Buttons
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.setStoryMood("Adventure") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("view_ai_story_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052FF))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Sparkles")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate AI Cinematic Story", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.returnToDashboard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Text("Back to Dashboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

fun RowScope.SpaceSpaceBy(dp: Int) = Arrangement.spacedBy(dp.dp)

// ==========================================
// SCREEN 9: AI CINEMATIC STORY SCREEN
// ==========================================
@Composable
fun StoryViewScreen(viewModel: DroneViewModel) {
    val storyText by viewModel.currentStoryText.collectAsState()
    val selectedMood by viewModel.currentStoryMood.collectAsState()
    val isLoading by viewModel.isStoryLoading.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Drone Story", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.returnToDashboard() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF030712))
            )
        },
        containerColor = Color(0xFF030712)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Mock Cinematic Video Player with play overlay & status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF13233F), Color(0xFF070D1E))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing visual audio peaks/waveform mockup
                        Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                            val count = 24
                            val itemWidth = size.width / count
                            for (i in 0 until count) {
                                val height = (sin(i * 0.5 + System.currentTimeMillis() / 400.0) * size.height * 0.4f).toFloat()
                                drawLine(
                                    color = Color(0xFF00D4FF).copy(alpha = 0.6f),
                                    start = androidx.compose.ui.geometry.Offset(i * itemWidth, size.height / 2f - height),
                                    end = androidx.compose.ui.geometry.Offset(i * itemWidth, size.height / 2f + height),
                                    strokeWidth = 4f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF0052FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Text(
                            text = "Soundtrack: Epic Mountain Ascent Active",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mood Selection
                Text("Select Story Mood", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val moods = listOf("Adventure", "Epic", "Relaxed", "Happy")
                    moods.forEach { mood ->
                        val active = selectedMood == mood
                        FilterChip(
                            selected = active,
                            onClick = { viewModel.setStoryMood(mood) },
                            label = { Text(mood, fontSize = 12.sp, color = if (active) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00D4FF)
                            ),
                            modifier = Modifier.testTag("mood_chip_${mood.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Generative AI Text Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Story Sparkle", tint = Color(0xFF00D4FF))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AeroGuard Storyteller", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                            Text("Gemini 3.5 Flash", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .size(24.dp),
                                color = Color(0xFF00D4FF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Generating custom cinematic journal story...", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        } else {
                            Text(
                                text = storyText,
                                fontSize = 14.sp,
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* Save action */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save story", fontSize = 13.sp)
                }

                Button(
                    onClick = { /* Share action */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052FF))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share reel", fontSize = 13.sp)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 6: TIMELINE / HISTORICAL GALLERY
// ==========================================
@Composable
fun GalleryScreen(viewModel: DroneViewModel) {
    val logs by viewModel.recentActivities.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Flight Memories", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF030712))
            )
        },
        containerColor = Color(0xFF030712)
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "No files", tint = Color(0xFF1E293B), modifier = Modifier.size(64.dp))
                    Text("No records found", color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 12.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text("Timeline of auto-captured cinematic content.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }

                items(logs) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSelectedMode(log.title.replace(" Mode Mission", ""))
                                viewModel.setStoryMood(log.mood)
                            }
                            .testTag("gallery_item_${log.id}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Left mini thumbnail placeholder
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF13233F), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Reel", tint = Color(0xFF00D4FF))
                            }

                            // Content Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Text(log.date, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Text("Duration: ${log.durationMin} min • ${log.distanceKm} km", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("${log.photosCount} photos", fontSize = 10.sp) }
                                    )
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("${log.videosCount} videos", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 7: VOICE ASSISTANT COPILOT (LOW LATENCY)
// ==========================================
@Composable
fun VoiceAssistantScreen(viewModel: DroneViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isListening by viewModel.assistantIsListening.collectAsState()
    val isThinking by viewModel.assistantIsThinking.collectAsState()

    var typedText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Copilot", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF030712))
            )
        },
        containerColor = Color(0xFF030712)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Conversational List of logs
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (msg.isUser) 16.dp else 0.dp,
                                bottomEnd = if (msg.isUser) 0.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (msg.isUser) Color(0xFF0052FF) else Color(0xFF0B1528)
                            ),
                            border = if (msg.isUser) null else BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.text, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (isThinking) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00D4FF), strokeWidth = 2.dp)
                            Text("AeroGuard Core thinking...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }

            // Waveform and input panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Waveform Canvas representation
                if (isListening) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        val stroke = 3f
                        val itemsCount = 30
                        val gap = size.width / itemsCount
                        for (i in 0 until itemsCount) {
                            val waveH = (sin(i * 0.4 + System.currentTimeMillis() / 200.0) * size.height * 0.6f).toFloat()
                            drawLine(
                                color = Color(0xFF00D4FF),
                                start = androidx.compose.ui.geometry.Offset(i * gap, size.height / 2f - waveH),
                                end = androidx.compose.ui.geometry.Offset(i * gap, size.height / 2f + waveH),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Suggested action chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val chips = listOf("Follow Me", "Orbit", "Sunset Photo", "Return Home")
                    chips.take(3).forEach { title ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.sendVoiceCommand(title) },
                            label = { Text(title, fontSize = 10.sp, color = Color.White) },
                            colors = InputChipDefaults.inputChipColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.testTag("assist_chip_${title.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                // Input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1528), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = typedText,
                        onValueChange = { typedText = it },
                        placeholder = { Text("Speak naturally to AeroGuard...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("chat_input_text")
                    )

                    IconButton(
                        onClick = {
                            if (typedText.isNotEmpty()) {
                                viewModel.sendVoiceCommand(typedText)
                                typedText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF00D4FF))
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 10: SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: DroneViewModel) {
    val obsAvoid by viewModel.obstacleAvoidance.collectAsState()
    val homeAlt by viewModel.returnHomeAltitude.collectAsState()
    val maxSpeed by viewModel.maxFlightSpeedSetting.collectAsState()
    val metricUnits by viewModel.unitsMetric.collectAsState()

    var showWallpaperGenerator by remember { mutableStateOf(false) }

    if (showWallpaperGenerator) {
        WallpaperGeneratorScreen(viewModel = viewModel) {
            showWallpaperGenerator = false
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Drone Configuration", fontWeight = FontWeight.Bold, color = Color.White) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF030712))
                )
            },
            containerColor = Color(0xFF030712)
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text("FLIGHT CONTROLLER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4FF), letterSpacing = 1.sp)
                }

                // Obstacle Avoidance switch
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Lidar Obstacle Avoidance", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Auto avoids obstacles using active radar", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                            Switch(
                                checked = obsAvoid,
                                onCheckedChange = { viewModel.toggleObstacleAvoidance() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D4FF)),
                                modifier = Modifier.testTag("obstacle_switch")
                            )
                        }
                    }
                }

                // Return Home Altitude Slider
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Return Home Altitude", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("${homeAlt} m", fontWeight = FontWeight.Bold, color = Color(0xFF00D4FF), fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = homeAlt.toFloat(),
                                onValueChange = { viewModel.setReturnHomeAltitude(it.toInt()) },
                                valueRange = 30f..150f,
                                modifier = Modifier.testTag("altitude_slider")
                            )
                        }
                    }
                }

                // Max Speed Slider
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Maximum Safe Flight Speed", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("${maxSpeed} km/h", fontWeight = FontWeight.Bold, color = Color(0xFF00D4FF), fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = maxSpeed.toFloat(),
                                onValueChange = { viewModel.setMaxFlightSpeedSetting(it.toInt()) },
                                valueRange = 10f..60f,
                                modifier = Modifier.testTag("speed_slider")
                            )
                        }
                    }
                }

                item {
                    Text("CREATIVE LAB & DECORATIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4FF), letterSpacing = 1.sp, modifier = Modifier.padding(top = 10.dp))
                }

                // AI wallpaper generator button link
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWallpaperGenerator = true }
                            .testTag("wallpaper_generator_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Wallpaper", tint = Color(0xFF00D4FF))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Creative Wallpaper Lab", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Generate stunning 4K wallpapers with gemini-3-pro-image-preview", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color.White)
                        }
                    }
                }

                item {
                    Text("SYSTEM CONFIGURATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D4FF), letterSpacing = 1.sp, modifier = Modifier.padding(top = 10.dp))
                }

                // Segmented Units Choice
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Measurement System", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (metricUnits) Color(0xFF0052FF) else Color.Transparent)
                                        .clickable { viewModel.setUnitsMetric(true) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Metric", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (!metricUnits) Color(0xFF0052FF) else Color.Transparent)
                                        .clickable { viewModel.setUnitsMetric(false) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Imperial", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CREATIVE FEATURE: WALLPAPER GENERATOR LAB (1K, 2K, 4K SUPPORT)
// ==========================================
@Composable
fun WallpaperGeneratorScreen(
    viewModel: DroneViewModel,
    onBack: () -> Unit
) {
    val prompt by viewModel.wallpaperPrompt.collectAsState()
    val size by viewModel.wallpaperSize.collectAsState()
    val status by viewModel.generatedWallpaperStatus.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Wallpaper Lab", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF030712))
            )
        },
        containerColor = Color(0xFF030712)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "AeroGuard Wallpaper Creator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Enter a creative description to render custom desktop or smartphone wallpapers using Google Gemini 3 Pro Image AI.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                // Text Input
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { viewModel.setWallpaperPrompt(it) },
                    label = { Text("Describe wallpaper theme...", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth().testTag("wallpaper_prompt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00D4FF),
                        unfocusedBorderColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Size Affordances (1K, 2K, 4K)
                Text("Select Target Resolution", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val resolutions = listOf("1K", "2K", "4K")
                    resolutions.forEach { res ->
                        val selected = size == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color(0xFF0052FF) else Color(0xFF0B1528))
                                .border(1.dp, if (selected) Color(0xFF00D4FF) else Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .clickable { viewModel.setWallpaperSize(res) }
                                .padding(vertical = 12.dp)
                                .testTag("resolution_choice_$res"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(res, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(if (res == "4K") "Ultra HD" else if (res == "2K") "Quad HD" else "Full HD", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                // Image Rendering Display Canvas Area
                if (status.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (status == "loading") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF00D4FF))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Generating premium $size wallpaper...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            } else {
                                // Dynamic beautiful abstract preview matching the custom prompt
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val grad = Brush.sweepGradient(
                                        colors = listOf(Color(0xFF0033A0), Color(0xFF00FFCC), Color(0xFF0052FF), Color(0xFF0033A0))
                                    )
                                    drawRect(brush = grad)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Wallpaper $size Ready for Download", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Generate Button Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (status == "success" || status == "simulated_success") {
                    Button(
                        onClick = { viewModel.clearWallpaper() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Text("Reset")
                    }
                }

                Button(
                    onClick = { viewModel.generateWallpaper() },
                    enabled = prompt.isNotEmpty() && status != "loading",
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp)
                        .testTag("wallpaper_generate_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Sparkle")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Render Wallpapers", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 11: PROFILE SCREEN & FIREBASE AUTH SYNC
// ==========================================
@Composable
fun ProfileScreen(viewModel: DroneViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val recent by viewModel.recentActivities.collectAsState()

    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pilot Command Center", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF030712)),
                actions = {
                    IconButton(onClick = { showGoogleLoginDialog = true }) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = Color(0xFF00D4FF))
                    }
                }
            )
        },
        containerColor = Color(0xFF030712)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Profile Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF0052FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.displayName.first().toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(userProfile.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                                if (userProfile.isPremium) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF00D4FF).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("PREMIUM", color = Color(0xFF00D4FF), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Text(userProfile.email, color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("Colorado, USA • Member since 2024", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }

            // Stat Cards Horizontal Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCardItem(label = "Flights", value = "${userProfile.flightsCount}", modifier = Modifier.weight(1f))
                    StatCardItem(label = "Total Km", value = "${userProfile.totalDistanceKm}", modifier = Modifier.weight(1f))
                    StatCardItem(label = "Badges", value = "${userProfile.achievements}", modifier = Modifier.weight(1f))
                }
            }

            // Cloud Storage Sync Backup Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudQueue, contentDescription = "Backup", tint = Color(0xFF00FFCC))
                                Text("Aero Cloud Synced Backup", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                            Text("23.4 GB / 100 GB used", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { 0.23f },
                            color = Color(0xFF00FFCC),
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            // Pilot Achievements Milestones
            item {
                Text("Pilot Achievements", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AchievementBadge(title = "First Flight", desc = "Completed", color = Color(0xFF0052FF), icon = Icons.Default.Verified)
                    AchievementBadge(title = "Explorer", desc = "10 KM Flown", color = Color(0xFF10B981), icon = Icons.Default.Explore)
                    AchievementBadge(title = "Photographer", desc = "Captured 100+", color = Color(0xFFFFB020), icon = Icons.Default.CameraAlt)
                }
            }

            // Log out link
            item {
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f), contentColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Log out")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Pilot Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Google Sign-In and Firestore Sync Setup Dialog
    if (showGoogleLoginDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleLoginDialog = false },
            title = { Text("Google Cloud Synchronizer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Securely identify using Google Sign-In & synchronize your drone flight logs to the Firestore Database.")
                    OutlinedButton(
                        onClick = {
                            viewModel.authenticateWithGoogle("sanjeev.aero@boeing.com", "Sanjeev Behera")
                            showGoogleLoginDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00D4FF))
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Google")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign In as Sanjeev Behera")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleLoginDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF0B1528)
        )
    }
}

@Composable
fun StatCardItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(84.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1528)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
            Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun AchievementBadge(title: String, desc: String, color: Color, icon: ImageVector) {
    Card(
        modifier = Modifier
            .width(108.dp)
            .height(116.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, textAlign = TextAlign.Center)
                Text(desc, fontSize = 9.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
            }
        }
    }
}
