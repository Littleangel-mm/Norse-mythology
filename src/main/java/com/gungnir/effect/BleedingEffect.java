package com.gungnir.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BleedingEffect extends MobEffect {
	private static final int TICKS_PER_DAMAGE = 20;
	private static final float DAMAGE_PER_SECOND = 0.5F;

	public BleedingEffect() {
		super(MobEffectCategory.HARMFUL, 0x8b0000);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % TICKS_PER_DAMAGE == 0;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (!entity.level().isClientSide) {
			entity.hurt(entity.damageSources().magic(), DAMAGE_PER_SECOND);
		}
	}
}
