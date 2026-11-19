package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ExpenseScreen
import com.example.ui.theme.KharchuTheme
import com.example.ui.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeColor by expenseViewModel.selectedThemeColor.collectAsStateWithLifecycle()
            KharchuTheme(themeColor = themeColor) {
                ExpenseScreen(
                    viewModel = expenseViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
