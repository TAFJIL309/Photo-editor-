package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val originalImagePath: String?,
    val generatedImagePath: String?,
    val maskImagePath: String?,
    val prompt: String,
    val negativePrompt: String,
    val modelUsed: String,
    val creativity: Float,
    val imageStrength: Float,
    val promptStrength: Float,
    val aspectRatio: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)
