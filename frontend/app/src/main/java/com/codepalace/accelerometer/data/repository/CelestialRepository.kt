package com.codepalace.accelerometer.data.repository

import android.content.Context
import com.codepalace.accelerometer.api.CelestialApi
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.local.SpaceObjectEntity
import com.codepalace.accelerometer.data.model.SpaceObjectDetail
import com.codepalace.accelerometer.data.model.SpaceObjectSummary   // your Retrofit model
import com.codepalace.accelerometer.data.model.dto.WikiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CelestialRepository(
    private val api: CelestialApi,
    private val database: AppDatabase
) {

    private val dao = database.spaceObjectDao()

    // Returns cached data immediately (Flow)
    fun getCachedObjects(): Flow<List<SpaceObjectEntity>> = dao.getAll()

    // Try network, fallback to cache, then save new data
    suspend fun refreshAllObjects(): Boolean {
        return try {
            val remoteList = api.getAllSpaceObjects()
            val entities = remoteList.map { it.toEntity() }
            dao.deleteAll()
            dao.insertAll(entities)
            true
        } catch (e: Exception) {
            // no internet or server down → keep cache
            false
        }
    }

    // Helper to convert your summary to entity
    private fun SpaceObjectSummary.toEntity() = SpaceObjectEntity(
        id = id,
        displayName = displayName,
        objectType = objectType,
        raDeg = if (raDeg <= 24.0) raDeg * 15.0 else raDeg,
        decDeg = decDeg,
        magnitude = magnitude
    )

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
}