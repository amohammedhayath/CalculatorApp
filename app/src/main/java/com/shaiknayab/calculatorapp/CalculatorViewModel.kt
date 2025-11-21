package com.shaiknayab.calculatorapp

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.ArrayDeque
import kotlin.math.*
import androidx.core.content.edit

// --- THEME DATA STRUCTURES ---
data class AppTheme(
    val id: String,
    val name: String,
    val background: Color,
    val buttonNum: Color,
    val buttonOp: Color,
    val buttonFunc: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

// Define our Themes
val DarkTheme = AppTheme(
    id = "dark",
    name = "Classic Dark",
    background = Color(0xFF000000),
    buttonNum = Color(0xFF333333),
    buttonOp = Color(0xFFFF9F0A),
    buttonFunc = Color(0xFFA5A5A5),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color.Gray
)

val LightTheme = AppTheme(
    id = "light",
    name = "Clean Light",
    background = Color(0xFFF2F2F7),
    buttonNum = Color(0xFFFFFFFF),
    buttonOp = Color(0xFFFF9F0A),
    buttonFunc = Color(0xFFD1D1D6),
    textPrimary = Color(0xFF000000),
    textSecondary = Color.DarkGray
)

val CyberTheme = AppTheme(
    id = "cyber",
    name = "Cyberpunk",
    background = Color(0xFF0D1117),
    buttonNum = Color(0xFF161B22),
    buttonOp = Color(0xFF00E5FF),
    buttonFunc = Color(0xFF21262D),
    textPrimary = Color(0xFFE6EDF3),
    textSecondary = Color(0xFF8B949E)
)

class CalculatorViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs: SharedPreferences = app.getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
    private val keyHistory = "history_list"
    private val keyTheme = "selected_theme"

    // --- THEME STATE ---
    private val _currentTheme = MutableStateFlow(DarkTheme)
    val currentTheme: StateFlow<AppTheme> = _currentTheme

    // --- CALCULATOR STATE ---
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result

    private val _history = MutableStateFlow(arrayListOf<String>())
    val history: StateFlow<ArrayList<String>> = _history

    private val _cursorPosition = MutableStateFlow(0)
    val cursorPosition: StateFlow<Int> = _cursorPosition

    init {
        loadHistory()
        loadTheme()
    }

    // --- THEME LOGIC ---
    fun setTheme(themeId: String) {
        val newTheme = when(themeId) {
            "light" -> LightTheme
            "cyber" -> CyberTheme
            else -> DarkTheme
        }
        _currentTheme.value = newTheme
        prefs.edit { putString(keyTheme, themeId) }
    }

    private fun loadTheme() {
        viewModelScope.launch {
            val themeId = prefs.getString(keyTheme, "dark") ?: "dark"
            setTheme(themeId)
        }
    }

    // --- CALCULATOR LOGIC ---
    fun updateCursor(pos: Int) {
        _cursorPosition.value = pos.coerceIn(0, _expression.value.length)
    }

    fun appendToExpression(s: String) {
        if (_result.value.isNotEmpty()) {
            _expression.value = ""
            _result.value = ""
            }
        }
        return if (st.isNotEmpty()) st.first() else 0.0
    }

    private fun evalFunc(func: String, arg: Double): Double {
        return when (func) {
            "sin" -> sin(Math.toRadians(arg))
            "cos" -> cos(Math.toRadians(arg))
            "tan" -> tan(Math.toRadians(arg))
            "log" -> log10(arg)
            "ln" -> ln(arg)
            "√" -> sqrt(arg)
            else -> 0.0
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val json = prefs.getString(keyHistory, "[]") ?: "[]"
                val arr = JSONArray(json)
                val list = arrayListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                _history.value = list
            } catch (_: Exception) {
                _history.value = arrayListOf<String>()
            }
        }
    }

    private fun saveHistory(list: List<String>) {
        viewModelScope.launch {
            val arr = JSONArray()
            for (s in list) arr.put(s)
            prefs.edit { putString(keyHistory, arr.toString()) }
        }
    }

    private fun addToHistory(entry: String) {
        val list = ArrayList(_history.value)
        list.add(0, entry)
        if (list.size > 50) list.subList(50, list.size).clear()
        _history.value = list
        saveHistory(list)
    }

    fun clearHistory() {
        _history.value = arrayListOf<String>()
        saveHistory(emptyList())
    }
}