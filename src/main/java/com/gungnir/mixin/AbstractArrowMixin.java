package com.gungnir.mixin;

import com.gungnir.entity.EinherjarEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	@Inject(method = "onHitBlock", at = @At("TAIL"))
	private void gungnir$discardEinherjarThrownTridentOnBlock(BlockHitResult hitResult, CallbackInfo info) {
		AbstractArrow arrow = (AbstractArrow) (Object) this;
		if (arrow instanceof ThrownTrident && arrow.getOwner() instanceof EinherjarEntity && !arrow.level().isClientSide) {
			arrow.discard();
		}
	}
}
