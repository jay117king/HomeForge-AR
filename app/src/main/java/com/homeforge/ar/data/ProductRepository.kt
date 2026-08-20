package com.homeforge.ar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ProductRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val productDao = db.productDao()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun ensureSeedData() = withContext(Dispatchers.IO) {
        val existing = productDao.getAll()
        if (existing.isNotEmpty()) return@withContext

        try {
            val input = context.assets.open("seed_products.json")
            val text = input.bufferedReader().use { it.readText() }
            val items = json.decodeFromString<List<ProductSeedItem>>(text)
            val products = items.map { item ->
                Product(
                    id = item.id,
                    name = item.name,
                    category = item.category,
                    lengthCm = item.lengthCm,
                    widthCm = item.widthCm,
                    heightCm = item.heightCm,
                    price = item.price,
                    sourceUrl = item.sourceUrl,
                    imageUrls = item.imageUrls,
                    brand = item.brand
                )
            }
            productDao.insertAll(products)
        } catch (e: Exception) {
            // Seed file missing or parse error – app still works with empty catalog
            e.printStackTrace()
        }
    }

    suspend fun search(
        query: String = "",
        minL: Float? = null, maxL: Float? = null,
        minW: Float? = null, maxW: Float? = null,
        minH: Float? = null, maxH: Float? = null
    ): List<Product> = withContext(Dispatchers.IO) {
        productDao.search(query.trim(), minL, maxL, minW, maxW, minH, maxH)
    }

    suspend fun getById(id: String): Product? = withContext(Dispatchers.IO) {
        productDao.getById(id)
    }

    suspend fun getAll(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getAll()
    }
}
