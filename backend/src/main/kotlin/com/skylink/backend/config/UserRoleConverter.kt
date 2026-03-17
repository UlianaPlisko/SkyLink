package com.skylink.backend.config

import com.skylink.backend.model.enums.UserRole
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class UserRoleConverter : AttributeConverter<UserRole, String> {
    override fun convertToDatabaseColumn(attribute: UserRole): String {
        return attribute.name
    }

    override fun convertToEntityAttribute(dbData: String): UserRole {
        return UserRole.valueOf(dbData)
    }
}