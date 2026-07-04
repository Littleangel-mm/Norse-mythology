package com.gungnir.mixin;

import com.gungnir.entity.EinherjarEntity;
import com.gungnir.entity.ValkyrieEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	@Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
	private void gungnir$preventEinherjarArrowFriendlyFire(EntityHitResult hitResult, CallbackInfo info) {
		AbstractArrow arrow = (AbstractArrow) (Object) this;
		Entity hitEntity = hitResult.getEntity();
		if (gungnir$isFriendlyFire(arrow.getOwner(), hitEntity) && !arrow.level().isClientSide) {
			arrow.discard();
			info.cancel();
		}
	}

	@Inject(method = "onHitBlock", at = @At("TAIL"))
	private void gungnir$discardEinherjarThrownTridentOnBlock(BlockHitResult hitResult, CallbackInfo info) {
		AbstractArrow arrow = (AbstractArrow) (Object) this;
		if (arrow instanceof ThrownTrident && arrow.getOwner() instanceof EinherjarEntity && !arrow.level().isClientSide) {
			arrow.discard();
		}
	}

	private boolean gungnir$isFriendlyFire(Entity owner, Entity hitEntity) {
		return (owner instanceof EinherjarEntity || owner instanceof ValkyrieEntity)
			&& (hitEntity instanceof EinherjarEntity || hitEntity instanceof ValkyrieEntity);
	}
}
