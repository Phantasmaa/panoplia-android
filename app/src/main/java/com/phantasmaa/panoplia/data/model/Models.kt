package com.phantasmaa.panoplia.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val ok: Boolean,
    @Json(name = "user") val user: User? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class User(
    val id: Long? = null,
    val username: String,
    @Json(name = "is_admin") val isAdmin: Boolean = false
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val status: String,
    val models: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class ServiceInfo(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val icon: String,
    val native: Boolean = false
)
