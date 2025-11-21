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
            // If there's a result showing, start fresh
            _expression.value = ""
            _result.value = ""
            _cursorPosition.value = 0
        }
        
        val currentExpr = _expression.value
        val cursorPos = _cursorPosition.value
        
        // Operator override logic: if last char is an operator and new input is an operator, replace it
        val operators = listOf("+", "-", "×", "/", "%")
        if (s in operators && currentExpr.isNotEmpty() && cursorPos > 0) {
            val charBeforeCursor = currentExpr.getOrNull(cursorPos - 1)?.toString()
            if (charBeforeCursor in operators) {
                // Replace the last operator
                val newExpr = currentExpr.substring(0, cursorPos - 1) + s + currentExpr.substring(cursorPos)
                _expression.value = newExpr
                _cursorPosition.value = cursorPos // Keep cursor at same position
                return
            }
        }
        
        // Normal insertion at cursor position
        val newExpr = currentExpr.substring(0, cursorPos) + s + currentExpr.substring(cursorPos)
        _expression.value = newExpr
        _cursorPosition.value = cursorPos + s.length
    }

    fun backspace() {
        val currentExpr = _expression.value
        val cursorPos = _cursorPosition.value
        
        if (currentExpr.isEmpty() || cursorPos == 0) return
        
        // Delete character before cursor
        val newExpr = currentExpr.substring(0, cursorPos - 1) + currentExpr.substring(cursorPos)
        _expression.value = newExpr
        _cursorPosition.value = (cursorPos - 1).coerceAtLeast(0)
        
        // Clear result when editing
        _result.value = ""
    }

    fun clearAll() {
        _expression.value = ""
        _result.value = ""
        _cursorPosition.value = 0
    }

    fun toggleParenthesis() {
        val currentExpr = _expression.value
        val cursorPos = _cursorPosition.value
        
        // Count open and close parentheses before cursor
        val openCount = currentExpr.substring(0, cursorPos).count { it == '(' }
        val closeCount = currentExpr.substring(0, cursorPos).count { it == ')' }
        
        // Smart parenthesis: if more open than close, add close; otherwise add open
        val toInsert = if (openCount > closeCount && cursorPos > 0) {
            val lastChar = currentExpr.getOrNull(cursorPos - 1)
            // Only close if last char is a number or closing paren
            if (lastChar?.isDigit() == true || lastChar == ')') ")" else "("
        } else {
            "("
        }
        
        appendToExpression(toInsert)
    }

    fun calculate() {
        try {
            val expr = _expression.value
            if (expr.isEmpty()) return
            
            // Convert × to * for calculation
            val evalExpr = expr.replace("×", "*").replace("−", "-")
            
            val result = evaluateExpression(evalExpr)
            
            // Format result
            val formattedResult = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                // Round to 10 decimal places to avoid floating point errors
                val rounded = (result * 10000000000).roundToLong() / 10000000000.0
                rounded.toString()
            }
            
            _result.value = formattedResult
            
            // Add to history
            addToHistory("$expr = $formattedResult")
            
            // Move cursor to end of result
            _cursorPosition.value = expr.length
            
        } catch (e: Exception) {
            _result.value = "Error"
        }
    }

    private fun evaluateExpression(expr: String): Double {
        // Tokenize the expression
        val tokens = tokenize(expr)
        
        // Convert to postfix notation (Reverse Polish Notation) using Shunting Yard algorithm
        val postfix = infixToPostfix(tokens)
        
        // Evaluate postfix expression
        return evaluatePostfix(postfix)
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        
        while (i < expr.length) {
            val char = expr[i]
            
            when {
                char.isWhitespace() -> i++
                char.isDigit() || char == '.' -> {
                    // Read number
                    var num = ""
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        num += expr[i]
                        i++
                    }
                    tokens.add(num)
                }
                char in "+-*/%()√" -> {
                    tokens.add(char.toString())
                    i++
                }
                char.isLetter() -> {
                    // Read function name
                    var func = ""
                    while (i < expr.length && expr[i].isLetter()) {
                        func += expr[i]
                        i++
                    }
                    tokens.add(func)
                }
                else -> i++
            }
        }
        
        return tokens
    }

    private fun infixToPostfix(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val operators = ArrayDeque<String>()
        
        val precedence = mapOf(
            "+" to 1, "-" to 1,
            "*" to 2, "/" to 2, "%" to 2,
            "sin" to 3, "cos" to 3, "tan" to 3, "log" to 3, "ln" to 3, "√" to 3
        )
        
        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> output.add(token)
                token in listOf("sin", "cos", "tan", "log", "ln", "√") -> operators.push(token)
                token == "(" -> operators.push(token)
                token == ")" -> {
                    while (operators.isNotEmpty() && operators.peek() != "(") {
                        output.add(operators.pop())
                    }
                    if (operators.isNotEmpty()) operators.pop() // Remove "("
                    // If there's a function on top, pop it too
                    if (operators.isNotEmpty() && operators.peek() in listOf("sin", "cos", "tan", "log", "ln", "√")) {
                        output.add(operators.pop())
                    }
                }
                token in precedence.keys -> {
                    while (operators.isNotEmpty() && 
                           operators.peek() != "(" && 
                           (precedence[operators.peek()] ?: 0) >= (precedence[token] ?: 0)) {
                        output.add(operators.pop())
                    }
                    operators.push(token)
                }
            }
        }
        
        while (operators.isNotEmpty()) {
            output.add(operators.pop())
        }
        
        return output
    }

    private fun evaluatePostfix(postfix: List<String>): Double {
        val stack = ArrayDeque<Double>()
        
        for (token in postfix) {
            when {
                token.toDoubleOrNull() != null -> stack.push(token.toDouble())
                token in listOf("+", "-", "*", "/", "%") -> {
                    if (stack.size < 2) throw IllegalArgumentException("Invalid expression")
                    val b = stack.pop()
                    val a = stack.pop()
                    val result = when (token) {
                        "+" -> a + b
                        "-" -> a - b
                        "*" -> a * b
                        "/" -> if (b != 0.0) a / b else throw ArithmeticException("Division by zero")
                        "%" -> a * b / 100.0
                        else -> 0.0
                    }
                    stack.push(result)
                }
                token in listOf("sin", "cos", "tan", "log", "ln", "√") -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Invalid expression")
                    val arg = stack.pop()
                    val result = evalFunc(token, arg)
                    stack.push(result)
                }
            }
        }
        
        return if (stack.isNotEmpty()) stack.first() else 0.0
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

    fun calculateSteps(entry: String): List<String> {
        // Parse the history entry "expression = result"
        val parts = entry.split(" = ")
        if (parts.size != 2) return listOf("Invalid entry")
        
        val expr = parts[0].replace("×", "*").replace("−", "-")
        val steps = mutableListOf<String>()
        
        steps.add("Original: $expr")
        
        try {
            // This is a simplified step-by-step breakdown
            // For a full BODMAS breakdown, we'd need to track each operation
            val tokens = tokenize(expr)
            
            // Show tokenization
            steps.add("Tokens: ${tokens.joinToString(" ")}")
            
            // Show final result
            val result = evaluateExpression(expr)
            val formattedResult = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                result.toString()
            }
            steps.add("Result: $formattedResult")
            
        } catch (e: Exception) {
            steps.add("Error: ${e.message}")
        }
        
        return steps
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