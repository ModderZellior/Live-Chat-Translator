package com.zellior.chattranslator.service;

import com.zellior.chattranslator.config.TranslatorConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class TranslationService {
	private static final Map<String, String> LANGUAGE_ALIASES = Map.ofEntries(
Map.entry("afrikaans", "af"),
			Map.entry("albanian", "sq"),
			Map.entry("amharic", "am"),
			Map.entry("arabic", "ar"),
			Map.entry("armenian", "hy"),
			Map.entry("assamese", "as"),
			Map.entry("azerbaijani", "az"),
			Map.entry("basque", "eu"),
			Map.entry("belarusian", "be"),
			Map.entry("bengali", "bn"),
			Map.entry("bosnian", "bs"),
			Map.entry("bulgarian", "bg"),
			Map.entry("burmese", "my"),
			Map.entry("catalan", "ca"),
			Map.entry("cebuano", "ceb"),
			Map.entry("chinese", "zh-CN"),
			Map.entry("chinese-simplified", "zh-CN"),
			Map.entry("chinese-traditional", "zh-TW"),
			Map.entry("corsican", "co"),
			Map.entry("croatian", "hr"),
			Map.entry("czech", "cs"),
			Map.entry("danish", "da"),
			Map.entry("dutch", "nl"),
			Map.entry("english", "en"),
			Map.entry("esperanto", "eo"),
			Map.entry("estonian", "et"),
			Map.entry("farsi", "fa"),
			Map.entry("filipino", "tl"),
			Map.entry("finnish", "fi"),
			Map.entry("french", "fr"),
			Map.entry("galician", "gl"),
			Map.entry("georgian", "ka"),
			Map.entry("german", "de"),
			Map.entry("greek", "el"),
			Map.entry("gujarati", "gu"),
			Map.entry("haitian-creole", "ht"),
			Map.entry("hebrew", "he"),
			Map.entry("hindi", "hi"),
			Map.entry("hmong", "hmn"),
			Map.entry("hungarian", "hu"),
			Map.entry("icelandic", "is"),
			Map.entry("igbo", "ig"),
			Map.entry("indonesian", "id"),
			Map.entry("irish", "ga"),
			Map.entry("italian", "it"),
			Map.entry("iw", "he"),
			Map.entry("japanese", "ja"),
			Map.entry("javanese", "jw"),
			Map.entry("kannada", "kn"),
			Map.entry("kazakh", "kk"),
			Map.entry("khmer", "km"),
			Map.entry("korean", "ko"),
			Map.entry("kurdish-kurmanji", "ku"),
			Map.entry("kurmanji", "ku"),
			Map.entry("kyrgyz", "ky"),
			Map.entry("lao", "lo"),
			Map.entry("latin", "la"),
			Map.entry("latvian", "lv"),
			Map.entry("lithuanian", "lt"),
			Map.entry("luxembourgish", "lb"),
			Map.entry("macedonian", "mk"),
			Map.entry("malay", "ms"),
			Map.entry("malayalam", "ml"),
			Map.entry("maltese", "mt"),
			Map.entry("maori", "mi"),
			Map.entry("marathi", "mr"),
			Map.entry("mongolian", "mn"),
			Map.entry("myanmar-burmese", "my"),
			Map.entry("nb", "no"),
			Map.entry("nepali", "ne"),
			Map.entry("norwegian", "no"),
			Map.entry("odia", "or"),
			Map.entry("pashto", "ps"),
			Map.entry("persian", "fa"),
			Map.entry("polish", "pl"),
			Map.entry("portuguese", "pt"),
			Map.entry("punjabi", "pa"),
			Map.entry("romanian", "ro"),
			Map.entry("russian", "ru"),
			Map.entry("samoan", "sm"),
			Map.entry("scots-gaelic", "gd"),
			Map.entry("serbian", "sr"),
			Map.entry("simplified", "zh-CN"),
			Map.entry("simplified-chinese", "zh-CN"),
			Map.entry("sinhala", "si"),
			Map.entry("slovak", "sk"),
			Map.entry("slovenian", "sl"),
			Map.entry("somali", "so"),
			Map.entry("spanish", "es"),
			Map.entry("sundanese", "su"),
			Map.entry("swahili", "sw"),
			Map.entry("swedish", "sv"),
			Map.entry("tagalog", "tl"),
			Map.entry("tamil", "ta"),
			Map.entry("telugu", "te"),
			Map.entry("thai", "th"),
			Map.entry("traditional", "zh-TW"),
			Map.entry("traditional-chinese", "zh-TW"),
			Map.entry("turkish", "tr"),
			Map.entry("ukrainian", "uk"),
			Map.entry("urdu", "ur"),
			Map.entry("uyghur", "ug"),
			Map.entry("uzbek", "uz"),
			Map.entry("vietnamese", "vi"),
			Map.entry("welsh", "cy"),
			Map.entry("xhosa", "xh"),
			Map.entry("yiddish", "yi"),
			Map.entry("yoruba", "yo"),
			Map.entry("zh", "zh-CN"),
			Map.entry("zulu", "zu")
	);

	private final TranslatorConfig config;
	private final HttpClient httpClient;

	public TranslationService(TranslatorConfig config) {
		this.config = config;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
				.build();
	}

	public CompletableFuture<TranslationResult> translate(String text, String targetLanguage) {
		Objects.requireNonNull(text, "text");
		String normalizedTarget = normalizeTarget(targetLanguage);
		if (text.isBlank()) {
			return CompletableFuture.completedFuture(new TranslationResult(text, "auto", normalizedTarget, false));
		}

		HttpRequest request = HttpRequest.newBuilder()
				.uri(buildGoogleTranslateUri(text, normalizedTarget))
				.timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
				.header("Accept", "application/json")
				.header("User-Agent", "Mozilla/5.0")
				.GET()
				.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenApply(response -> applyChatCorrections(parseGoogleTranslateResponse(response, normalizedTarget), text));
	}

	private URI buildGoogleTranslateUri(String text, String targetLanguage) {
		String query = "client=gtx"
				+ "&sl=auto"
				+ "&tl=" + URLEncoder.encode(toGoogleTargetLanguage(targetLanguage), StandardCharsets.UTF_8)
				+ "&dt=t"
				+ "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

		return URI.create(config.getGoogleTranslateEndpoint() + "?" + query);
	}

	private TranslationResult parseGoogleTranslateResponse(HttpResponse<String> response, String targetLanguage) {
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new TranslationException("Google Translate returned HTTP " + response.statusCode() + ": " + response.body());
		}

		JsonArray root = new JsonParser().parse(response.body()).getAsJsonArray();
		if (root.size() == 0 || !root.get(0).isJsonArray()) {
			throw new TranslationException("Google Translate response did not include translation parts.");
		}

		StringBuilder translatedText = new StringBuilder();
		for (JsonElement partElement : root.get(0).getAsJsonArray()) {
			if (partElement.isJsonArray()) {
				JsonArray part = partElement.getAsJsonArray();
				if (part.size() > 0 && !part.get(0).isJsonNull()) {
					translatedText.append(part.get(0).getAsString());
				}
			}
		}

		String detectedLanguage = "auto";
		if (root.size() > 2 && root.get(2).isJsonPrimitive()) {
			detectedLanguage = root.get(2).getAsString();
		}

		if (translatedText.isEmpty()) {
			throw new TranslationException("Google Translate response did not include translated text.");
		}

		return new TranslationResult(translatedText.toString(), detectedLanguage, targetLanguage, true);
	}

	private String normalizeTarget(String targetLanguage) {
		if (targetLanguage == null || targetLanguage.isBlank()) {
			return "en";
		}

		String normalized = targetLanguage.trim().toLowerCase(Locale.ROOT);
		String aliased = LANGUAGE_ALIASES.get(normalized);
		if (aliased != null) {
			return aliased;
		}

		for (String isoLanguage : Locale.getISOLanguages()) {
			if (new Locale(isoLanguage).getDisplayLanguage(Locale.ENGLISH).toLowerCase(Locale.ROOT).equals(normalized)) {
				return isoLanguage;
			}
		}

		return normalized;
	}

	private String toGoogleTargetLanguage(String targetLanguage) {
		if (targetLanguage.equalsIgnoreCase("zh")) {
			return "zh-CN";
		}

		if (targetLanguage.equalsIgnoreCase("zh-CN") || targetLanguage.equalsIgnoreCase("zh-TW")) {
			return targetLanguage;
		}

		return targetLanguage.toLowerCase(Locale.ROOT);
	}

	private TranslationResult applyChatCorrections(TranslationResult result, String originalText) {
		if (!result.targetLanguage().equals("en") || !originalText.toLowerCase(Locale.ROOT).contains("baguette")) {
			return result;
		}

		String corrected = result.translatedText()
				.replace("a wand", "a baguette")
				.replace("the wand", "the baguette")
				.replace("my wand", "my baguette")
				.replace("wand", "baguette");

		return new TranslationResult(corrected, result.detectedSourceLanguage(), result.targetLanguage(), result.wasTranslated());
	}

	public record TranslationResult(String translatedText, String detectedSourceLanguage, String targetLanguage, boolean wasTranslated) {
	}

	public static final class TranslationException extends RuntimeException {
		public TranslationException(String message) {
			super(message);
		}

		public TranslationException(String message, IOException cause) {
			super(message, cause);
		}
	}
}
