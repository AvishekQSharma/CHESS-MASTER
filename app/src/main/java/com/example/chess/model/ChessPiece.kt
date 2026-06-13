package com.example.chess.model

enum class PieceType {
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
}

enum class PieceColor {
    WHITE, BLACK;
    fun opponent(): PieceColor = if (this == WHITE) BLACK else WHITE
}

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7
    
    fun toAlgebraic(): String {
        val file = ('a' + col)
        val rank = ('8' - row)
        return "$file$rank"
    }

    companion object {
        fun fromAlgebraic(s: String): Position? {
            if (s.length != 2) return null
            val col = s[0] - 'a'
            val row = '8' - s[1]
            val pos = Position(row, col)
            return if (pos.isValid()) pos else null
        }
    }
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    fun getSymbol(): String {
        return when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
    }
}

data class ChessMove(
    val piece: ChessPiece,
    val from: Position,
    val to: Position,
    val isCapture: Boolean = false,
    val capturedPiece: ChessPiece? = null,
    val isCastlingKingSide: Boolean = false,
    val isCastlingQueenSide: Boolean = false,
    val isEnPassant: Boolean = false,
    val isPromotion: Boolean = false,
    val promotedTo: PieceType? = null,
    val originalHasMoved: Boolean = false,
    val originalRookHasMoved: Boolean = false // for castling undo
) {
    fun toSan(): String {
        if (isCastlingKingSide) return "O-O"
        if (isCastlingQueenSide) return "O-O-O"
        
        val piecePrefix = when (piece.type) {
            PieceType.PAWN -> if (isCapture) ('a' + from.col).toString() else ""
            PieceType.KNIGHT -> "N"
            PieceType.BISHOP -> "B"
            PieceType.ROOK -> "R"
            PieceType.QUEEN -> "Q"
            PieceType.KING -> "K"
        }
        
        val captureIndicator = if (isCapture) "x" else ""
        val dest = to.toAlgebraic()
        
        val promoSuffix = if (isPromotion && promotedTo != null) {
            val symbol = when (promotedTo) {
                PieceType.QUEEN -> "Q"
                PieceType.ROOK -> "R"
                PieceType.BISHOP -> "B"
                PieceType.KNIGHT -> "N"
                else -> ""
            }
            "=$symbol"
        } else ""
        
        return "$piecePrefix$captureIndicator$dest$promoSuffix"
    }
}
