package com.homeforge.ar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.homeforge.ar.data.Product
import com.homeforge.ar.data.ProductRepository

@Composable
fun HomeForgeApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repo = remember { ProductRepository(context) }

    LaunchedEffect(Unit) {
        repo.ensureSeedData()
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onStartScan = { navController.navigate("scan") },
                onOpenProjects = { /* future */ }
            )
        }
        composable("scan") {
            ScanScreen(
                onScanComplete = { navController.navigate("room") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("room") {
            RoomViewScreen(
                repo = repo,
                onBack = { navController.popBackStack("home", inclusive = false) }
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
                title = { Text("HomeForge AR", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Remodel any room in true 3D",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan \u2192 Place real products \u2192 See them in AR",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Scan a Room")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenProjects,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Folder, null)
                Spacer(Modifier.width(8.dp))
                Text("My Projects")
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { resetTrigger++ }) {
                        Icon(Icons.Default.Refresh, "Reset measure")
                    }
                }
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

            // Status chip
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = if (planeCount == 0) "Move phone to detect surfaces"
                           else "$planeCount plane${if (planeCount == 1) "" else "s"} detected",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Bottom controls
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                    .padding(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Distance", style = MaterialTheme.typography.labelMedium)
                        Text(
                            distanceText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocked) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text("$planeCount planes") },
                        leadingIcon = {
                            Icon(Icons.Default.Layers, null, Modifier.size(16.dp))
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { measureTrigger++ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Straighten, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Measure")
                    }

                    Button(
                        onClick = { lockTrigger++ },
                        modifier = Modifier.weight(1f),
                        colors = if (isLocked)
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        else
                            ButtonDefaults.buttonColors()
                    ) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            null,
                            Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isLocked) "Locked" else "Lock")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onScanComplete,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = planeCount > 0
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Finalize Room")
                }

                Text(
                    "Tip: Tap the screen or press Measure to set points. Lock when happy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomViewScreen(repo: ProductRepository, onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var minL by remember { mutableFloatStateOf(0f) }
    var maxL by remember { mutableFloatStateOf(300f) }
    var minW by remember { mutableFloatStateOf(0f) }
    var maxW by remember { mutableFloatStateOf(200f) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showSidebar by remember { mutableStateOf(true) }

    LaunchedEffect(searchQuery, minL, maxL, minW, maxW) {
        products = repo.search(
            query = searchQuery,
            minL = minL.takeIf { it > 0f },
            maxL = maxL.takeIf { it < 300f },
            minW = minW.takeIf { it > 0f },
            maxW = maxW.takeIf { it < 200f }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D Room") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSidebar = !showSidebar }) {
                        Icon(
                            if (showSidebar) Icons.Default.ViewSidebar else Icons.Default.Search,
                            "Toggle products"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            Box(
                Modifier
                    .weight(if (showSidebar) 0.55f else 1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ViewInAr, null, Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Filament 3D / AR Overlay", style = MaterialTheme.typography.titleMedium)
                    Text("Orbit • Place • Scale", style = MaterialTheme.typography.bodySmall)
                    selectedProduct?.let { p ->
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Placed: ${p.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (showSidebar) {
                Column(
                    Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
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
                    RangeSlider(
                        value = minL..maxL,
                        onValueChange = { minL = it.start; maxL = it.endInclusive },
                        valueRange = 0f..300f,
                        steps = 29
                    )
                    Text("Width (cm)", style = MaterialTheme.typography.labelSmall)
                    RangeSlider(
                        value = minW..maxW,
                        onValueChange = { minW = it.start; maxW = it.endInclusive },
                        valueRange = 0f..200f,
                        steps = 19
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("${products.size} products", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                isSelected = selectedProduct?.id == product.id,
                                onClick = { selectedProduct = product }
                            )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUrls.firstOrNull(),
                contentDescription = product.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${product.lengthCm.toInt()} \u00d7 ${product.widthCm.toInt()} \u00d7 ${product.heightCm.toInt()} cm",
                    style = MaterialTheme.typography.labelSmall
                )
                product.price?.let {
                    Text(
                        "$${\"%.0f\".format(it)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
