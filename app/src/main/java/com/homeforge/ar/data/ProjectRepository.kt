package com.homeforge.ar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProjectRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val projectDao = db.projectDao()
    private val placedDao = db.placedObjectDao()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveProject(
        name: String,
        roomType: String,
        scan: ScanResult,
        placedProductIds: List<String>
    ): Long = withContext(Dispatchers.IO) {
        val meshJson = json.encodeToString(
            mapOf(
                "width" to scan.widthMeters,
                "depth" to scan.depthMeters,
                "height" to scan.heightMeters,
                "planeCount" to scan.planeCount,
                "lockedDistance" to scan.lockedDistanceMeters
            )
        )
        val project = Project(
            name = name,
            roomType = roomType,
            meshJson = meshJson,
            updatedAt = System.currentTimeMillis()
        )
        val id = projectDao.insert(project)

        placedDao.clearProject(id)
        placedProductIds.forEachIndexed { index, productId ->
            placedDao.insert(
                PlacedObject(
                    projectId = id,
                    productId = productId,
                    transformJson = "[1,0,0,0, 0,1,0,0, 0,0,1,0, ${index * 0.8},0,0,1]",
                    scaleX = 1f, scaleY = 1f, scaleZ = 1f
                )
            )
        }
        id
    }

    suspend fun getAllProjects(): List<Project> = withContext(Dispatchers.IO) {
        projectDao.getAll()
    }

    suspend fun getProject(id: Long): Project? = withContext(Dispatchers.IO) {
        projectDao.getById(id)
    }

    suspend fun getPlacedForProject(projectId: Long): List<PlacedObject> = withContext(Dispatchers.IO) {
        placedDao.getForProject(projectId)
    }

    suspend fun deleteProject(project: Project) = withContext(Dispatchers.IO) {
        placedDao.clearProject(project.id)
        projectDao.delete(project)
    }
}
