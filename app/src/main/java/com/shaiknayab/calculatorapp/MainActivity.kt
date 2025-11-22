package com.shaiknayab.calculatorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shaiknayab.calculatorapp.ui.theme.CalculatorAppTheme
import kotlin.system.exitProcess
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.withStyle


class MainActivity : ComponentActivity() {
    private val vm: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorAppTheme {
                // Observe Theme State
                val currentTheme by vm.currentTheme.collectAsState()

                // Animate Background Color Change
                val animatedBgColor by animateColorAsState(
                    targetValue = currentTheme.background,
                    animationSpec = tween(500)
                )

                // Direct App Entry - No Custom Splash Screen
                Surface(modifier = Modifier.fillMaxSize(), color = animatedBgColor) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "calculator") {
                        composable("calculator") {
                            CalculatorScreen(vm, currentTheme) { navController.navigate("history") }
                        }
                        composable("history") {
                            HistoryScreen(vm, currentTheme) { navController.popBackStack() }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel, theme: AppTheme, onOpenHistory: () -> Unit) {
    val expr by viewModel.expression.collectAsState()
    val res by viewModel.result.collectAsState()
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    // UI States
    var showMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
        // Top Bar Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            // Left: History
            IconButton(
                modifier = Modifier.align(Alignment.TopStart),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenHistory()
                }
            ) {
                Icon(Icons.Filled.History, "History", tint = theme.textSecondary, modifier = Modifier.size(32.dp))
            }

            // Right: 3-Dot Menu
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(Icons.Filled.MoreVert, "Menu", tint = theme.textSecondary, modifier = Modifier.size(32.dp))
                }

                // Dropdown Menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(theme.buttonNum)
                ) {
                    DropdownMenuItem(
                        text = { Text("Choose Theme", color = theme.textPrimary) },
                        onClick = {
                            showMenu = false
                            showThemeDialog = true
                        },
                        leadingIcon = { Icon(Icons.Filled.Palette, null, tint = theme.textPrimary) }
                    )
                    DropdownMenuItem(
                        text = { Text("About", color = theme.textPrimary) },
                        onClick = {
                            showMenu = false
                            showAboutDialog = true
                        },
                        leadingIcon = { Icon(Icons.Filled.Info, null, tint = theme.textPrimary) }
                    )
                    HorizontalDivider(color = theme.textSecondary)
                    DropdownMenuItem(
                        text = { Text("Exit", color = theme.textPrimary) },
                        onClick = {
                            exitProcess(0)
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = theme.textPrimary) }
                    )
                }
            }
        }

        // Display Section
        Box(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Results
            val verticalScrollState = rememberScrollState()
            LaunchedEffect(res, expr) {
                verticalScrollState.animateScrollTo(verticalScrollState.maxValue)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if(res.isNotEmpty()) expr else "",
                    fontSize = 32.sp,
                    color = theme.textSecondary,
                    textAlign = TextAlign.End,
                    lineHeight = 36.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                val mainText = res.ifEmpty { expr.ifEmpty { "0" } }
                val dynamicFontSize = when {
                    mainText.length > 15 -> 32.sp
                    mainText.length > 10 -> 48.sp
                    mainText.length > 7 -> 60.sp
                    else -> 72.sp
                }
                val cursorPosition by viewModel.cursorPosition.collectAsState()
                
                // Only show cursor when editing (no result), and clamp to text length
                val showCursor = res.isEmpty()
                val actualCursorPos = if (showCursor) {
                    cursorPosition.coerceIn(0, mainText.length)
                } else {
                    mainText.length // Put cursor at end when showing result
                }

                // Create TextFieldValue for BasicTextField
                val textFieldValue = TextFieldValue(
                    text = mainText,
                    selection = TextRange(actualCursorPos)
                )

                // FocusRequester to keep cursor visible
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                // BasicTextField (readOnly) with visible cursor and vertical scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { /* readOnly, ignore */ },
                        readOnly = true,
                        cursorBrush = SolidColor(theme.buttonOp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = dynamicFontSize,
                            fontWeight = FontWeight.Light,
                            color = theme.textPrimary,
                            textAlign = TextAlign.End,
                            lineHeight = dynamicFontSize * 1.2f
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }
        }

        // Buttons Section
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = if (!isExpanded) {
                listOf(
                    listOf("</>", "%", "÷", "⌫"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "−"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "( )", "=")
                )
            } else {
                listOf(
                    listOf("</>", "%", "÷", "⌫"),
                    listOf("sin", "7", "8", "9"),
                    listOf("cos", "4", "5", "6"),
                    listOf("tan", "1", "2", "3"),
                    listOf("log", "0", ".", "( )"),
                    listOf("√", "×", "−", "+", "=")
                )
            }

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(if(isExpanded) 8.dp else 14.dp)
                ) {
                    for (label in row) {
                        val (btnColor, txtColor) = when (label) {
                            in listOf("</>", "( )", "%", "⌫") -> theme.buttonFunc to theme.textPrimary
                            in listOf("÷", "×", "−", "+", "=") -> theme.buttonOp to Color.White
                            in listOf("sin", "cos", "tan", "log", "√") -> theme.buttonNum.copy(alpha=0.8f) to theme.textPrimary
                            else -> theme.buttonNum to theme.textPrimary
                        }

                        val finalTxtColor = if (theme.id == "light" && label !in listOf("÷", "×", "−", "+", "=")) Color.Black else txtColor

                        CalculatorButton(
                            label = label,
                            backgroundColor = btnColor,
                            textColor = finalTxtColor,
                            isSmall = isExpanded,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onLongClick = if (label == "⌫") {
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.clearAll()
                                }
                            } else null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (label) {
                                "</>" -> isExpanded = !isExpanded
                                "( )" -> viewModel.toggleParenthesis()
                                "%" -> viewModel.appendToExpression("%")
                                "÷" -> viewModel.appendToExpression("/")
                                "×" -> viewModel.appendToExpression("×") // Send exact symbol
                                "−" -> viewModel.appendToExpression("-")
                                "+" -> viewModel.appendToExpression("+")
                                "⌫" -> viewModel.backspace()
                                "=" -> viewModel.calculate()
                                "." -> viewModel.appendToExpression(".")
                                "sin", "cos", "tan", "log", "√" -> viewModel.appendToExpression(if(label == "√") "√" else "$label(")
                                else -> viewModel.appendToExpression(label)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = theme.buttonNum,
            title = { Text("About Calculator", color = theme.textPrimary) },
            text = { Text("Version 1.0\nDesigned with Jetpack Compose.\nFeatures: Scientific Mode, History, Themes.", color = theme.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK", color = theme.buttonOp) }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = theme.buttonNum,
            title = { Text("Choose Theme", color = theme.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOption("Classic Dark", onClick = { viewModel.setTheme("dark"); showThemeDialog = false }, theme)
                    ThemeOption("Clean Light", onClick = { viewModel.setTheme("light"); showThemeDialog = false }, theme)
                    ThemeOption("Cyberpunk", onClick = { viewModel.setTheme("cyber"); showThemeDialog = false }, theme)
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel", color = theme.buttonOp) }
            }
        )
    }
}

@Composable
fun ThemeOption(name: String, onClick: () -> Unit, theme: AppTheme) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = name,
            color = theme.textPrimary,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Composable
fun CalculatorButton(
    label: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val animateBg by animateColorAsState(targetValue = backgroundColor, animationSpec = tween(300))
    val animateText by animateColorAsState(targetValue = textColor, animationSpec = tween(300))

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(35))
            .background(animateBg)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable { onClick() }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (isSmall) 26.sp else 36.sp,
            fontWeight = FontWeight.Medium,
            color = animateText
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: CalculatorViewModel, theme: AppTheme, onBack: () -> Unit) {
    val items by viewModel.history.collectAsState()
    var selectedHistoryItem by remember { mutableStateOf<String?>(null) }
    var stepDetails by remember { mutableStateOf<List<String>>(emptyList()) }

    if (selectedHistoryItem != null) {
        AlertDialog(
            onDismissRequest = { selectedHistoryItem = null },
            containerColor = theme.buttonNum,
            titleContentColor = theme.textPrimary,
            textContentColor = theme.textPrimary,
            title = { Text("BODMAS Breakdown") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (stepDetails.isEmpty()) {
                        item { Text("No steps available.", color = theme.textSecondary) }
                    } else {
                        items(stepDetails) { step ->
                            Text(
                                text = step,
                                fontSize = 22.sp,
                                color = theme.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedHistoryItem = null }) {
                    Text("Close", color = theme.buttonOp)
                }
            }
        )
    }

    Scaffold(
        containerColor = theme.background,
        topBar = {
            TopAppBar(
                title = { Text("History", color = theme.textPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.background,
                    navigationIconContentColor = theme.textPrimary,
                    actionIconContentColor = theme.textPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Use the Back Arrow here
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::clearHistory) {
                        Text("Clear", color = theme.buttonOp)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (items.isEmpty()) {
                Text("No history", modifier = Modifier.align(Alignment.Center), color = theme.textSecondary, fontSize = 18.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items) { entry ->
                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                stepDetails = viewModel.calculateSteps(entry)
                                selectedHistoryItem = entry
                            }
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(entry, color = theme.textPrimary, fontSize = 24.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 8.dp),
                                    thickness = 0.5.dp,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}