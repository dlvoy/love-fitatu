package pl.lovefitatu.bridge.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Reads a label photo with one provider's model, using the catering-specific [prompt]. */
class LabelReader(
    private val provider: LlmProvider,
    private val apiKey: String,
    private val model: String,
    private val prompt: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun readLabel(imageDataUrl: String): LabelResult = withContext(Dispatchers.IO) {
        // Not every model accepts a strict schema (or a temperature), so fall back to a plain request once.
        val reply = try {
            complete(imageDataUrl, structured = true)
        } catch (first: LlmException) {
            if (LlmWire.worthPlainRetry(first.httpCode)) complete(imageDataUrl, structured = false) else throw first
        }
        try {
            LabelResult.parse(reply)
        } catch (e: Exception) {
            throw LlmException("Nie udało się odczytać odpowiedzi modelu: ${e.message}", reply)
        }
    }

    private fun complete(imageDataUrl: String, structured: Boolean): String {
        val anthropic = provider.api == LlmProvider.Api.ANTHROPIC
        val body = if (anthropic) LlmWire.anthropicBody(model, prompt, imageDataUrl, structured)
        else LlmWire.openAiBody(model, prompt, imageDataUrl, structured)
        val request = Request.Builder()
            .url(provider.completionsUrl)
            .apply { provider.authHeaders(apiKey).forEach { (name, value) -> header(name, value) } }
            .apply { if (provider == LlmProvider.OPENROUTER) header("X-Title", "Label bridge") }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) {
                throw LlmException("HTTP ${response.code}: ${LlmWire.errorMessage(text)}", text, response.code)
            }
            val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
                ?: throw LlmException("Nieprawidłowa odpowiedź ${provider.label}", text)
            if (root["error"] is JsonObject) throw LlmException("${provider.label}: ${LlmWire.errorMessage(text)}", text)
            return (if (anthropic) LlmWire.anthropicReply(root) else LlmWire.openAiReply(root))
                ?: throw LlmException(LlmWire.emptyReplyMessage(root), text)
        }
    }
}

/** Request and reply shapes of the two wire formats. Pure, so the unit tests cover them. */
object LlmWire {
    private val TEXT_FIELDS = listOf("day", "code", "dish_name")
    private val NUMBER_FIELDS = listOf("kcal", "fat_g", "carbs_g", "protein_g")

    /** A 4xx may mean "this model rejects the schema"; a bad key, no credit or a rate limit does not. */
    fun worthPlainRetry(httpCode: Int?): Boolean =
        httpCode != null && httpCode in 400..499 && httpCode !in setOf(401, 402, 403, 429)

    fun errorMessage(body: String): String = runCatching {
        Json.parseToJsonElement(body).jsonObject["error"]!!.jsonObject["message"]!!.jsonPrimitive.content
    }.getOrDefault(body.take(300))

    fun labelSchema(): JsonObject = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        putJsonObject("properties") {
            for (name in TEXT_FIELDS) putJsonObject(name) { putJsonArray("type") { add("string"); add("null") } }
            for (name in NUMBER_FIELDS) putJsonObject(name) { putJsonArray("type") { add("number"); add("null") } }
        }
        putJsonArray("required") { (TEXT_FIELDS + NUMBER_FIELDS).forEach { add(it) } }
    }

    // ------------------------------------------- OpenAI-style chat completions

    fun openAiBody(model: String, prompt: String, imageDataUrl: String, structured: Boolean): JsonObject = buildJsonObject {
        put("model", model)
        // Some models only accept their default temperature, so the plain retry leaves it out.
        if (structured) put("temperature", 0)
        putJsonArray("messages") {
            add(buildJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    add(buildJsonObject { put("type", "text"); put("text", prompt) })
                    add(buildJsonObject {
                        put("type", "image_url")
                        putJsonObject("image_url") { put("url", imageDataUrl) }
                    })
                }
            })
        }
        if (structured) putJsonObject("response_format") {
            put("type", "json_schema")
            putJsonObject("json_schema") {
                put("name", "label")
                put("strict", true)
                put("schema", labelSchema())
            }
        }
    }

    /** The assistant text, or null when there is none (for example a refusal). */
    fun openAiReply(root: JsonObject): String? {
        val content = firstMessage(root)?.get("content")
        return when (content) {
            is JsonNull -> null
            is JsonPrimitive -> content.content
            // Some models return the content as a list of parts.
            is JsonArray -> content.joinToString("") { part -> (part as? JsonObject)?.text("text").orEmpty() }
            else -> null
        }?.takeIf { it.isNotBlank() }
    }

    // -------------------------------------------------- Anthropic Messages API

    fun anthropicBody(model: String, prompt: String, imageDataUrl: String, structured: Boolean): JsonObject = buildJsonObject {
        val (mediaType, data) = splitDataUrl(imageDataUrl)
        put("model", model)
        // Covers the model's thinking as well as the short JSON answer. No temperature: current
        // Claude models reject sampling parameters.
        put("max_tokens", 16000)
        putJsonArray("messages") {
            add(buildJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    add(buildJsonObject {
                        put("type", "image")
                        putJsonObject("source") {
                            put("type", "base64")
                            put("media_type", mediaType)
                            put("data", data)
                        }
                    })
                    add(buildJsonObject { put("type", "text"); put("text", prompt) })
                }
            })
        }
        if (structured) putJsonObject("output_config") {
            putJsonObject("format") {
                put("type", "json_schema")
                put("schema", labelSchema())
            }
        }
    }

    /** The joined text blocks, or null on a refusal or when the reply holds no text. */
    fun anthropicReply(root: JsonObject): String? {
        if (root.text("stop_reason") == "refusal") return null
        return (root["content"] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .filter { it.text("type") == "text" }
            .joinToString("") { it.text("text").orEmpty() }
            .takeIf { it.isNotBlank() }
    }

    /** Why [openAiReply] or [anthropicReply] returned null. */
    fun emptyReplyMessage(root: JsonObject): String {
        val refusal = firstMessage(root)?.text("refusal")
        return when {
            root.text("stop_reason") == "refusal" -> "Model odmówił odczytania etykiety"
            refusal != null -> "Model odmówił odczytania etykiety: $refusal"
            root.text("stop_reason") == "max_tokens" -> "Model nie zmieścił odpowiedzi w limicie tokenów"
            else -> "Brak treści w odpowiedzi modelu"
        }
    }

    /** `data:image/jpeg;base64,AAAA` → (`image/jpeg`, `AAAA`). */
    fun splitDataUrl(dataUrl: String): Pair<String, String> {
        val comma = dataUrl.indexOf(',')
        require(dataUrl.startsWith("data:") && comma > 0) { "not a data URL" }
        return dataUrl.substring(5, comma).substringBefore(';') to dataUrl.substring(comma + 1)
    }

    private fun firstMessage(root: JsonObject): JsonObject? =
        ((root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject)?.get("message") as? JsonObject

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
}
