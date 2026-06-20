package com.bookkeeping.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bookkeeping.app.ui.screen.transactions.TransactionListScreen
import com.bookkeeping.app.ui.theme.BookkeepingAppTheme
import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint = 让 Hilt 能给这个 Activity 及其下属 Composable 注入依赖
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BookkeepingAppTheme {
                TransactionListScreen()
            }
        }
    }
}
