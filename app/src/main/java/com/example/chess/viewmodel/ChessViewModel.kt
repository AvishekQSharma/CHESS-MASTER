package com.example.chess.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chess.api.ChessCoachService
import com.example.chess.database.ChessDatabase
import com.example.chess.database.ChessRepository
import com.example.chess.database.SavedGame
import com.example.chess.model.ChessEngine
import com.example.chess.model.ChessGame
import com.example.chess.model.ChessMove
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class PlayMode {
    PVP, VS_AI
}

enum class AIDifficulty(val depth: Int) {
    EASY(1), MEDIUM(2), HARD(3)
}

data class ChessUiState(
    val board: Map<Position, ChessPiece> = emptyMap(),
    val activeColor: PieceColor = PieceColor.WHITE,
    val selectedSquare: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val moveHistory: List<ChessMove> = emptyList(),
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val playMode: PlayMode = PlayMode.VS_AI,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val isAiThinking: Boolean = false,
    
    // AI Coach advice state
    val coachFeedback: String? = null,
    val isCoachThinking: Boolean = false,
    
    // Promotion pending state
    val promotionFrom: Position? = null,
    val promotionTo: Position? = null,
    
    // Undo/Redo availabilities
    val isUndoAvailable: Boolean = false,
    val isRedoAvailable: Boolean = false,

    // Database listing
    val savedGames: List<SavedGame> = emptyList()
)

class ChessViewModel(
    application: Application,
    private val repository: ChessRepository
) : AndroidViewModel(application) {

    private val game = ChessGame()

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    init {
        // Observe database list of saved matches
        viewModelScope.launch {
            repository.allSavedGames.collectLatest { list ->
                _uiState.value = _uiState.value.copy(savedGames = list)
            }
        }
        syncGameState()
    }

    fun resetGame() {
        game.resetGame()
        _uiState.value = _uiState.value.copy(
            selectedSquare = null,
            validMoves = emptyList(),
            coachFeedback = null,
            promotionFrom = null,
            promotionTo = null
        )
        syncGameState()
    }

    fun setPlayMode(mode: PlayMode) {
        if (_uiState.value.playMode != mode) {
            _uiState.value = _uiState.value.copy(playMode = mode)
            resetGame()
        }
    }

    fun setDifficulty(diff: AIDifficulty) {
        _uiState.value = _uiState.value.copy(aiDifficulty = diff)
    }

    fun selectSquare(pos: Position) {
        // Prevent action if game is over or AI is thinking
        if (_uiState.value.isCheckmate || _uiState.value.isStalemate || _uiState.value.isAiThinking) return

        val state = _uiState.value
        val piece = state.board[pos]

        if (state.selectedSquare == null) {
            // Select piece
            if (piece != null && piece.color == state.activeColor) {
                val legalMoves = game.getLegalMoves(pos)
                _uiState.value = _uiState.value.copy(
                    selectedSquare = pos,
                    validMoves = legalMoves.map { it.to }
                )
            }
        } else {
            val from = state.selectedSquare
            val destinationLegalMoves = game.getLegalMoves(from)
            val move = destinationLegalMoves.find { it.to == pos }

            if (move != null) {
                // If it is a promotion move, trigger selection dialog
                if (move.isPromotion) {
                    _uiState.value = _uiState.value.copy(
                        promotionFrom = from,
                        promotionTo = pos
                    )
                } else {
                    // Make the regular move
                    executeMove(move)
                }
            } else {
                // Change selection or deselect
                if (piece != null && piece.color == state.activeColor) {
                    val legalMoves = game.getLegalMoves(pos)
                    _uiState.value = _uiState.value.copy(
                        selectedSquare = pos,
                        validMoves = legalMoves.map { it.to }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        selectedSquare = null,
                        validMoves = emptyList()
                    )
                }
            }
        }
    }

    fun promotePawn(type: PieceType) {
        val from = _uiState.value.promotionFrom ?: return
        val to = _uiState.value.promotionTo ?: return

        val legal = game.getLegalMoves(from)
        val pMove = legal.find { it.to == to && it.promotedTo == type }
        if (pMove != null) {
            _uiState.value = _uiState.value.copy(
                promotionFrom = null,
                promotionTo = null
            )
            executeMove(pMove)
        }
    }

    fun cancelPromotion() {
        _uiState.value = _uiState.value.copy(
            promotionFrom = null,
            promotionTo = null,
            selectedSquare = null,
            validMoves = emptyList()
        )
    }

    private fun executeMove(move: ChessMove) {
        val success = game.makeMove(move)
        if (success) {
            _uiState.value = _uiState.value.copy(
                selectedSquare = null,
                validMoves = emptyList(),
                coachFeedback = null // clear coach feedback on new move
            )
            syncGameState()

            // Trigger AI if it's VS AI mode and game is not over
            if (_uiState.value.playMode == PlayMode.VS_AI &&
                !game.isCheckmate() && !game.isStalemate() &&
                game.activeColor == PieceColor.BLACK
            ) {
                triggerAiMove()
            }
        }
    }

    private fun triggerAiMove() {
        _uiState.value = _uiState.value.copy(isAiThinking = true)
        viewModelScope.launch {
            val bestMove = withContext(Dispatchers.Default) {
                ChessEngine.findBestMove(game, _uiState.value.aiDifficulty.depth)
            }
            _uiState.value = _uiState.value.copy(isAiThinking = false)
            if (bestMove != null) {
                game.makeMove(bestMove)
                syncGameState()
            }
        }
    }

    fun undo() {
        if (game.undo()) {
            // For VS AI, undoing should revert BOTH Black's AI move and White's preceding move
            if (_uiState.value.playMode == PlayMode.VS_AI && game.activeColor == PieceColor.BLACK) {
                game.undo()
            }
            _uiState.value = _uiState.value.copy(
                selectedSquare = null,
                validMoves = emptyList(),
                coachFeedback = null
            )
            syncGameState()
        }
    }

    fun redo() {
        if (game.redo()) {
            if (_uiState.value.playMode == PlayMode.VS_AI && game.activeColor == PieceColor.WHITE) {
                game.redo()
            }
            _uiState.value = _uiState.value.copy(
                selectedSquare = null,
                validMoves = emptyList(),
                coachFeedback = null
            )
            syncGameState()
        }
    }

    fun requestCoachFeedback() {
        _uiState.value = _uiState.value.copy(isCoachThinking = true, coachFeedback = null)
        viewModelScope.launch {
            val currentBoard = game.board
            val active = game.activeColor
            val history = game.getHistory()
            val feedback = ChessCoachService.getCoachFeedback(currentBoard, active, history)
            _uiState.value = _uiState.value.copy(
                coachFeedback = feedback,
                isCoachThinking = false
            )
        }
    }

    fun dismissCoachFeedback() {
        _uiState.value = _uiState.value.copy(coachFeedback = null)
    }

    private fun syncGameState() {
        _uiState.value = _uiState.value.copy(
            board = game.board,
            activeColor = game.activeColor,
            moveHistory = game.getHistory(),
            isCheck = game.isCheck(),
            isCheckmate = game.isCheckmate(),
            isStalemate = game.isStalemate(),
            isUndoAvailable = game.isUndoAvailable(),
            isRedoAvailable = game.isRedoAvailable()
        )
    }

    // --- Save and Load Game State using Room ---

    fun saveGame(title: String) {
        val formattedTitle = if (title.trim().isEmpty()) {
            "Match on " + java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
            ).format(java.util.Date())
        } else title

        viewModelScope.launch {
            // Serialize moves history to simple CSV entries
            val history = game.getHistory()
            val movesCsv = history.joinToString(";") { m ->
                val promoType = m.promotedTo?.name ?: "NONE"
                "${m.from.row},${m.from.col},${m.to.row},${m.to.col},$promoType"
            }

            val savedGameRecord = SavedGame(
                id = UUID.randomUUID().toString(),
                title = formattedTitle,
                timestamp = System.currentTimeMillis(),
                movesCsv = movesCsv
            )
            repository.saveGame(savedGameRecord)
        }
    }

    fun loadGame(savedGame: SavedGame) {
        game.resetGame()
        _uiState.value = _uiState.value.copy(
            selectedSquare = null,
            validMoves = emptyList(),
            coachFeedback = null,
            promotionFrom = null,
            promotionTo = null
        )

        // Parse CSV moves and reapply them to reconstruct Board
        if (savedGame.movesCsv.isNotEmpty()) {
            val tokens = savedGame.movesCsv.split(";")
            for (token in tokens) {
                if (token.isEmpty()) continue
                val parts = token.split(",")
                if (parts.size >= 5) {
                    val fromRow = parts[0].toIntOrNull() ?: continue
                    val fromCol = parts[1].toIntOrNull() ?: continue
                    val toRow = parts[2].toIntOrNull() ?: continue
                    val toCol = parts[3].toIntOrNull() ?: continue
                    val promoStr = parts[4]

                    val from = Position(fromRow, fromCol)
                    val to = Position(toRow, toCol)
                    val promoType = if (promoStr != "NONE") PieceType.valueOf(promoStr) else null

                    // Find corresponding legal move
                    val moves = game.getLegalMoves(from)
                    val match = moves.find { it.to == to && it.promotedTo == promoType }
                    if (match != null) {
                        game.makeMove(match)
                    } else {
                        // Edge case fallback: construct raw move if engine state differed
                        val rawPiece = game.board[from]
                        if (rawPiece != null) {
                            val dummyMove = ChessMove(
                                piece = rawPiece,
                                from = from,
                                to = to,
                                isPromotion = promoType != null,
                                promotedTo = promoType
                            )
                            game.makeMove(dummyMove)
                        }
                    }
                }
            }
        }
        syncGameState()
    }

    fun deleteGame(id: String) {
        viewModelScope.launch {
            repository.deleteGame(id)
        }
    }
}

class ChessViewModelFactory(
    private val application: Application,
    private val repository: ChessRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChessViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
