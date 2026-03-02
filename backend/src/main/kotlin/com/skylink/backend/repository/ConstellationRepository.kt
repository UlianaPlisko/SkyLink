package com.skylink.backend.repository

import com.skylink.backend.model.entity.Constellation
import org.springframework.data.jpa.repository.JpaRepository

interface ConstellationRepository : JpaRepository<Constellation, Long>