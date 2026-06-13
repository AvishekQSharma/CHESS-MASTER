package com.example.chess.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games ORDER BY timestamp DESC")
    fun getAllSavedGames(): Flow<List<SavedGame>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: SavedGame)

    @Query("DELETE FROM saved_games WHERE id = :gameId")
    suspend fun deleteGame(gameId: String)
}
