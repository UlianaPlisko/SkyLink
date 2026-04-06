package com.skylink.backend.repository

import com.skylink.backend.model.entity.UserPfp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserPfpRepository : JpaRepository<UserPfp, Long>