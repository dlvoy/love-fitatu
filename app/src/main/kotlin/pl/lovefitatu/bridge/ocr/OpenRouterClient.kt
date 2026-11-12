package pl.lovefitatu.bridge.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
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

class OpenRouterException(message: String, val rawReply: String? = null) : Exception(message)

class OpenRouterClient(private val apiKey: String, private val model: String) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun readLabel(imageDataUrl: String): LabelResult = withContext(Dispatchers.IO) {
        // Not every model accepts a strict schema, so fall back to a plain request once.
        val reply = runCatching { complete(imageDataUrl, structured = true) }
            .getOrElse { first ->
                if (first is OpenRouterException && first.message?.startsWith("HTTP 4") == true &&
                    !first.message!!.startsWith("HTTP 401") && !first.message!!.startsWith("HTTP 402")
                ) complete(imageDataUrl, structured = false)
                else throw first
            }
        try {
            LabelResult.parse(reply)
        } catch (e: Exception) {
            throw OpenRouterException("Nie udało się odczytać odpowiedzi modelu: ${e.message}", reply)
        }
    }

    private fun complete(imageDataUrl: String, structured: Boolean): String {
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("X-Title", "Label bridge")
            .post(requestBody(imageDataUrl, structured).toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) throw OpenRouterException("HTTP ${response.code}: ${errorMessage(text)}", text)
            val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
                ?: throw OpenRouterException("Nieprawidłowa odpowiedź OpenRouter", text)
            root["error"]?.let { throw OpenRouterException("OpenRouter: ${errorMessage(text)}", text) }
            return runCatching {
                root["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
            }.getOrElse { throw OpenRouterException("Brak treści w odpowiedzi modelu", text) }
        }
    }

    private fun errorMessage(body: String): String = runCatching {
        Json.parseToJsonElement(body).jsonObject["error"]!!.jsonObject["message"]!!.jsonPrimitive.content
    }.getOrDefault(body.take(300))

    private fun requestBody(imageDataUrl: String, structured: Boolean): JsonObject = buildJsonObject {
        put("model", model)
        put("temperature", 0)
        putJsonArray("messages") {
            add(buildJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    add(buildJsonObject { put("type", "text"); put("text", PROMPT) })
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
                putJsonObject("schema") {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        for (name in listOf("day", "code", "dish_name")) putJsonObject(name) {
                            putJsonArray("type") { add("string"); add("null") }
                        }
                        for (name in listOf("kcal", "fat_g", "carbs_g", "protein_g")) putJsonObject(name) {
                            putJsonArray("type") { add("number"); add("null") }
                        }
                    }
                    putJsonArray("required") {
                        listOf("day", "code", "dish_name", "kcal", "fat_g", "carbs_g", "protein_g").forEach { add(it) }
                    }
                }
            }
        }
    }

    companion object {
        val PROMPT = """
            This is a photo of a Polish diet-catering meal label (Love Catering). The photo may be rotated.
            Read it and reply with ONLY a JSON object with these keys:
            - "day": the weekday name printed on the label in Polish (e.g. "Czwartek"), or null.
              Only the weekday word; ignore every printed date ("Data produkcji", best-before dates)
            - "code": the dish code exactly as printed, in the form number/letter/letter (e.g. "10284/K/M"), or null
            - "dish_name": the dish title in Polish, as printed, or null
            - "kcal": number after "Wartość energetyczna" (kcal)
            - "fat_g": number after "Tłuszcz"
            - "carbs_g": number after "Węglowodany"
            - "protein_g": number after "Białko"
            Rules: values on the label use a decimal comma; output JSON numbers with a decimal point.
            The values are for the whole meal; copy them as printed, do not convert or recalculate.
            Do NOT confuse the main values with "W tym cukry" (sugars), "Nas. kw. tłuszczowe"
            (saturated fat) or "Błonnik" (fibre). The large handwritten-style number on the label
            is not part of the code. Use null for anything you cannot read with confidence; never guess.
        """.trimIndent()
    }
}
