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
            // But if the new input is an operator, use the previous result as the starting number
            if (s in listOf("+", "-", "×", "/", "%")) {
                _expression.value = _result.value
                 // Move cursor to end
                _cursorPosition.value = _result.value.length
                _result.value = ""
            } else {
                _expression.value = ""
                _result.value = ""
                _cursorPosition.value = 0
            }
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
        
        // Check for decimal point validity
        if (s == ".") {
            // Find the current number being edited
            // Search backwards from cursor for a non-digit/non-dot
            var start = cursorPos - 1
            while (start >= 0 && (currentExpr[start].isDigit() || currentExpr[start] == '.')) {
                start--
            }
            // Search forwards from cursor
            var end = cursorPos
            while (end < currentExpr.length && (currentExpr[end].isDigit() || currentExpr[end] == '.')) {
                end++
            }
            
            val currentNumber = currentExpr.substring(start + 1, end)
            if (currentNumber.contains(".")) {
                return // Already has a decimal, ignore
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
        
        // Smart backspace: check for functions
        val functions = listOf("sin(", "cos(", "tan(", "log(", "ln(", "nan", "inf", "error")
        var deleteCount = 1
        
        // Check if we are deleting a function suffix
        for (func in functions) {
            if (cursorPos >= func.length) {
                val sub = currentExpr.substring(cursorPos - func.length, cursorPos)
                if (sub == func) {
                    deleteCount = func.length
                    break
                }
            }
        }
        
        // Delete character(s) before cursor
        val newExpr = currentExpr.substring(0, cursorPos - deleteCount) + currentExpr.substring(cursorPos)
        _expression.value = newExpr
        _cursorPosition.value = (cursorPos - deleteCount).coerceAtLeast(0)
        
        // Clear result when editing
        _result.value = ""
    }

    fun clearAll() {
        _expression.value = ""
        _result.value = ""
        _cursorPosition.value = 0
    }

    fun overwriteExpression(expr: String) {
        _expression.value = expr
        _result.value = ""
        _cursorPosition.value = expr.length
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
            var evalExpr = expr.replace("×", "*").replace("−", "-")
            
            // Remove trailing operators (e.g., 6-6- -> 6-6)
            evalExpr = removeTrailingOperators(evalExpr)
            
            // Resolve business percentages (e.g., 100+10% -> 100+(100*10/100))
            evalExpr = resolveBusinessPercentages(evalExpr)
            
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
            
            // Move cursor to end of result (result length, not expression length)
            _cursorPosition.value = formattedResult.length
            
        } catch (e: Exception) {
            _result.value = "Error"
        }
    }

    private fun resolveBusinessPercentages(expr: String): String {
        var currentExpr = expr
        // Matches pattern: Number (A) followed by + or - followed by Number (B) and %
        // Group 1: A (can be float, negative)
        // Group 2: Operator (+ or -)
        // Group 3: B (can be float)
        val pattern = Regex("(-?[\\d.]+)\\s*([+\\-])\\s*([\\d.]+)%")
        
        while (true) {
            val match = pattern.find(currentExpr) ?: break
            val a = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val op = match.groupValues[2]
            val b = match.groupValues[3].toDoubleOrNull() ?: 0.0
            
            // logic: A + B% -> A + (A * B / 100)
            val percentValue = (a * b) / 100.0
            // We replace just the "B%" part with the calculated value, 
            // but since we need to reconstruct the string, it's easier to replace the whole "A op B%" pattern
            // However, we must be careful with overlapping matches or if A was part of a previous calculation.
            // Since we iterate, replacing the first match is safe.
            
            // Reconstruct: A op calculated_value
            // e.g., 100 + 10 -> 110 (this will be evaluated later)
            // But wait, the expression evaluator expects valid math. 
            // 100 + 10 is valid.
            // What if A is negative? -100 + 10% -> -100 + (-10) = -110.
            
            val replacement = "$a $op $percentValue"
            currentExpr = currentExpr.replaceFirst(match.value, replacement)
        }
        return currentExpr
    }

    private fun removeTrailingOperators(expr: String): String {
        // Remove trailing operators (+, -, *, /, ×, −) and whitespace
        var result = expr.trimEnd()
        
        // Keep removing trailing operators until we hit a number, closing paren, or function
        while (result.isNotEmpty()) {
            val lastChar = result.last()
            if (lastChar in "+-*/×−") {
                result = result.dropLast(1).trimEnd()
            } else {
                break
            }
        }
        
        return result
    }
    
    private fun evaluateExpression(expr: String): Double {
        // Tokenize the expression
        val tokens = tokenize(expr)
        
        // Add implicit multiplication operators (e.g., 10(10) -> 10*(10))
        val tokensWithImplicitMult = addImplicitMultiplication(tokens)
        
        // Convert to postfix notation (Reverse Polish Notation) using Shunting Yard algorithm
        val postfix = infixToPostfix(tokensWithImplicitMult)
        
        // Evaluate postfix expression
        return evaluatePostfix(postfix)
    }
    
    private fun addImplicitMultiplicationToString(expr: String): String {
        // Add implicit multiplication operators to string expression
        // Cases: number( -> number*(, )( -> )*(, )number -> )*number, numberfunction -> number*function
        var result = expr
        var i = 0
        
        while (i < result.length - 1) {
            val char = result[i]
            val nextChar = result[i + 1]
            
            // Case 1: number or ) followed by ( or function name
            if (char.isDigit() || char == '.' || char == ')') {
                if (nextChar == '(') {
                    result = result.substring(0, i + 1) + "*" + result.substring(i + 1)
                    i += 2
                    continue
                } else if (nextChar.isLetter()) {
                    // Check if it's a function name
                    var j = i + 1
                    while (j < result.length && result[j].isLetter()) j++
                    val potentialFunc = result.substring(i + 1, j)
                    if (potentialFunc in listOf("sin", "cos", "tan", "log", "ln")) {
                        result = result.substring(0, i + 1) + "*" + result.substring(i + 1)
                        i += 2
                        continue
                    }
                }
            }
            
            // Case 2: ) followed by number
            if (char == ')') {
                if (nextChar.isDigit() || nextChar == '.') {
                    result = result.substring(0, i + 1) + "*" + result.substring(i + 1)
                    i += 2
                    continue
                }
            }
            
            i++
        }
        
        return result
    }
    
    private fun addImplicitMultiplication(tokens: List<String>): List<String> {
        if (tokens.isEmpty()) return tokens
        
        val result = mutableListOf<String>()
        
        for (i in tokens.indices) {
            val current = tokens[i]
            result.add(current)
            
            // Check if we need to add implicit multiplication
            if (i < tokens.size - 1) {
                val next = tokens[i + 1]
                
                // Case 1: number followed by ( or function
                val isNumber = current.toDoubleOrNull() != null
                val isClosingParen = current == ")"
                val isFunction = current in listOf("sin", "cos", "tan", "log", "ln", "√")
                
                val nextIsOpeningParen = next == "("
                val nextIsFunction = next in listOf("sin", "cos", "tan", "log", "ln", "√")
                val nextIsNumber = next.toDoubleOrNull() != null
                
                if ((isNumber || isClosingParen) && (nextIsOpeningParen || nextIsFunction)) {
                    result.add("*")
                } else if (isClosingParen && nextIsNumber) {
                    result.add("*")
                }
            }
        }
        
        return result
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
                    // Check if next character is % (unary postfix operator)
                    if (i < expr.length && expr[i] == '%') {
                        // Convert number to percentage (divide by 100) and add as number
                        val numValue = num.toDoubleOrNull() ?: 0.0
                        tokens.add((numValue / 100.0).toString())
                        i++ // Skip the '%'
                    } else {
                        tokens.add(num)
                    }
                }
                char == '-' -> {
                    // Check if this is a unary minus (negative number) or binary operator
                    val isUnaryMinus = tokens.isEmpty() || 
                                      tokens.last() in listOf("+", "-", "*", "/", "%", "(", "sin", "cos", "tan", "log", "ln", "√")
                    
                    if (isUnaryMinus && i + 1 < expr.length && (expr[i + 1].isDigit() || expr[i + 1] == '.')) {
                        // This is a unary minus - combine with next number
                        i++ // Skip the '-'
                        var num = "-"
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                            num += expr[i]
                            i++
                        }
                        // Check if next character is % (unary postfix operator)
                        if (i < expr.length && expr[i] == '%') {
                            // Convert number to percentage (divide by 100) and add as number
                            val numValue = num.toDoubleOrNull() ?: 0.0
                            tokens.add((numValue / 100.0).toString())
                            i++ // Skip the '%'
                        } else {
                            tokens.add(num)
                        }
                    } else {
                        // Binary operator
                        tokens.add("-")
                        i++
                    }
                }
                char == '%' -> {
                    // Standalone % should not happen if we handle it after numbers
                    // But if it does, treat as error or ignore
                    i++
                }
                char in "+*/()√" -> {
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
            "*" to 2, "/" to 2,
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
                    if (operators.isNotEmpty()) {
                        val top = operators.peek()
                        if (top != null && top in listOf("sin", "cos", "tan", "log", "ln", "√")) {
                            output.add(operators.pop())
                        }
                    }
                }
                token in precedence.keys -> {
                    while (operators.isNotEmpty()) {
                        val top = operators.peek()
                        if (top == null || top == "(") break
                        if ((precedence[top] ?: 0) < (precedence[token] ?: 0)) break
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
                token in listOf("+", "-", "*", "/") -> {
                    if (stack.size < 2) throw IllegalArgumentException("Invalid expression")
                    val b = stack.pop()
                    val a = stack.pop()
                    val result = when (token) {
                        "+" -> a + b
                        "-" -> a - b
                        "*" -> a * b
                        "/" -> if (b != 0.0) a / b else throw ArithmeticException("Division by zero")
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
        
        return if (stack.isNotEmpty()) stack.pop() else 0.0
    }

    private fun evalFunc(func: String, arg: Double): Double {
        val result = when (func) {
            "sin" -> sin(Math.toRadians(arg))
            "cos" -> cos(Math.toRadians(arg))
            "tan" -> tan(Math.toRadians(arg))
            "log" -> if (arg > 0) log10(arg) else throw IllegalArgumentException("Logarithm of non-positive number")
            "ln" -> if (arg > 0) ln(arg) else throw IllegalArgumentException("Natural logarithm of non-positive number")
            "√" -> if (arg >= 0) sqrt(arg) else throw IllegalArgumentException("Square root of negative number")
            else -> 0.0
        }
        
        // Check for invalid results
        if (result.isNaN() || result.isInfinite()) {
            throw IllegalArgumentException("Invalid function result: ${if (result.isNaN()) "NaN" else "Infinity"}")
        }
        
        return result
    }

    fun calculateSteps(entry: String): List<String> {
        // Parse the history entry "expression = result"
        val parts = entry.split(" = ")
        if (parts.size != 2) return listOf("Invalid entry")
        
        val originalExpr = parts[0]
        var expr = originalExpr.replace("×", "*").replace("−", "-")
        
        // Remove trailing operators for step-by-step breakdown
        expr = removeTrailingOperators(expr)
        
        val steps = mutableListOf<String>()
        
        try {
            // Generate clean step-by-step BODMAS breakdown
            val detailedSteps = evaluateWithSteps(expr)
            steps.addAll(detailedSteps)
            
        } catch (e: Exception) {
            steps.add("Error: ${e.message}")
        }
        
        return steps
    }
    
    private fun evaluateWithSteps(expr: String): List<String> {
        val steps = mutableListOf<String>()
        var currentExpr = expr
        
        // Step 0: Add implicit multiplication (e.g., 10(10) -> 10*(10))
        currentExpr = addImplicitMultiplicationToString(currentExpr)
        
        // Step 0.5: Resolve Business Percentages (Add/Sub)
        // We do this loop manually to record steps
        var bizChanged = true
        while (bizChanged) {
            bizChanged = false
            val pattern = Regex("(-?[\\d.]+)\\s*([+\\-])\\s*([\\d.]+)%")
            val match = pattern.find(currentExpr)
            if (match != null) {
                val a = match.groupValues[1].toDoubleOrNull() ?: 0.0
                val op = match.groupValues[2]
                val b = match.groupValues[3].toDoubleOrNull() ?: 0.0
                val percentValue = (a * b) / 100.0
                
                // Format for display
                val percentFormatted = if (percentValue % 1.0 == 0.0) percentValue.toLong().toString() else String.format("%.4f", percentValue)
                val fullReplacement = "$a $op $percentFormatted"
                
                steps.add("${match.value} -> $fullReplacement")
                currentExpr = currentExpr.replaceFirst(match.value, fullReplacement)
                bizChanged = true
            }
        }
        
        // Step 1: Handle remaining percentage operators (mult/div/standalone)
        var changed = true
        while (changed) {
            changed = false
            val percentPattern = Regex("(-?[\\d.]+)%")
            val match = percentPattern.find(currentExpr)
            if (match != null) {
                val num = match.groupValues[1].toDoubleOrNull() ?: 0.0
                val percentValue = num / 100.0
                val formatted = if (percentValue % 1.0 == 0.0) percentValue.toLong().toString() else String.format("%.4f", percentValue)
                val numFormatted = if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
                steps.add("$numFormatted% = $formatted")
                currentExpr = currentExpr.replaceFirst(match.value, formatted)
                changed = true
            }
        }
        
        // Step 1: Handle functions and square roots (with nested parentheses support)
        changed = true
        while (changed) {
            changed = false
            // Find innermost function calls by matching from inside out
            var depth = 0
            var startIdx = -1
            var funcStart = -1
            var funcName = ""
            
            for (i in currentExpr.indices) {
                when (currentExpr[i]) {
                    '(' -> {
                        if (depth == 0) {
                            // Check if there's a function name before this opening paren
                            var j = i - 1
                            while (j >= 0 && currentExpr[j].isWhitespace()) j--
                            if (j >= 0) {
                                var k = j
                                while (k >= 0 && currentExpr[k].isLetter()) k--
                                val potentialFunc = currentExpr.substring(k + 1, j + 1)
                                if (potentialFunc in listOf("sin", "cos", "tan", "log", "ln")) {
                                    funcStart = k + 1
                                    funcName = potentialFunc
                                } else if (j >= 0 && currentExpr[j] == '√') {
                                    funcStart = j
                                    funcName = "√"
                                }
                            }
                            startIdx = i
                        }
                        depth++
                    }
                    ')' -> {
                        depth--
                        if (depth == 0 && startIdx >= 0) {
                            // Found innermost parentheses
                            val innerExpr = currentExpr.substring(startIdx + 1, i)
                            if (funcStart >= 0 && funcStart < startIdx) {
                                // This is a function call
                                val fullMatch = currentExpr.substring(funcStart, i + 1)
                                try {
                                    val arg = evaluateSimpleExpression(innerExpr)
                                    val result = evalFunc(funcName, arg)
                                    val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.4f", result)
                                    currentExpr = currentExpr.replaceRange(funcStart, i + 1, formatted)
                                    steps.add("$fullMatch = $formatted")
                                    changed = true
                                } catch (e: Exception) {
                                    steps.add("$fullMatch = Error: ${e.message}")
                                    changed = true
                                }
                            } else {
                                // Regular parentheses
                                try {
                                    val result = evaluateSimpleExpression(innerExpr)
                                    val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.4f", result)
                                    currentExpr = currentExpr.replaceRange(startIdx, i + 1, formatted)
                                    steps.add("($innerExpr) = $formatted")
                                    changed = true
                                } catch (e: Exception) {
                                    steps.add("($innerExpr) = Error: ${e.message}")
                                    changed = true
                                }
                            }
                            break
                        }
                    }
                }
            }
        }
        
        // Step 2: Evaluate multiplication and division, then addition and subtraction
        val finalSteps = evaluateSimpleExpressionWithSteps(currentExpr)
        steps.addAll(finalSteps)
        
        return steps
    }
    
    private fun evaluateSimpleExpressionWithSteps(expr: String): List<String> {
        val steps = mutableListOf<String>()
        var currentExpr = expr.replace(" ", "")
        
        // First pass: Handle * and / (left to right) - support negative numbers
        var changed = true
        while (changed) {
            changed = false
            // Pattern matches: optional minus, digits/decimal, operator, optional minus, digits/decimal
            val mulDivPattern = Regex("(-?[\\d.]+)\\s*([*/])\\s*(-?[\\d.]+)")
            val match = mulDivPattern.find(currentExpr)
            if (match != null) {
                val a = match.groupValues[1].toDouble()
                val op = match.groupValues[2]
                val b = match.groupValues[3].toDouble()
                val result = if (op == "*") a * b else {
                    if (b == 0.0) throw ArithmeticException("Division by zero")
                    a / b
                }
                val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.4f", result)
                val opDisplay = if (op == "*") "×" else op
                
                // Format the numbers nicely
                val aFormatted = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
                val bFormatted = if (b % 1.0 == 0.0) b.toLong().toString() else b.toString()
                
                steps.add("$aFormatted $opDisplay $bFormatted = $formatted")
                currentExpr = currentExpr.replaceFirst(match.value, formatted)
                changed = true
            }
        }
        
        // Second pass: Handle + and - (left to right) - support negative numbers
        // Need to be careful: match from left, but handle unary minus at start
        changed = true
        while (changed) {
            changed = false
            // Pattern: number, operator, number (but not unary minus at start)
            // For expressions starting with negative, handle separately
            if (currentExpr.startsWith("-") && currentExpr.length > 1) {
                // Check if there's an operator after the first number
                val firstNumPattern = Regex("^-?[\\d.]+")
                val firstMatch = firstNumPattern.find(currentExpr)
                if (firstMatch != null) {
                    val afterFirst = currentExpr.substring(firstMatch.range.last + 1).trimStart()
                    if (afterFirst.isNotEmpty() && afterFirst[0] in "+-") {
                        // Binary operation
                        val addSubPattern = Regex("(-?[\\d.]+)\\s*([+\\-])\\s*(-?[\\d.]+)")
                        val match = addSubPattern.find(currentExpr)
                        if (match != null) {
                            val a = match.groupValues[1].toDouble()
                            val op = match.groupValues[2]
                            val b = match.groupValues[3].toDouble()
                            val result = if (op == "+") a + b else a - b
                            val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.4f", result)
                            
                            val aFormatted = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
                            val bFormatted = if (b % 1.0 == 0.0) b.toLong().toString() else b.toString()
                            
                            steps.add("$aFormatted $op $bFormatted = $formatted")
                            currentExpr = currentExpr.replaceFirst(match.value, formatted)
                            changed = true
                        }
                    } else {
                        // Only one number left, we're done
                        break
                    }
                }
            } else {
                // Normal case: number operator number
                val addSubPattern = Regex("(-?[\\d.]+)\\s*([+\\-])\\s*(-?[\\d.]+)")
                val match = addSubPattern.find(currentExpr)
                if (match != null) {
                    val a = match.groupValues[1].toDouble()
                    val op = match.groupValues[2]
                    val b = match.groupValues[3].toDouble()
                    val result = if (op == "+") a + b else a - b
                    val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.4f", result)
                    
                    val aFormatted = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
                    val bFormatted = if (b % 1.0 == 0.0) b.toLong().toString() else b.toString()
                    
                    steps.add("$aFormatted $op $bFormatted = $formatted")
                    currentExpr = currentExpr.replaceFirst(match.value, formatted)
                    changed = true
                }
            }
        }
        
        return steps
    }
    
    private fun evaluateSimpleExpression(expr: String): Double {
        // Simple evaluation without steps for parentheses content
        return try {
            evaluateExpression(expr)
        } catch (e: Exception) {
            0.0
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