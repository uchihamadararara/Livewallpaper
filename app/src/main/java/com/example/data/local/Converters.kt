package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.models.AdvancedConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromAdvancedConfig(value: AdvancedConfig?): String? {
        return value?.let { Json.encodeToString(it) }
    }
    @TypeConverter
    fun toAdvancedConfig(value: String?): AdvancedConfig? {
        return value?.let { Json.decodeFromString<AdvancedConfig>(it) }
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { Json.encodeToString(it) }
    }
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { Json.decodeFromString<List<String>>(it) }
    }
}
