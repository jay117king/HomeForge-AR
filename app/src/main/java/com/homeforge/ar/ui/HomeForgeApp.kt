package com.homeforge.ar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun HomeForgeApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onStartScan = { navController.navigate("scan") },
                onOpenProjects = { /* TODO */ }
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
                onBack = { navController.popBackStack("home", inclusive = false) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartScan: () -> Unit,
    onOpenProjects: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HomeForge AR") },
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
            Text(
                text = "Remodel any room in true 3D",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Scan → Place real products → See them in AR",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onStartScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan a Room")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onOpenProjects,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("My Projects")
            }
        }
    }
}

@Composable
fun ScanScreen(
    onScanComplete: () -> Unit,
    onBack: () -> Unit
) {
    // Placeholder – will be replaced with full ARCore + Filament view
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AR Scanning View", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("ARCore + Depth API will live here")
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onScanComplete) {
                Text("Simulate Scan Complete")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
fun RoomViewScreen(
    onBack: () -> Unit
) {
    // Placeholder for the interactive 3D room + product sidebar
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("3D Room View", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Filament renderer + product placement")
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onBack) {
                Text("Back to Home")
            }
        }
    }
}
