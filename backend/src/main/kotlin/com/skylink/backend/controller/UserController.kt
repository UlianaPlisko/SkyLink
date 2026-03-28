package com.skylink.backend.controller

import com.skylink.backend.dto.user.*
import com.skylink.backend.service.user.UserProfileService
import com.skylink.backend.service.user.UserSecurityService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
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
}