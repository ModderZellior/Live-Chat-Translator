package com.zellior.chattranslator.command;

import com.zellior.chattranslator.ChatTranslatorClient;
import com.zellior.chattranslator.config.TranslatorConfig;
import com.zellior.chattranslator.service.TranslationService;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class TranslatorCommands {
	private static final int MAX_CHAT_MESSAGE_LENGTH = 256;
	private static final String[] LANGUAGE_SUGGESTIONS = {
"af",
			"afrikaans",
			"sq",
			"albanian",
			"am",
			"amharic",
			"ar",
			"arabic",
			"hy",
			"armenian",
			"as",
			"assamese",
			"az",
			"azerbaijani",
			"eu",
			"basque",
			"be",
			"belarusian",
			"bn",
			"bengali",
			"bs",
			"bosnian",
			"bg",
			"bulgarian",
			"ca",
			"catalan",
			"ceb",
			"cebuano",
			"zh-CN",
			"chinese-simplified",
			"simplified",
			"zh",
			"chinese",
			"simplified-chinese",
			"zh-TW",
			"chinese-traditional",
			"traditional",
			"traditional-chinese",
			"co",
			"corsican",
			"hr",
			"croatian",
			"cs",
			"czech",
			"da",
			"danish",
			"nl",
			"dutch",
			"en",
			"english",
			"eo",
			"esperanto",
			"et",
			"estonian",
			"tl",
			"filipino",
			"tagalog",
			"fi",
			"finnish",
			"fr",
			"french",
			"gl",
			"galician",
			"ka",
			"georgian",
			"de",
			"german",
			"el",
			"greek",
			"gu",
			"gujarati",
			"ht",
			"haitian-creole",
			"he",
			"hebrew",
			"iw",
			"hi",
			"hindi",
			"hmn",
			"hmong",
			"hu",
			"hungarian",
			"is",
			"icelandic",
			"ig",
			"igbo",
			"id",
			"indonesian",
			"ga",
			"irish",
			"it",
			"italian",
			"ja",
			"japanese",
			"jw",
			"javanese",
			"kn",
			"kannada",
			"kk",
			"kazakh",
			"km",
			"khmer",
			"ko",
			"korean",
			"ku",
			"kurdish-kurmanji",
			"kurmanji",
			"ky",
			"kyrgyz",
			"lo",
			"lao",
			"la",
			"latin",
			"lv",
			"latvian",
			"lt",
			"lithuanian",
			"lb",
			"luxembourgish",
			"mk",
			"macedonian",
			"ms",
			"malay",
			"ml",
			"malayalam",
			"mt",
			"maltese",
			"mi",
			"maori",
			"mr",
			"marathi",
			"mn",
			"mongolian",
			"my",
			"myanmar-burmese",
			"burmese",
			"ne",
			"nepali",
			"no",
			"norwegian",
			"nb",
			"or",
			"odia",
			"ps",
			"pashto",
			"fa",
			"persian",
			"farsi",
			"pl",
			"polish",
			"pt",
			"portuguese",
			"pa",
			"punjabi",
			"ro",
			"romanian",
			"ru",
			"russian",
			"sm",
			"samoan",
			"gd",
			"scots-gaelic",
			"sr",
			"serbian",
			"si",
			"sinhala",
			"sk",
			"slovak",
			"sl",
			"slovenian",
			"so",
			"somali",
			"es",
			"spanish",
			"su",
			"sundanese",
			"sw",
			"swahili",
			"sv",
			"swedish",
			"ta",
			"tamil",
			"te",
			"telugu",
			"th",
			"thai",
			"tr",
			"turkish",
			"uk",
			"ukrainian",
			"ur",
			"urdu",
			"ug",
			"uyghur",
			"uz",
			"uzbek",
			"vi",
			"vietnamese",
			"cy",
			"welsh",
			"xh",
			"xhosa",
			"yi",
			"yiddish",
			"yo",
			"yoruba",
			"zu",
			"zulu"
	};

	private TranslatorCommands() {
	}

	public static void register(TranslatorConfig config, TranslationService translationService) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher, registryAccess, config, translationService));
	}

	private static void register(com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher,
								 CommandRegistryAccess registryAccess,
								 TranslatorConfig config,
								 TranslationService translationService) {
		dispatcher.register(literal("tr")
				.then(literal("chat")
						.then(argument("language", StringArgumentType.word())
								.suggests((context, builder) -> suggestLanguages(builder))
								.executes(context -> {
									String language = StringArgumentType.getString(context, "language");
									config.setIncomingTargetLanguage(language);
									config.setChatTranslationEnabled(true);
									config.save();
									context.getSource().sendFeedback(Text.literal("Chat translation target set to " + language + ".").formatted(Formatting.GREEN));
									return 1;
								})))
				.then(literal("off")
						.executes(context -> {
							config.setChatTranslationEnabled(false);
							config.save();
							context.getSource().sendFeedback(Text.literal("Chat translation disabled.").formatted(Formatting.YELLOW));
							return 1;
						}))
				.then(literal("on")
						.executes(context -> {
							config.setChatTranslationEnabled(true);
							config.save();
							context.getSource().sendFeedback(Text.literal("Chat translation enabled for " + config.getIncomingTargetLanguage() + ".").formatted(Formatting.GREEN));
							return 1;
						}))
				.then(literal("msg")
						.then(argument("language", StringArgumentType.word())
								.suggests((context, builder) -> suggestLanguages(builder))
								.then(argument("message", StringArgumentType.greedyString())
										.executes(context -> {
											String language = StringArgumentType.getString(context, "language");
											String message = StringArgumentType.getString(context, "message");
											translateAndSend(language, message, translationService);
											context.getSource().sendFeedback(Text.literal("Translating message...").formatted(Formatting.GRAY));
											return 1;
										})))));
	}

	private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestLanguages(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
		String remaining = builder.getRemainingLowerCase();
		for (String language : LANGUAGE_SUGGESTIONS) {
			if (language.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
				builder.suggest(language);
			}
		}

		return builder.buildFuture();
	}

	private static void translateAndSend(String language, String message, TranslationService translationService) {
		CompletableFuture<TranslationService.TranslationResult> future = translationService.translate(message, language);
		future.thenAccept(result -> MinecraftClient.getInstance().execute(() -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || client.player.networkHandler == null) {
				return;
			}

			String translatedMessage = trimToChatLimit(result.translatedText());
			client.player.networkHandler.sendChatMessage(translatedMessage);
			client.player.sendMessage(Text.literal("[Translator] Sent: ").formatted(Formatting.AQUA)
					.append(Text.literal(translatedMessage).formatted(Formatting.WHITE)), false);

			if (!translatedMessage.equals(result.translatedText())) {
				client.player.sendMessage(Text.literal("[Translator] Message was shortened to fit Minecraft's 256 character chat limit.").formatted(Formatting.YELLOW), false);
			}
		})).exceptionally(error -> {
			ChatTranslatorClient.sendLocalStatus("Message translation failed: " + error.getMessage(), Formatting.RED);
			return null;
		});
	}

	private static String trimToChatLimit(String message) {
		if (message.length() <= MAX_CHAT_MESSAGE_LENGTH) {
			return message;
		}

		return message.substring(0, MAX_CHAT_MESSAGE_LENGTH);
	}
}
