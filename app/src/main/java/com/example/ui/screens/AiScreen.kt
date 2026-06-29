package com.example.ui.screens

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AiMessageItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MathViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiScreen(
    viewModel: MathViewModel,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues()
) {
    val sessions by viewModel.aiSessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val selectedModel by viewModel.selectedModelBranding.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()

    // Tutor and Voice states from ViewModel
    val aiTutorLevel by viewModel.aiTutorLevel.collectAsState()
    val isVoiceListening by viewModel.isVoiceListening.collectAsState()
    val voiceWaveform by viewModel.voiceWaveform.collectAsState()
    val voiceStatus by viewModel.voiceStatus.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isModelMenuExpanded by remember { mutableStateOf(false) }
    var isTutorMenuExpanded by remember { mutableStateOf(false) }

    val modelsList = listOf(
        Triple("FYY-Llama 3.3", AccentIndigo, "General conversation & conceptual math explanation"),
        Triple("FYY-Llama Scout", AccentPurple, "Long context analysis & detailed research"),
        Triple("FYY-GPT OSS 120B", AccentBlue, "Complex reasoning, logical steps & proofing"),
        Triple("FYY-Qwen 3 32B", AccentPink, "Technical formulas, advanced coding & multilingual")
    )

    val quickPrompts = listOf(
        "Selesaikan integral: ∫(3x² + 2x) dx dari x=0 sampai x=2",
        "Jelaskan rumus kuadratik dan cara pembuktiannya",
        "Buat kode Python untuk merender grafik sinus",
        "Mengapa pembagian dengan nol menghasilkan nilai tak terdefinisi?"
    )

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Infinite breathing scale for mic icon pulse
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val isKeyboardVisible = WindowInsets.isImeVisible

    AccentGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // BRAND HEADER & CONTROLLERS (Only show full size when keyboard is NOT open)
            if (!isKeyboardVisible) {
                // BRAND HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fx-Math AI Core",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RapXCode Multi-Agent Engine",
                            color = AccentIndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // New Session Button
                    TextButton(
                        onClick = { viewModel.createNewSession(selectedModel) },
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentIndigoLight)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Session", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Chat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // 1. ENGINE CONTROL CENTER ROW (Saves space, ultra premium design, selected-popup Concept)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LEFT DROPDOWN: AI ENGINE CHATTER
                    Box(
                        modifier = Modifier
                            .weight(1.2f) // slightly more space for the engine name
                    ) {
                        val activeModelInfo = modelsList.find { it.first == selectedModel } ?: modelsList[0]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardGlassDark)
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable { isModelMenuExpanded = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(activeModelInfo.second, CircleShape)
                                )
                                Text(
                                    text = activeModelInfo.first,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select AI Model",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // The Premium Pop-up Menu
                        DropdownMenu(
                            expanded = isModelMenuExpanded,
                            onDismissRequest = { isModelMenuExpanded = false },
                            modifier = Modifier
                                .width(280.dp)
                                .background(Color(0xFF1C1C1E).copy(alpha = 0.95f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "SELECT MATH ENGINE Core",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentIndigoLight,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            modelsList.forEach { (modelName, color, desc) ->
                                val isSelected = selectedModel == modelName
                                DropdownMenuItem(
                                    text = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(color, CircleShape)
                                                )
                                                Text(
                                                    text = modelName,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = color,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = desc,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setModelBranding(modelName)
                                        isModelMenuExpanded = false
                                    },
                                    modifier = Modifier.background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                )
                            }
                        }
                    }

                    // RIGHT DROPDOWN: AI TUTOR LEVEL
                    Box(
                        modifier = Modifier
                            .weight(0.8f) // slightly less space for level
                    ) {
                        val levels = listOf(
                            "SD" to "🎒 SD Level",
                            "SMP" to "🏫 SMP Level",
                            "SMA" to "🎓 SMA Level",
                            "Kuliah" to "🏛️ College"
                        )
                        val activeLevelInfo = levels.find { it.first.uppercase() == aiTutorLevel.uppercase() } ?: levels[2]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardGlassDark)
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable { isTutorMenuExpanded = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = activeLevelInfo.second,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Tutor Level",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isTutorMenuExpanded,
                            onDismissRequest = { isTutorMenuExpanded = false },
                            modifier = Modifier
                                .width(160.dp)
                                .background(Color(0xFF1C1C1E).copy(alpha = 0.95f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "TUTOR COMPLEXITY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentIndigoLight,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            levels.forEach { (lvl, label) ->
                                val isSelected = aiTutorLevel.uppercase() == lvl.uppercase()
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = AccentPink,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.setTutorLevel(lvl)
                                        isTutorMenuExpanded = false
                                    },
                                    modifier = Modifier.background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            } else {
                // COMPACT RE-STYLED HEADER TO SAVE SPACE WHEN KEYBOARD IS ACTIVE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AccentPink, CircleShape)
                        )
                        Text(
                            text = "Fx-Math AI ($selectedModel)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Tutor: $aiTutorLevel",
                        color = AccentPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            // 3. MAIN MESSAGE HISTORY CHATBOX
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(CardGlassDark, CircleShape)
                                .border(1.dp, GlassBorderDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Intelligent AI",
                                tint = AccentPink,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Fx-Math Smart AI Assistant",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Buka kekuatan super AI matematika. Ajukan soal teori, kode pemrograman, rumus rumit, atau pilih rekomendasi di bawah ini:",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickPrompts) { prompt ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { inputText = prompt },
                                    colors = CardDefaults.cardColors(containerColor = CardGlassDark),
                                    border = BorderStroke(1.dp, GlassBorderDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = prompt,
                                            color = AccentBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages) { message ->
                            ChatBubble(message = message, context = context)
                        }
                    }
                }
            }

            // 4. ANIMATED VOICE WAVEFORM PANEL (Voice Math Assistant HUD)
            AnimatedVisibility(
                visible = isVoiceListening,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(CardGlassDark, RoundedCornerShape(16.dp))
                        .border(1.dp, AccentPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(AccentPink, CircleShape)
                        )
                        Text(
                            text = "Voice Math Assistant",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Pulse waveform rendering
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        val canvasWidth = this.size.width
                        val canvasHeight = this.size.height
                        val barCount = voiceWaveform.size
                        val spacing = 8f
                        val totalSpacing = spacing * (barCount - 1)
                        val barWidth = (canvasWidth - totalSpacing) / barCount

                        voiceWaveform.forEachIndexed { idx, heightFactor ->
                            val barHeight = canvasHeight * heightFactor
                            val x = idx * (barWidth + spacing)
                            val y = (canvasHeight - barHeight) / 2f

                            drawRoundRect(
                                color = AccentPink,
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = voiceStatus,
                        color = AccentIndigoLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Button(
                        onClick = { viewModel.stopVoiceListening() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPink)
                    ) {
                        Text("Selesai Berbicara", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // 5. INPUT ROW & MICROPHONE INTEGRATION
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Generative Loader
                AnimatedVisibility(
                    visible = isAiGenerating && !isVoiceListening,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AccentPink
                            )
                            Text(
                                text = "Fx-Math AI sedang merumuskan solusi...",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = { viewModel.stopAiGeneration() },
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentIndigoLight)
                        ) {
                            Icon(Icons.Default.StopCircle, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Batal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Input box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardGlassDark, RoundedCornerShape(24.dp))
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Mic Toggle
                    IconButton(
                        onClick = {
                            if (isVoiceListening) {
                                viewModel.stopVoiceListening()
                            } else {
                                viewModel.startVoiceListening()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .then(
                                if (isVoiceListening) {
                                    Modifier.scale(pulseScale)
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                if (isVoiceListening) AccentPink else Color.Black.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Assistant",
                            tint = if (isVoiceListening) Color.White else AccentPink,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ketik soal matematika, fisika...", color = Color.Gray, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_input_text_field")
                    )

                    IconButton(
                        onClick = {
                            if (inputText.trim().isNotEmpty()) {
                                viewModel.sendAiMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(AccentIndigo, AccentPurple)),
                                CircleShape
                            )
                            .testTag("ai_send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: AiMessageItem, context: Context) {
    val isUser = message.sender == "user"

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleBrush = if (isUser) {
        Brush.horizontalGradient(listOf(AccentIndigo, AccentPurple))
    } else {
        Brush.horizontalGradient(listOf(CardGlassDark, CardGlassDark))
    }
    val bubbleBorder = if (isUser) Color.Transparent else GlassBorderDark
    val textAlignment = if (isUser) TextAlign.End else TextAlign.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleBrush)
                .border(
                    1.dp,
                    bubbleBorder,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                val processedText = message.text
                val isFormula = processedText.startsWith("$$") || processedText.contains("=") && processedText.length < 50

                if (isFormula) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = processedText.replace("$$", ""),
                            color = AccentBlue,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = textAlignment
                        )
                    }
                } else {
                    Text(
                        text = processedText,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = textAlignment
                    )
                }

                if (!isUser) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AI Explanation", message.text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied explanation!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = Color.Gray.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = if (isUser) "Me" else "Fx-Math AI Assistant",
            color = Color.Gray,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}
