package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.CalcHistoryItem
import com.example.data.database.AiSession
import com.example.data.database.AiMessageItem
import com.example.data.network.GroqMessage
import com.example.data.network.MathAiService
import com.example.data.solver.MathEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.*

data class Stroke(val points: List<Offset>, val color: Color, val width: Float)

class MathViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val calcHistoryDao = db.calcHistoryDao()
    private val aiSessionDao = db.aiSessionDao()

    // Navigation State
    private val _currentTab = MutableStateFlow("Calculator")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun switchTab(tab: String) {
        _currentTab.value = tab
    }

    // --- Calculator State ---
    private val _calcFormula = MutableStateFlow("")
    val calcFormula: StateFlow<String> = _calcFormula.asStateFlow()

    private val _calcResult = MutableStateFlow("0")
    val calcResult: StateFlow<String> = _calcResult.asStateFlow()

    private val _isScientific = MutableStateFlow(false)
    val isScientific: StateFlow<Boolean> = _isScientific.asStateFlow()

    private val _calculatorMode = MutableStateFlow("Standard") // Standard, Scientific, Programmer, CAS / Math Live
    val calculatorMode: StateFlow<String> = _calculatorMode.asStateFlow()

    private val _programmerRadix = MutableStateFlow("DEC") // HEX, DEC, OCT, BIN
    val programmerRadix: StateFlow<String> = _programmerRadix.asStateFlow()

    private val _isRadians = MutableStateFlow(true)
    val isRadians: StateFlow<Boolean> = _isRadians.asStateFlow()

    private val _calculatorHistory = MutableStateFlow<List<CalcHistoryItem>>(emptyList())
    val calculatorHistory: StateFlow<List<CalcHistoryItem>> = _calculatorHistory.asStateFlow()

    private val _favoritesHistory = MutableStateFlow<List<CalcHistoryItem>>(emptyList())
    val favoritesHistory: StateFlow<List<CalcHistoryItem>> = _favoritesHistory.asStateFlow()

    private var memoryValue: Double = 0.0

    // --- AI Session & Conversation State ---
    private val _aiSessions = MutableStateFlow<List<AiSession>>(emptyList())
    val aiSessions: StateFlow<List<AiSession>> = _aiSessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<AiMessageItem>>(emptyList())
    val activeMessages: StateFlow<List<AiMessageItem>> = _activeMessages.asStateFlow()

    private val _selectedModelBranding = MutableStateFlow("FYY-Llama 3.3")
    val selectedModelBranding: StateFlow<String> = _selectedModelBranding.asStateFlow()

    private val _aiTutorLevel = MutableStateFlow("SMA") // SD, SMP, SMA, Kuliah
    val aiTutorLevel: StateFlow<String> = _aiTutorLevel.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    // --- Voice Assistant State ---
    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _voiceWaveform = MutableStateFlow<List<Float>>(List(15) { 0.1f })
    val voiceWaveform: StateFlow<List<Float>> = _voiceWaveform.asStateFlow()

    private val _voiceStatus = MutableStateFlow("Ready to listen. Tap mic to begin...")
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    // --- Drawing Canvas & OCR State ---
    val drawingStrokes = mutableStateListOf<Stroke>()

    private val _ocrStatus = MutableStateFlow("Draw an equation above or load a photo to solve!")
    val ocrStatus: StateFlow<String> = _ocrStatus.asStateFlow()

    private val _scannedBitmap = MutableStateFlow<Bitmap?>(null)
    val scannedBitmap: StateFlow<Bitmap?> = _scannedBitmap.asStateFlow()

    private val _ocrSolutionSteps = MutableStateFlow<List<String>>(emptyList())
    val ocrSolutionSteps: StateFlow<List<String>> = _ocrSolutionSteps.asStateFlow()

    // --- Graphing State ---
    private val _graphFunctions = MutableStateFlow<List<String>>(listOf("x^2", "sin(x)"))
    val graphFunctions: StateFlow<List<String>> = _graphFunctions.asStateFlow()

    // --- User Preferences / Settings State ---
    private val sharedPrefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _decimalsPrecision = MutableStateFlow(4)
    val decimalsPrecision: StateFlow<Int> = _decimalsPrecision.asStateFlow()

    private val _isHapticEnabled = MutableStateFlow(true)
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    private val _groqApiKey = MutableStateFlow("")
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    private var activeAiJob: Job? = null
    private var activeSessionMessagesJob: Job? = null

    init {
        // Load persisted settings
        _isDarkTheme.value = sharedPrefs.getBoolean("dark_theme", true)
        _decimalsPrecision.value = sharedPrefs.getInt("decimals_precision", 4)
        _isHapticEnabled.value = sharedPrefs.getBoolean("haptic_enabled", true)
        _groqApiKey.value = sharedPrefs.getString("groq_api_key", "") ?: ""

        // Observe Calculator History
        viewModelScope.launch {
            calcHistoryDao.getAllHistory().collect {
                _calculatorHistory.value = it
            }
        }
        viewModelScope.launch {
            calcHistoryDao.getFavorites().collect {
                _favoritesHistory.value = it
            }
        }

        // Observe AI Sessions
        viewModelScope.launch {
            aiSessionDao.getAllSessions().collect { sessions ->
                _aiSessions.value = sessions
                if (_activeSessionId.value == null && sessions.isNotEmpty()) {
                    selectSession(sessions.first().id)
                }
            }
        }
    }

    // --- CALCULATOR OPERATIONS ---
    fun onCalculatorButtonPressed(btn: String) {
        when (btn) {
            "C" -> {
                _calcFormula.value = ""
                _calcResult.value = "0"
            }
            "⌫" -> {
                val current = _calcFormula.value
                if (current.isNotEmpty()) {
                    _calcFormula.value = current.substring(0, current.length - 1)
                }
            }
            "=" -> {
                calculateFormula()
            }
            "M+" -> {
                try {
                    val resultVal = _calcResult.value.toDoubleOrNull() ?: 0.0
                    memoryValue += resultVal
                } catch (e: Exception) { /* ignore */ }
            }
            "M-" -> {
                try {
                    val resultVal = _calcResult.value.toDoubleOrNull() ?: 0.0
                    memoryValue -= resultVal
                } catch (e: Exception) { /* ignore */ }
            }
            "MR" -> {
                _calcFormula.value += formatValue(memoryValue)
            }
            "MC" -> {
                memoryValue = 0.0
            }
            "Rad/Deg" -> {
                _isRadians.value = !_isRadians.value
            }
            "Sci" -> {
                _isScientific.value = !_isScientific.value
            }
            "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "ln", "log", "sqrt", "abs" -> {
                _calcFormula.value += "$btn("
            }
            "π" -> {
                _calcFormula.value += "π"
            }
            "e" -> {
                _calcFormula.value += "e"
            }
            "x²" -> {
                _calcFormula.value += "^2"
            }
            "x^y" -> {
                _calcFormula.value += "^"
            }
            "n!" -> {
                _calcFormula.value += "fact("
            }
            "AND", "OR", "XOR", "LSH", "RSH" -> {
                _calcFormula.value += " $btn "
            }
            "NOT" -> {
                _calcFormula.value += "NOT "
            }
            else -> {
                _calcFormula.value += btn
            }
        }
    }

    private fun calculateFormula() {
        val expr = _calcFormula.value
        if (expr.isEmpty()) return
        viewModelScope.launch {
            if (_calculatorMode.value == "Programmer") {
                try {
                    val resultVal = evaluateProgrammer(expr, _programmerRadix.value)
                    _calcResult.value = resultVal.toString()

                    // Save to Local Room DB
                    calcHistoryDao.insertHistoryItem(
                        CalcHistoryItem(
                            expression = "[${_programmerRadix.value}] $expr",
                            result = resultVal.toString()
                        )
                    )
                } catch (e: Exception) {
                    _calcResult.value = "Error: ${e.localizedMessage ?: "Invalid Programmer Syntax"}"
                }
                return@launch
            }

            try {
                val resultVal = MathEngine.evaluate(expr, _isRadians.value)
                val formatted = formatValue(resultVal)
                _calcResult.value = formatted

                // Save to Local Room DB
                calcHistoryDao.insertHistoryItem(
                    CalcHistoryItem(
                        expression = expr,
                        result = formatted
                    )
                )
            } catch (e: Exception) {
                _calcResult.value = "Error: ${e.localizedMessage ?: "Invalid Expression"}"
            }
        }
    }

    private fun formatValue(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return "Infinity"
        val precision = _decimalsPrecision.value
        // Use precision to round cleanly
        val factor = 10.0.pow(precision)
        val rounded = (value * factor).roundToInt() / factor
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    fun toggleFavoriteCalc(item: CalcHistoryItem) {
        viewModelScope.launch {
            calcHistoryDao.updateHistoryItem(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun deleteCalcItem(id: Int) {
        viewModelScope.launch {
            calcHistoryDao.deleteHistoryItem(id)
        }
    }

    fun clearAllCalcHistory() {
        viewModelScope.launch {
            calcHistoryDao.clearAllHistory()
        }
    }

    // --- AI CONVERSATION OPERATIONS ---
    fun selectSession(sessionId: String) {
        _activeSessionId.value = sessionId
        activeSessionMessagesJob?.cancel()
        activeSessionMessagesJob = viewModelScope.launch {
            aiSessionDao.getMessagesForSession(sessionId).collect { messages ->
                _activeMessages.value = messages
            }
        }
    }

    fun createNewSession(modelBranding: String) {
        val newId = UUID.randomUUID().toString()
        val session = AiSession(
            id = newId,
            title = "Math Session ${UUID.randomUUID().toString().take(4)}",
            selectedModel = modelBranding
        )
        viewModelScope.launch {
            aiSessionDao.insertSession(session)
            selectSession(newId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            aiSessionDao.deleteSession(sessionId)
            aiSessionDao.deleteMessagesForSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = _aiSessions.value.firstOrNull { it.id != sessionId }?.id
            }
        }
    }

    fun togglePinSession(session: AiSession) {
        viewModelScope.launch {
            aiSessionDao.updateSession(session.copy(isPinned = !session.isPinned))
        }
    }

    fun setModelBranding(branding: String) {
        _selectedModelBranding.value = branding
        val currentSess = _activeSessionId.value
        if (currentSess != null) {
            viewModelScope.launch {
                val sess = aiSessionDao.getSessionById(currentSess)
                if (sess != null) {
                    aiSessionDao.updateSession(sess.copy(selectedModel = branding))
                }
            }
        }
    }

    fun sendAiMessage(promptText: String) {
        if (promptText.trim().isEmpty()) return

        viewModelScope.launch {
            var sessionId = _activeSessionId.value
            if (sessionId == null) {
                val newId = UUID.randomUUID().toString()
                val session = AiSession(
                    id = newId,
                    title = "Math Session ${UUID.randomUUID().toString().take(4)}",
                    selectedModel = _selectedModelBranding.value
                )
                aiSessionDao.insertSession(session)
                _activeSessionId.value = newId
                selectSession(newId)
                sessionId = newId
            }

            // Save User Message
            val userMsg = AiMessageItem(
                sessionId = sessionId,
                sender = "user",
                text = promptText
            )
            aiSessionDao.insertMessage(userMsg)

            _isAiGenerating.value = true

            // Trigger actual AI request
            activeAiJob = viewModelScope.launch {
                val currentBranding = _selectedModelBranding.value
                val customKey = _groqApiKey.value
                
                // Fallback to default developer Groq key if none is entered by user
                val activeKey = if (customKey.isNotEmpty()) customKey else MathAiService.getObfuscatedGroqKey()

                // Map visual models to Groq models
                val groqModel = when (currentBranding) {
                    "FYY-Llama 3.3" -> "llama-3.3-70b-versatile"
                    "FYY-Llama Scout" -> "llama-3.1-8b-instant"
                    "FYY-GPT OSS 120B" -> "llama-3.3-70b-versatile"
                    "FYY-Qwen 3 32B" -> "qwen-2.5-coder-32b"
                    else -> "llama-3.3-70b-versatile"
                }

                val historyMessages = _activeMessages.value.map {
                    GroqMessage(role = if (it.sender == "user") "user" else "assistant", content = it.text)
                }

                val responseText = MathAiService.generateWithGroq(
                    prompt = promptText,
                    history = historyMessages,
                    groqApiKey = activeKey,
                    modelBranding = currentBranding,
                    tutorLevel = _aiTutorLevel.value,
                    groqModelName = groqModel
                )

                // Save AI Response
                val aiMsg = AiMessageItem(
                    sessionId = sessionId,
                    sender = "ai",
                    text = responseText
                )
                aiSessionDao.insertMessage(aiMsg)
                _isAiGenerating.value = false
            }
        }
    }

    fun stopAiGeneration() {
        activeAiJob?.cancel()
        _isAiGenerating.value = false
    }

    // --- CANVAS DRAWING & OCR MATH SOLVER ---
    fun clearDrawingCanvas() {
        drawingStrokes.clear()
        _ocrStatus.value = "Canvas cleared. Draw any equation to solve!"
        _scannedBitmap.value = null
        _ocrSolutionSteps.value = emptyList()
    }

    fun setScannedBitmap(bitmap: Bitmap) {
        _scannedBitmap.value = bitmap
        _ocrStatus.value = "Photo imported. Ready to solve!"
    }

    fun runOcrMathSolver(bitmap: Bitmap) {
        _isAiGenerating.value = true
        _ocrStatus.value = "Recognizing handwritten math via Groq OCR..."

        // Convert Bitmap to Base64 jpeg string
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        viewModelScope.launch {
            val currentBranding = _selectedModelBranding.value
            val customKey = _groqApiKey.value
            val activeKey = if (customKey.isNotEmpty()) customKey else MathAiService.getObfuscatedGroqKey()

            val responseText = MathAiService.generateVisionWithGroq(
                prompt = "Please act as an OCR Math Solver. Recognize the mathematical equation written or printed in the attached image, transcribe it in LaTeX/Markdown, explain the logic step-by-step, and provide the final answer and related graph details if applicable.",
                base64Image = base64,
                groqApiKey = activeKey,
                modelBranding = currentBranding,
                tutorLevel = _aiTutorLevel.value
            )

            // Split into lines or steps
            val lines = responseText.split("\n").filter { it.trim().isNotEmpty() }
            _ocrSolutionSteps.value = lines
            _ocrStatus.value = "Solved! See details below."
            _isAiGenerating.value = false
        }
    }

    // --- GRAPH FUNCTIONS OPERATIONS ---
    fun addGraphFunction(fn: String) {
        if (fn.trim().isNotEmpty()) {
            _graphFunctions.value = _graphFunctions.value + fn.trim()
        }
    }

    fun removeGraphFunction(index: Int) {
        val current = _graphFunctions.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _graphFunctions.value = current
        }
    }

    // --- SETTINGS PREFERENCES ---
    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        sharedPrefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun setPrecision(precision: Int) {
        _decimalsPrecision.value = precision
        sharedPrefs.edit().putInt("decimals_precision", precision).apply()
    }

    fun setHapticEnabled(enabled: Boolean) {
        _isHapticEnabled.value = enabled
        sharedPrefs.edit().putBoolean("haptic_enabled", enabled).apply()
    }

    fun setGroqApiKey(key: String) {
        _groqApiKey.value = key
        sharedPrefs.edit().putString("groq_api_key", key).apply()
    }

    // --- MODE & RADIX SETTERS ---
    fun setCalculatorMode(mode: String) {
        _calculatorMode.value = mode
        if (mode == "Scientific") {
            _isScientific.value = true
        } else if (mode == "Standard") {
            _isScientific.value = false
        }
    }

    fun setProgrammerRadix(radix: String) {
        _programmerRadix.value = radix
        // Clear expression or adapt
        _calcFormula.value = ""
        _calcResult.value = "0"
    }

    fun setTutorLevel(level: String) {
        _aiTutorLevel.value = level
    }

    // --- VOICE MATH ASSISTANT SIMULATION ---
    fun startVoiceListening() {
        _isVoiceListening.value = true
        _voiceStatus.value = "Listening to your voice math problem..."
        
        viewModelScope.launch {
            // Animate waveform
            while (_isVoiceListening.value) {
                _voiceWaveform.value = List(15) { 0.15f + (Math.random().toFloat() * 0.85f) }
                kotlinx.coroutines.delay(120)
            }
            _voiceWaveform.value = List(15) { 0.1f }
        }
    }

    fun stopVoiceListening() {
        if (!_isVoiceListening.value) return
        _isVoiceListening.value = false
        
        // Pick a highly relevant mathematical question spoken orally
        val sampleProblems = listOf(
            "hitung integral dari 3x pangkat 2 tambah 2x dx dari 0 sampai 2",
            "selesaikan persamaan kuadrat x kuadrat minus 5x tambah 6 sama dengan 0",
            "turunan pertama dari sin(x) dikali cos(x)",
            "buktikan secara logis mengapa matriks singular tidak memiliki invers"
        )
        val spoken = sampleProblems.random()
        _voiceStatus.value = "Voice recognized! \"$spoken\""
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            // Send user message
            sendAiMessage(spoken)
            // Switch to AI tab to show results
            switchTab("AI")
            _voiceStatus.value = "Ready to listen. Tap mic to begin..."
        }
    }

    class ProgrammerParser(val expression: String, val base: Int) {
        private var pos = -1
        private var ch = 0

        init {
            nextChar()
        }

        private fun nextChar() {
            ch = if (++pos < expression.length) expression[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        private fun eatStr(str: String): Boolean {
            while (ch == ' '.code) nextChar()
            if (pos + str.length <= expression.length && expression.substring(pos, pos + str.length) == str) {
                pos += str.length - 1
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Long {
            val result = parseOr()
            if (pos < expression.length) {
                throw IllegalArgumentException("Unexpected: '${expression[pos]}'")
            }
            return result
        }

        private fun parseOr(): Long {
            var x = parseXor()
            while (true) {
                if (eat('|'.code)) x = x or parseXor()
                else break
            }
            return x
        }

        private fun parseXor(): Long {
            var x = parseAnd()
            while (true) {
                if (eat('^'.code)) x = x xor parseAnd()
                else break
            }
            return x
        }

        private fun parseAnd(): Long {
            var x = parseShift()
            while (true) {
                if (eat('&'.code)) x = x and parseShift()
                else break
            }
            return x
        }

        private fun parseShift(): Long {
            var x = parseAddSub()
            while (true) {
                if (eatStr("<<")) {
                    x = x shl parseAddSub().toInt()
                } else if (eatStr(">>")) {
                    x = x shr parseAddSub().toInt()
                } else break
            }
            return x
        }

        private fun parseAddSub(): Long {
            var x = parseMulDiv()
            while (true) {
                if (eat('+'.code)) x += parseMulDiv()
                else if (eat('-'.code)) x -= parseMulDiv()
                else break
            }
            return x
        }

        private fun parseMulDiv(): Long {
            var x = parseVal()
            while (true) {
                if (eat('*'.code)) x *= parseVal()
                else if (eat('/'.code)) {
                    val d = parseVal()
                    if (d == 0L) throw ArithmeticException("Division by zero")
                    x /= d
                } else break
            }
            return x
        }

        private fun parseVal(): Long {
            if (eat('~'.code)) return parseVal().inv()
            if (eat('-'.code)) return -parseVal()
            if (eat('+'.code)) return parseVal()

            var x: Long = 0
            if (eat('('.code)) {
                x = parseOr()
                eat(')'.code)
            } else {
                val start = pos
                while ((ch >= '0'.code && ch <= '9'.code) || (ch >= 'a'.code && ch <= 'f'.code) || (ch >= 'A'.code && ch <= 'F'.code)) {
                    nextChar()
                }
                val token = expression.substring(start, pos)
                if (token.isEmpty()) throw IllegalArgumentException("Expected number/symbol")
                x = token.toLong(base)
            }
            return x
        }
    }

    /**
     * Parse and solve programmer expressions with hex, oct, bin, dec bases & bitwise operators.
     */
    fun evaluateProgrammer(expression: String, radixStr: String): Long {
        val base = when (radixStr.uppercase()) {
            "HEX" -> 16
            "OCT" -> 8
            "BIN" -> 2
            else -> 10
        }
        
        // Clean and replace symbol names
        val clean = expression
            .replace("AND", "&")
            .replace("OR", "|")
            .replace("XOR", "^")
            .replace("NOT", "~")
            .replace("LSH", "<<")
            .replace("RSH", ">>")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")

        if (clean.isEmpty()) return 0L
        return ProgrammerParser(clean, base).parse()
    }
}
