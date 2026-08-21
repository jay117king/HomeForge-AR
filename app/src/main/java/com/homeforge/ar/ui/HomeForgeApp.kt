package com.homeforge.ar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.homeforge.ar.ar.ArCameraView
import com.homeforge.ar.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeForgeApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val productRepo = remember { ProductRepository(context) }
    val projectRepo = remember { ProjectRepository(context) }

    LaunchedEffect(Unit) {
        productRepo.ensureSeedData()
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartScan = { navController.navigate("scan") },
                onOpenProjects = { navController.navigate("projects") }
            )
        }
        composable("scan") {
            ScanScreen(
                onScanComplete = {
                    navController.navigate("room") {
                        popUpTo("home")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("room") {
            RoomViewScreen(
                productRepo = productRepo,
                projectRepo = projectRepo,
                onBack = { navController.popBackStack("home", inclusive = false) }
            )
        }
        composable("projects") {
            ProjectsScreen(
                projectRepo = projectRepo,
                onBack = { navController.popBackStack() },
                onOpenProject = { /* future: load into room view */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onStartScan: () -> Unit, onOpenProjects: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HomeForge AR", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Home, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Remodel any room in true 3D", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Scan \u2192 Place real products \u2192 See them in 3D", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(48.dp))
            Button(onClick = onStartScan, Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Scan a Room")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenProjects, Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Default.Folder, null); Spacer(Modifier.width(8.dp)); Text("My Projects")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onScanComplete: () -> Unit, onBack: () -> Unit) {
    var distanceText by remember { mutableStateOf("—") }
    var isLocked by remember { mutableStateOf(false) }
    var planeCount by remember { mutableIntStateOf(0) }
    var measureTrigger by remember { mutableIntStateOf(0) }
    var lockTrigger by remember { mutableIntStateOf(0) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Room") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { resetTrigger++ }) { Icon(Icons.Default.Refresh, null) } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            ArCameraView(
                modifier = Modifier.fillMaxSize(),
                onPlaneCountChanged = { planeCount = it },
                onDistanceChanged = { distanceText = it },
                onLockedChanged = { isLocked = it },
                measureTrigger = measureTrigger,
                lockTrigger = lockTrigger,
                resetTrigger = resetTrigger
            )

            Surface(
                Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 4.dp
            ) {
                Text(
                    if (planeCount == 0) "Move phone to detect surfaces" else "$planeCount plane${if (planeCount == 1) "" else "s"} detected",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)).padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Distance", style = MaterialTheme.typography.labelMedium)
                        Text(distanceText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            color = if (isLocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                    }
                    AssistChip(onClick = {}, label = { Text("$planeCount planes") },
                        leadingIcon = { Icon(Icons.Default.Layers, null, Modifier.size(16.dp)) })
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { measureTrigger++ }, Modifier.weight(1f)) {
                        Icon(Icons.Default.Straighten, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Measure")
                    }
                    Button(onClick = { lockTrigger++ }, Modifier.weight(1f),
                        colors = if (isLocked) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        else ButtonDefaults.buttonColors()) {
                        Icon(if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text(if (isLocked) "Locked" else "Lock")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        // Store scan result for the 3D room view
                        val distM = distanceText.replace(" cm", "").replace(" m", "").toFloatOrNull()?.let {
                            if (distanceText.contains("m")) it else it / 100f
                        }
                        ScanResultHolder.latest = ScanResult(
                            widthMeters = distM?.coerceIn(1.5f, 12f) ?: 4.0f,
                            depthMeters = (distM?.coerceIn(1.5f, 12f) ?: 4.0f) * 0.85f,
                            heightMeters = 2.5f,
                            planeCount = planeCount,
                            lockedDistanceMeters = distM
                        )
                        onScanComplete()
                    },
                    Modifier.fillMaxWidth().height(48.dp),
                    enabled = planeCount > 0
                ) {
                    Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Finalize Room")
                }
                Text("Tip: Tap screen or Measure to set points. Lock when happy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomViewScreen(
    productRepo: ProductRepository,
    projectRepo: ProjectRepository,
    onBack: () -> Unit
) {
    val scan = remember { ScanResultHolder.latest }
    var searchQuery by remember { mutableStateOf("") }
    var minL by remember { mutableFloatStateOf(0f) }
    var maxL by remember { mutableFloatStateOf(300f) }
    var minW by remember { mutableFloatStateOf(0f) }
    var maxW by remember { mutableFloatStateOf(200f) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var placed by remember { mutableStateOf<List<Product>>(emptyList()) }
    var showSidebar by remember { mutableStateOf(true) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("My Room") }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Simple 3D-ish top-down view state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(searchQuery, minL, maxL, minW, maxW) {
        products = productRepo.search(
            query = searchQuery,
            minL = minL.takeIf { it > 0f },
            maxL = maxL.takeIf { it < 300f },
            minW = minW.takeIf { it > 0f },
            maxW = maxW.takeIf { it < 200f }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Project") },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        projectRepo.saveProject(
                            name = projectName.ifBlank { "My Room" },
                            roomType = "Kitchen",
                            scan = scan,
                            placedProductIds = placed.map { it.id }
                        )
                        saveMessage = "Saved!"
                        showSaveDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    saveMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2000)
            saveMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D Room") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) { Icon(Icons.Default.Save, "Save") }
                    IconButton(onClick = { showSidebar = !showSidebar }) {
                        Icon(if (showSidebar) Icons.Default.ViewSidebar else Icons.Default.Search, null)
                    }
                }
            )
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            // ── Room visualization (top-down + perspective hint) ─────────────
            Box(
                Modifier
                    .weight(if (showSidebar) 0.55f else 1f)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1D23))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            offsetX += pan.x
                            offsetY += pan.y
                            scale = (scale * zoom).coerceIn(0.4f, 3f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2 + offsetX
                    val cy = size.height / 2 + offsetY
                    val roomW = scan.widthMeters * 80f * scale
                    val roomD = scan.depthMeters * 80f * scale

                    // Floor
                    drawRect(
                        color = Color(0xFF3A3F4B),
                        topLeft = Offset(cx - roomW / 2, cy - roomD / 2),
                        size = androidx.compose.ui.geometry.Size(roomW, roomD)
                    )
                    // Border
                    drawRect(
                        color = Color(0xFF6B8CFF),
                        topLeft = Offset(cx - roomW / 2, cy - roomD / 2),
                        size = androidx.compose.ui.geometry.Size(roomW, roomD),
                        style = Stroke(width = 3f)
                    )

                    // Placed products as rectangles
                    placed.forEachIndexed { i, p ->
                        val pw = (p.lengthCm / 100f) * 80f * scale
                        val pd = (p.widthCm / 100f) * 80f * scale
                        val px = cx - roomW / 2 + 40f * scale + i * (pw + 20f * scale)
                        val py = cy - pd / 2
                        drawRect(
                            color = Color(0xFF4FC3F7),
                            topLeft = Offset(px, py),
                            size = androidx.compose.ui.geometry.Size(pw, pd)
                        )
                    }
                }

                Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                    Text(
                        "${\"%.1f\".format(scan.widthMeters)} m \u00d7 ${\"%.1f\".format(scan.depthMeters)} m",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${scan.planeCount} planes \u2022 ${placed.size} items",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium)
                    Text("Pinch to zoom \u2022 Drag to pan", color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall)
                }

                if (saveMessage != null) {
                    Surface(
                        Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(saveMessage!!, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }

            // ── Product sidebar ───────────────────────────────────────────────
            if (showSidebar) {
                Column(
                    Modifier.weight(0.45f).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface).padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search table, cupboard\u2026") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Length (cm)", style = MaterialTheme.typography.labelSmall)
                    RangeSlider(value = minL..maxL, onValueChange = { minL = it.start; maxL = it.endInclusive }, valueRange = 0f..300f, steps = 29)
                    Text("Width (cm)", style = MaterialTheme.typography.labelSmall)
                    RangeSlider(value = minW..maxW, onValueChange = { minW = it.start; maxW = it.endInclusive }, valueRange = 0f..200f, steps = 19)

                    if (placed.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Placed (${placed.size})", style = MaterialTheme.typography.labelMedium)
                        placed.forEach { p ->
                            Text("\u2022 ${p.name}", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { placed = emptyList() }) { Text("Clear all") }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("${products.size} products", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                isSelected = placed.any { it.id == product.id },
                                onClick = {
                                    placed = if (placed.any { it.id == product.id }) {
                                        placed.filter { it.id != product.id }
                                    } else {
                                        placed + product
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projectRepo: ProjectRepository,
    onBack: () -> Unit,
    onOpenProject: (Long) -> Unit
) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        projects = projectRepo.getAllProjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Projects") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No saved projects yet.\nScan a room and tap Save.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects, key = { it.id }) { project ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenProject(project.id) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, null, Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(project.name, fontWeight = FontWeight.Medium)
                                Text(project.roomType + " \u2022 " + dateFormat.format(Date(project.updatedAt)),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    projectRepo.deleteProject(project)
                                    projects = projectRepo.getAllProjects()
                                }
                            }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUrls.firstOrNull(),
                contentDescription = product.name,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${product.lengthCm.toInt()} \u00d7 ${product.widthCm.toInt()} \u00d7 ${product.heightCm.toInt()} cm",
                    style = MaterialTheme.typography.labelSmall)
                product.price?.let {
                    Text("$${\"%.0f\".format(it)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (isSelected) Text("Tap to remove", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
