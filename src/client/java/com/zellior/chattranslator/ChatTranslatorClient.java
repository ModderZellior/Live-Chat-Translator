package com.zellior.chattranslator;

import com.zellior.chattranslator.command.TranslatorCommands;
import com.zellior.chattranslator.config.TranslatorConfig;
import com.zellior.chattranslator.service.TranslationService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
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
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> translateIncoming(message));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				translateIncoming(message);
			}
		});
	}

	private static void translateIncoming(Text message) {
		if (!config.isChatTranslationEnabled()) {
			return;
		}

		String plainMessage = message.getString();
		if (plainMessage.isBlank() || looksLikeTranslatedMessage(plainMessage)) {
			return;
		}

		CompletableFuture<TranslationService.TranslationResult> future = translationService.translate(
				plainMessage,
				config.getIncomingTargetLanguage()
		);

		future.thenAccept(result -> MinecraftClient.getInstance().execute(() -> {
			if (!result.wasTranslated() || result.translatedText().equalsIgnoreCase(plainMessage)) {
				return;
			}

			MinecraftClient client = MinecraftClient.getInstance();
			if (client.inGameHud == null) {
				return;
			}

			Text translatedLine = Text.literal("[TR ")
					.formatted(Formatting.AQUA)
					.append(Text.literal(result.detectedSourceLanguage().toUpperCase(Locale.ROOT)).formatted(Formatting.GRAY))
					.append(Text.literal(" -> ").formatted(Formatting.DARK_GRAY))
					.append(Text.literal(result.targetLanguage().toUpperCase(Locale.ROOT)).formatted(Formatting.GRAY))
					.append(Text.literal("] ").formatted(Formatting.AQUA))
					.append(Text.literal(result.translatedText()).formatted(Formatting.WHITE));

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
		MinecraftClient.getInstance().execute(() -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null) {
				client.player.sendMessage(Text.literal("[Translator] ").formatted(Formatting.AQUA)
						.append(Text.literal(message).formatted(formatting)), false);
			}
		});
	}
}
