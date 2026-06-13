package com.example.chess.database

import kotlinx.coroutines.flow.Flow

class ChessRepository(private val savedGameDao: SavedGameDao) {
    val allSavedGames: Flow<List<SavedGame>> = savedGameDao.getAllSavedGames()

    suspend fun saveGame(game: SavedGame) {
        savedGameDao.insertGame(game)
    }

    suspend fun deleteGame(id: String) {
        savedGameDao.deleteGame(id)
    }
}
