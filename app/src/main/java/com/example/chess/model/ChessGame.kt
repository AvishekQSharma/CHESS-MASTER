package com.example.chess.model

import java.util.UUID
import kotlin.math.abs

class ChessGame {
    var board: Map<Position, ChessPiece> = emptyMap()
        private set
    var activeColor: PieceColor = PieceColor.WHITE
        private set
    var lastMove: ChessMove? = null
        private set
    
    private val moveHistory = mutableListOf<ChessMove>()
    private val undoStack = mutableListOf<MoveRecord>()
    private val redoStack = mutableListOf<MoveRecord>()

    private data class MoveRecord(
        val move: ChessMove,
        val boardBefore: Map<Position, ChessPiece>,
        val activeColorBefore: PieceColor,
        val lastMoveBefore: ChessMove?
    )

    init {
        resetGame()
    }

    fun resetGame() {
        board = createInitialBoard()
        activeColor = PieceColor.WHITE
        lastMove = null
        moveHistory.clear()
        undoStack.clear()
        redoStack.clear()
    }

    private fun createInitialBoard(): Map<Position, ChessPiece> {
        val b = mutableMapOf<Position, ChessPiece>()
        
        // Rows 0 and 1: Black
        b[Position(0, 0)] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        b[Position(0, 1)] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        b[Position(0, 2)] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        b[Position(0, 3)] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
        b[Position(0, 4)] = ChessPiece(PieceType.KING, PieceColor.BLACK)
        b[Position(0, 5)] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        b[Position(0, 6)] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        b[Position(0, 7)] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        for (col in 0..7) {
            b[Position(1, col)] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)
        }

        // Rows 6 and 7: White
        for (col in 0..7) {
            b[Position(6, col)] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        }
        b[Position(7, 0)] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        b[Position(7, 1)] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        b[Position(7, 2)] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        b[Position(7, 3)] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
        b[Position(7, 4)] = ChessPiece(PieceType.KING, PieceColor.WHITE)
        b[Position(7, 5)] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        b[Position(7, 6)] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        b[Position(7, 7)] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)

        return b
    }

    fun getHistory(): List<ChessMove> = moveHistory

    fun isUndoAvailable(): Boolean = undoStack.isNotEmpty()
    fun isRedoAvailable(): Boolean = redoStack.isNotEmpty()

    fun undo(): Boolean {
        if (!isUndoAvailable()) return false
        val record = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(MoveRecord(record.move, board, activeColor, lastMove))
        
        board = record.boardBefore
        activeColor = record.activeColorBefore
        lastMove = record.lastMoveBefore
        moveHistory.removeLastOrNull()
        return true
    }

    fun redo(): Boolean {
        if (!isRedoAvailable()) return false
        val record = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(MoveRecord(record.move, board, activeColor, lastMove))
        
        // Reapply the move
        board = applyMoveToBoard(board, record.move)
        activeColor = activeColor.opponent()
        lastMove = record.move
        moveHistory.add(record.move)
        return true
    }

    /**
     * Public method to make a move. Ensures the move is legal before executing,
     * and performs en passant, castling, promotion, etc.
     */
    fun makeMove(move: ChessMove): Boolean {
        val legalMoves = getLegalMoves(move.from)
        val validMove = legalMoves.find { it.to == move.to && it.promotedTo == move.promotedTo }
        
        if (validMove != null) {
            // Save state for undo
            val record = MoveRecord(validMove, board, activeColor, lastMove)
            undoStack.add(record)
            redoStack.clear() // Clear redo on any new move

            // Apply move
            board = applyMoveToBoard(board, validMove)
            lastMove = validMove
            moveHistory.add(validMove)
            activeColor = activeColor.opponent()
            return true
        }
        return false
    }

    fun getLegalMoves(pos: Position): List<ChessMove> {
        val piece = board[pos] ?: return emptyList()
        if (piece.color != activeColor) return emptyList()

        val pseudoMoves = getPseudoLegalMoves(board, pos)
        // Filter pseudo-legal moves that would leave the king in check
        return pseudoMoves.filter { move ->
            val simulatedBoard = applyMoveToBoard(board, move)
            !isKingInCheck(simulatedBoard, piece.color)
        }
    }

    fun getAllLegalMoves(color: PieceColor): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = board[pos]
                if (piece != null && piece.color == color) {
                    moves.addAll(getLegalMoves(pos))
                }
            }
        }
        return moves
    }

    fun isCheck(): Boolean {
        return isKingInCheck(board, activeColor)
    }

    fun isCheckmate(): Boolean {
        if (!isCheck()) return false
        return getAllLegalMoves(activeColor).isEmpty()
    }

    fun isStalemate(): Boolean {
        if (isCheck()) return false
        return getAllLegalMoves(activeColor).isEmpty()
    }

    // --- Core Engine Checking Functions ---

    private fun isKingInCheck(b: Map<Position, ChessPiece>, color: PieceColor): Boolean {
        // Find king
        val kingPos = b.entries.find { it.value.type == PieceType.KING && it.value.color == color }?.key
            ?: return false
        
        val opponentColor = color.opponent()
        // Check if any opponent piece can capture the king square
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = b[pos]
                if (piece != null && piece.color == opponentColor) {
                    val pseudoMoves = getPseudoLegalMoves(b, pos, skipCastlingCheck = true)
                    if (pseudoMoves.any { it.to == kingPos }) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getPseudoLegalMoves(
        b: Map<Position, ChessPiece>,
        from: Position,
        skipCastlingCheck: Boolean = false
    ): List<ChessMove> {
        val piece = b[from] ?: return emptyList()
        val moves = mutableListOf<ChessMove>()
        val row = from.row
        val col = from.col

        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
                val promoRow = if (piece.color == PieceColor.WHITE) 0 else 7

                // 1 step forward
                val next1 = Position(row + dir, col)
                if (next1.isValid() && !b.containsKey(next1)) {
                    if (next1.row == promoRow) {
                        // Pawn promotion
                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { type ->
                            moves.add(ChessMove(piece, from, next1, isPromotion = true, promotedTo = type))
                        }
                    } else {
                        moves.add(ChessMove(piece, from, next1))
                    }

                    // 2 steps forward
                    val next2 = Position(row + dir * 2, col)
                    if (row == startRow && !b.containsKey(next2)) {
                        moves.add(ChessMove(piece, from, next2))
                    }
                }

                // Standard diagonal captures
                for (dCol in listOf(-1, 1)) {
                    val capturePos = Position(row + dir, col + dCol)
                    if (capturePos.isValid()) {
                        val capPiece = b[capturePos]
                        if (capPiece != null && capPiece.color != piece.color) {
                            if (capturePos.row == promoRow) {
                                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { type ->
                                    moves.add(ChessMove(piece, from, capturePos, isCapture = true, capturedPiece = capPiece, isPromotion = true, promotedTo = type))
                                }
                            } else {
                                moves.add(ChessMove(piece, from, capturePos, isCapture = true, capturedPiece = capPiece))
                            }
                        }
                    }
                }

                // En Passant
                val epRow = if (piece.color == PieceColor.WHITE) 3 else 4
                if (row == epRow && lastMove != null) {
                    val prevMove = lastMove!!
                    if (prevMove.piece.type == PieceType.PAWN &&
                        abs(prevMove.from.row - prevMove.to.row) == 2 &&
                        prevMove.to.row == epRow &&
                        abs(prevMove.to.col - col) == 1
                    ) {
                        val capturePos = Position(row + dir, prevMove.to.col)
                        moves.add(
                            ChessMove(
                                piece = piece,
                                from = from,
                                to = capturePos,
                                isCapture = true,
                                capturedPiece = prevMove.piece,
                                isEnPassant = true
                            )
                        )
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1),
                    Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2),
                    Pair(2, -1), Pair(2, 1)
                )
                for (offset in offsets) {
                    val to = Position(row + offset.first, col + offset.second)
                    if (to.isValid()) {
                        val destPiece = b[to]
                        if (destPiece == null) {
                            moves.add(ChessMove(piece, from, to))
                        } else if (destPiece.color != piece.color) {
                            moves.add(ChessMove(piece, from, to, isCapture = true, capturedPiece = destPiece))
                        }
                    }
                }
            }

            PieceType.BISHOP -> {
                addSlidingMoves(b, from, piece, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), moves)
            }

            PieceType.ROOK -> {
                addSlidingMoves(b, from, piece, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), moves)
            }

            PieceType.QUEEN -> {
                addSlidingMoves(b, from, piece, listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1)), moves)
            }

            PieceType.KING -> {
                val directions = listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1))
                for (dir in directions) {
                    val to = Position(row + dir.first, col + dir.second)
                    if (to.isValid()) {
                        val destPiece = b[to]
                        if (destPiece == null) {
                            moves.add(ChessMove(piece, from, to))
                        } else if (destPiece.color != piece.color) {
                            moves.add(ChessMove(piece, from, to, isCapture = true, capturedPiece = destPiece))
                        }
                    }
                }

                // Castling Enforcements
                if (!skipCastlingCheck && !piece.hasMoved && !isKingInCheck(b, piece.color)) {
                    val kingRow = if (piece.color == PieceColor.WHITE) 7 else 0
                    
                    // King-side castling (Rook is at col 7)
                    val rRookPos = Position(kingRow, 7)
                    val rRook = b[rRookPos]
                    if (rRook != null && rRook.type == PieceType.ROOK && !rRook.hasMoved) {
                        val pathClear = (5..6).all { !b.containsKey(Position(kingRow, it)) }
                        if (pathClear) {
                            // Squares king moves through (col 4, 5, 6) must not be under attack
                            val safetyClear = (4..6).none { colIndex ->
                                val testBoard = applyMoveToBoard(b, ChessMove(piece, from, Position(kingRow, colIndex)))
                                isKingInCheck(testBoard, piece.color)
                            }
                            if (safetyClear) {
                                moves.add(
                                    ChessMove(
                                        piece = piece,
                                        from = from,
                                        to = Position(kingRow, 6),
                                        isCastlingKingSide = true,
                                        originalRookHasMoved = rRook.hasMoved
                                    )
                                )
                            }
                        }
                    }

                    // Queen-side castling (Rook is at col 0)
                    val lRookPos = Position(kingRow, 0)
                    val lRook = b[lRookPos]
                    if (lRook != null && lRook.type == PieceType.ROOK && !lRook.hasMoved) {
                        val pathClear = (1..3).all { !b.containsKey(Position(kingRow, it)) }
                        if (pathClear) {
                            // Squares king moves through (col 2, 3, 4) must not be under attack
                            val safetyClear = (2..4).none { colIndex ->
                                val testBoard = applyMoveToBoard(b, ChessMove(piece, from, Position(kingRow, colIndex)))
                                isKingInCheck(testBoard, piece.color)
                            }
                            if (safetyClear) {
                                moves.add(
                                    ChessMove(
                                        piece = piece,
                                        from = from,
                                        to = Position(kingRow, 2),
                                        isCastlingQueenSide = true,
                                        originalRookHasMoved = lRook.hasMoved
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addSlidingMoves(
        b: Map<Position, ChessPiece>,
        from: Position,
        piece: ChessPiece,
        dirs: List<Pair<Int, Int>>,
        outMoves: MutableList<ChessMove>
    ) {
        for (dir in dirs) {
            var step = 1
            while (true) {
                val to = Position(from.row + dir.first * step, from.col + dir.second * step)
                if (!to.isValid()) break
                val destPiece = b[to]
                if (destPiece == null) {
                    outMoves.add(ChessMove(piece, from, to))
                } else {
                    if (destPiece.color != piece.color) {
                        outMoves.add(ChessMove(piece, from, to, isCapture = true, capturedPiece = destPiece))
                    }
                    break // Blocked
                }
                step++
            }
        }
    }

    private fun applyMoveToBoard(b: Map<Position, ChessPiece>, move: ChessMove): Map<Position, ChessPiece> {
        val nextBoard = b.toMutableMap()
        
        // Remove old position
        val piece = nextBoard.remove(move.from) ?: return b
        
        // Handle castling rook movement
        if (move.isCastlingKingSide) {
            val rook = nextBoard.remove(Position(move.from.row, 7))
            if (rook != null) {
                nextBoard[Position(move.from.row, 5)] = rook.copy(hasMoved = true)
            }
        } else if (move.isCastlingQueenSide) {
            val rook = nextBoard.remove(Position(move.from.row, 0))
            if (rook != null) {
                nextBoard[Position(move.from.row, 3)] = rook.copy(hasMoved = true)
            }
        }
        
        // Handle en passant capture removal
        if (move.isEnPassant) {
            nextBoard.remove(Position(move.from.row, move.to.col))
        }

        // Handle promotion or basic move
        val finalPiece = if (move.isPromotion && move.promotedTo != null) {
            ChessPiece(move.promotedTo, piece.color, hasMoved = true)
        } else {
            piece.copy(hasMoved = true)
        }

        nextBoard[move.to] = finalPiece
        return nextBoard
    }
}
