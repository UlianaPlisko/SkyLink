package com.skylink.backend.controller

import com.skylink.backend.dto.user.ChangePasswordRequest
import com.skylink.backend.dto.user.FcmTokenRequest
import com.skylink.backend.dto.user.UpdateProfileRequest
import com.skylink.backend.dto.user.UserProfileResponse
import com.skylink.backend.service.user.UserProfileService
import com.skylink.backend.service.user.UserSecurityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.security.Principal

@Tag(
    name = "Profile",
    description = "Operations related to user profile management"
)
@RestController
@RequestMapping("/api/profile")
class UserController(
    private val profileService: UserProfileService,
    private val securityService: UserSecurityService
) {

    @Operation(summary = "Get current user profile")
    @GetMapping("")
    fun getProfile(principal: Principal): UserProfileResponse {
        return profileService.getProfile(principal.name)
    }

    @Operation(summary = "Update user profile")
    @PutMapping("")
    fun updateProfile(
        principal: Principal,
        @Valid @RequestBody request: UpdateProfileRequest
    ): UserProfileResponse {
        return profileService.updateProfile(principal.name, request)
    }

    @Operation(summary = "Change user password")
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        principal: Principal,
        @Valid @RequestBody request: ChangePasswordRequest
    ) {
        securityService.changePassword(principal.name, request)
    }

    @Operation(summary = "Upload profile picture")
    @PostMapping("/pfp", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun uploadPfp(
        principal: Principal,
        @RequestParam("file") file: MultipartFile
    ) {
        profileService.updatePfp(principal.name, file)
    }

    @Operation(summary = "Get profile picture")
    @GetMapping("/pfp")
    fun getPfp(principal: Principal): ResponseEntity<ByteArray> {
        val pfp = profileService.getPfp(principal.name)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(pfp.contentType))
            .cacheControl(CacheControl.noCache())
            .body(pfp.bytes)
    }

    @Operation(summary = "Delete profile picture")
    @DeleteMapping("/pfp")
    fun deletePfp(principal: Principal): ResponseEntity<Void> {
        return if (profileService.deletePfp(principal.name)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "Register/update FCM token for push notifications")
    @PostMapping("/fcm-token")
    @ResponseStatus(HttpStatus.OK)
    fun registerFcmToken(
        @Valid @RequestBody request: FcmTokenRequest,
        principal: Principal
    ) {
        profileService.registerFcmToken(principal.name, request.fcmToken)
    }
}