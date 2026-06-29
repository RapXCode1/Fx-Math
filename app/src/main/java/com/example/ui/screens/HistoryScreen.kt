package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CalcHistoryItem
import com.example.data.database.AiSession
import com.example.ui.theme.*
import com.example.ui.viewmodel.MathViewModel

@Composable
fun HistoryScreen(
    viewModel: MathViewModel,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(),
    onNavigateToAiChat: () -> Unit
) {
    val calcHistory by viewModel.calculatorHistory.collectAsState()
    val aiSessions by viewModel.aiSessions.collectAsState()

    var activeSubTab by remember { mutableStateOf("Calculations") }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    AccentGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "History Manager",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Local SQLite Encrypted Databases",
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Clear History Option
                if (activeSubTab == "Calculations" && calcHistory.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.clearAllCalcHistory()
                            Toast.makeText(context, "All calculations cleared!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Calculations", tint = AccentPink)
                    }
                }
            }

            // SEARCH BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardGlassDark, RoundedCornerShape(20.dp))
                    .border(1.dp, GlassBorderDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(18.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search logs or models...", color = Color.Gray, fontSize = 13.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_search_field")
                )
            }

            Spacer(Modifier.height(12.dp))

            // SUB-TAB CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardGlassDark, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                val tabs = listOf("Calculations", "AI Chats")
                tabs.forEach { tabName ->
                    val isSelected = activeSubTab == tabName
                    val tabBg = if (isSelected) AccentPurple else Color.Transparent
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tabBg)
                            .clickable { activeSubTab = tabName }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabName,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // SEARCH FILTERS LOGIC
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeSubTab == "Calculations") {
                    val filteredCalcs = calcHistory.filter {
                        it.expression.contains(searchQuery, ignoreCase = true) ||
                                it.result.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredCalcs.isEmpty()) {
                        EmptyHistoryPlaceholder(msg = "No math operations found!")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredCalcs) { item ->
                                CalcHistoryCard(
                                    item = item,
                                    onFavoriteToggle = { viewModel.toggleFavoriteCalc(item) },
                                    onDelete = { viewModel.deleteCalcItem(item.id) }
                                )
                            }
                        }
                    }
                } else {
                    val filteredSessions = aiSessions.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.selectedModel.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredSessions.isEmpty()) {
                        EmptyHistoryPlaceholder(msg = "No AI sessions found!")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredSessions) { session ->
                                AiSessionCard(
                                    session = session,
                                    onSelect = {
                                        viewModel.selectSession(session.id)
                                        viewModel.setModelBranding(session.selectedModel)
                                        onNavigateToAiChat()
                                    },
                                    onPinToggle = { viewModel.togglePinSession(session) },
                                    onDelete = { viewModel.deleteSession(session.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryPlaceholder(msg: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CloudQueue, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(msg, color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
fun CalcHistoryCard(
    item: CalcHistoryItem,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGlassDark),
        border = BorderStroke(1.dp, GlassBorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.expression,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "= ${item.result}",
                    color = AccentBlue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Favorite Button
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (item.isFavorite) AccentPink else Color.Gray
                    )
                }

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete entry", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AiSessionCard(
    session: AiSession,
    onSelect: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = CardGlassDark),
        border = BorderStroke(1.dp, if (session.isPinned) AccentPink else GlassBorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (session.isPinned) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = AccentPink, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = session.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Model: ${session.selectedModel}",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Pin toggle button
                IconButton(onClick = onPinToggle) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin session",
                        tint = if (session.isPinned) AccentPink else Color.Gray
                    )
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete session", tint = Color.Gray)
                }
            }
        }
    }
}
