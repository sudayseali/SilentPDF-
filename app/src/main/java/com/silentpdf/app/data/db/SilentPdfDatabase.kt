package com.silentpdf.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PdfEntity::class, BookmarkEntity::class, NoteEntity::class], version = 3, exportSchema = false)
abstract class SilentPdfDatabase : RoomDatabase() {
    abstract fun pdfDao(): SilentPdfDao

    companion object {
        @Volatile
        private var INSTANCE: SilentPdfDatabase? = null

        fun getDatabase(context: Context): SilentPdfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SilentPdfDatabase::class.java,
                    "silent_pdf_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
