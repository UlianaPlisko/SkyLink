package com.skylink.backend.controller

import com.skylink.backend.dto.chat.ChatMessageResponse
import com.skylink.backend.dto.chat.ChatRoomResponse
import com.skylink.backend.dto.chat.ChatSubscriptionResponse
import com.skylink.backend.dto.chat.CreateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateReadStateRequest
import com.skylink.backend.model.enums.ChatRoomType
import com.skylink.backend.service.ChatRoomServiceInterface
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Chat Rooms",
    description = "Operations related to chat rooms, subscriptions and messages"
)
@RestController
@RequestMapping("/api/chat/rooms")
class ChatRoomController(
    private val chatRoomService: ChatRoomServiceInterface
) {

    @Operation(summary = "Create a new chat room")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createRoom(
        authentication: Authentication,
        @Valid @RequestBody request: CreateChatRoomRequest
    ): ChatRoomResponse {
        return chatRoomService.createRoom(authentication.name, request)
    }

    @Operation(summary = "Get all chat rooms (optionally filtered by type)")
    @GetMapping
    fun getRooms(
        @RequestParam(required = false) type: ChatRoomType?
    ): List<ChatRoomResponse> {
        return chatRoomService.getAllRooms(type)
    }

    @Operation(summary = "Get chat room by ID")
    @GetMapping("/{roomId}")
    fun getRoomById(
        @PathVariable roomId: Long
    ): ChatRoomResponse {
        return chatRoomService.getRoomById(roomId)
    }

    @Operation(summary = "Update chat room")
    @PutMapping("/{roomId}")
    fun updateRoom(
        @PathVariable roomId: Long,
        @Valid @RequestBody request: UpdateChatRoomRequest
    ): ChatRoomResponse {
        return chatRoomService.updateRoom(roomId, request)
    }

    @Operation(summary = "Delete chat room")
    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRoom(
        @PathVariable roomId: Long
    ) {
        chatRoomService.deleteRoom(roomId)
    }

    @Operation(summary = "Subscribe current user to a chat room")
    @PostMapping("/{roomId}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    fun subscribeCurrentUser(
        authentication: Authentication,
        @PathVariable roomId: Long
    ): ChatSubscriptionResponse {
        return chatRoomService.subscribeCurrentUser(authentication.name, roomId)
    }

    @Operation(summary = "Unsubscribe current user from a chat room")
    @DeleteMapping("/{roomId}/subscriptions/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unsubscribeCurrentUser(
        authentication: Authentication,
        @PathVariable roomId: Long
    ) {
        chatRoomService.unsubscribeCurrentUser(authentication.name, roomId)
    }

    @Operation(summary = "Get all subscriptions for a chat room")
    @GetMapping("/{roomId}/subscriptions")
    fun getRoomSubscriptions(
        @PathVariable roomId: Long
    ): List<ChatSubscriptionResponse> {
        return chatRoomService.getRoomSubscriptions(roomId)
    }

    @Operation(summary = "Get all chat rooms current user is subscribed to")
    @GetMapping("/me")
    fun getMySubscribedRooms(
        authentication: Authentication
    ): List<ChatRoomResponse> {
        return chatRoomService.getUserSubscribedRooms(authentication.name)
    }

    @Operation(summary = "Get all messages from a chat room")
    @GetMapping("/{roomId}/messages")
    fun getRoomMessages(
        @PathVariable roomId: Long
    ): List<ChatMessageResponse> {
        return chatRoomService.getRoomMessages(roomId)
    }

    @Operation(summary = "Get latest message from a chat room")
    @GetMapping("/{roomId}/messages/latest")
    fun getLatestMessage(
        @PathVariable roomId: Long
    ): ChatMessageResponse? {
        return chatRoomService.getLatestMessage(roomId)
    }

    @Operation(summary = "Update read state for current user in a chat room")
    @PatchMapping("/{roomId}/read-state")
    fun updateReadState(
        authentication: Authentication,
        @PathVariable roomId: Long,
        @Valid @RequestBody request: UpdateReadStateRequest
    ): ChatSubscriptionResponse {
        return chatRoomService.updateReadState(authentication.name, roomId, request)
    }
}