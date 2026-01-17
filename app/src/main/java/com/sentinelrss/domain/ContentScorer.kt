package com.sentinelrss.domain

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import com.sentinelrss.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class ContentScorer(context: Context) {
    private var textEmbedder: TextEmbedder? = null
    private val database = AppDatabase.getDatabase(context)

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("universal_sentence_encoder.tflite")
                .build()
            val options = TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .build()
            textEmbedder = TextEmbedder.createFromOptions(context, options)
        } catch (e: Exception) {
            // Model not found or init failed
        }
    }

    fun isModelLoaded(): Boolean {
        return textEmbedder != null
    }

    suspend fun score(title: String, description: String): Float {
        val text = "$title $description"

        // 1. Generate Embedding for the article
        val articleEmbedding = generateEmbedding(text) ?: return fallbackScore(text)

        // 2. Fetch User Interests
        val userInterests = withContext(Dispatchers.IO) {
            database.userInterestDao().getAllInterests()
        }

        if (userInterests.isEmpty()) {
            return 0.5f // Neutral score if no history
        }

        // 3. Calculate Similarity (Max or Average Cosine Similarity)
        var maxSimilarity = 0f
        for (interest in userInterests) {
            val interestVector = parseEmbedding(interest.vectorEmbedding)
            val similarity = cosineSimilarity(articleEmbedding, interestVector)
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity
            }
        }

        return maxSimilarity
    }

    fun generateEmbedding(text: String): FloatArray? {
        if (textEmbedder == null) {
            // Return dummy embedding for fallback/testing when model is missing
            return generateDummyEmbedding(text)
        }
        return try {
            val result = textEmbedder?.embed(text)
            result?.embeddingResult()?.embeddings()?.firstOrNull()?.floatEmbedding()
        } catch (e: Exception) {
            generateDummyEmbedding(text)
        }
    }

    private fun generateDummyEmbedding(text: String): FloatArray {
        // Hash the text to create a deterministic "embedding" for testing
        val hash = text.hashCode()
        val embedding = FloatArray(10)
        for (i in 0 until 10) {
            embedding[i] = ((hash shr i) % 100).toFloat() / 100f
        }
        return embedding
    }

    private fun fallbackScore(text: String): Float {
        // Simple keyword matching if model fails
        val keywords = listOf("Android", "Privacy", "Security", "Kotlin", "Hack")
        var hits = 0
        for (keyword in keywords) {
            if (text.contains(keyword, ignoreCase = true)) hits++
        }
        return if (hits > 0) 0.8f else 0.2f
    }

    private fun parseEmbedding(embeddingStr: String): FloatArray {
        return embeddingStr.split(",").map { it.toFloat() }.toFloatArray()
    }

    private fun cosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vecA.indices) {
            dotProduct += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }
        return if (normA > 0 && normB > 0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0.0f
        }
    }
}
