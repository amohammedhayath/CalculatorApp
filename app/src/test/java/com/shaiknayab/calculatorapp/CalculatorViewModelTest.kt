package com.shaiknayab.calculatorapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.*

/**
 * Comprehensive unit tests for CalculatorViewModel
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var viewModel: CalculatorViewModel
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Application>()
        sharedPreferences = context.getSharedPreferences("calc_prefs_test", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        viewModel = CalculatorViewModel(context.applicationContext as Application)
    }

    @After
    fun tearDown() {
        sharedPreferences.edit().clear().apply()
    }

    // ========== BASIC ARITHMETIC OPERATIONS ==========

    @Test
    fun testAddition() = runTest {
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("5", viewModel.result.value)
    }

    @Test
    fun testSubtraction() = runTest {
        viewModel.appendToExpression("5")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("3")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("2", viewModel.result.value)
    }

    @Test
    fun testMultiplication() = runTest {
        viewModel.appendToExpression("4")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("5")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("20", viewModel.result.value)
    }

    @Test
    fun testDivision() = runTest {
        viewModel.appendToExpression("10")
        viewModel.appendToExpression("/")
        viewModel.appendToExpression("2")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("5", viewModel.result.value)
    }

    @Test
    fun testDivisionByZero() = runTest {
        viewModel.appendToExpression("10")
        viewModel.appendToExpression("/")
        viewModel.appendToExpression("0")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("Error", viewModel.result.value)
    }

    @Test
    fun testPercentageBinary() = runTest {
        // Old binary percentage behavior: 50% of 200 = 100
        // This now requires explicit multiplication: 50% * 200
        viewModel.appendToExpression("50")
        viewModel.appendToExpression("%")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("200")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("100", viewModel.result.value)
    }

    @Test
    fun testPercentageAsUnaryOperator() = runTest {
        // Test that 100*20% = 20 (20% should be converted to 0.2)
        viewModel.appendToExpression("100")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("20")
        viewModel.appendToExpression("%")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("20", viewModel.result.value)
    }

    @Test
    fun testPercentageAlone() = runTest {
        // Test that 20% = 0.2
        viewModel.appendToExpression("20")
        viewModel.appendToExpression("%")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("0.2", viewModel.result.value)
    }

    @Test
    fun testPercentageWithDecimal() = runTest {
        // Test that 50*25.5% = 12.75
        viewModel.appendToExpression("50")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("25.5")
        viewModel.appendToExpression("%")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("12.75", viewModel.result.value)
    }

    // ========== NEGATIVE NUMBERS ==========

    @Test
    fun testNegativeNumberAtStart() = runTest {
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("5")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("-5", viewModel.result.value)
    }

    @Test
    fun testNegativeNumberInExpression() = runTest {
        viewModel.appendToExpression("10")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("3")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("7", viewModel.result.value)
    }

    @Test
    fun testSubtractionWithNegativeResult() = runTest {
        viewModel.appendToExpression("5")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("10")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("-5", viewModel.result.value)
    }

    @Test
    fun testNegativeNumberMultiplication() = runTest {
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("4")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("5")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("-20", viewModel.result.value)
    }

    @Test
    fun testNegativeNumberDivision() = runTest {
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("10")
        viewModel.appendToExpression("/")
        viewModel.appendToExpression("2")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("-5", viewModel.result.value)
    }

    // ========== DECIMAL NUMBERS ==========

    @Test
    fun testDecimalAddition() = runTest {
        viewModel.appendToExpression("2.5")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3.7")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("6.2", viewModel.result.value)
    }

    @Test
    fun testDecimalMultiplication() = runTest {
        viewModel.appendToExpression("2.5")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("4")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("10", viewModel.result.value)
    }

    // ========== PARENTHESES ==========

    @Test
    fun testParentheses() = runTest {
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression(")")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("4")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("20", viewModel.result.value)
    }

    @Test
    fun testImplicitMultiplication() = runTest {
        // Test 10(10) = 100 (implicit multiplication)
        viewModel.appendToExpression("10")
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("10")
        viewModel.appendToExpression(")")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("100", viewModel.result.value)
    }

    @Test
    fun testImplicitMultiplicationWithExpression() = runTest {
        // Test 5(2+3) = 25
        viewModel.appendToExpression("5")
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression(")")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("25", viewModel.result.value)
    }

    @Test
    fun testImplicitMultiplicationAfterParentheses() = runTest {
        // Test (2+3)(4+1) = 25
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression(")")
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("4")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("1")
        viewModel.appendToExpression(")")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("25", viewModel.result.value)
    }

    @Test
    fun testImplicitMultiplicationWithFunction() = runTest {
        // Test 2sin(30) = 2 * sin(30)
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("sin")
        viewModel.appendToExpression("30")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = 2.0 * sin(Math.toRadians(30.0))
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    @Test
    fun testNestedParentheses() = runTest {
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("4")
        viewModel.appendToExpression(")")
        viewModel.appendToExpression(")")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("14", viewModel.result.value)
    }

    @Test
    fun testToggleParenthesis() = runTest {
        viewModel.toggleParenthesis()
        assertEquals("(", viewModel.expression.value)
        
        viewModel.appendToExpression("5")
        viewModel.toggleParenthesis()
        assertEquals("(5)", viewModel.expression.value)
    }

    // ========== OPERATOR PRECEDENCE (BODMAS) ==========

    @Test
    fun testMultiplicationBeforeAddition() = runTest {
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("4")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("14", viewModel.result.value) // 2 + (3×4) = 14
    }

    @Test
    fun testDivisionBeforeSubtraction() = runTest {
        viewModel.appendToExpression("10")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("8")
        viewModel.appendToExpression("/")
        viewModel.appendToExpression("2")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("6", viewModel.result.value) // 10 - (8/2) = 6
    }

    @Test
    fun testComplexExpression() = runTest {
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("4")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("5")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("26", viewModel.result.value) // (2×3) + (4×5) = 6 + 20 = 26
    }

    // ========== SCIENTIFIC FUNCTIONS ==========

    @Test
    fun testSquareRoot() = runTest {
        viewModel.appendToExpression("√")
        viewModel.appendToExpression("16")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("4", viewModel.result.value)
    }

    @Test
    fun testSquareRootOfDecimal() = runTest {
        viewModel.appendToExpression("√")
        viewModel.appendToExpression("25")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("5", viewModel.result.value)
    }

    @Test
    fun testSquareRootOfNegativeNumber() = runTest {
        viewModel.appendToExpression("√")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("4")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("Error", viewModel.result.value)
    }

    @Test
    fun testSine() = runTest {
        viewModel.appendToExpression("sin")
        viewModel.appendToExpression("30")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = sin(Math.toRadians(30.0))
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    @Test
    fun testCosine() = runTest {
        viewModel.appendToExpression("cos")
        viewModel.appendToExpression("60")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = cos(Math.toRadians(60.0))
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    @Test
    fun testTangent() = runTest {
        viewModel.appendToExpression("tan")
        viewModel.appendToExpression("45")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = tan(Math.toRadians(45.0))
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    @Test
    fun testLogarithm() = runTest {
        viewModel.appendToExpression("log")
        viewModel.appendToExpression("100")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("2", viewModel.result.value) // log10(100) = 2
    }

    @Test
    fun testLogarithmOfNegativeNumber() = runTest {
        viewModel.appendToExpression("log")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("5")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("Error", viewModel.result.value)
    }

    @Test
    fun testNaturalLogarithm() = runTest {
        viewModel.appendToExpression("ln")
        viewModel.appendToExpression("1")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = ln(1.0)
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    @Test
    fun testFunctionWithParentheses() = runTest {
        viewModel.appendToExpression("sin")
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("30")
        viewModel.appendToExpression(")")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = sin(Math.toRadians(30.0))
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    @Test
    fun testFunctionInExpression() = runTest {
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("sin")
        viewModel.appendToExpression("30")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = 2.0 * sin(Math.toRadians(30.0))
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    // ========== EXPRESSION MANIPULATION ==========

    @Test
    fun testAppendToExpression() = runTest {
        viewModel.appendToExpression("1")
        assertEquals("1", viewModel.expression.value)
        
        viewModel.appendToExpression("2")
        assertEquals("12", viewModel.expression.value)
        
        viewModel.appendToExpression("+")
        assertEquals("12+", viewModel.expression.value)
    }

    @Test
    fun testBackspace() = runTest {
        viewModel.appendToExpression("123")
        viewModel.backspace()
        assertEquals("12", viewModel.expression.value)
        
        viewModel.backspace()
        assertEquals("1", viewModel.expression.value)
        
        viewModel.backspace()
        assertEquals("", viewModel.expression.value)
    }

    @Test
    fun testBackspaceOnEmptyExpression() = runTest {
        viewModel.backspace()
        assertEquals("", viewModel.expression.value)
    }

    @Test
    fun testClearAll() = runTest {
        viewModel.appendToExpression("123")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("456")
        viewModel.calculate()
        advanceUntilIdle()
        
        viewModel.clearAll()
        assertEquals("", viewModel.expression.value)
        assertEquals("", viewModel.result.value)
        assertEquals(0, viewModel.cursorPosition.value)
    }

    @Test
    fun testClearResultOnNewInput() = runTest {
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("5", viewModel.result.value)
        
        viewModel.appendToExpression("4")
        assertEquals("4", viewModel.expression.value)
        assertEquals("", viewModel.result.value)
    }

    @Test
    fun testOperatorOverride() = runTest {
        viewModel.appendToExpression("5")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("-") // Should replace +
        assertEquals("5-", viewModel.expression.value)
        
        viewModel.appendToExpression("×") // Should replace -
        assertEquals("5×", viewModel.expression.value)
    }

    // ========== CURSOR POSITION ==========

    @Test
    fun testCursorPosition() = runTest {
        viewModel.appendToExpression("123")
        assertEquals(3, viewModel.cursorPosition.value)
        
        viewModel.updateCursor(1)
        assertEquals(1, viewModel.cursorPosition.value)
        
        viewModel.appendToExpression("+")
        assertEquals("1+23", viewModel.expression.value)
        assertEquals(2, viewModel.cursorPosition.value)
    }

    @Test
    fun testCursorPositionAfterCalculation() = runTest {
        viewModel.appendToExpression("123")
        viewModel.calculate()
        advanceUntilIdle()
        
        // Cursor should be at end of result
        assertTrue(viewModel.cursorPosition.value >= 0)
    }

    @Test
    fun testCursorPositionClamping() = runTest {
        viewModel.appendToExpression("123")
        viewModel.updateCursor(10) // Beyond length
        assertEquals(3, viewModel.cursorPosition.value) // Should clamp to length
        
        viewModel.updateCursor(-5) // Negative
        assertEquals(0, viewModel.cursorPosition.value) // Should clamp to 0
    }

    // ========== HISTORY ==========

    @Test
    fun testAddToHistory() = runTest {
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertTrue(viewModel.history.value.isNotEmpty())
        assertTrue(viewModel.history.value[0].contains("2+3 = 5"))
    }

    @Test
    fun testHistoryLimit() = runTest {
        // Add more than 50 entries
        for (i in 1..55) {
            viewModel.appendToExpression(i.toString())
            viewModel.appendToExpression("+")
            viewModel.appendToExpression("0")
            viewModel.calculate()
            advanceUntilIdle()
        }
        
        assertEquals(50, viewModel.history.value.size)
    }

    @Test
    fun testClearHistory() = runTest {
        viewModel.appendToExpression("1")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("1")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertTrue(viewModel.history.value.isNotEmpty())
        
        viewModel.clearHistory()
        assertTrue(viewModel.history.value.isEmpty())
    }

    @Test
    fun testCalculateSteps() = runTest {
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("4")
        viewModel.calculate()
        advanceUntilIdle()
        
        val entry = viewModel.history.value[0]
        val steps = viewModel.calculateSteps(entry)
        
        assertTrue(steps.isNotEmpty())
        // Should show multiplication before addition
        assertTrue(steps.any { it.contains("×") || it.contains("*") })
    }

    @Test
    fun testCalculateStepsWithParentheses() = runTest {
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression(")")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("4")
        viewModel.calculate()
        advanceUntilIdle()
        
        val entry = viewModel.history.value[0]
        val steps = viewModel.calculateSteps(entry)
        
        assertTrue(steps.isNotEmpty())
        assertTrue(steps.any { it.contains("(") })
    }

    // ========== THEME MANAGEMENT ==========

    @Test
    fun testSetTheme() = runTest {
        viewModel.setTheme("light")
        advanceUntilIdle()
        
        assertEquals("light", viewModel.currentTheme.value.id)
        
        viewModel.setTheme("cyber")
        advanceUntilIdle()
        
        assertEquals("cyber", viewModel.currentTheme.value.id)
        
        viewModel.setTheme("dark")
        advanceUntilIdle()
        
        assertEquals("dark", viewModel.currentTheme.value.id)
    }

    @Test
    fun testDefaultTheme() = runTest {
        advanceUntilIdle()
        assertEquals("dark", viewModel.currentTheme.value.id)
    }

    // ========== EDGE CASES ==========

    @Test
    fun testEmptyExpressionCalculation() = runTest {
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("", viewModel.result.value)
    }

    @Test
    fun testSingleNumber() = runTest {
        viewModel.appendToExpression("42")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("42", viewModel.result.value)
    }

    @Test
    fun testMultipleOperators() = runTest {
        viewModel.appendToExpression("5")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.calculate()
        advanceUntilIdle()
        
        // Should handle operator override
        assertEquals("8", viewModel.result.value)
    }

    @Test
    fun testDecimalPrecision() = runTest {
        viewModel.appendToExpression("1")
        viewModel.appendToExpression("/")
        viewModel.appendToExpression("3")
        viewModel.calculate()
        advanceUntilIdle()
        
        val result = viewModel.result.value.toDoubleOrNull()
        assertNotNull(result)
        assertEquals(1.0 / 3.0, result!!, 0.0000000001)
    }

    @Test
    fun testLargeNumbers() = runTest {
        viewModel.appendToExpression("999999")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("999999")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = 999999L * 999999L
        assertEquals(expected.toString(), viewModel.result.value)
    }

    @Test
    fun testComplexScientificExpression() = runTest {
        viewModel.appendToExpression("sin")
        viewModel.appendToExpression("30")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("cos")
        viewModel.appendToExpression("60")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = sin(Math.toRadians(30.0)) * 2.0 + cos(Math.toRadians(60.0))
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    @Test
    fun testNestedFunctions() = runTest {
        viewModel.appendToExpression("√")
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("sin")
        viewModel.appendToExpression("90")
        viewModel.appendToExpression(")")
        viewModel.calculate()
        advanceUntilIdle()
        
        val expected = sqrt(sin(Math.toRadians(90.0)))
        val actual = viewModel.result.value.toDoubleOrNull()
        assertNotNull(actual)
        assertEquals(expected, actual!!, 0.0001)
    }

    // ========== ERROR HANDLING ==========

    @Test
    fun testInvalidExpression() = runTest {
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("+")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("Error", viewModel.result.value)
    }

    @Test
    fun testUnclosedParentheses() = runTest {
        viewModel.appendToExpression("(")
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.calculate()
        advanceUntilIdle()
        
        // Should either calculate or show error
        assertTrue(viewModel.result.value.isNotEmpty())
    }

    @Test
    fun testFunctionWithoutArgument() = runTest {
        viewModel.appendToExpression("sin")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("Error", viewModel.result.value)
    }

    // ========== TRAILING OPERATORS ==========

    @Test
    fun testTrailingOperator() = runTest {
        // Test 6-6- should evaluate to 0 (ignore trailing -)
        viewModel.appendToExpression("6")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("6")
        viewModel.appendToExpression("-")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("0", viewModel.result.value)
    }

    @Test
    fun testTrailingPlusOperator() = runTest {
        // Test 5+3+ should evaluate to 8 (ignore trailing +)
        viewModel.appendToExpression("5")
        viewModel.appendToExpression("+")
        viewModel.appendToExpression("3")
        viewModel.appendToExpression("+")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("8", viewModel.result.value)
    }

    @Test
    fun testTrailingMultiplyOperator() = runTest {
        // Test 4×5× should evaluate to 20 (ignore trailing ×)
        viewModel.appendToExpression("4")
        viewModel.appendToExpression("×")
        viewModel.appendToExpression("5")
        viewModel.appendToExpression("×")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("20", viewModel.result.value)
    }

    @Test
    fun testTrailingDivideOperator() = runTest {
        // Test 10/2/ should evaluate to 5 (ignore trailing /)
        viewModel.appendToExpression("10")
        viewModel.appendToExpression("/")
        viewModel.appendToExpression("2")
        viewModel.appendToExpression("/")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("5", viewModel.result.value)
    }

    @Test
    fun testMultipleTrailingOperators() = runTest {
        // Test 10-5--- should evaluate to 5 (ignore all trailing operators)
        viewModel.appendToExpression("10")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("5")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("-")
        viewModel.appendToExpression("-")
        viewModel.calculate()
        advanceUntilIdle()
        
        assertEquals("5", viewModel.result.value)
    }
}
