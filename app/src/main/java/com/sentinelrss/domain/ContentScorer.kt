package com.sentinelrss.domain

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions

class ContentScorer(context: Context) {
    private var textEmbedder: TextEmbedder? = null

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
            // Model not found or init failed, falling back to dummy scorer
        }
    }

    fun score(title: String, description: String): Float {
        val text = "$title $description"

        // Mock logic if embedder is missing
        if (textEmbedder == null) return Math.random().toFloat()

        try {
            val result = textEmbedder?.embed(text)
            // In a real implementation, we would compare this embedding (Cosine Similarity)
            // against a user interest profile stored in DB.
            // For now, return a dummy score.
            return Math.random().toFloat()
        } catch (e: Exception) {
            return 0f
        }
    }
}
