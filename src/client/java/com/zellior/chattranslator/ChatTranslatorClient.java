package com.zellior.chattranslator;

import com.zellior.chattranslator.command.TranslatorCommands;
import com.zellior.chattranslator.config.TranslatorConfig;
import com.zellior.chattranslator.service.TranslationService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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

	private static void translateIncoming(Component message) {
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

		future.thenAccept(result -> Minecraft.getInstance().execute(() -> {
			if (!result.wasTranslated() || result.translatedText().equalsIgnoreCase(plainMessage)) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
Component translatedLine = Component.literal("[TR ")
					.withStyle(ChatFormatting.AQUA)
					.append(Component.literal(result.detectedSourceLanguage().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY))
					.append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
					.append(Component.literal(result.targetLanguage().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY))
					.append(Component.literal("] ").withStyle(ChatFormatting.AQUA))
					.append(Component.literal(result.translatedText()).withStyle(ChatFormatting.WHITE));

			client.gui.getChat().addClientSystemMessage(translatedLine);
		})).exceptionally(error -> {
			sendLocalStatus("Translation failed: " + error.getMessage(), ChatFormatting.RED);
			return null;
		});
	}

	private static boolean looksLikeTranslatedMessage(String message) {
		return message.startsWith("[TR ");
	}

	public static void sendLocalStatus(String message, ChatFormatting formatting) {
		Minecraft.getInstance().execute(() -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player != null) {
				client.player.sendSystemMessage(Component.literal("[Translator] ").withStyle(ChatFormatting.AQUA)
						.append(Component.literal(message).withStyle(formatting)));
			}
		});
	}
}


