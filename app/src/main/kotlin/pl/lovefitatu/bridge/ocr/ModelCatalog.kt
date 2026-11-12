package pl.lovefitatu.bridge.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ModelInfo(val id: String, val name: String)

object ModelCatalog {
    /** Always offered at the top of the picker. */
    val PREFERRED = listOf("anthropic/claude-haiku-5.5", "google/gemini-2.5-flash")

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Models on OpenRouter that accept image input, sorted by id. */
    suspend fun fetchVisionModels(apiKey: String): List<ModelInfo> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .header("Authorization", "Bearer $apiKey")
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) throw OpenRouterException("HTTP ${response.code}", body)
            parse(body)
        }
    }

    fun parse(body: String): List<ModelInfo> =
        Json.parseToJsonElement(body).jsonObject["data"]!!.jsonArray.mapNotNull { element ->
            val model = element.jsonObject
            val inputs = (model["architecture"] as? JsonObject)?.get("input_modalities")?.jsonArray
                ?.map { it.jsonPrimitive.content } ?: return@mapNotNull null
            if ("image" !in inputs) return@mapNotNull null
            val id = model["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            ModelInfo(id, model["name"]?.jsonPrimitive?.content ?: id)
        }.sortedBy { it.id }

    // Cache format kept in preferences: one "id<TAB>name" per line.
    fun encode(models: List<ModelInfo>): String = models.joinToString("\n") { "${it.id}\t${it.name}" }

    fun decode(cache: String): List<ModelInfo> = cache.lines().mapNotNull { line ->
        val parts = line.split('\t', limit = 2)
        parts[0].takeIf { it.isNotBlank() }?.let { ModelInfo(it, parts.getOrElse(1) { parts[0] }) }
    }
}
