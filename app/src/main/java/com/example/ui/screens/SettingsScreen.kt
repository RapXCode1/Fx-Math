package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MathViewModel

@Composable
fun SettingsScreen(
    viewModel: MathViewModel,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues()
) {
    val isDark by viewModel.isDarkTheme.collectAsState()
    val precision by viewModel.decimalsPrecision.collectAsState()
    val isHaptic by viewModel.isHapticEnabled.collectAsState()
    val groqKey by viewModel.groqApiKey.collectAsState()

    var tempApiKey by remember { mutableStateOf(groqKey) }
    val context = LocalContext.current

    AccentGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // HEADER
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Platform Settings",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure Preferences & API Credentials",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // GENERAL PREFERENCES SECTION
            Text(
                text = "Preferences",
                color = AccentPink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardGlassDark),
                border = BorderStroke(1.dp, GlassBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Dark theme toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.DarkMode, contentDescription = "Dark Theme", tint = Color.White)
                            Text("Deep Space Dark Theme", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.setDarkTheme(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentPink, checkedTrackColor = AccentPurple)
                        )
                    }

                    Divider(color = GlassBorderDark)

                    // Haptic feedback toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Vibration, contentDescription = "Haptic Vibration", tint = Color.White)
                            Text("Tactile Click Haptics", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = isHaptic,
                            onCheckedChange = { viewModel.setHapticEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentPink, checkedTrackColor = AccentPurple)
                        )
                    }

                    Divider(color = GlassBorderDark)

                    // Decimals Precision Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Grid3x3, contentDescription = "Precision decimals", tint = Color.White)
                                Text("Decimal Calculations Precision", color = Color.White, fontSize = 14.sp)
                            }
                            Text("$precision places", color = AccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = precision.toFloat(),
                            onValueChange = { viewModel.setPrecision(it.toInt()) },
                            valueRange = 2f..8f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentPurple)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SECURE API CONFIGURATION
            Text(
                text = "Credentials & API Security",
                color = AccentPink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardGlassDark),
                border = BorderStroke(1.dp, GlassBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Custom Groq API Key Integration (Optional)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Leave empty to use the built-in high-performance default Groq key configured by the developer. Entering a valid custom Groq API key allows you to use your personal limits.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    TextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        placeholder = { Text("Enter your gsk_... key", color = Color.Gray, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedIndicatorColor = AccentBlue,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("groq_key_field")
                    )

                    Button(
                        onClick = {
                            viewModel.setGroqApiKey(tempApiKey)
                            Toast.makeText(context, "API Configuration updated securely!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonColors(AccentPurple, Color.White, Color.Transparent, Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_settings_btn")
                    ) {
                        Text("Save Credentials Locally", fontWeight = FontWeight.Bold)
                    }

                    // SECURITY WARNING MANDATE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentPink.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentPink.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Security, contentDescription = "Security Alert", tint = AccentPink, modifier = Modifier.size(16.dp))
                                Text("Security Warning", color = AccentPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // BRAND BIOGRAPHY INFO
            Text(
                text = "Platform Specifications",
                color = AccentPink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardGlassDark),
                border = BorderStroke(1.dp, GlassBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Fx-Math Smart Platform",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Versi: 1.0.0-Release\nDeveloper Tunggal: RapXCode (Solo Developer)\nTanggal Diciptakan: Juni 2026\nMesin Inti: Kotlin Symbolic Math Engine & Multimodal Generative AI.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Fx-Math AI dirancang dan dibangun sepenuhnya dari nol oleh RapXCode secara mandiri sebagai platform pembelajaran interaktif guna mempermudah kalkulus, aljabar linear, fisika, serta grafik fungsi 2D/3D lengkap dengan pemecah rumus OCR tulisan tangan.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Model AI Terintegrasi (Powered by Groq):",
                        color = AccentPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "• FYY-Llama 3.3 (llama-3.3-70b-versatile) — Model 70B ultra-cerdas untuk pemecahan masalah & penalaran kalkulus kompleks.\n" +
                               "• FYY-Llama Scout (llama-3.1-8b-instant) — Model ringkas yang sangat responsif untuk asisten cepat.\n" +
                               "• FYY-GPT OSS 120B (llama-3.3-70b-versatile) — Model berkapasitas besar untuk pembuktian teorema matematis.\n" +
                               "• FYY-Qwen 3 32B (qwen-2.5-coder-32b) — Model spesialis pemrograman & logika matematika numerik presisi tinggi.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
