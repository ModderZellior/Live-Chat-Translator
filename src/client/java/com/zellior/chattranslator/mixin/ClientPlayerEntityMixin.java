package com.zellior.chattranslator.mixin;

import com.zellior.chattranslator.command.TranslatorCommands;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
	private void chattranslator$handleLocalTranslatorCommand(String message, CallbackInfo ci) {
		if (TranslatorCommands.handleLocalCommand(message)) {
			ci.cancel();
		}
	}
}
