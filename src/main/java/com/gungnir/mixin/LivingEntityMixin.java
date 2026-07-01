package com.gungnir.mixin;

import com.gungnir.GungnirMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	private static final int BLEEDING_DURATION = Integer.MAX_VALUE;

	protected LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(method = "removeEffect", at = @At("TAIL"))
	private void gungnir$restoreBleeding(MobEffect effect, CallbackInfoReturnable<Boolean> info) {
		if (effect == GungnirMod.BLEEDING) {
			gungnir$restoreBleedingIfMarked();
		}
	}

	@Inject(method = "removeAllEffects", at = @At("TAIL"))
	private void gungnir$restoreBleedingAfterClear(CallbackInfoReturnable<Boolean> info) {
		gungnir$restoreBleedingIfMarked();
	}

	private void gungnir$restoreBleedingIfMarked() {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!this.level().isClientSide && this.getTags().contains(GungnirMod.BLEEDING_TAG) && !self.hasEffect(GungnirMod.BLEEDING)) {
			self.addEffect(new MobEffectInstance(GungnirMod.BLEEDING, BLEEDING_DURATION, 0, false, true, true));
		}
	}
}
