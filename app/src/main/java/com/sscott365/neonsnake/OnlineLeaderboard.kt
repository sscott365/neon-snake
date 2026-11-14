package com.sscott365.neonsnake

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/** A public leaderboard row. */
data class OnlineLeaderboardEntry(
    val name: String,
    val score: Int
)

/** Firebase-backed worldwide leaderboard using anonymous Firebase Authentication. */
class OnlineLeaderboard {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /** Saves the player's display name and keeps their best score. */
    fun submitScore(
        score: Int,
        playerName: String,
        callback: (uploaded: Boolean, newBest: Boolean, error: Exception?) -> Unit
    ) {
        if (score <= 0) {
            callback(true, false, null)
            return
        }

        val safeName = playerName.trim().take(MAX_NAME_LENGTH)
        if (safeName.isBlank()) {
            callback(false, false, IllegalArgumentException("Player name cannot be empty"))
            return
        }

        withSignedInUser { uid, authError ->
            if (authError != null || uid == null) {
                callback(false, false, authError)
                return@withSignedInUser
            }

            val scoreDocument = firestore.collection("leaderboard").document(uid)
            firestore.runTransaction { transaction ->
                val existing = transaction.get(scoreDocument)
                val previousBest = existing.getLong("score") ?: 0L
                val bestScore = maxOf(previousBest, score.toLong())
                transaction.set(
                    scoreDocument,
                    hashMapOf(
                        "uid" to uid,
                        "name" to safeName,
                        "score" to bestScore,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                score.toLong() > previousBest
            }.addOnSuccessListener { isNewBest -> callback(true, isNewBest, null) }
                .addOnFailureListener { error -> callback(false, false, error) }
        }
    }

    fun loadTopScores(callback: (entries: List<OnlineLeaderboardEntry>?, error: Exception?) -> Unit) {
        firestore.collection("leaderboard")
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                val entries = snapshot.documents.mapNotNull { document ->
                    val name = document.getString("name") ?: return@mapNotNull null
                    val score = document.getLong("score")?.toInt() ?: return@mapNotNull null
                    OnlineLeaderboardEntry(name, score)
                }
                callback(entries, null)
            }
            .addOnFailureListener { error -> callback(null, error) }
    }

    private fun withSignedInUser(callback: (uid: String?, error: Exception?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            callback(currentUser.uid, null)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result -> callback(result.user?.uid, null) }
            .addOnFailureListener { error -> callback(null, error) }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 20
    }
}
