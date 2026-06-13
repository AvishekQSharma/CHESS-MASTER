package com.example.chess.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

object ChessEngine {

    // Piece weights
    private val PIECE_VALUES = mapOf(
        PieceType.PAWN to 100,
        PieceType.KNIGHT to 320,
        PieceType.BISHOP to 330,
        PieceType.ROOK to 500,
        PieceType.QUEEN to 900,
        PieceType.KING to 20000
    )

    // Positional evaluation tables (From perspective of White. Will be mirrored for Black).
    // Row 0 is Rank 8 (Black side), Row 7 is Rank 1 (White side).
    private val pawnTable = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    )

    private val knightTable = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val bishopTable = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val rookTable = intArrayOf(
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0
    )

    private val queenTable = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  5,-10,
        -10,  0,  5,  0,  0,  5,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val kingMiddleGameTable = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    )

    /**
     * Determines the best move using alpha-beta depth-first search.
     * Starts with a random move fallback in case no valid legal moves are present.
     */
    fun findBestMove(game: ChessGame, depth: Int): ChessMove? {
        val legalMoves = game.getAllLegalMoves(game.activeColor)
        if (legalMoves.isEmpty()) return null
        
        // Dynamic move sorting: prioritize captures and promotions for better alpha-beta pruning
        val sortedMoves = legalMoves.sortedByDescending { move ->
            var score = 0
            if (move.isCapture && move.capturedPiece != null) {
                score += PIECE_VALUES[move.capturedPiece.type] ?: 0
                score -= (PIECE_VALUES[move.piece.type] ?: 0) / 10
            }
            if (move.isPromotion) {
                score += 800
            }
            score
        }

        var bestMove: ChessMove? = null
        val color = game.activeColor
        val isMaximizing = color == PieceColor.WHITE

        var alpha = Int.MIN_VALUE
        var beta = Int.MAX_VALUE

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in sortedMoves) {
                val simulatedBoard = applySimulatedMove(game.board, move)
                val eval = minimax(simulatedBoard, depth - 1, alpha, beta, false, color.opponent(), move)
                if (eval > maxEval) {
                    maxEval = eval
                    bestMove = move
                }
                alpha = max(alpha, eval)
                if (beta <= alpha) break
            }
        } else {
            var minEval = Int.MAX_VALUE
            for (move in sortedMoves) {
                val simulatedBoard = applySimulatedMove(game.board, move)
                val eval = minimax(simulatedBoard, depth - 1, alpha, beta, true, color.opponent(), move)
                if (eval < minEval) {
                    minEval = eval
                    bestMove = move
                }
                beta = min(beta, eval)
                if (beta <= alpha) break
            }
        }

        // Return a solid selected move or random fallback
        return bestMove ?: sortedMoves.randomOrNull()
    }

    private fun minimax(
        b: Map<Position, ChessPiece>,
        depth: Int,
        initAlpha: Int,
        initBeta: Int,
        isMaximizing: Boolean,
        activeColor: PieceColor,
        lastSimulatedMove: ChessMove?
    ): Int {
        if (depth == 0) {
            return evaluateBoard(b)
        }

        var alpha = initAlpha
        var beta = initBeta

        val moves = getSimulatedLegalMoves(b, activeColor, lastSimulatedMove)
        
        if (moves.isEmpty()) {
            // Evaluates checkmate or draw
            val kingPos = b.entries.find { it.value.type == PieceType.KING && it.value.color == activeColor }?.key
            val inCheck = if (kingPos != null) isKingUnderAttack(b, kingPos, activeColor) else false
            return if (inCheck) {
                if (activeColor == PieceColor.WHITE) -100000 - depth else 100000 + depth
            } else {
                0 // Draw / Stalemate
            }
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves) {
                val nextBoard = applySimulatedMove(b, move)
                val eval = minimax(nextBoard, depth - 1, alpha, beta, false, activeColor.opponent(), move)
                maxEval = max(maxEval, eval)
                alpha = max(alpha, eval)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves) {
                val nextBoard = applySimulatedMove(b, move)
                val eval = minimax(nextBoard, depth - 1, alpha, beta, true, activeColor.opponent(), move)
                minEval = min(minEval, eval)
                beta = min(beta, eval)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    private fun evaluateBoard(b: Map<Position, ChessPiece>): Int {
        var score = 0
        for ((pos, piece) in b) {
            val weightMultiplier = if (piece.color == PieceColor.WHITE) 1 else -1
            val baseValue = PIECE_VALUES[piece.type] ?: 0
            
            // Mirror table index check for black pieces
            val index = if (piece.color == PieceColor.WHITE) {
                pos.row * 8 + pos.col
            } else {
                (7 - pos.row) * 8 + pos.col
            }

            val tableBonus = when (piece.type) {
                PieceType.PAWN -> pawnTable[index]
                PieceType.KNIGHT -> knightTable[index]
                PieceType.BISHOP -> bishopTable[index]
                PieceType.ROOK -> rookTable[index]
                PieceType.QUEEN -> queenTable[index]
                PieceType.KING -> kingMiddleGameTable[index]
            }

            score += weightMultiplier * (baseValue + tableBonus)
        }
        return score
    }

    // --- Lightweight Simulation Helpers ---

    private fun getSimulatedLegalMoves(
        b: Map<Position, ChessPiece>,
        color: PieceColor,
        lastSimMove: ChessMove?
    ): List<ChessMove> {
        val list = mutableListOf<ChessMove>()
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val p = b[pos]
                if (p != null && p.color == color) {
                    val pseudo = getSimulatedPseudoMoves(b, pos, lastSimMove)
                    val legal = pseudo.filter { move ->
                        val simulated = applySimulatedMove(b, move)
                        val kingPos = simulated.entries.find { it.value.type == PieceType.KING && it.value.color == color }?.key
                        if (kingPos != null) {
                            !isKingUnderAttack(simulated, kingPos, color)
                        } else {
                            true
                        }
                    }
                    list.addAll(legal)
                }
            }
        }
        return list
    }

    private fun isKingUnderAttack(b: Map<Position, ChessPiece>, kingPos: Position, kingColor: PieceColor): Boolean {
        val opponent = kingColor.opponent()
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                val p = b[pos]
                if (p != null && p.color == opponent) {
                    val pseudo = getSimulatedPseudoMoves(b, pos, null, skipAllCastling = true)
                    if (pseudo.any { it.to == kingPos }) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getSimulatedPseudoMoves(
        b: Map<Position, ChessPiece>,
        from: Position,
        lastSimMove: ChessMove?,
        skipAllCastling: Boolean = false
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

                val next1 = Position(row + dir, col)
                if (next1.isValid() && !b.containsKey(next1)) {
                    if (next1.row == promoRow) {
                        moves.add(ChessMove(piece, from, next1, isPromotion = true, promotedTo = PieceType.QUEEN))
                    } else {
                        moves.add(ChessMove(piece, from, next1))
                    }
                    val next2 = Position(row + dir * 2, col)
                    if (row == startRow && !b.containsKey(next2)) {
                        moves.add(ChessMove(piece, from, next2))
                    }
                }

                for (dCol in listOf(-1, 1)) {
                    val capturePos = Position(row + dir, col + dCol)
                    if (capturePos.isValid()) {
                        val capPiece = b[capturePos]
                        if (capPiece != null && capPiece.color != piece.color) {
                            if (capturePos.row == promoRow) {
                                moves.add(ChessMove(piece, from, capturePos, isCapture = true, capturedPiece = capPiece, isPromotion = true, promotedTo = PieceType.QUEEN))
                            } else {
                                moves.add(ChessMove(piece, from, capturePos, isCapture = true, capturedPiece = capPiece))
                            }
                        }
                    }
                }

                val epRow = if (piece.color == PieceColor.WHITE) 3 else 4
                if (row == epRow && lastSimMove != null) {
                    if (lastSimMove.piece.type == PieceType.PAWN &&
                        abs(lastSimMove.from.row - lastSimMove.to.row) == 2 &&
                        lastSimMove.to.row == epRow &&
                        abs(lastSimMove.to.col - col) == 1
                    ) {
                        moves.add(ChessMove(piece, from, Position(row + dir, lastSimMove.to.col), isCapture = true, capturedPiece = lastSimMove.piece, isEnPassant = true))
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2), Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1))
                for (offset in offsets) {
                    val to = Position(row + offset.first, col + offset.second)
                    if (to.isValid()) {
                        val pDest = b[to]
                        if (pDest == null) moves.add(ChessMove(piece, from, to))
                        else if (pDest.color != piece.color) moves.add(ChessMove(piece, from, to, isCapture = true, capturedPiece = pDest))
                    }
                }
            }

            PieceType.BISHOP -> addSimulatedSliding(b, from, piece, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), moves)
            PieceType.ROOK -> addSimulatedSliding(b, from, piece, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), moves)
            PieceType.QUEEN -> addSimulatedSliding(b, from, piece, listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1)), moves)

            PieceType.KING -> {
                val directions = listOf(Pair(-1, -1), Pair(-1, 0), Pair(-1, 1), Pair(0, -1), Pair(0, 1), Pair(1, -1), Pair(1, 0), Pair(1, 1))
                for (dir in directions) {
                    val to = Position(row + dir.first, col + dir.second)
                    if (to.isValid()) {
                        val pDest = b[to]
                        if (pDest == null) moves.add(ChessMove(piece, from, to))
                        else if (pDest.color != piece.color) moves.add(ChessMove(piece, from, to, isCapture = true, capturedPiece = pDest))
                    }
                }
            }
        }
        return moves
    }

    private fun addSimulatedSliding(
        b: Map<Position, ChessPiece>,
        from: Position,
        piece: ChessPiece,
        dirs: List<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        for (dir in dirs) {
            var step = 1
            while (true) {
                val to = Position(from.row + dir.first * step, from.col + dir.second * step)
                if (!to.isValid()) break
                val destPiece = b[to]
                if (destPiece == null) {
                    out.add(ChessMove(piece, from, to))
                } else {
                    if (destPiece.color != piece.color) {
                        out.add(ChessMove(piece, from, to, isCapture = true, capturedPiece = destPiece))
                    }
                    break
                }
                step++
            }
        }
    }

    private fun applySimulatedMove(b: Map<Position, ChessPiece>, move: ChessMove): Map<Position, ChessPiece> {
        val next = b.toMutableMap()
        val p = next.remove(move.from) ?: return b
        
        if (move.isCastlingKingSide) {
            val rook = next.remove(Position(move.from.row, 7))
            if (rook != null) next[Position(move.from.row, 5)] = rook.copy(hasMoved = true)
        } else if (move.isCastlingQueenSide) {
            val rook = next.remove(Position(move.from.row, 0))
            if (rook != null) next[Position(move.from.row, 3)] = rook.copy(hasMoved = true)
        }
        
        if (move.isEnPassant) {
            next.remove(Position(move.from.row, move.to.col))
        }

        val finalPiece = if (move.isPromotion && move.promotedTo != null) {
            ChessPiece(move.promotedTo, p.color, hasMoved = true)
        } else {
            p.copy(hasMoved = true)
        }
        next[move.to] = finalPiece
        return next
    }
}
