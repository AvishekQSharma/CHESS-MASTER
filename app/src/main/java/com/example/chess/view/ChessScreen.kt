package com.example.chess.view

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.database.SavedGame
import com.example.chess.viewmodel.AIDifficulty
import com.example.chess.viewmodel.ChessUiState
import com.example.chess.viewmodel.ChessViewModel
import com.example.chess.viewmodel.PlayMode
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(
    viewModel: ChessViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Chess Master",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("saved_games_button")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Saved Matches")
                    }
                    IconButton(
                        onClick = { viewModel.resetGame() },
                        modifier = Modifier.testTag("reset_game_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Match")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    )
                )
        ) {
            val isWideScreen = maxWidth > 600.dp
            
            if (isWideScreen) {
                // Wide tablet layout (Side-by-side split pane)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Board and Match Indicator
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MatchIndicatorCard(uiState)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            ChessboardWidget(
                                uiState = uiState,
                                onSquareSelect = { viewModel.selectSquare(it) }
                            )
                        }
                    }

                    // Right Column: Controls, Moves list, & AI Coach
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GameSettingsCard(
                            uiState = uiState,
                            onPlayModeChange = { viewModel.setPlayMode(it) },
                            onDifficultyChange = { viewModel.setDifficulty(it) }
                        )

                        ActionToolbar(
                            uiState = uiState,
                            onUndo = { viewModel.undo() },
                            onRedo = { viewModel.redo() },
                            onSave = {
                                saveTitleInput = ""
                                showSaveDialog = true
                            }
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                MoveHistoryCard(uiState)
                            }
                            Box(modifier = Modifier.weight(1.2f)) {
                                AiCoachCard(
                                    uiState = uiState,
                                    onRequestFeedback = { viewModel.requestCoachFeedback() },
                                    onDismissFeedback = { viewModel.dismissCoachFeedback() }
                                )
                            }
                        }
                    }
                }
            } else {
                // Compact phone layout (Vertical stacked design)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()), // support small height panels
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MatchIndicatorCard(uiState)
                    
                    ChessboardWidget(
                        uiState = uiState,
                        onSquareSelect = { viewModel.selectSquare(it) }
                    )

                    ActionToolbar(
                        uiState = uiState,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onSave = {
                            saveTitleInput = ""
                            showSaveDialog = true
                        }
                    )

                    GameSettingsCard(
                        uiState = uiState,
                        onPlayModeChange = { viewModel.setPlayMode(it) },
                        onDifficultyChange = { viewModel.setDifficulty(it) }
                    )

                    AiCoachCard(
                        uiState = uiState,
                        onRequestFeedback = { viewModel.requestCoachFeedback() },
                        onDismissFeedback = { viewModel.dismissCoachFeedback() }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        MoveHistoryCard(uiState)
                    }
                }
            }
        }
        
        // --- Pawn Promotion Dialog ---
        if (uiState.promotionFrom != null && uiState.promotionTo != null) {
            PromotionSelectionDialog(
                color = uiState.activeColor,
                onSelect = { viewModel.promotePawn(it) },
                onDismiss = { viewModel.cancelPromotion() }
            )
        }

        // --- Save Game Dialog ---
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Game Match") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter a memorable name or title for this match:")
                        OutlinedTextField(
                            value = saveTitleInput,
                            onValueChange = { saveTitleInput = it },
                            placeholder = { Text("E.g. Sicilian Defense analysis") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_title_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveGame(saveTitleInput)
                            showSaveDialog = false
                        },
                        modifier = Modifier.testTag("save_confirm_button")
                    ) {
                        Text("Save Match")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Saved Matches History Dialog ---
        if (showHistoryDialog) {
            SavedGamesDialog(
                savedGames = uiState.savedGames,
                onSelect = {
                    viewModel.loadGame(it)
                    showHistoryDialog = false
                },
                onDelete = { viewModel.deleteGame(it.id) },
                onDismiss = { showHistoryDialog = false }
            )
        }
    }
}

@Composable
fun MatchIndicatorCard(uiState: ChessUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                uiState.isCheckmate -> MaterialTheme.colorScheme.errorContainer
                uiState.isStalemate -> MaterialTheme.colorScheme.secondaryContainer
                uiState.isCheck -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val bannerTitle = when {
                    uiState.isCheckmate -> "Checkmate! 🏆"
                    uiState.isStalemate -> "Stalemate 🤝"
                    uiState.isCheck -> "Check! ⚠️"
                    else -> if (uiState.activeColor == PieceColor.WHITE) "White to Move" else "Black to Move"
                }
                
                val bannerSub = when {
                    uiState.isCheckmate -> "Winner: " + uiState.activeColor.opponent().name
                    uiState.isStalemate -> "Match Drawn. No legal moves available."
                    uiState.isCheck -> "The king is under direct attack!"
                    uiState.isAiThinking -> "Computer AI is calculating moves..."
                    else -> "Move your pieces carefully."
                }

                Text(
                    text = bannerTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = contentColorFor(backgroundColor = CardDefaults.cardColors().containerColor)
                )
                Text(
                    text = bannerSub,
                    fontSize = 13.sp,
                    color = contentColorFor(backgroundColor = CardDefaults.cardColors().containerColor).copy(alpha = 0.8f)
                )
            }
            
            // Turn indicator icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (uiState.activeColor == PieceColor.WHITE) Color.White else Color.Black,
                        shape = CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isAiThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = if (uiState.activeColor == PieceColor.WHITE) "W" else "B",
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.activeColor == PieceColor.WHITE) Color.Black else Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChessboardWidget(
    uiState: ChessUiState,
    onSquareSelect: (Position) -> Unit
) {
    // Elegant wooden-clay board visual theme
    val lightSquareColor = Color(0xFFF0D9B5)
    val darkSquareColor = Color(0xFFB58863)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(4.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .testTag("chessboard")
    ) {
        for (row in 0..7) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..7) {
                    val pos = Position(row, col)
                    val piece = uiState.board[pos]
                    val isLight = (row + col) % 2 == 0
                    val baseColor = if (isLight) lightSquareColor else darkSquareColor
                    
                    // Highlights
                    val isSelected = uiState.selectedSquare == pos
                    val isValidTarget = uiState.validMoves.contains(pos)
                    val isKingInThreat = uiState.isCheck && piece?.type == PieceType.KING && piece.color == uiState.activeColor

                    val tileColor = when {
                        isSelected -> Color(0xAAFFF176) // Bright soft yellow for selection
                        isKingInThreat -> Color(0xAAFF8A80) // High visibility red alert for matching king
                        isValidTarget && piece != null -> Color(0xAA80F1D5) // Soft green for capture destinations
                        else -> baseColor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(tileColor)
                            .clickable { onSquareSelect(pos) }
                            .testTag("tile_${row}_${col}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Legal move circle dot for empty squares
                        if (isValidTarget && piece == null) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(Color(0x7700B0FF), CircleShape)
                            )
                        }

                        // Piece text rendering
                        if (piece != null) {
                            val pieceColor = if (piece.color == PieceColor.WHITE) {
                                Color(0xFFFFFDF5) // Ivory white
                            } else {
                                Color(0xFF15181F) // Coal black
                            }
                            
                            // Glowing aura for selecting piece
                            Text(
                                text = piece.getSymbol(),
                                fontSize = 36.sp,
                                color = pieceColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionToolbar(
    uiState: ChessUiState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onUndo,
            enabled = uiState.isUndoAvailable && !uiState.isAiThinking,
            modifier = Modifier
                .weight(1f)
                .testTag("undo_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Undo")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Undo")
        }

        Button(
            onClick = onRedo,
            enabled = uiState.isRedoAvailable && !uiState.isAiThinking,
            modifier = Modifier
                .weight(1f)
                .testTag("redo_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text("Redo")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = "Redo")
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1.2f)
                .testTag("save_game_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.Done, contentDescription = "Save Match")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save")
        }
    }
}

@Composable
fun GameSettingsCard(
    uiState: ChessUiState,
    onPlayModeChange: (PlayMode) -> Unit,
    onDifficultyChange: (AIDifficulty) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Match Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Play mode Selector tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    Pair(PlayMode.VS_AI, "Computer AI 🤖"),
                    Pair(PlayMode.PVP, "Local PVP 👥")
                ).forEach { (mode, label) ->
                    val isSelected = uiState.playMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onPlayModeChange(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // AI Difficulty Slider (only visible in VS AI mode)
            if (uiState.playMode == PlayMode.VS_AI) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("AI Difficulty:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = uiState.aiDifficulty.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AIDifficulty.values().forEach { diff ->
                            val isSelected = uiState.aiDifficulty == diff
                            Button(
                                onClick = { onDifficultyChange(diff) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                Text(diff.name, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiCoachCard(
    uiState: ChessUiState,
    onRequestFeedback: () -> Unit,
    onDismissFeedback: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "AI Coach",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Chess Coach",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (uiState.coachFeedback != null) {
                    IconButton(
                        onClick = onDismissFeedback,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = Triple(uiState.isCoachThinking, uiState.coachFeedback, uiState.moveHistory.isEmpty()),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "CoachContent"
            ) { (isThinking, feedback, emptyHistory) ->
                when {
                    isThinking -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text(
                                "Analyzing grandmaster tactics...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    feedback != null -> {
                        Text(
                            text = feedback,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (emptyHistory) {
                                    "Start the game! Once the match has begun, tap below to ask the grandmaster coach for feedback."
                                } else {
                                    "Tap below to analyze options, identify tactical threats, or look for winning ideas."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Button(
                                onClick = onRequestFeedback,
                                enabled = !emptyHistory,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ask_coach_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Face, contentDescription = "Coach Action", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask Coach Feedback")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoveHistoryCard(uiState: ChessUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Move Log",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.moveHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No moves recorded.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Display moves grouped into Standard turns: 1. e4 e5
                    val pairs = uiState.moveHistory.chunked(2)
                    items(pairs.size) { index ->
                        val pair = pairs[index]
                        val whiteMove = pair.getOrNull(0)?.toSan() ?: ""
                        val blackMove = pair.getOrNull(1)?.toSan() ?: ""
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(
                                    if (index % 2 == 0) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                    else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(
                                text = whiteMove,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = blackMove,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionSelectionDialog(
    color: PieceColor,
    onSelect: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Promote Pawn",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val pieceSelections = listOf(
                        PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
                    )

                    pieceSelections.forEach { type ->
                        val dummyPiece = com.example.chess.model.ChessPiece(type, color)
                        
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(type) }
                                .padding(8.dp)
                                .testTag("promote_to_${type.name}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = dummyPiece.getSymbol(),
                                fontSize = 32.sp,
                                color = if (color == PieceColor.WHITE) Color.Black else Color.Gray
                            )
                            Text(
                                text = type.name.lowercase().capitalize(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun SavedGamesDialog(
    savedGames: List<SavedGame>,
    onSelect: (SavedGame) -> Unit,
    onDelete: (SavedGame) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(320.dp)
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Matches",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (savedGames.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No saved matches found.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedGames) { game ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelect(game) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    val dateStr = java.text.DateFormat.getDateTimeInstance(
                                        java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
                                    ).format(java.util.Date(game.timestamp))
                                    Text(
                                        text = dateStr,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onDelete(game) },
                                    modifier = Modifier.size(32.dp).testTag("delete_saved_${game.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Saved Match",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


