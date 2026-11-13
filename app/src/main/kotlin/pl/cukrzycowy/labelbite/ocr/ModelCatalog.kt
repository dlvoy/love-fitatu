package pl.cukrzycowy.labelbite.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ModelInfo(val id: String, val name: String)

object ModelCatalog {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** OpenAI lists every kind of model with no modality field; these id fragments mark the non-chat ones. */
    private val OPENAI_NOT_CHAT = listOf(
        "embedding", "whisper", "tts", "dall-e", "moderation", "audio", "realtime", "transcribe",
        "image", "sora", "babbage", "davinci", "instruct", "search",
    )

    /** The provider's models usable for label reading, sorted by id. */
    suspend fun fetchModels(provider: LlmProvider, apiKey: String): List<ModelInfo> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(provider.modelsUrl)
            .apply { provider.authHeaders(apiKey).forEach { (name, value) -> header(name, value) } }
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) throw LlmException("HTTP ${response.code}", body, response.code)
            parse(provider, body)
        }
    }

    fun parse(provider: LlmProvider, body: String): List<ModelInfo> {
        val root = Json.parseToJsonElement(body)
        // Together returns a bare array, the others wrap the list in "data".
        val items = (root as? JsonArray ?: (root as? JsonObject)?.get("data") as? JsonArray).orEmpty()
        return items.mapNotNull { element ->
            val model = element as? JsonObject ?: return@mapNotNull null
            val id = model.text("id") ?: return@mapNotNull null
            val keep = when (provider) {
                LlmProvider.OPENROUTER ->
                    ((model["architecture"] as? JsonObject)?.get("input_modalities") as? JsonArray)
                        ?.any { (it as? JsonPrimitive)?.content == "image" } == true
                LlmProvider.ANTHROPIC -> true
                LlmProvider.OPENAI -> OPENAI_NOT_CHAT.none { id.contains(it, ignoreCase = true) }
                LlmProvider.TOGETHER -> model.text("type").let { it == null || it == "chat" }
            }
            if (keep) ModelInfo(id, model.text("display_name") ?: model.text("name") ?: id) else null
        }.sortedBy { it.id }
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

    // Cache format kept in preferences: one "id<TAB>name" per line.
    fun encode(models: List<ModelInfo>): String = models.joinToString("\n") { "${it.id}\t${it.name}" }

    fun decode(cache: String): List<ModelInfo> = cache.lines().mapNotNull { line ->
        val parts = line.split('\t', limit = 2)
        parts[0].takeIf { it.isNotBlank() }?.let { ModelInfo(it, parts.getOrElse(1) { parts[0] }) }
    }
}
