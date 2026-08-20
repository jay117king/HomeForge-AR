package com.homeforge.ar.data

import android.content.Context
import androidx.room.*
import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Project>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    suspend fun getAll(): List<Product>

    @Query("SELECT * FROM products WHERE category = :category")
    suspend fun getByCategory(category: String): List<Product>

    @Query("""
        SELECT * FROM products 
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        AND (:minL IS NULL OR lengthCm >= :minL)
        AND (:maxL IS NULL OR lengthCm <= :maxL)
        AND (:minW IS NULL OR widthCm >= :minW)
        AND (:maxW IS NULL OR widthCm <= :maxW)
        AND (:minH IS NULL OR heightCm >= :minH)
        AND (:maxH IS NULL OR heightCm <= :maxH)
    """)
    suspend fun search(
        query: String,
        minL: Float? = null, maxL: Float? = null,
        minW: Float? = null, maxW: Float? = null,
        minH: Float? = null, maxH: Float? = null
    ): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): Product?
}

@Dao
interface PlacedObjectDao {
    @Query("SELECT * FROM placed_objects WHERE projectId = :projectId")
    suspend fun getForProject(projectId: Long): List<PlacedObject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(obj: PlacedObject): Long

    @Delete
    suspend fun delete(obj: PlacedObject)

    @Query("DELETE FROM placed_objects WHERE projectId = :projectId")
    suspend fun clearProject(projectId: Long)
}

@Database(
    entities = [Project::class, Product::class, PlacedObject::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun productDao(): ProductDao
    abstract fun placedObjectDao(): PlacedObjectDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "homeforge.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
