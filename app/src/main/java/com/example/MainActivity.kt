package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MathViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppStructure()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppStructure() {
    val viewModel: MathViewModel = viewModel()
    val currentTab by viewModel.currentTab.collectAsState()
    val isKeyboardVisible = WindowInsets.isImeVisible

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepSpaceBlack,
        bottomBar = {
            if (!isKeyboardVisible) {
                // Premium custom bottom navigation bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // Respect device gesture pill bottom insets
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(CardGlassDark)
                            .border(1.dp, GlassBorderDark, RoundedCornerShape(24.dp))
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Calculator tab
                        BottomNavItem(
                            icon = Icons.Default.Calculate,
                            label = "Calc",
                            isSelected = currentTab == "Calculator",
                            onClick = { viewModel.switchTab("Calculator") },
                            modifier = Modifier.testTag("nav_tab_calc")
                        )

                        // Graph tab
                        BottomNavItem(
                            icon = Icons.Default.Timeline,
                            label = "Graph",
                            isSelected = currentTab == "Graph",
                            onClick = { viewModel.switchTab("Graph") },
                            modifier = Modifier.testTag("nav_tab_graph")
                        )

                        // Spacer for the Floating AI action button
                        Spacer(modifier = Modifier.width(48.dp))

                        // Scanner tab
                        BottomNavItem(
                            icon = Icons.Default.DocumentScanner,
                            label = "Scan",
                            isSelected = currentTab == "Scanner",
                            onClick = { viewModel.switchTab("Scanner") },
                            modifier = Modifier.testTag("nav_tab_scan")
                        )

                        // Settings tab
                        BottomNavItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            isSelected = currentTab == "Settings",
                            onClick = { viewModel.switchTab("Settings") },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }

                    // SUBTLE GLOWING FAB AI BUTTON
                    // Positioned floating elegantly in the center of the bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-20).dp)
                            .size(56.dp)
                            .clip(CircleShape)
                             .background(
                                 Brush.linearGradient(
                                     colors = listOf(AccentIndigo, AccentBlue)
                                 )
                             )
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                // Switches to AI Assistant chat screen
                                viewModel.switchTab("AI")
                            }
                            .testTag("fab_ai_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Fx-Math AI Solver Core",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Safe drawing notch and status bars insets
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { targetTab ->
                when (targetTab) {
                    "Calculator" -> CalculatorScreen(viewModel = viewModel, innerPadding = innerPadding, onShowHistorySnippet = { viewModel.switchTab("History") })
                    "Graph" -> GraphScreen(viewModel = viewModel, innerPadding = innerPadding)
                    "Scanner" -> ScannerScreen(viewModel = viewModel, innerPadding = innerPadding)
                    "Settings" -> SettingsScreen(viewModel = viewModel, innerPadding = innerPadding)
                    "History" -> HistoryScreen(viewModel = viewModel, innerPadding = innerPadding, onNavigateToAiChat = { viewModel.switchTab("AI") })
                    "AI" -> AiScreen(viewModel = viewModel, innerPadding = innerPadding)
                    else -> CalculatorScreen(viewModel = viewModel, innerPadding = innerPadding, onShowHistorySnippet = {})
                }
            }
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) AccentIndigoLight else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
