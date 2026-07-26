package com.phantasmaa.panoplia.data.remote

import com.phantasmaa.panoplia.data.model.HealthResponse
import com.phantasmaa.panoplia.data.model.LoginRequest
import com.phantasmaa.panoplia.data.model.LoginResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

interface PanopliaApi {

    @POST("/auth/verify")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @GET("/api/health")
    suspend fun health(): HealthResponse

    @POST("/proxy/image-enhancer/enhance")
    @Streaming
    suspend fun enhanceImage(@Part image: MultipartBody.Part): ResponseBody
}
