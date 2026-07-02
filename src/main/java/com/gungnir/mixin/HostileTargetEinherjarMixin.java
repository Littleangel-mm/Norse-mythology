package com.gungnir.mixin;

import com.gungnir.entity.EinherjarEntity;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class HostileTargetEinherjarMixin {
	private static final double EINHERJAR_TARGET_RANGE = 16.0D;

	@Inject(method = "serverAiStep", at = @At("TAIL"))
	private void gungnir$targetNearbyEinherjar(CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (!(mob instanceof Enemy) || mob.tickCount % 20 != 0) {
			return;
		}

		LivingEntity currentTarget = mob.getTarget();
		if (currentTarget != null && currentTarget.isAlive()) {
			return;
		}

		List<EinherjarEntity> targets = mob.level().getEntitiesOfClass(
			EinherjarEntity.class,
			mob.getBoundingBox().inflate(EINHERJAR_TARGET_RANGE),
			einherjar -> einherjar.isAlive() && mob.canAttack(einherjar) && mob.hasLineOfSight(einherjar)
		);
		targets.stream()
			.min(Comparator.comparingDouble(mob::distanceToSqr))
			.ifPresent(mob::setTarget);
	}
}
