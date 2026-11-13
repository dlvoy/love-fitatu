package pl.lovefitatu.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lovefitatu.bridge.automation.AapsGuard
import pl.lovefitatu.bridge.automation.Format
import pl.lovefitatu.bridge.data.DishCode
import pl.lovefitatu.bridge.data.LabelDate
import pl.lovefitatu.bridge.data.Meal
import pl.lovefitatu.bridge.data.MealMapping
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pl.lovefitatu.bridge.data.Catering
import pl.lovefitatu.bridge.data.LabelPrompt
import pl.lovefitatu.bridge.ocr.LabelResult
import pl.lovefitatu.bridge.ocr.LlmProvider
import pl.lovefitatu.bridge.ocr.LlmWire
import pl.lovefitatu.bridge.ocr.ModelCatalog
import pl.lovefitatu.bridge.ocr.ModelInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import pl.lovefitatu.bridge.data.History
import pl.lovefitatu.bridge.data.Toml
import pl.lovefitatu.bridge.ui.clampPan
import pl.lovefitatu.bridge.ui.detailRows
import pl.lovefitatu.bridge.ui.weekTitle
import pl.lovefitatu.bridge.ui.canUseTypedId
import pl.lovefitatu.bridge.ui.filterModels
import pl.lovefitatu.bridge.ui.preferredModels
import java.time.LocalDate

class BridgeTest {

    @Test
    fun parsesModelReplyWithCodeFence() {
        val reply = """```json
            {"day":"Czwartek","code":"10284/K/M","dish_name":"Sałatka z mango","kcal":302,"fat_g":13.3,"carbs_g":16.8,"protein_g":26.8}
            ```"""
        val r = LabelResult.parse(reply)
        assertEquals("10284/K/M", r.code)
        assertEquals(302.0, r.kcal!!, 0.0)
        assertEquals(16.8, r.carbs!!, 0.0)
        assertFalse(r.energyMismatch())
    }

    @Test
    fun nullsStayNullAndMismatchIsFlagged() {
        assertNull(LabelResult.parse("""{"kcal":null,"code":null}""").kcal)
        assertTrue(LabelResult(kcal = 302.0, fat = 13.3, carbs = 13.2, protein = 2.5).energyMismatch())
    }

    @Test
    fun dishCode() {
        assertEquals(DishCode("10284", "K", "M"), DishCode.parse("10284/K/M"))
        assertEquals(DishCode("4738", "Ś", "L"), DishCode.parse("kod 4738 / ś / l"))
        assertNull(DishCode.parse("174"))
    }

    @Test
    fun mealLetters() {
        val spec = MealMapping.DEFAULT
        assertEquals(Meal.SUPPER, MealMapping.mealFor("K", spec))
        assertEquals(Meal.BREAKFAST, MealMapping.mealFor("Ś", spec))
        assertEquals(Meal.BREAKFAST, MealMapping.mealFor("S", spec))
        assertEquals(Meal.DINNER, MealMapping.mealFor("O", spec))
        assertEquals(Meal.SNACK, MealMapping.mealFor("P", spec))
        assertNull(MealMapping.mealFor("X", spec))
    }

    @Test
    fun labelWeekdayMapsIntoCurrentWeek() {
        val thursday = LocalDate.of(2026, 11, 12)
        assertEquals(thursday, LabelDate.inCurrentWeek("Czwartek", thursday))
        assertEquals(thursday.plusDays(1), LabelDate.inCurrentWeek("Piątek", thursday))
        assertEquals(thursday.minusDays(1), LabelDate.inCurrentWeek("ŚRODA", thursday))
        assertEquals(LocalDate.of(2026, 11, 9), LabelDate.inCurrentWeek("poniedziałek", thursday))
        assertEquals(LocalDate.of(2026, 11, 15), LabelDate.inCurrentWeek("Niedziela", thursday))
        // On a Sunday the week is still the one that started last Monday.
        assertEquals(LocalDate.of(2026, 11, 9), LabelDate.inCurrentWeek("Poniedziałek", LocalDate.of(2026, 11, 15)))
        assertNull(LabelDate.inCurrentWeek(null, thursday))
        assertNull(LabelDate.inCurrentWeek("11/11/2026", thursday))
    }

    @Test
    fun fitatuNumbers() {
        assertEquals("302", Format.fitatuNumber(302.0))
        assertEquals("13,3", Format.fitatuNumber(13.3))
        assertEquals("16,8", Format.fitatuNumber(16.84))
        assertTrue(Format.sameNumber("13.3", "13,3"))
        assertFalse(Format.sameNumber("133", "13,3"))
    }

    @Test
    fun aapsGuardRefusesConfirmControls() {
        for (label in listOf("Potwierdź", "OK", " ok ", "Confirm", "POTWIERDŹ", "Zatwierdź bolus")) {
            assertTrue(label, AapsGuard.isForbiddenLabel(label))
        }
        for (label in listOf("Kalkulator bolusa", "Węglowodany", "Insulina", null)) {
            assertFalse(label.toString(), AapsGuard.isForbiddenLabel(label))
        }
        assertTrue(AapsGuard.isCalculatorLabel("Kalkulator bolusa"))
        assertFalse(AapsGuard.isCalculatorLabel("Potwierdź"))
    }

    @Test
    fun aapsCarbsRange() {
        assertTrue(AapsGuard.validCarbs(17))
        assertFalse(AapsGuard.validCarbs(0))
        assertFalse(AapsGuard.validCarbs(121))
    }

    @Test
    fun tomlRoundTrip() {
        val doc = mapOf(
            "meta" to mapOf("taken" to "2026-11-12T22:41:07", "photo" to "22-41-07.jpg"),
            "detected" to mapOf("code" to "10284/K/M", "dish_name" to "Sałatka \"z mango\" \\ chili", "kcal" to 302.0, "fat_g" to 13.3),
            "saved" to mapOf("aaps_carbs_g" to 17, "done" to true, "note" to "a # b = c\nnext"),
        )
        val text = Toml.write(doc)
        assertTrue(text.startsWith("[meta]\ntaken = \"2026-11-12T22:41:07\"\n"))
        assertTrue(text.contains("kcal = 302.0\n"))
        val parsed = Toml.parse(text)
        assertEquals(listOf("meta", "detected", "saved"), parsed.keys.toList())
        assertEquals("Sałatka \"z mango\" \\ chili", parsed["detected"]!!["dish_name"])
        assertEquals(302.0, parsed["detected"]!!["kcal"])
        assertEquals(17L, parsed["saved"]!!["aaps_carbs_g"])
        assertEquals(true, parsed["saved"]!!["done"])
        assertEquals("a # b = c\nnext", parsed["saved"]!!["note"])
        assertEquals(13.3, LabelResult.fromToml(parsed["detected"]!!).fat!!, 0.0)
        assertTrue(Toml.parse("garbage\n# comment\n[x]\nbroken").values.all { it.isEmpty() })
    }

    @Test
    fun historyWeeksStartOnMondayNewestFirst() {
        val days = listOf("2026-11-15", "2026-11-12", "2026-11-09", "2026-11-08").map(LocalDate::parse)
        val weeks = History.byWeek(days) { it }
        assertEquals(listOf(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-02")), weeks.map { it.first })
        assertEquals(3, weeks[0].second.size)
        assertEquals("Tydzień 46 · 9.11–15.11.2026", weekTitle(weeks[0].first))
    }

    @Test
    fun zoomPanIsClampedToImageEdges() {
        val size = IntSize(1000, 2000)
        fun check(x: Float, y: Float, actual: Offset) {
            assertEquals(x, actual.x, 0.001f)
            assertEquals(y, actual.y, 0.001f)
        }
        check(0f, 0f, clampPan(Offset(300f, -300f), 1f, size))
        check(500f, -1000f, clampPan(Offset(900f, -5000f), 2f, size))
        check(120f, 40f, clampPan(Offset(120f, 40f), 2f, size))
    }

    @Test
    fun detailRowsLabelAndFormat() {
        val rows = detailRows(mapOf("kcal" to 302.0, "fat_g" to 13.3, "meal" to "supper", "meal_label" to "Kolacja", "x" to 1L))
        assertEquals(listOf("kcal" to "302", "Tłuszcz (g)" to "13,3", "Posiłek" to "Kolacja", "x" to "1"), rows)
    }

    @Test
    fun modelCatalogKeepsOnlyVisionModels() {
        val body = """{"data":[
            {"id":"b/vision","name":"B Vision","architecture":{"input_modalities":["text","image"]}},
            {"id":"a/text","name":"A Text","architecture":{"input_modalities":["text"]}},
            {"id":"a/vision","name":"A Vision","architecture":{"input_modalities":["image","text"]}},
            {"id":"c/unknown","name":"C"}]}"""
        val models = ModelCatalog.parse(LlmProvider.OPENROUTER, body)
        assertEquals(listOf("a/vision", "b/vision"), models.map { it.id })
        assertEquals(models, ModelCatalog.decode(ModelCatalog.encode(models)))
        assertTrue(ModelCatalog.decode("").isEmpty())
    }

    @Test
    fun pickerPreferredAndSearch() {
        val fixed = LlmProvider.OPENROUTER.preferred
        assertEquals(fixed, preferredModels(fixed, ""))
        assertEquals(fixed, preferredModels(fixed, fixed[1]))
        assertEquals(fixed + "x/y", preferredModels(fixed, "x/y"))
        val models = listOf(ModelInfo("google/gemini-2.5-flash", "Gemini 2.5 Flash"), ModelInfo("openai/gpt-x", "GPT X"))
        assertEquals(1, filterModels(models, "GEMINI").size)
        assertEquals(1, filterModels(models, "gpt x").size)
        assertEquals(2, filterModels(models, " ").size)
        assertTrue(canUseTypedId("claude-haiku-5-5", models))
        assertFalse(canUseTypedId("openai/gpt-x", models))
        assertFalse(canUseTypedId("gpt x", models))
        assertFalse(canUseTypedId("", models))
    }

    @Test
    fun providerDefaultsAndLookup() {
        assertEquals("claude-haiku-5-5", LlmProvider.ANTHROPIC.defaultModel)
        assertEquals(listOf("claude-haiku-5-5", "claude-sonnet-5-5"), LlmProvider.ANTHROPIC.preferred)
        assertEquals("gpt-6-luna", LlmProvider.OPENAI.defaultModel)
        assertEquals("Qwen/Qwen3.5-9B", LlmProvider.TOGETHER.defaultModel)
        assertEquals("google/gemini-2.5-flash", LlmProvider.OPENROUTER.defaultModel)
        for (provider in LlmProvider.entries) {
            assertTrue(provider.id, provider.defaultModel in provider.preferred)
            assertEquals(provider, LlmProvider.byId(provider.id))
        }
        // An unknown or missing id is a configuration from before providers existed.
        assertEquals(LlmProvider.OPENROUTER, LlmProvider.byId(null))
        assertEquals("https://api.anthropic.com/v1/messages", LlmProvider.ANTHROPIC.completionsUrl)
        assertEquals("https://api.together.xyz/v1/chat/completions", LlmProvider.TOGETHER.completionsUrl)
        assertEquals(mapOf("Authorization" to "Bearer k"), LlmProvider.OPENAI.authHeaders("k"))
        assertEquals("k", LlmProvider.ANTHROPIC.authHeaders("k")["x-api-key"])
        assertFalse(LlmProvider.ANTHROPIC.authHeaders("k").containsKey("Authorization"))
    }

    @Test
    fun openAiStyleRequestAndReply() {
        val url = "data:image/jpeg;base64,QUJD"
        val structured = LlmWire.openAiBody("m", "read it", url, structured = true)
        assertEquals("m", structured["model"]!!.jsonPrimitive.content)
        assertEquals("0", structured["temperature"]!!.jsonPrimitive.content)
        val content = structured["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
        assertEquals("read it", content[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals(url, content[1].jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content)
        val schema = structured["response_format"]!!.jsonObject["json_schema"]!!.jsonObject["schema"]!!.jsonObject
        assertEquals(7, schema["required"]!!.jsonArray.size)

        val plain = LlmWire.openAiBody("m", "read it", url, structured = false)
        assertFalse(plain.containsKey("response_format"))
        assertFalse(plain.containsKey("temperature"))

        fun reply(json: String) = LlmWire.openAiReply(Json.parseToJsonElement(json).jsonObject)
        assertEquals("{}", reply("""{"choices":[{"message":{"content":"{}"}}]}"""))
        assertEquals("ab", reply("""{"choices":[{"message":{"content":[{"type":"text","text":"a"},{"type":"text","text":"b"}]}}]}"""))
        val refused = """{"choices":[{"message":{"content":null,"refusal":"no"}}]}"""
        assertNull(reply(refused))
        assertTrue(LlmWire.emptyReplyMessage(Json.parseToJsonElement(refused).jsonObject).endsWith(": no"))
        assertNull(reply("""{"choices":[]}"""))

        assertTrue(LlmWire.worthPlainRetry(400))
        assertTrue(LlmWire.worthPlainRetry(422))
        for (code in listOf(401, 402, 403, 429, 500, null)) assertFalse(code.toString(), LlmWire.worthPlainRetry(code))
        assertEquals("bad key", LlmWire.errorMessage("""{"error":{"message":"bad key"}}"""))
        assertEquals("plain", LlmWire.errorMessage("plain"))
    }

    @Test
    fun anthropicRequestAndReply() {
        val structured = LlmWire.anthropicBody("claude-haiku-5-5", "read it", "data:image/jpeg;base64,QUJD", structured = true)
        assertEquals("16000", structured["max_tokens"]!!.jsonPrimitive.content)
        // Current Claude models reject sampling parameters.
        assertFalse(structured.containsKey("temperature"))
        val content = structured["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
        val source = content[0].jsonObject["source"]!!.jsonObject
        assertEquals("base64", source["type"]!!.jsonPrimitive.content)
        assertEquals("image/jpeg", source["media_type"]!!.jsonPrimitive.content)
        assertEquals("QUJD", source["data"]!!.jsonPrimitive.content)
        assertEquals("read it", content[1].jsonObject["text"]!!.jsonPrimitive.content)
        val format = structured["output_config"]!!.jsonObject["format"]!!.jsonObject
        assertEquals("json_schema", format["type"]!!.jsonPrimitive.content)
        assertEquals(LlmWire.labelSchema(), format["schema"])
        assertFalse(LlmWire.anthropicBody("m", "p", "data:image/png;base64,QQ==", structured = false).containsKey("output_config"))

        fun root(json: String) = Json.parseToJsonElement(json).jsonObject
        val ok = root("""{"stop_reason":"end_turn","content":[{"type":"thinking","thinking":""},{"type":"text","text":"{\"kcal\":302}"}]}""")
        assertEquals(302.0, LabelResult.parse(LlmWire.anthropicReply(ok)!!).kcal!!, 0.0)
        val refusal = root("""{"stop_reason":"refusal","content":[{"type":"text","text":"partial"}]}""")
        assertNull(LlmWire.anthropicReply(refusal))
        assertEquals("Model odmówił odczytania etykiety", LlmWire.emptyReplyMessage(refusal))
        assertNull(LlmWire.anthropicReply(root("""{"stop_reason":"max_tokens","content":[]}""")))
    }

    @Test
    fun modelCatalogPerProvider() {
        val anthropic = """{"data":[{"id":"claude-sonnet-5-5","display_name":"Claude Sonnet 5.5"},{"id":"claude-haiku-5-5","display_name":"Claude Haiku 5.5"}]}"""
        assertEquals(
            listOf(ModelInfo("claude-haiku-5-5", "Claude Haiku 5.5"), ModelInfo("claude-sonnet-5-5", "Claude Sonnet 5.5")),
            ModelCatalog.parse(LlmProvider.ANTHROPIC, anthropic),
        )
        val openAi = """{"data":[{"id":"gpt-6-luna"},{"id":"text-embedding-3-small"},{"id":"whisper-1"},{"id":"gpt-image-1"}]}"""
        assertEquals(listOf(ModelInfo("gpt-6-luna", "gpt-6-luna")), ModelCatalog.parse(LlmProvider.OPENAI, openAi))
        val together = """[{"id":"Qwen/Qwen3.5-9B","display_name":"Qwen 3.5 9B","type":"chat"},{"id":"x/flux","type":"image"}]"""
        assertEquals(listOf(ModelInfo("Qwen/Qwen3.5-9B", "Qwen 3.5 9B")), ModelCatalog.parse(LlmProvider.TOGETHER, together))
    }

    @Test
    fun cateringPrompts() {
        val love = LabelPrompt.forCatering(Catering.LOVE, "ignored")
        assertEquals(LabelPrompt.LOVE, love)
        assertTrue(love.contains("Love Catering"))
        val custom = LabelPrompt.forCatering(Catering.CUSTOM, "  Kalorie stoją po słowie Energia.  ")
        assertTrue(custom.contains("Kalorie stoją po słowie Energia.\n\n"))
        assertFalse(custom.contains("Love Catering"))
        assertTrue(custom.endsWith(LabelPrompt.OUTPUT_RULES))
        // The reply keys are fixed whatever the user writes, and an empty description falls back.
        for (key in listOf("day", "code", "dish_name", "kcal", "fat_g", "carbs_g", "protein_g")) {
            assertTrue(key, love.contains("\"" + key + "\""))
            assertTrue(key, custom.contains("\"" + key + "\""))
        }
        assertTrue(LabelPrompt.forCatering(Catering.CUSTOM, " ").contains(LabelPrompt.CUSTOM_DEFAULT))
        assertEquals(Catering.LOVE, Catering.byId(null))
        assertEquals(Catering.CUSTOM, Catering.byId("custom"))
    }
}
