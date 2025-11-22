# Calculator App

A modern, feature-rich Calculator application built for Android using **Kotlin** and **Jetpack Compose**. This app goes beyond basic arithmetic, offering scientific functions, history tracking with step-by-step breakdowns, and customizable themes.

## Features

- **Standard & Scientific Modes**:
  - Basic arithmetic (+, -, ×, ÷, %)
  - Scientific operations (sin, cos, tan, log, √)
  - Parentheses support for complex expressions
  - Expandable keypad for advanced functions
- **Smart Input**:
  - Multi-line input with vertical scrolling
  - Cursor support for easy editing
  - Real-time result preview
- **History & Breakdown**:
  - Saves calculation history
  - **BODMAS Breakdown**: Tap on any history item to see a step-by-step explanation of how the result was calculated.
- **Theming**:
  - **Classic Dark**: Easy on the eyes.
  - **Clean Light**: Bright and crisp.
  - **Cyberpunk**: A vibrant, futuristic look.
  - Smooth animations when switching themes.
- **Modern UI/UX**:
  - Material 3 Design
  - Haptic feedback
  - Fluid animations

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Jetpack Compose Navigation
- **State Management**: Kotlin Coroutines & StateFlow

## Installation

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/amohammedhayath/CalculatorApp.git
    ```
2.  **Open in Android Studio**:
    - Launch Android Studio.
    - Select "Open" and navigate to the cloned directory.
3.  **Build and Run**:
    - Wait for Gradle sync to complete.
    - Connect an Android device or start an emulator.
    - Click the **Run** button (green play icon).

## Usage

1.  **Basic Calculations**: Use the number pad and operators to perform calculations.
2.  **Scientific Mode**: Tap the `< />` button to expand the keypad and access scientific functions like sin, cos, tan, etc.
3.  **History**: Tap the clock icon in the top-left corner to view your calculation history.
    - **View Steps**: Tap on a history item to see the step-by-step BODMAS breakdown.
    - **Clear History**: Tap the "Clear" button in the history screen to remove all entries.
4.  **Change Theme**: Tap the three-dot menu in the top-right corner and select "Choose Theme". Pick your preferred look.

## Contributing

Contributions are welcome! If you have ideas for improvements or find bugs, please feel free to:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/YourFeature`).
3.  Commit your changes (`git commit -m 'Add some feature'`).
4.  Push to the branch (`git push origin feature/YourFeature`).
5.  Open a Pull Request.

## License

This project is open source and available under the [MIT License](LICENSE).
