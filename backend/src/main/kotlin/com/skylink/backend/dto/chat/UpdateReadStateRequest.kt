package com.skylink.backend.dto.chat

import jakarta.validation.constraints.NotNull

data class UpdateReadStateRequest(

    val lastReadMsgId: Long
)