package pl.lovefitatu.bridge.ocr

import pl.lovefitatu.bridge.R

/**
 * [rawReply] is what the provider sent back; [httpCode] is set when it answered with an HTTP error;
 * [usage] is set when the reply was billed although the app could not use it.
 */
class LlmException(
    message: String,
    val rawReply: String? = null,
    val httpCode: Int? = null,
    val usage: LlmUsage? = null,
) : Exception(message)

/**
 * A service that runs vision models. [preferred] is shown on top of the model picker and
 * [defaultModel] is used until the user picks one. [visionOnlyList] says whether the provider's
 * model list can be narrowed to models that accept images.
 */
enum class LlmProvider(
    val id: String,
    val label: String,
    val icon: Int,
    val keysUrl: String,
    val api: Api,
    val baseUrl: String,
    val defaultModel: String,
    val preferred: List<String>,
    val visionOnlyList: Boolean = false,
) {
    OPENROUTER(
        "openrouter", "OpenRouter", R.drawable.logo_openrouter, "https://openrouter.ai/settings/keys",
        Api.OPENAI, "https://openrouter.ai/api/v1",
        "google/gemini-2.5-flash", listOf("anthropic/claude-haiku-5.5", "google/gemini-2.5-flash"),
        visionOnlyList = true,
    ),
    ANTHROPIC(
        "anthropic", "Anthropic", R.drawable.logo_anthropic, "https://platform.claude.com/settings/keys",
        Api.ANTHROPIC, "https://api.anthropic.com/v1",
        "claude-haiku-5-5", listOf("claude-haiku-5-5", "claude-sonnet-5-5"),
    ),
    OPENAI(
        "openai", "OpenAI", R.drawable.logo_openai, "https://platform.openai.com/api-keys",
        Api.OPENAI, "https://api.openai.com/v1",
        "gpt-6-luna", listOf("gpt-6-luna"),
    ),
    TOGETHER(
        "together", "Together AI", R.drawable.logo_together, "https://api.together.ai/settings/api-keys",
        Api.OPENAI, "https://api.together.xyz/v1",
        "Qwen/Qwen3.5-9B", listOf("Qwen/Qwen3.5-9B"),
    );

    /** Wire format: OpenAI-style chat completions, or Anthropic's Messages API. */
    enum class Api { OPENAI, ANTHROPIC }

    val completionsUrl: String get() = if (api == Api.ANTHROPIC) "$baseUrl/messages" else "$baseUrl/chat/completions"
    val modelsUrl: String get() = if (api == Api.ANTHROPIC) "$baseUrl/models?limit=1000" else "$baseUrl/models"

    fun authHeaders(apiKey: String): Map<String, String> = when (api) {
        Api.ANTHROPIC -> mapOf("x-api-key" to apiKey, "anthropic-version" to "2026-10-21")
        Api.OPENAI -> mapOf("Authorization" to "Bearer $apiKey")
    }

    companion object {
        fun byId(id: String?): LlmProvider = entries.firstOrNull { it.id == id } ?: OPENROUTER
    }
}
