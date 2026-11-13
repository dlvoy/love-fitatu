package pl.cukrzycowy.labelbite.ocr

/**
 * Tokens billed for one label read and what they cost. [costUsd] is null when the provider did not
 * report a cost and the model has no list price; [costEstimated] is true when the app worked the
 * cost out from [ModelPrices] instead of getting it from the provider.
 */
data class LlmUsage(
    val inputTokens: Long,
    val outputTokens: Long,
    val costUsd: Double? = null,
    val costEstimated: Boolean = false,
) {
    /** The usage keys of the history `[meta]` section. */
    fun toToml(): Map<String, Any> = buildMap {
        put("input_tokens", inputTokens)
        put("output_tokens", outputTokens)
        costUsd?.let {
            put("cost_usd", it)
            put("cost_estimated", costEstimated)
        }
    }
}

/**
 * List prices in USD per million tokens (input, output) for the providers that report tokens but
 * no cost. Taken on 2026-11-13 from the providers' public price lists (Anthropic and OpenAI as
 * mirrored by openrouter.ai/api/v1/models, Together AI from together.ai/pricing). Volume tiers,
 * batch and cache discounts are left out: a label read is one small uncached request.
 */
object ModelPrices {
    private val TABLE: Map<LlmProvider, Map<String, Pair<Double, Double>>> = mapOf(
        LlmProvider.ANTHROPIC to mapOf(
            "claude-fable-5-2" to (8.0 to 40.0),
            "claude-fable-5-1" to (10.0 to 50.0),
            "claude-fable-5" to (10.0 to 50.0),
            "claude-opus-5-5" to (4.0 to 20.0),
            "claude-opus-5" to (5.0 to 25.0),
            "claude-opus-4-8" to (5.0 to 25.0),
            "claude-opus-4-7" to (5.0 to 25.0),
            "claude-opus-4-6" to (5.0 to 25.0),
            "claude-opus-4-5" to (5.0 to 25.0),
            "claude-opus-4-1" to (15.0 to 75.0),
            "claude-sonnet-5-5" to (2.0 to 10.0),
            "claude-sonnet-5" to (2.0 to 10.0),
            "claude-sonnet-4-6" to (3.0 to 15.0),
            "claude-sonnet-4-5" to (3.0 to 15.0),
            "claude-haiku-5-5" to (0.1 to 0.5),
            "claude-haiku-4-5" to (1.0 to 5.0),
        ),
        LlmProvider.OPENAI to mapOf(
            "gpt-6.1-sol" to (2.0 to 10.0),
            "gpt-6.1-terra" to (1.0 to 5.0),
            "gpt-6-astra" to (10.0 to 50.0),
            "gpt-6-sol" to (2.0 to 10.0),
            "gpt-6-luna" to (0.1 to 0.5),
            "gpt-5.6-terra" to (2.0 to 12.0),
            "gpt-5.6-luna" to (0.2 to 1.2),
            "gpt-5.5-pro" to (30.0 to 180.0),
            "gpt-5.5" to (5.0 to 30.0),
            "gpt-5.4-pro" to (30.0 to 180.0),
            "gpt-5.4-mini" to (0.75 to 4.5),
            "gpt-5.4-nano" to (0.2 to 1.25),
            "gpt-5.4" to (2.5 to 15.0),
        ),
        LlmProvider.TOGETHER to mapOf(
            "qwen/qwen3.5-9b" to (0.17 to 0.25),
        ),
    )

    /**
     * (input, output) USD per million tokens. An id matches an entry when it equals it or continues
     * it after a `-` (a date or variant suffix); the longest entry wins.
     */
    fun priceOf(provider: LlmProvider, model: String): Pair<Double, Double>? {
        val id = model.trim().lowercase()
        return TABLE[provider].orEmpty()
            .filterKeys { id == it || id.startsWith("$it-") }
            .maxByOrNull { it.key.length }?.value
    }

    /** The cost in USD of one call, or null for a model without a list price. */
    fun estimate(provider: LlmProvider, model: String, inputTokens: Long, outputTokens: Long): Double? =
        priceOf(provider, model)?.let { (input, output) -> (inputTokens * input + outputTokens * output) / 1_000_000 }
}
