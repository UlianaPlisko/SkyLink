package com.skylink.backend

import com.skylink.backend.config.AppDataSourceProperties
import com.skylink.backend.dto.auth.AuthResponse
import com.skylink.backend.dto.auth.CompleteGoogleRequest
import com.skylink.backend.dto.auth.GoogleAuthRequest
import com.skylink.backend.dto.auth.GoogleCallbackResponse
import com.skylink.backend.dto.auth.LoginRequest
import com.skylink.backend.dto.auth.RegisterRequest
import com.skylink.backend.dto.celestial.ConstellationCultureResponse
import com.skylink.backend.dto.celestial.ConstellationDetailResponse
import com.skylink.backend.dto.celestial.ConstellationResponse
import com.skylink.backend.dto.celestial.DeepSkyObjectResponse
import com.skylink.backend.dto.celestial.PlanetResponse
import com.skylink.backend.dto.celestial.SpaceObjectSummary
import com.skylink.backend.dto.celestial.StarResponse
import com.skylink.backend.dto.celestial.WikiResponse
import com.skylink.backend.dto.chat.ChatMessageResponse
import com.skylink.backend.dto.chat.ChatMessageSendRequest
import com.skylink.backend.dto.chat.ChatRoomResponse
import com.skylink.backend.dto.chat.ChatSubscriptionResponse
import com.skylink.backend.dto.chat.CreateChatRoomRequest
import com.skylink.backend.dto.chat.RawSocketRequest
import com.skylink.backend.dto.chat.RawSocketResponse
import com.skylink.backend.dto.chat.UpdateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateReadStateRequest
import com.skylink.backend.dto.event.CreateEventRequest
import com.skylink.backend.dto.event.EventDetailsResponse
import com.skylink.backend.dto.event.EventResponse
import com.skylink.backend.dto.event.IsParticipantResponse
import com.skylink.backend.dto.favorite.FavoriteRequest
import com.skylink.backend.dto.favorite.FavoriteResponse
import com.skylink.backend.dto.favorite.FavoriteUpdateRequest
import com.skylink.backend.dto.user.ChangePasswordRequest
import com.skylink.backend.dto.user.FcmTokenRequest
import com.skylink.backend.dto.user.UpdateProfileRequest
import com.skylink.backend.dto.user.UserProfileResponse
import com.skylink.backend.model.ConstStarConnectionId
import com.skylink.backend.model.EventParticipantId
import com.skylink.backend.model.UserChatSubscriptionId
import com.skylink.backend.model.entity.ChatMessage
import com.skylink.backend.model.entity.ChatRoom
import com.skylink.backend.model.entity.ConstStarConnection
import com.skylink.backend.model.entity.Constellation
import com.skylink.backend.model.entity.ConstellationCulture
import com.skylink.backend.model.entity.DeepSkyObjectData
import com.skylink.backend.model.entity.Event
import com.skylink.backend.model.entity.EventParticipant
import com.skylink.backend.model.entity.Favorite
import com.skylink.backend.model.entity.ObjectName
import com.skylink.backend.model.entity.PlanetData
import com.skylink.backend.model.entity.SpaceObject
import com.skylink.backend.model.entity.StarData
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserChatSubscription
import com.skylink.backend.model.entity.UserCredentials
import com.skylink.backend.model.entity.UserPfp
import com.skylink.backend.model.enums.AuthProvider
import com.skylink.backend.model.enums.ChatRoomType
import com.skylink.backend.model.enums.DeepSkyObjectClass
import com.skylink.backend.model.enums.EventSource
import com.skylink.backend.model.enums.EventType
import com.skylink.backend.model.enums.ObjectNameSource
import com.skylink.backend.model.enums.SpaceObjectType
import com.skylink.backend.model.enums.UserRole
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.time.Instant

class ModelContractTest {

    private val now: Instant = Instant.parse("2026-05-23T08:00:00Z")

    @Test
    fun `dto data classes expose stable generated contracts`() {
        val spaceObjectSummary = SpaceObjectSummary(
            id = 1L,
            displayName = "Vega",
            magnitude = 0.03,
            objectType = "STAR",
            raDeg = 1.0,
            decDeg = 2.0,
            description = "Bright star"
        )

        listOf(
            AuthResponse("token", UserRole.OBSERVER, "User", 1L),
            CompleteGoogleRequest("pending", "User", UserRole.CONTRIBUTOR),
            GoogleAuthRequest("id-token"),
            GoogleCallbackResponse("token", true, "User", UserRole.ADMIN, 2L),
            LoginRequest("user@test.com", "password"),
            RegisterRequest("user@test.com", "User", "password123", UserRole.OBSERVER),
            ConstellationCultureResponse(1L, "Western", "North", "description", true),
            ConstellationDetailResponse(1L, "Orion", "Western", "North", "description", listOf(spaceObjectSummary)),
            ConstellationResponse(1L, "Orion", "Western", "North", "description"),
            DeepSkyObjectResponse(1L, "M31", 3.4, 1.0, 2.0, "Galaxy", "summary", "url", "M31", "GALAXY", 2.5),
            PlanetResponse(1L, "Mars", 1.2, 1.0, 2.0, "Planet", "summary", "url", "{}", now),
            spaceObjectSummary,
            StarResponse(1L, "Vega", 0.03, 1.0, 2.0, "Star", "summary", "url", "LYR", "A0V", 25.0),
            WikiResponse("Mars", "summary", "url", "image"),
            ChatMessageResponse(1L, 2L, 3L, "user@test.com", "hello", now, now),
            ChatMessageSendRequest("hello"),
            ChatRoomResponse(1L, "Global", ChatRoomType.GLOBAL, "{}", null, 1L, now),
            ChatSubscriptionResponse(1L, 2L, now, 3L),
            CreateChatRoomRequest("Room", ChatRoomType.EVENT, "{}", 1L),
            RawSocketRequest("SEND", "token", 1L, "hello"),
            RawSocketResponse("MESSAGE", "ok", "user@test.com", 1L, mapOf("id" to 1)),
            UpdateChatRoomRequest("Room", ChatRoomType.REGIONAL, "{}", null),
            UpdateReadStateRequest(1L),
            CreateEventRequest("Event", "description", EventType.METEOR_SHOWER, now.plusSeconds(3600), now.plusSeconds(7200), "{}", "{}", 10),
            EventDetailsResponse(1L, "Event", "description", "METEOR_SHOWER", now, now.plusSeconds(3600), 2L, 3, true, 10),
            EventResponse(1L, "Event", "METEOR_SHOWER", now, "Creator"),
            IsParticipantResponse(true),
            FavoriteRequest(1L, "note", 0.8),
            FavoriteResponse(1L, spaceObjectSummary, "note", 0.8, now),
            FavoriteUpdateRequest("note", 0.7),
            ChangePasswordRequest("old-password", "new-password"),
            FcmTokenRequest("fcm-token"),
            UpdateProfileRequest("Display Name"),
            UserProfileResponse(1L, "user@test.com", "User", AuthProvider.LOCAL, UserRole.OBSERVER, now, now, "/pfp")
        ).forEach(::exerciseDataContract)
    }

    @Test
    fun `entity and embeddable data classes expose stable generated contracts`() {
        val culture = ConstellationCulture(1L, "Western", "North", "description", true)
        val constellation = Constellation(1L, culture, "Orion", "description")
        val user = User(
            id = 1L,
            email = "user@test.com",
            displayName = "User",
            provider = AuthProvider.LOCAL,
            role = UserRole.OBSERVER,
            settings = "{}",
            createdAt = now,
            lastUsedAt = now,
            fcmToken = "fcm"
        )
        val room = ChatRoom(1L, "Room", ChatRoomType.GLOBAL, "{}", null, user.id, now)
        val spaceObject = SpaceObject(1L, SpaceObjectType.STAR, 1.0, 2.0, 0.03, "Vega", "description")
        val event = Event(
            id = 1L,
            title = "Meteor Watch",
            description = "description",
            eventType = EventType.METEOR_SHOWER,
            startAt = now.plusSeconds(3600),
            endAt = now.plusSeconds(7200),
            visibilityGeom = "{}",
            locationGeom = "{}",
            creatorId = user.id,
            source = EventSource.USER,
            metadata = "{}",
            chatRoomId = room.id,
            maxCapacity = 10
        )
        val chatMessage = ChatMessage(1L, room, user, "hello", now, now)

        listOf(
            ConstStarConnectionId(1L, 2L, 3L),
            EventParticipantId(1L, 2L),
            UserChatSubscriptionId(1L, 2L),
            chatMessage,
            room,
            culture,
            constellation,
            ConstStarConnection(ConstStarConnectionId(1L, 1L, 2L), constellation, spaceObject, spaceObject.copy(id = 2L)),
            DeepSkyObjectData(1L, spaceObject, "M31", DeepSkyObjectClass.SPIRAL_GALAXY, 2.5),
            event,
            EventParticipant(EventParticipantId(event.id, user.id), event, user, now),
            Favorite(1L, user, spaceObject, "note", 0.8, now),
            ObjectName(1L, spaceObject, "Vega", ObjectNameSource.COMMON, true),
            PlanetData(1L, spaceObject.copy(objectType = SpaceObjectType.PLANET, displayName = "Mars"), "{}", now),
            spaceObject,
            StarData(1L, spaceObject, 1.0, 0.1, 0.2, 0.3, "LYR", "A0V", 25.0),
            user,
            UserChatSubscription(UserChatSubscriptionId(user.id, room.id), user, room, now, chatMessage),
            UserCredentials(1L, user, "hash"),
            UserPfp(user.id, byteArrayOf(1, 2, 3), "image/png")
        ).forEach(::exerciseDataContract)
    }

    @Test
    fun `configuration properties embeddables and enums expose default contracts`() {
        listOf(
            AppDataSourceProperties("jdbc:test", "user", "password"),
            AppDataSourceProperties(),
            ConstStarConnectionId(),
            EventParticipantId(),
            UserChatSubscriptionId(),
            UserPfp()
        ).forEach(::exerciseDataContract)

        listOf(
            AuthProvider.values().toList(),
            ChatRoomType.values().toList(),
            DeepSkyObjectClass.values().toList(),
            EventSource.values().toList(),
            EventType.values().toList(),
            ObjectNameSource.values().toList(),
            SpaceObjectType.values().toList(),
            UserRole.values().toList()
        ).flatten().forEach { enumValue ->
            assertNotNull(enumValue.name)
            assertNotNull(java.lang.Enum.valueOf(enumValue.declaringJavaClass, enumValue.name))
        }
    }

    private fun exerciseDataContract(instance: Any) {
        assertNotNull(instance.toString())
        assertNotNull(instance.hashCode())
        assertFalse(instance == "different-type")

        val components = instance.javaClass.methods
            .filter { it.parameterCount == 0 && it.name.matches(Regex("component\\d+")) }
            .sortedBy { it.name.removePrefix("component").toInt() }
            .map { it.invoke(instance) }

        assertDoesNotThrow { invokeNoArgConstructor(instance.javaClass) }
        assertDoesNotThrow { invokeDefaultConstructors(instance.javaClass, components) }
        components.forEach(::assertNotNullOrNullableContract)
        instance.javaClass.methods
            .filter { it.parameterCount == 0 && (it.name.startsWith("get") || it.name.startsWith("is")) }
            .filterNot { it.name == "getClass" }
            .forEach { assertDoesNotThrow { it.invoke(instance) } }
        instance.javaClass.methods
            .filter { it.parameterCount == 1 && it.name.startsWith("set") }
            .forEach { method ->
                assertDoesNotThrow {
                    method.invoke(instance, sampleValue(method.parameterTypes.single()))
                }
            }

        val copyMethod = instance.javaClass.methods.firstOrNull { it.name == "copy" }
        if (copyMethod != null) {
            val copy = copyMethod.invoke(instance, *components.toTypedArray())
            assertNotNull(copy)
            assertNotNull(copy.toString())
            assertNotNull(copy.hashCode())
            assertDoesNotThrow { instance == copy }
            invokeCopyDefault(instance, components)
            invokeCopyDefaultWithProvidedValues(instance, components)
        }
    }

    private fun invokeNoArgConstructor(type: Class<*>) {
        type.declaredConstructors
            .filter { it.parameterCount == 0 }
            .forEach {
                it.isAccessible = true
                it.newInstance()
            }
    }

    private fun invokeDefaultConstructors(type: Class<*>, components: List<Any?>) {
        type.declaredConstructors
            .filter { constructor ->
                constructor.parameterTypes.lastOrNull()?.name == "kotlin.jvm.internal.DefaultConstructorMarker"
            }
            .forEach { constructor ->
                constructor.isAccessible = true
                val args = arrayOfNulls<Any?>(constructor.parameterCount)
                val maskIndex = constructor.parameterCount - 2
                for (index in 0 until maskIndex) {
                    args[index] = components.getOrNull(index) ?: sampleValue(constructor.parameterTypes[index])
                }
                args[maskIndex] = -1
                args[constructor.parameterCount - 1] = null
                constructor.newInstance(*args)
            }
    }

    private fun invokeCopyDefault(instance: Any, components: List<Any?>) {
        invokeCopyDefault(instance, components, -1)
    }

    private fun invokeCopyDefaultWithProvidedValues(instance: Any, components: List<Any?>) {
        invokeCopyDefault(instance, components, 0)
    }

    private fun invokeCopyDefault(instance: Any, components: List<Any?>, mask: Int) {
        instance.javaClass.declaredMethods
            .filter { it.name == "copy\$default" && Modifier.isStatic(it.modifiers) }
            .forEach { method ->
                method.isAccessible = true
                val args = arrayOfNulls<Any?>(method.parameterCount)
                args[0] = instance
                components.forEachIndexed { index, value ->
                    args[index + 1] = value ?: sampleValue(method.parameterTypes[index + 1])
                }
                args[method.parameterCount - 2] = mask
                args[method.parameterCount - 1] = null
                method.invoke(null, *args)
            }
    }

    private fun sampleValue(type: Class<*>): Any? = when {
        type == java.lang.Long.TYPE || type == Long::class.javaObjectType -> 1L
        type == java.lang.Integer.TYPE || type == Int::class.javaObjectType -> 1
        type == java.lang.Double.TYPE || type == Double::class.javaObjectType -> 1.0
        type == java.lang.Boolean.TYPE || type == Boolean::class.javaObjectType -> true
        type == String::class.java -> "value"
        type == Instant::class.java -> now
        type == ByteArray::class.java -> byteArrayOf(1)
        type.isEnum -> type.enumConstants.first()
        List::class.java.isAssignableFrom(type) -> emptyList<Any>()
        Map::class.java.isAssignableFrom(type) -> emptyMap<String, Any>()
        else -> null
    }

    private fun assertNotNullOrNullableContract(value: Any?) {
        if (value != null) {
            assertNotNull(value)
        }
    }
}
