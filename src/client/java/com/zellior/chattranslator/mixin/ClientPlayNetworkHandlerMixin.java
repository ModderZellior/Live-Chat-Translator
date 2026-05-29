package com.zellior.chattranslator.mixin;

import com.zellior.chattranslator.ChatTranslatorClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(method = "onChatMessage", at = @At("TAIL"))
	private void chattranslator$translateIncoming(ChatMessageS2CPacket packet, CallbackInfo ci) {
		if (!packet.isNonChat()) {
			ChatTranslatorClient.translateIncoming(packet.getMessage());
		}
	}
}
