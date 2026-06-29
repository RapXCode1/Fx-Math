package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MathViewModel
import com.example.ui.theme.*
import com.example.data.solver.MathEngine
import kotlin.math.*

@Composable
fun GraphScreen(
    viewModel: MathViewModel,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues()
) {
    val graphFunctions by viewModel.graphFunctions.collectAsState()
    val context = LocalContext.current

    var is3DMode by remember { mutableStateOf(false) }

    // 2D Coordinates states
    var zoomScale by remember { mutableStateOf(45f) } // Pixels per unit
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }

    // 2D Tangent calculation
    var selectXForTangent by remember { mutableStateOf(1.0f) }
    var showCalculusOverlays by remember { mutableStateOf(true) }

    // 3D Navigation states
    var azimuth3D by remember { mutableStateOf(45f) } // horizontal angle (yaw)
    var elevation3D by remember { mutableStateOf(30f) } // vertical angle (pitch)
    var zoomScale3D by remember { mutableStateOf(32f) }
    var selected3DType by remember { mutableStateOf("Ripple") } // Ripple, Saddle, Waves, Dome

    var newFuncText by remember { mutableStateOf("") }
    val graphColors = listOf(Color(0xFF0A84FF), Color(0xFFFF9F0A), Color(0xFFFF453A), Color(0xFFE5E5EA))

    // Substitute variable x in formula and evaluate
    fun evaluateFuncAtX(formula: String, x: Double): Double {
        return try {
            val parsedFormula = formula.replace("x", "($x)")
            MathEngine.evaluate(parsedFormula, isRadians = true)
        } catch (e: Exception) {
            Double.NaN
        }
    }

    // Evaluate 3D Height Z = f(X, Y)
    fun calculateZ(type: String, x: Double, y: Double): Double {
        return when (type) {
            "Ripple" -> {
                val r = sqrt(x * x + y * y)
                if (r < 0.1) 2.5 else sin(r) * 2.5 / r
            }
            "Saddle" -> (x * x - y * y) * 0.15
            "Waves" -> cos(x) * sin(y) * 1.5
            "Dome" -> cos(sqrt(x * x + y * y)) * 1.5
            else -> 0.0
        }
    }

    AccentGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // HEADER SECTION WITH SEGMENTED SWITCH
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Math Graph Plotter",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (is3DMode) "Interactive 3D Perspective Mesh" else "Visual 2D Functions & Calculus Tangents",
                        color = AccentIndigoLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2D vs 3D Switch
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardGlassDark)
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!is3DMode) AccentIndigo else Color.Transparent)
                            .clickable { is3DMode = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("2D", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (is3DMode) AccentPink else Color.Transparent)
                            .clickable { is3DMode = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("3D", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (!is3DMode) {
                // --- 2D GRAPH CONTROLS AND EQUATION INPUT ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardGlassDark, RoundedCornerShape(16.dp))
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = newFuncText,
                            onValueChange = { newFuncText = it },
                            placeholder = { Text("y = f(x) (e.g. x^2 - 3)", color = Color.Gray, fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedIndicatorColor = AccentIndigo,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("graph_function_input")
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (newFuncText.trim().isNotEmpty()) {
                                    viewModel.addGraphFunction(newFuncText)
                                    newFuncText = ""
                                } else {
                                    Toast.makeText(context, "Please enter an expression!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .background(AccentIndigo, CircleShape)
                                .testTag("add_graph_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add function", tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Active plotted list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        graphFunctions.forEachIndexed { index, fn ->
                            val col = graphColors[index % graphColors.size]
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardGlassDark)
                                    .border(1.dp, col.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.removeGraphFunction(index) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("y = $fn", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // --- 2D CANVAS RENDERING ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepSpaceBlack)
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(20.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                panOffsetX += dragAmount.x
                                panOffsetY += dragAmount.y
                            }
                        }
                        .testTag("graph_plotter_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val centerX = width / 2f + panOffsetX
                        val centerY = height / 2f + panOffsetY

                        // DRAW GRID
                        val stepGrid = zoomScale
                        var currentGridX = centerX
                        while (currentGridX < width) {
                            drawLine(Color(0x1F808080), Offset(currentGridX, 0f), Offset(currentGridX, height), 1f)
                            currentGridX += stepGrid
                        }
                        currentGridX = centerX - stepGrid
                        while (currentGridX > 0) {
                            drawLine(Color(0x1F808080), Offset(currentGridX, 0f), Offset(currentGridX, height), 1f)
                            currentGridX -= stepGrid
                        }

                        var currentGridY = centerY
                        while (currentGridY < height) {
                            drawLine(Color(0x1F808080), Offset(0f, currentGridY), Offset(width, currentGridY), 1f)
                            currentGridY += stepGrid
                        }
                        currentGridY = centerY - stepGrid
                        while (currentGridY > 0) {
                            drawLine(Color(0x1F808080), Offset(0f, currentGridY), Offset(width, currentGridY), 1f)
                            currentGridY -= stepGrid
                        }

                        // DRAW AXES
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(0f, centerY), Offset(width, centerY), 2f)
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(centerX, 0f), Offset(centerX, height), 2f)

                        // PLOT 2D FUNCTIONS
                        graphFunctions.forEachIndexed { fnIndex, fn ->
                            val fnColor = graphColors[fnIndex % graphColors.size]
                            val path = Path()
                            var firstPoint = true

                            for (screenX in 0..width.toInt() step 3) {
                                val graphX = (screenX - centerX) / zoomScale
                                val graphY = evaluateFuncAtX(fn, graphX.toDouble())

                                if (!graphY.isNaN() && !graphY.isInfinite()) {
                                    val screenY = centerY - (graphY.toFloat() * zoomScale)
                                    if (screenY in 0f..height) {
                                        if (firstPoint) {
                                            path.moveTo(screenX.toFloat(), screenY)
                                            firstPoint = false
                                        } else {
                                            path.lineTo(screenX.toFloat(), screenY)
                                        }
                                    }
                                }
                            }
                            drawPath(path, fnColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = StrokeCap.Round))
                        }

                        // CALCULUS OVERLAYS
                        if (showCalculusOverlays && graphFunctions.isNotEmpty()) {
                            val activeFn = graphFunctions.first()
                            val col = graphColors[0]

                            val graphX = selectXForTangent.toDouble()
                            val graphY = evaluateFuncAtX(activeFn, graphX)

                            if (!graphY.isNaN() && !graphY.isInfinite()) {
                                val h = 0.001
                                val yPlusH = evaluateFuncAtX(activeFn, graphX + h)
                                val derivative = (yPlusH - graphY) / h

                                val ptScreenX = centerX + (graphX.toFloat() * zoomScale)
                                val ptScreenY = centerY - (graphY.toFloat() * zoomScale)

                                drawCircle(col, 8f, Offset(ptScreenX, ptScreenY))

                                val tPath = Path()
                                val minX = graphX - 4.0
                                val maxX = graphX + 4.0

                                val tY1 = derivative * (minX - graphX) + graphY
                                val tY2 = derivative * (maxX - graphX) + graphY

                                val tScreenX1 = centerX + (minX.toFloat() * zoomScale)
                                val tScreenY1 = centerY - (tY1.toFloat() * zoomScale)
                                val tScreenX2 = centerX + (maxX.toFloat() * zoomScale)
                                val tScreenY2 = centerY - (tY2.toFloat() * zoomScale)

                                drawLine(AccentPink, Offset(tScreenX1, tScreenY1), Offset(tScreenX2, tScreenY2), 3f, cap = StrokeCap.Round)
                            }
                        }
                    }

                    // 2D ZOOM CONTROLS OVERLAY
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { zoomScale = (zoomScale * 1.2f).coerceAtMost(200f) },
                            modifier = Modifier.background(CardGlassDark, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                        }

                        IconButton(
                            onClick = { zoomScale = (zoomScale / 1.2f).coerceAtLeast(15f) },
                            modifier = Modifier.background(CardGlassDark, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                panOffsetX = 0f
                                panOffsetY = 0f
                                zoomScale = 45f
                            },
                            modifier = Modifier.background(CardGlassDark, CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Center", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // CALCULUS TANGENT EXPERIMENT SLIDER
                AnimatedVisibility(
                    visible = showCalculusOverlays && graphFunctions.isNotEmpty()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardGlassDark, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        val activeFn = graphFunctions.first()
                        val slope = try {
                            val xVal = selectXForTangent.toDouble()
                            val yVal = evaluateFuncAtX(activeFn, xVal)
                            val h = 0.001
                            val yPlusH = evaluateFuncAtX(activeFn, xVal + h)
                            ((yPlusH - yVal) / h)
                        } catch (e: Exception) {
                            0.0
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Derivative Tangent Slope at x = ${String.format("%.2f", selectXForTangent)}",
                                color = AccentPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "dy/dx ≈ ${String.format("%.4f", slope)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = selectXForTangent,
                            onValueChange = { selectXForTangent = it },
                            valueRange = -4f..4f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentPink,
                                activeTrackColor = AccentPurple
                            )
                        )
                    }
                }
            } else {
                // --- 3D PERSPECTIVE WIREFRAME GRAPH MODE ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardGlassDark, RoundedCornerShape(16.dp))
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Select 3D Function Template:",
                        color = AccentIndigoLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 3D Selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Ripple", "Saddle", "Waves", "Dome").forEach { type ->
                            val isActive = selected3DType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isActive) AccentPink else Color.Black.copy(alpha = 0.2f))
                                    .border(1.dp, if (isActive) AccentPink else GlassBorderDark, RoundedCornerShape(10.dp))
                                    .clickable { selected3DType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    val desc = when (selected3DType) {
                        "Ripple" -> "z = sin(√(x² + y²)) / √(x² + y²)"
                        "Saddle" -> "z = (x² - y²) * 0.15"
                        "Waves" -> "z = cos(x) * sin(y) * 1.5"
                        else -> "z = cos(√(x² + y²)) * 1.5"
                    }
                    Text(
                        text = "Formula: $desc",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // --- 3D CANVAS VIEW ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepSpaceBlack)
                        .border(1.dp, GlassBorderDark, RoundedCornerShape(20.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                azimuth3D = (azimuth3D - dragAmount.x / 4f) % 360f
                                elevation3D = (elevation3D - dragAmount.y / 4f).coerceIn(5f, 85f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val centerX = width / 2f
                        val centerY = height / 2f

                        // Convert orbit angles to radians
                        val azRad = Math.toRadians(azimuth3D.toDouble())
                        val elRad = Math.toRadians(elevation3D.toDouble())

                        val cosAz = cos(azRad)
                        val sinAz = sin(azRad)
                        val cosEl = cos(elRad)
                        val sinEl = sin(elRad)

                        // Coordinate projection function
                        fun project3DTo2D(x: Double, y: Double, z: Double): Offset {
                            // Rotate Z-axis
                            val rx = x * cosAz - y * sinAz
                            val ry = x * sinAz + y * cosAz
                            val rz = z

                            // Rotate X-axis
                            val finalX = rx
                            val finalY = ry * cosEl - rz * sinEl
                            val finalZ = ry * sinEl + rz * cosEl

                            // Project to screen
                            val screenX = centerX + finalX.toFloat() * zoomScale3D
                            val screenY = centerY - finalZ.toFloat() * zoomScale3D // Z height corresponds to screen Y

                            return Offset(screenX, screenY)
                        }

                        // Draw base coordinate square platform
                        val platformPoints = listOf(
                            project3DTo2D(-4.0, -4.0, -2.0),
                            project3DTo2D(4.0, -4.0, -2.0),
                            project3DTo2D(4.0, 4.0, -2.0),
                            project3DTo2D(-4.0, 4.0, -2.0)
                        )
                        val platPath = Path().apply {
                            moveTo(platformPoints[0].x, platformPoints[0].y)
                            lineTo(platformPoints[1].x, platformPoints[1].y)
                            lineTo(platformPoints[2].x, platformPoints[2].y)
                            lineTo(platformPoints[3].x, platformPoints[3].y)
                            close()
                        }
                        drawPath(platPath, Color.White.copy(alpha = 0.05f))
                        drawLine(Color.White.copy(alpha = 0.2f), platformPoints[0], platformPoints[1], 1f)
                        drawLine(Color.White.copy(alpha = 0.2f), platformPoints[1], platformPoints[2], 1f)
                        drawLine(Color.White.copy(alpha = 0.2f), platformPoints[2], platformPoints[3], 1f)
                        drawLine(Color.White.copy(alpha = 0.2f), platformPoints[3], platformPoints[0], 1f)

                        // Render 3D Mesh Grid nodes
                        val step = 0.4
                        val xMin = -4.0
                        val xMax = 4.0
                        val yMin = -4.0
                        val yMax = 4.0

                        // Generate all heights and projected points
                        val gridDimX = ((xMax - xMin) / step).toInt() + 1
                        val gridDimY = ((yMax - yMin) / step).toInt() + 1
                        val pointMatrix = Array(gridDimX) { Array(gridDimY) { Offset.Zero } }
                        val heightMatrix = Array(gridDimX) { DoubleArray(gridDimY) }

                        for (i in 0 until gridDimX) {
                            val x = xMin + i * step
                            for (j in 0 until gridDimY) {
                                val y = yMin + j * step
                                val z = calculateZ(selected3DType, x, y)
                                pointMatrix[i][j] = project3DTo2D(x, y, z)
                                heightMatrix[i][j] = z
                            }
                        }

                        // Draw horizontal lines connecting grids
                        for (i in 0 until gridDimX) {
                            for (j in 0 until gridDimY) {
                                val p1 = pointMatrix[i][j]

                                // Line along X
                                if (i + 1 < gridDimX) {
                                    val p2 = pointMatrix[i + 1][j]
                                    val zVal = (heightMatrix[i][j] + heightMatrix[i + 1][j]) / 2.0
                                    val color = when {
                                        zVal > 1.2 -> Color(0xFFFF453A) // iOS Red
                                        zVal > 0.3 -> Color(0xFFFF9F0A) // iOS Orange
                                        zVal < -0.3 -> Color(0xFF0A84FF) // iOS Blue
                                        else -> Color(0xFFE5E5EA) // iOS Silver/White
                                    }
                                    drawLine(color.copy(alpha = 0.6f), p1, p2, 1.5f)
                                }

                                // Line along Y
                                if (j + 1 < gridDimY) {
                                    val p3 = pointMatrix[i][j + 1]
                                    val zVal = (heightMatrix[i][j] + heightMatrix[i][j + 1]) / 2.0
                                    val color = when {
                                        zVal > 1.2 -> Color(0xFFFF453A) // iOS Red
                                        zVal > 0.3 -> Color(0xFFFF9F0A) // iOS Orange
                                        zVal < -0.3 -> Color(0xFF0A84FF) // iOS Blue
                                        else -> Color(0xFFE5E5EA) // iOS Silver/White
                                    }
                                    drawLine(color.copy(alpha = 0.6f), p1, p3, 1.5f)
                                }
                            }
                        }
                    }

                    // 3D CONTROLS AND ORIENTATION METRICS OVERLAY
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text("Azimuth: ${azimuth3D.toInt()}°", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Elevation: ${elevation3D.toInt()}°", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Drag to orbit/rotate", color = AccentIndigoLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }

                    // 3D Zoom Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { zoomScale3D = (zoomScale3D * 1.15f).coerceAtMost(100f) },
                            modifier = Modifier.background(CardGlassDark, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                        }

                        IconButton(
                            onClick = { zoomScale3D = (zoomScale3D / 1.15f).coerceAtLeast(10f) },
                            modifier = Modifier.background(CardGlassDark, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                azimuth3D = 45f
                                elevation3D = 30f
                                zoomScale3D = 32f
                            },
                            modifier = Modifier.background(CardGlassDark, CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Perspective", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardGlassDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Gunakan gerakan drag jari pada kanvas untuk memutar (orbit) model matematika 3D di atas secara real-time.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}
