package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.model.ReadingTheme
import com.example.ui.screens.AllNotesScreen
import com.example.ui.screens.CloudScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SyncSettingsScreen
import com.example.ui.theme.PageCraftTheme
import com.example.ui.viewmodel.PdfViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentReadingTheme by viewModel.readingTheme.collectAsState()
            val isDarkTheme = currentReadingTheme == ReadingTheme.CHARCOAL || currentReadingTheme == ReadingTheme.OLED_NIGHT

            PageCraftTheme(darkTheme = isDarkTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PdfViewModel) {
    val activeDocument by viewModel.activeDocument.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    if (activeDocument != null) {
        // Active Reader Screen (full-screen immersive "Read like a book" experience)
        ReaderScreen(
            document = activeDocument!!,
            viewModel = viewModel,
            onBack = { viewModel.closeDocument() }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("main_navigation_bar")) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                        label = { Text("Library") },
                        modifier = Modifier.testTag("nav_library")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(imageVector = Icons.Default.Cloud, contentDescription = null) },
                        label = { Text("Cloud") },
                        modifier = Modifier.testTag("nav_cloud")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(imageVector = Icons.Default.EditNote, contentDescription = null) },
                        label = { Text("Notes") },
                        modifier = Modifier.testTag("nav_notes")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Sync") },
                        modifier = Modifier.testTag("nav_sync")
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Crossfade(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    0 -> LibraryScreen(
                        viewModel = viewModel,
                        onOpenDocument = { /* Document opened inside viewModel */ }
                    )
                    1 -> CloudScreen(viewModel = viewModel)
                    2 -> AllNotesScreen(viewModel = viewModel)
                    3 -> SyncSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
