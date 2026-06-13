package com.example.chess.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_games")
data class SavedGame(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val movesCsv: String // e.g. "6,4,4,4,NONE;1,4,3,4,NONE" (representing fromRow,fromCol,toRow,toCol,promotionType)
)
