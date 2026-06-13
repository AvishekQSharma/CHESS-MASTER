package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.chess.database.ChessDatabase
import com.example.chess.database.ChessRepository
import com.example.chess.view.ChessScreen
import com.example.chess.viewmodel.ChessViewModel
import com.example.chess.viewmodel.ChessViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ChessDatabase.getDatabase(this)
        val repository = ChessRepository(database.savedGameDao())

        val viewModel: ChessViewModel by viewModels {
            ChessViewModelFactory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                ChessScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
