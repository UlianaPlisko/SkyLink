package com.codepalace.accelerometer.data.repository

import android.util.Log
import com.codepalace.accelerometer.api.CelestialApi
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.local.SpaceObjectDetailEntity
import com.codepalace.accelerometer.data.local.SpaceObjectEntity
import com.codepalace.accelerometer.data.model.SpaceObjectDetail
import com.codepalace.accelerometer.data.model.SpaceObjectSummary   // your Retrofit model
import com.codepalace.accelerometer.data.model.dto.ConstellationCultureResponse
import com.codepalace.accelerometer.data.model.dto.WikiResponse
import kotlinx.coroutines.flow.Flow

class CelestialRepository(
    private val api: CelestialApi,
    private val database: AppDatabase
) {

    private val dao = database.spaceObjectDao()

    private val detailDao = database.spaceObjectDetailDao()

    // Returns cached data immediately (Flow)
    fun getCachedObjects(): Flow<List<SpaceObjectEntity>> = dao.getAll()

    // Try network, fallback to cache, then save new data
    suspend fun refreshAllObjects(): Boolean {
        return try {
            Log.d("CelestialRepository", "🌐 Fetching space objects from API...")

            val remoteList = api.getAllSpaceObjects()
            Log.d("CelestialRepository", "✅ API returned ${remoteList.size} objects")

            val entities = remoteList.map { it.toEntity() }
            Log.d("CelestialRepository", "🧩 Mapped to ${entities.size} entities")

            dao.deleteAll()
            Log.d("CelestialRepository", "🗑️ Old DB data cleared")

            dao.insertAll(entities)
            Log.d("CelestialRepository", "💾 New data inserted into DB")

            true
        } catch (e: Exception) {
            Log.e("CelestialRepository", "❌ refreshAllObjects FAILED", e)

            // extra detail if it's HTTP
            if (e is retrofit2.HttpException) {
                Log.e("CelestialRepository", "🌍 HTTP ERROR ${e.code()} - ${e.message()}")
            }

            false
        }
    }

    suspend fun getSpaceObjectDetail(id: Long): SpaceObjectDetail {
        return api.getSpaceObjectDetail(id)
    }

    suspend fun getSpaceObjectWiki(id: Long): WikiResponse? {
        return try {
            api.getSpaceObjectWiki(id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCachedDetail(id: Long): SpaceObjectDetail? {
        return detailDao.getById(id)?.toDomain()
    }

    suspend fun saveDetail(detail: SpaceObjectDetail) {
        detailDao.insert(detail.toEntity())
        detailDao.keepOnlyLatest50()
    }

    suspend fun getAllCultures(): List<ConstellationCultureResponse> {
        return api.getAllCultures()
    }

    suspend fun setCurrentCulture(id: Long) {
        api.setCurrentCulture(id)
    }

    private fun SpaceObjectSummary.toEntity() = SpaceObjectEntity(
        id = id,
        displayName = displayName,
        objectType = objectType,
        raDeg = if (raDeg <= 24.0) raDeg * 15.0 else raDeg,
        decDeg = decDeg,
        magnitude = magnitude
    )

    private fun SpaceObjectDetail.toEntity() = SpaceObjectDetailEntity(
        id = id,
        displayName = displayName,
        objectClass = objectClass,
        spectralType = spectralType,
        constellation = constellation,
        magnitude = magnitude,
        distanceLy = distanceLy,
        raDeg = raDeg,
        decDeg = decDeg,
        description = description,
        wikiSummary = wikiSummary,
        wikiUrl = wikiUrl,
        imageUrl = imageUrl,
        orbitalModel = orbitalModel,
        lastComputed = lastComputed,
        catalogId = catalogId,
        angularSize = angularSize
    )

    private fun SpaceObjectDetailEntity.toDomain() = SpaceObjectDetail(
        id = id,
        displayName = displayName,
        objectClass = objectClass,
        spectralType = spectralType,
        constellation = constellation,
        magnitude = magnitude,
        distanceLy = distanceLy,
        raDeg = raDeg,
        decDeg = decDeg,
        description = description,
        wikiSummary = wikiSummary,
        wikiUrl = wikiUrl,
        imageUrl = imageUrl,
        orbitalModel = orbitalModel,
        lastComputed = lastComputed,
        catalogId = catalogId,
        angularSize = angularSize
    )
}
