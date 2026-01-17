package com.sentinelrss.utils

import android.content.Context
import java.io.File

object ModelUtils {
    const val MODEL_FILENAME = "universal_sentence_encoder.tflite"
    // Public URL for the model (LiteRT / MediaPipe Text Embedder)
    // Using a known reliable URL for the Universal Sentence Encoder TFLite model.
    const val MODEL_URL = "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/1/universal_sentence_encoder.tflite"

    fun getModelPath(context: Context): String? {
        // 1. Check internal storage (downloaded)
        val file = File(context.filesDir, MODEL_FILENAME)
        if (file.exists()) {
            return file.absolutePath
        }

        // 2. Check assets (built-in)
        // MediaPipe Tasks TextEmbedder options expects an asset path or absolute path.
        // If it's in assets, we just pass the filename.
        // However, we need to verify it exists in assets to know which path to return.
        try {
            context.assets.open(MODEL_FILENAME).use { return MODEL_FILENAME }
        } catch (e: Exception) {
            // Not in assets
        }

        return null
    }
}
