package com.codepalace.accelerometer.data.repository

import com.codepalace.accelerometer.api.CelestialApi
import com.codepalace.accelerometer.api.FavoriteApi
import com.codepalace.accelerometer.api.dto.FavoriteResponse
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.local.FavoriteEntity
import com.codepalace.accelerometer.data.local.PendingFavoriteActionEntity
import com.codepalace.accelerometer.data.local.SpaceObjectDetailEntity
import com.codepalace.accelerometer.data.model.SpaceObjectDetail
import com.codepalace.accelerometer.data.model.SpaceObjectSummary
import retrofit2.HttpException

class FavoriteRepository(
    private val favoriteApi: FavoriteApi,
    private val celestialApi: CelestialApi,
    private val database: AppDatabase
) {
    private val favoriteDao = database.favoriteDao()
    private val pendingFavoriteActionDao = database.pendingFavoriteActionDao()
    private val detailDao = database.spaceObjectDetailDao()

    suspend fun refreshFavorites(): List<FavoriteResponse> {
        syncPendingActions()
        val favorites = favoriteApi.getFavorites()
        favoriteDao.replaceAll(favorites.map { it.toEntity() })
        cacheSummaryDetailsIfMissing(favorites)
        refreshFavoriteDetails(favorites)
        return favorites
    }

    suspend fun getCachedFavorites(): List<FavoriteResponse> {
        return favoriteDao.getAll().map { it.toDomain() }
    }

    suspend fun isFavoriteCached(spaceObjectId: Long): Boolean {
        return favoriteDao.exists(spaceObjectId)
    }

    suspend fun cacheFavorite(favorite: FavoriteResponse) {
        pendingFavoriteActionDao.deleteBySpaceObjectId(favorite.spaceObject.id)
        favoriteDao.insert(favorite.toEntity())
        cacheSummaryDetailsIfMissing(listOf(favorite))
    }

    suspend fun removeFavoriteOnline(spaceObjectId: Long) {
        val response = favoriteApi.removeFavorite(spaceObjectId)
        if (!response.isSuccessful && response.code() != 404) {
            throw HttpException(response)
        }

        favoriteDao.deleteBySpaceObjectId(spaceObjectId)
        pendingFavoriteActionDao.deleteBySpaceObjectId(spaceObjectId)
    }

    suspend fun removeFavoriteOffline(spaceObjectId: Long) {
        favoriteDao.deleteBySpaceObjectId(spaceObjectId)
        pendingFavoriteActionDao.insert(
            PendingFavoriteActionEntity(
                spaceObjectId = spaceObjectId,
                action = ACTION_DELETE
            )
        )
    }

    private suspend fun syncPendingActions() {
        pendingFavoriteActionDao.getAll().forEach { pendingAction ->
            if (pendingAction.action == ACTION_DELETE) {
                val response = favoriteApi.removeFavorite(pendingAction.spaceObjectId)
                if (!response.isSuccessful && response.code() != 404) {
                    throw HttpException(response)
                }

                pendingFavoriteActionDao.deleteBySpaceObjectId(pendingAction.spaceObjectId)
                favoriteDao.deleteBySpaceObjectId(pendingAction.spaceObjectId)
            }
        }
    }

    private suspend fun cacheSummaryDetailsIfMissing(favorites: List<FavoriteResponse>) {
        favorites.forEach { favorite ->
            val summary = favorite.spaceObject
            if (detailDao.getById(summary.id) == null) {
                detailDao.insert(
                    SpaceObjectDetailEntity(
                        id = summary.id,
                        displayName = summary.displayName,
                        objectClass = summary.objectType,
                        spectralType = null,
                        constellation = null,
                        magnitude = summary.magnitude,
                        distanceLy = null,
                        raDeg = summary.raDeg,
                        decDeg = summary.decDeg,
                        description = summary.description,
                        wikiSummary = null,
                        wikiUrl = null,
                        imageUrl = null,
                        orbitalModel = null,
                        lastComputed = null,
                        catalogId = null,
                        angularSize = null
                    )
                )
            }
        }
    }

    private suspend fun refreshFavoriteDetails(favorites: List<FavoriteResponse>) {
        favorites.forEach { favorite ->
            try {
                val detail = celestialApi.getSpaceObjectDetail(favorite.spaceObject.id)
                val wiki = try {
                    celestialApi.getSpaceObjectWiki(favorite.spaceObject.id)
                } catch (_: Exception) {
                    null
                }

                val fullDetail = if (wiki == null) {
                    detail
                } else {
                    detail.copy(
                        wikiSummary = wiki.summary,
                        wikiUrl = wiki.url,
                        imageUrl = wiki.imageUrl
                    )
                }

                detailDao.insert(fullDetail.toDetailEntity())
            } catch (_: Exception) {
                // Favorite list must remain usable even if one detail endpoint is unavailable.
            }
        }
    }

    private fun FavoriteResponse.toEntity() = FavoriteEntity(
        spaceObjectId = spaceObject.id,
        favoriteId = id,
        displayName = spaceObject.displayName,
        magnitude = spaceObject.magnitude,
        objectType = spaceObject.objectType,
        raDeg = spaceObject.raDeg,
        decDeg = spaceObject.decDeg,
        description = spaceObject.description,
        note = note,
        visibility = visibility,
        addedAt = addedAt
    )

    private fun FavoriteEntity.toDomain() = FavoriteResponse(
        id = favoriteId,
        spaceObject = SpaceObjectSummary(
            id = spaceObjectId,
            displayName = displayName,
            magnitude = magnitude,
            objectType = objectType,
            raDeg = raDeg,
            decDeg = decDeg,
            description = description
        ),
        note = note,
        visibility = visibility,
        addedAt = addedAt
    )

    private fun SpaceObjectDetail.toDetailEntity() = SpaceObjectDetailEntity(
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

    companion object {
        private const val ACTION_DELETE = "DELETE"
    }
}
