package com.skylink.backend.controller

import com.skylink.backend.dto.user.ChangePasswordRequest
import com.skylink.backend.dto.user.UpdateProfileRequest
import com.skylink.backend.dto.user.UserProfileResponse
import com.skylink.backend.service.user.UserProfileService
import com.skylink.backend.service.user.UserSecurityService
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.security.Principal

@RestController
@RequestMapping("/api/profile")
class UserController(
    private val profileService: UserProfileService,
    private val securityService: UserSecurityService
) {

    @GetMapping("")
    fun getProfile(principal: Principal): UserProfileResponse {
        return profileService.getProfile(principal.name)
    }

    @PutMapping("")
    fun updateProfile(
        principal: Principal,
        @Valid @RequestBody request: UpdateProfileRequest
    ): UserProfileResponse {
        return profileService.updateProfile(principal.name, request)
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        principal: Principal,
        @Valid @RequestBody request: ChangePasswordRequest
    ) {
        securityService.changePassword(principal.name, request)
    }

    @PostMapping("/pfp", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun uploadPfp(
        principal: Principal,
        @RequestParam("file") file: MultipartFile
    ) {
        profileService.updatePfp(principal.name, file)
    }

    @GetMapping("/pfp")
    fun getPfp(principal: Principal): ResponseEntity<ByteArray> {
        val pfp = profileService.getPfp(principal.name)

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(pfp.contentType))
            .cacheControl(CacheControl.noCache())
            .body(pfp.bytes)
    }

    @DeleteMapping("/pfp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePfp(principal: Principal) {
        profileService.deletePfp(principal.name)
    }
}