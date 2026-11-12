package dev.returntozero.mixin;

import dev.returntozero.ModConfig;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

/** Reconstruido a partir do rbd 1.0.0: so concede conquistas (exceto receitas) com a chance configurada. */
@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
	@Inject(method = "grantCriterion", at = @At("HEAD"), cancellable = true)
	private void rtz$advancementChance(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
		double chance = ModConfig.get().advancementChance;
		if (chance >= 100.0) {
			return;
		}
		if (advancement.getId().getPath().startsWith("recipes/")) {
			return;
		}
		if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) {
			cir.setReturnValue(false);
		}
	}
}
