package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MathViewModel
import com.example.ui.viewmodel.Stroke
import com.example.ui.theme.*

@Composable
fun ScannerScreen(
    viewModel: MathViewModel,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues()
) {
    val ocrStatus by viewModel.ocrStatus.collectAsState()
    val solutionSteps by viewModel.ocrSolutionSteps.collectAsState()
    val isGenerating by viewModel.isAiGenerating.collectAsState()
    val drawingStrokes = viewModel.drawingStrokes
    val context = LocalContext.current

    // Local brush customization
    var strokeColor by remember { mutableStateOf(Color.White) }
    var strokeWidth by remember { mutableStateOf(8f) }

    // Gesture temp point storage
    val tempPoints = remember { mutableStateListOf<Offset>() }

    // Preloaded equations to simulate scanner photo
    val presetEquations = listOf(
        "∫(3x² + 2x) dx" to "Calculus Integral",
        "sin²(θ) + cos²(θ) = 1" to "Trig Identity",
        "dy/dx = 4x³ - 5" to "Diff Equation",
        "lim(x→0) sin(x)/x" to "Calculus Limit"
    )

    // Function to draw strokes onto a programmatically created Bitmap
    fun exportCanvasToBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        // Fill background with black for high contrast OCR
        canvas.drawColor(android.graphics.Color.BLACK)

        // Draw saved strokes
        drawingStrokes.forEach { stroke ->
            paint.color = stroke.color.value.toInt()
            paint.strokeWidth = stroke.width
            if (stroke.points.size > 1) {
                for (i in 0 until stroke.points.size - 1) {
                    val p1 = stroke.points[i]
                    val p2 = stroke.points[i + 1]
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                }
            }
        }
        return bitmap
    }

    AccentGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // SCREEN HEADER
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "OCR Math Scanner",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Handwritten Sketchpad & Lens Solver",
                    color = AccentIndigoLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // CANVAS BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepSpaceBlack)
                    .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                tempPoints.clear()
                                tempPoints.add(offset)
                            },
                            onDrag = { change, _ ->
                                tempPoints.add(change.position)
                            },
                            onDragEnd = {
                                if (tempPoints.isNotEmpty()) {
                                    drawingStrokes.add(Stroke(tempPoints.toList(), strokeColor, strokeWidth))
                                    tempPoints.clear()
                                }
                            }
                        )
                    }
                    .testTag("drawing_canvas_pad")
            ) {
                // Drawing rendering
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    // Draw historical strokes
                    drawingStrokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            for (i in 0 until stroke.points.size - 1) {
                                drawLine(
                                    color = stroke.color,
                                    start = stroke.points[i],
                                    end = stroke.points[i + 1],
                                    strokeWidth = stroke.width,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                    // Draw current active stroke
                    if (tempPoints.size > 1) {
                        for (i in 0 until tempPoints.size - 1) {
                            drawLine(
                                color = strokeColor,
                                start = tempPoints[i],
                                end = tempPoints[i + 1],
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Status overlay
                if (drawingStrokes.isEmpty()) {
                    Text(
                        text = "✍️ Write equation here\n(e.g., x² - 4 = 0)",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // CANVAS TOOLBAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brush color pickers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(Color.White, AccentIndigo, AccentPurple, AccentPink)
                    colors.forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (strokeColor == col) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { strokeColor = col }
                        )
                    }
                }

                // Slider & Clear Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear Canvas
                    IconButton(
                        onClick = { viewModel.clearDrawingCanvas() },
                        modifier = Modifier.background(CardGlassDark, CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = AccentIndigoLight)
                    }

                    // Solve Button
                    Button(
                        onClick = {
                            if (drawingStrokes.isEmpty()) {
                                Toast.makeText(context, "Draw an expression first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Compile canvas to bitmap (standard 800x600 size)
                            val bitmap = exportCanvasToBitmap(800, 600)
                            viewModel.runOcrMathSolver(bitmap)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        modifier = Modifier.testTag("solve_canvas_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Solve")
                        Spacer(Modifier.width(6.dp))
                        Text("Solve Drawing", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // DEMO SCAN LIBRARY FOR TESTING
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardGlassDark, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "No stylus? Instant Test Scan library:",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetEquations.forEach { (equationText, title) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardGlassDark)
                                .border(1.dp, GlassBorderDark, RoundedCornerShape(8.dp))
                                .clickable {
                                    // Generate a mock handwritten bitmap containing the equation
                                    val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bitmap)
                                    val paint = Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 48f
                                        textAlign = Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    canvas.drawColor(android.graphics.Color.BLACK)
                                    canvas.drawText(equationText, 400f, 300f, paint)

                                    viewModel.runOcrMathSolver(bitmap)
                                    Toast.makeText(context, "Scanning '$title' preset...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(equationText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(title, color = Color.Gray, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // SOLVER RESPONSES
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isGenerating) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentPink)
                        Spacer(Modifier.height(8.dp))
                        Text(ocrStatus, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else if (solutionSteps.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(ocrStatus, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    // Display Solution steps cards
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Mathematical Derivation:",
                                color = AccentPink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(solutionSteps) { step ->
                            val isFinalAnswer = step.contains("Answer:") || step.contains("x =") || step.contains("root")
                            val cardBrush = if (isFinalAnswer) {
                                Brush.horizontalGradient(listOf(AccentPurple.copy(alpha = 0.2f), AccentPink.copy(alpha = 0.2f)))
                            } else {
                                Brush.horizontalGradient(listOf(CardGlassDark, CardGlassDark))
                            }
                            val borderCol = if (isFinalAnswer) AccentPink else GlassBorderDark

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, borderCol),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBrush)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = step,
                                        color = if (isFinalAnswer) Color.White else DarkOnBackground,
                                        fontSize = 13.sp,
                                        fontWeight = if (isFinalAnswer) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = if (isFinalAnswer) FontFamily.Monospace else FontFamily.SansSerif
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
