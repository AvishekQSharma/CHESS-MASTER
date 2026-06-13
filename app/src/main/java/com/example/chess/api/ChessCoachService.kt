package com.example.chess.api

import android.util.Log
import com.example.BuildConfig
import com.example.chess.model.ChessMove
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ChessCoachService {
    private const val TAG = "ChessCoachService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getCoachFeedback(
        board: Map<Position, ChessPiece>,
        activeColor: PieceColor,
        movesPlayed: List<ChessMove>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing or invalid. Please configure your GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        // Build a readable description of the current board
        val boardDesc = StringBuilder()
        boardDesc.append("Turn: $activeColor\n")
        boardDesc.append("Board State:\n")
        
        for (row in 0..7) {
            val rank = 8 - row
            boardDesc.append("Rank $rank: ")
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = board[pos]
                if (piece != null) {
                    val colorChar = if (piece.color == PieceColor.WHITE) "W" else "B"
                    val pieceChar = when (piece.type) {
                        com.example.chess.model.PieceType.KING -> "K"
                        com.example.chess.model.PieceType.QUEEN -> "Q"
                        com.example.chess.model.PieceType.ROOK -> "R"
                        com.example.chess.model.PieceType.BISHOP -> "B"
                        com.example.chess.model.PieceType.KNIGHT -> "N"
                        com.example.chess.model.PieceType.PAWN -> "P"
                    }
                    boardDesc.append("$colorChar$pieceChar(${pos.toAlgebraic()}) ")
                } else {
                    boardDesc.append(". ")
                }
            }
            boardDesc.append("\n")
        }

        val moveHistoryString = movesPlayed.takeLast(10).joinToString(", ") { it.toSan() }
        
        val prompt = """
            You are an expert, encouraging, and professional Chess Grandmaster and Coach. 
            Analyze this current Android game match. Provide a short tactical feedback.
            
            Current Active Player: ${activeColor.name}
            Last 10 moves played: $moveHistoryString
            
            $boardDesc
            
            Please tell the active player:
            1. A brief tactical analysis of the board (points of interest, potential forks/pins/threats, or safety of the kings).
            2. Suggest the top 1 or 2 best moves with brief grandmaster explanation.
            
            Keep your feedback engaging, highly professional, encouraging, and limit it to under 150 words.
        """.trimIndent()

        // Build direct Gemini REST API request body
        val jsonPayload = JSONObject()
        val contentsArray = org.json.JSONArray()
        val contentsObj = JSONObject()
        val partsArray = org.json.JSONArray()
        val partsObj = JSONObject()
        
        partsObj.put("text", prompt)
        partsArray.put(partsObj)
        contentsObj.put("parts", partsArray)
        contentsArray.put(contentsObj)
        jsonPayload.put("contents", contentsArray)

        // Add System Instruction
        val systemInstructionObj = JSONObject()
        val sysPartsArray = org.json.JSONArray()
        val sysPartsObj = JSONObject()
        sysPartsObj.put("text", "You are an encouraging, friendly, and expert Chess Grandmaster Coach.")
        sysPartsArray.put(sysPartsObj)
        systemInstructionObj.put("parts", sysPartsArray)
        jsonPayload.put("systemInstruction", systemInstructionObj)

        // Add Configuration
        val configObj = JSONObject()
        configObj.put("temperature", 0.7)
        configObj.put("maxOutputTokens", 500)
        jsonPayload.put("generationConfig", configObj)

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "API call failed code ${response.code}: $errorBody")
                    return@withContext "Error: Gemini coach analysis failed. API returned code ${response.code}."
                }

                val responseBody = response.body?.string() ?: ""
                val jsonObj = JSONObject(responseBody)
                val candidates = jsonObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No feedback compiled.")
                        }
                    }
                }
                "Could not parse Chess Coach response."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call", e)
            "Error: ${e.message ?: "Failed to connect to AI Coach."}"
        }
    }
}
