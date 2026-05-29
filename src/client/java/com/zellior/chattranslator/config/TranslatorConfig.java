package com.zellior.chattranslator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Locale;

public final class TranslatorConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_FILE_NAME = "chat-translator.json";
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

	private boolean chatTranslationEnabled = true;
	private String incomingTargetLanguage = "en";
	private String googleTranslateEndpoint = "https://translate.googleapis.com/translate_a/single";
	private int requestTimeoutSeconds = 8;

	public static TranslatorConfig load() {
		Path path = configPath();
		if (!Files.exists(path)) {
			TranslatorConfig config = new TranslatorConfig();
			config.save();
			return config;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			TranslatorConfig config = GSON.fromJson(reader, TranslatorConfig.class);
			return config == null ? new TranslatorConfig() : config;
		} catch (IOException exception) {
			return new TranslatorConfig();
		}
	}

	public void save() {
		try {
			Files.createDirectories(configPath().getParent());
			try (Writer writer = Files.newBufferedWriter(configPath())) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public boolean isChatTranslationEnabled() {
		return chatTranslationEnabled;
	}

	public void setChatTranslationEnabled(boolean chatTranslationEnabled) {
		this.chatTranslationEnabled = chatTranslationEnabled;
	}

	public String getIncomingTargetLanguage() {
		return normalizeLanguage(incomingTargetLanguage);
	}

	public void setIncomingTargetLanguage(String incomingTargetLanguage) {
		this.incomingTargetLanguage = normalizeLanguage(incomingTargetLanguage);
	}

	public String getGoogleTranslateEndpoint() {
		return googleTranslateEndpoint == null || googleTranslateEndpoint.isBlank()
				? "https://translate.googleapis.com/translate_a/single"
				: googleTranslateEndpoint;
	}

	public int getRequestTimeoutSeconds() {
		return Math.max(2, requestTimeoutSeconds);
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}

	private static String normalizeLanguage(String language) {
		if (language == null || language.isBlank()) {
			return "en";
		}

		String normalized = language.trim().toLowerCase(Locale.ROOT);
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
}
