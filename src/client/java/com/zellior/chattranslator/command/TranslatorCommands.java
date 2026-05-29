package com.zellior.chattranslator.command;

import com.zellior.chattranslator.ChatTranslatorClient;
import com.zellior.chattranslator.config.TranslatorConfig;
import com.zellior.chattranslator.service.TranslationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

public final class TranslatorCommands {
	private static final int MAX_CHAT_MESSAGE_LENGTH = 100;

	private TranslatorCommands() {
	}

	public static void register(TranslatorConfig config, TranslationService translationService) {
	}

	public static boolean handleLocalCommand(String message) {
		if (!message.startsWith("/tr")) {
			return false;
		}

		String[] parts = message.trim().split("\\s+", 4);
		TranslatorConfig config = ChatTranslatorClient.getConfig();
		TranslationService translationService = ChatTranslatorClient.getTranslationService();

		if (parts.length >= 3 && parts[1].equalsIgnoreCase("chat")) {
			config.setIncomingTargetLanguage(parts[2]);
			config.setChatTranslationEnabled(true);
			config.save();
			ChatTranslatorClient.sendLocalStatus("Chat translation target set to " + parts[2] + ".", Formatting.GREEN);
			return true;
		}

		if (parts.length >= 2 && parts[1].equalsIgnoreCase("off")) {
			config.setChatTranslationEnabled(false);
			config.save();
			ChatTranslatorClient.sendLocalStatus("Chat translation disabled.", Formatting.YELLOW);
			return true;
		}

		if (parts.length >= 2 && parts[1].equalsIgnoreCase("on")) {
			config.setChatTranslationEnabled(true);
			config.save();
			ChatTranslatorClient.sendLocalStatus("Chat translation enabled for " + config.getIncomingTargetLanguage() + ".", Formatting.GREEN);
			return true;
		}

		if (parts.length >= 4 && parts[1].equalsIgnoreCase("msg")) {
			translateAndSend(parts[2], parts[3], translationService);
			ChatTranslatorClient.sendLocalStatus("Translating message...", Formatting.GRAY);
			return true;
		}

		ChatTranslatorClient.sendLocalStatus("Use /tr chat <language>, /tr on, /tr off, or /tr msg <language> <message>.", Formatting.YELLOW);
		return true;
	}

	private static void translateAndSend(String language, String message, TranslationService translationService) {
		CompletableFuture<TranslationService.TranslationResult> future = translationService.translate(message, language);
		future.thenAccept(result -> MinecraftClient.getInstance().submit(() -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null) {
				return;
			}

			String translatedMessage = trimToChatLimit(result.translatedText());
			client.player.sendChatMessage(translatedMessage);
			client.player.addMessage(ChatTranslatorClient.literal("[Translator] Sent: ", Formatting.AQUA)
					.append(ChatTranslatorClient.literal(translatedMessage, Formatting.WHITE)));

			if (!translatedMessage.equals(result.translatedText())) {
				client.player.addMessage(ChatTranslatorClient.literal("[Translator] Message was shortened to fit Minecraft's 100 character chat limit.", Formatting.YELLOW));
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
