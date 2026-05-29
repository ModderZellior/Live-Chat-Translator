package com.zellior.chattranslator;

import com.zellior.chattranslator.command.TranslatorCommands;
import com.zellior.chattranslator.config.TranslatorConfig;
import com.zellior.chattranslator.service.TranslationService;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ChatTranslatorClient implements ClientModInitializer {
	public static final String MOD_ID = "chat-translator";

	private static TranslatorConfig config;
	private static TranslationService translationService;

	@Override
	public void onInitializeClient() {
		config = TranslatorConfig.load();
		translationService = new TranslationService(config);

		TranslatorCommands.register(config, translationService);
	}

	public static void translateIncoming(Text message) {
		if (!config.isChatTranslationEnabled()) {
			return;
		}

		String plainMessage = message.asUnformattedString();
		if (plainMessage.isBlank() || looksLikeTranslatedMessage(plainMessage)) {
			return;
		}

		CompletableFuture<TranslationService.TranslationResult> future = translationService.translate(
				plainMessage,
				config.getIncomingTargetLanguage()
		);

		future.thenAccept(result -> MinecraftClient.getInstance().submit(() -> {
			if (!result.wasTranslated() || result.translatedText().equalsIgnoreCase(plainMessage)) {
				return;
			}

			MinecraftClient client = MinecraftClient.getInstance();
			if (client.inGameHud == null) {
				return;
			}

			Text translatedLine = literal("[TR ", Formatting.AQUA)
					.append(literal(result.detectedSourceLanguage().toUpperCase(Locale.ROOT), Formatting.GRAY))
					.append(literal(" -> ", Formatting.DARK_GRAY))
					.append(literal(result.targetLanguage().toUpperCase(Locale.ROOT), Formatting.GRAY))
					.append(literal("] ", Formatting.AQUA))
					.append(literal(result.translatedText(), Formatting.WHITE));

			client.inGameHud.getChatHud().addMessage(translatedLine);
		})).exceptionally(error -> {
			sendLocalStatus("Translation failed: " + error.getMessage(), Formatting.RED);
			return null;
		});
	}

	private static boolean looksLikeTranslatedMessage(String message) {
		return message.startsWith("[TR ");
	}

	public static void sendLocalStatus(String message, Formatting formatting) {
		MinecraftClient.getInstance().submit(() -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null) {
				client.player.addMessage(literal("[Translator] ", Formatting.AQUA)
						.append(literal(message, formatting)));
			}
		});
	}

	public static TranslatorConfig getConfig() {
		return config;
	}

	public static TranslationService getTranslationService() {
		return translationService;
	}

	public static Text literal(String value, Formatting formatting) {
		return new LiteralText(value).setStyle(new Style().setFormatting(formatting));
	}
}
