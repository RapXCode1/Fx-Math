package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MathViewModel
import com.example.ui.theme.*

@Composable
fun CalculatorScreen(
    viewModel: MathViewModel,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(),
    onShowHistorySnippet: () -> Unit
) {
    val formula by viewModel.calcFormula.collectAsState()
    val result by viewModel.calcResult.collectAsState()
    val calculatorMode by viewModel.calculatorMode.collectAsState() // Standard, Scientific
    val isRadians by viewModel.isRadians.collectAsState()
    val context = LocalContext.current

    val standardButtons = listOf(
        "C", "MR", "%", "÷",
        "7", "8", "9", "×",
        "4", "5", "6", "−",
        "1", "2", "3", "+",
        "0", ".", "⌫", "="
    )

    val scientificButtons = listOf(
        "sin", "cos", "tan", "sqrt",
        "asin", "acos", "atan", "x²",
        "sinh", "cosh", "tanh", "x^y",
        "ln", "log", "π", "e",
        "abs", "n!", "(", ")"
    )

    AccentGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER BAR WITH MODE TABS
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FX-MATH PLATFORM",
                            color = AccentIndigoLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "by RapXCode",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Angle Mode for Scientific
                        if (calculatorMode == "Scientific") {
                            TextButton(
                                onClick = { viewModel.onCalculatorButtonPressed("Rad/Deg") },
                                colors = ButtonDefaults.textButtonColors(contentColor = AccentIndigoLight),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isRadians) "RAD" else "DEG",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // History snippet icon
                        IconButton(
                            onClick = onShowHistorySnippet,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Outlined.History, contentDescription = "History", tint = Color.White)
                        }

                        // Clipboard Copy
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Calculation Result", result)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied result!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy Result",
                                tint = Color.White
                            )
                        }
                    }
                }

                // SEGMENTED TAB MODE SELECTOR (Standard, Scientific)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardGlassDark)
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    listOf("Standard", "Scientific").forEach { mode ->
                        val isActive = calculatorMode == mode
                        val tabColor = if (mode == "Standard") AccentIndigo else AccentPurple
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) tabColor else Color.Transparent)
                                .clickable { viewModel.setCalculatorMode(mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // RESULTS & INPUT PANEL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                // Formula display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState(), reverseScrolling = true)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = formula.ifEmpty { "Enter expression..." },
                        fontSize = 24.sp,
                        color = if (formula.isEmpty()) Color.Gray else Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }

                // Main Result display
                Text(
                    text = result,
                    fontSize = if (result.length > 12) 36.sp else 48.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_result_text")
                )

            }

            // BUTTON PAD GRID AREA
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SCIENTIFIC GRID MODE
                if (calculatorMode == "Scientific") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardGlassDark, RoundedCornerShape(16.dp))
                            .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.height(136.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(scientificButtons) { label ->
                                val bgColor = when (label) {
                                    "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh" -> CalcFnBg
                                    "π", "e" -> CalcSpecialBg.copy(alpha = 0.2f)
                                    else -> CalcNumBg
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgColor)
                                        .clickable { viewModel.onCalculatorButtonPressed(label) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (label == "π" || label == "e") AccentBlue else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // RENDERING FOR STANDARD KEYPAD
                val currentKeypadList = standardButtons
                val rowHeight = 290.dp
                val gridCols = 4

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridCols),
                    modifier = Modifier.height(rowHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentKeypadList) { label ->
                        val isOperator = label in listOf("÷", "×", "−", "+", "=")
                        val isSpecial = label in listOf("C", "MR", "%", "⌫")

                        val backgroundBrush = when {
                            isOperator -> SolidColor(CalcOpBg) // Apple Orange
                            isSpecial -> SolidColor(CalcSpecialBg) // Light Translucent Glass Gray
                            else -> SolidColor(CalcNumBg) // Dark Translucent Glass Gray (shows background)
                        }

                        val textColor = Color.White

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(backgroundBrush)
                                .then(
                                    if (isOperator) {
                                        // Slight white reflection on orange keys
                                        Modifier.border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    } else {
                                        // Subtle edge refraction border for premium liquid glass feel
                                        Modifier.border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                    }
                                )
                                .clickable { viewModel.onCalculatorButtonPressed(label) }
                                .padding(vertical = 13.dp)
                                .testTag("btn_$label"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontSize = if (label.length > 2) 13.sp else 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
