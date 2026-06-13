package com.example

import com.example.chess.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    
    @Test
    fun testInitialBoardPieces() {
        val game = ChessGame()
        val board = game.board
        
        // Ensure starting counts are correct
        assertEquals(32, board.size)
        
        // Confirm White King is at e1 (Row 7, Col 4)
        val wKing = board[Position(7, 4)]
        assertNotNull(wKing)
        assertEquals(PieceType.KING, wKing!!.type)
        assertEquals(PieceColor.WHITE, wKing.color)

        // Confirm Black King is at e8 (Row 0, Col 4)
        val bKing = board[Position(0, 4)]
        assertNotNull(bKing)
        assertEquals(PieceType.KING, bKing!!.type)
        assertEquals(PieceColor.BLACK, bKing.color)
    }

    @Test
    fun testInitialPawnLegality() {
        val game = ChessGame()
        
        // White pawn on e2 (Row 6, Col 4) can move to e3 or e4
        val moves = game.getLegalMoves(Position(6, 4))
        assertEquals(2, moves.size)
        
        val destinations = moves.map { it.to }
        assertTrue(destinations.contains(Position(5, 4))) // e3
        assertTrue(destinations.contains(Position(4, 4))) // e4
    }

    @Test
    fun testKnightLegality() {
        val game = ChessGame()
        
        // White Knight on g1 (Row 7, Col 6) can jump over pawns to f3 or h3
        val moves = game.getLegalMoves(Position(7, 6))
        assertEquals(2, moves.size)
        
        val destinations = moves.map { it.to }
        assertTrue(destinations.contains(Position(5, 5))) // f3
        assertTrue(destinations.contains(Position(5, 7))) // h3
    }

    @Test
    fun testCheckmateHypotheticalDetection() {
        val game = ChessGame()
        
        // Since starting board is not checkmate, confirm checkmate is false
        assertFalse(game.isCheckmate())
        assertFalse(game.isStalemate())
    }
}
