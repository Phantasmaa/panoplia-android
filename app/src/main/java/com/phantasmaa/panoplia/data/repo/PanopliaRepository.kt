package com.phantasmaa.panoplia.data.repo

import com.phantasmaa.panoplia.data.model.HealthResponse
import com.phantasmaa.panoplia.data.model.LoginRequest
import com.phantasmaa.panoplia.data.model.ServiceInfo
import com.phantasmaa.panoplia.data.remote.PanopliaApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PanopliaRepository @Inject constructor(
    private val api: PanopliaApi
) {
    suspend fun login(username: String, password: String) =
        api.login(LoginRequest(username, password))

    suspend fun health(): HealthResponse = api.health()

    suspend fun enhanceImage(part: okhttp3.MultipartBody.Part): okhttp3.ResponseBody =
        api.enhanceImage(part)

    /**
     * Static catalog of microservicios. Mirrors /herramientas on the web.
     * Adding an app here is the only place we need to touch.
     */
    fun services(): List<ServiceInfo> = listOf(
        ServiceInfo(
            id = "image-enhancer",
            name = "Mejorar imagen",
            description = "Real-ESRGAN · x4 upscaling local",
            url = "/proxy/image-enhancer/",
            icon = "🖼️",
            native = true
        ),
        ServiceInfo(
            id = "image-crop",
            name = "Recortar imagen",
            description = "Crop circular, rectangular, libre",
            url = "/proxy/image-crop/",
            icon = "✂️"
        ),
        ServiceInfo(
            id = "pdf-tools",
            name = "PDF Tools",
            description = "Unir, dividir, rotar, comprimir",
            url = "/proxy/pdf-tools/",
            icon = "📄"
        ),
        ServiceInfo(
            id = "pdf-chat",
            name = "PDF Chat",
            description = "Preguntale a tus PDFs (LLM local)",
            url = "/proxy/pdf-chat/",
            icon = "💬"
        ),
        ServiceInfo(
            id = "chordbook",
            name = "Chordbook",
            description = "Cifrados, exportar PDF",
            url = "/proxy/chordbook/",
            icon = "🎸"
        ),
        ServiceInfo(
            id = "metube",
            name = "MeTube",
            description = "Descargas YouTube",
            url = "/metube/",
            icon = "📥"
        ),
        ServiceInfo(
            id = "finanzas",
            name = "Finanzas",
            description = "Gastos, saldos, presupuesto",
            url = "/",
            icon = "💰"
        )
    )
}
