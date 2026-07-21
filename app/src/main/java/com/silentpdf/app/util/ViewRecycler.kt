package com.silentpdf.app.util

import android.util.Log

object ViewRecycler {
    fun clearMemory() {
        try {
            System.gc()
            Runtime.getRuntime().gc()
            Log.d("ViewRecycler", "Memory cleared.")
        } catch (e: Exception) {
            Log.e("ViewRecycler", "Failed to clear memory", e)
        }
    }
}
