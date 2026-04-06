package com.skylink.backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "\"UserPfp\"")
data class UserPfp(
    @Id
    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(name = "pfp_data", nullable = false, columnDefinition = "bytea")
    var data: ByteArray = byteArrayOf(),

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String = "application/octet-stream",

)