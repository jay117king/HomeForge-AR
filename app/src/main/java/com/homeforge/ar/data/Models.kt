package com.homeforge.ar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val roomType: String,          // Kitchen, Living, Bathroom, Bedroom
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val meshJson: String? = null,  // Serialized room mesh (vertices + indices)
    val thumbnailPath: String? = null
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,    // Stable ID from seed or parsed URL
    val name: String,
    val category: String,          // table, cupboard, tap, sofa, etc.
    val lengthCm: Float,
    val widthCm: Float,
    val heightCm: Float,
    val price: Double? = null,
    val currency: String = "USD",
    val sourceUrl: String,
    val imageUrls: List<String>,   // Stored as JSON string in DB converter
    val glbUrl: String? = null,    // Cached generated or pre-made model
    val brand: String? = null
)

@Entity(tableName = "placed_objects")
data class PlacedObject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val productId: String,
    // Transform as 4x4 matrix stored as JSON array of 16 floats
    val transformJson: String,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f
)

@Serializable
data class RoomMesh(
    val vertices: List<Float>,     // x,y,z interleaved
    val indices: List<Int>,
    val normals: List<Float>? = null,
    val uvs: List<Float>? = null
)

@Serializable
data class ProductSeedItem(
    val id: String,
    val name: String,
    val category: String,
    val lengthCm: Float,
    val widthCm: Float,
    val heightCm: Float,
    val price: Double? = null,
    val sourceUrl: String,
    val imageUrls: List<String>,
    val brand: String? = null
)
