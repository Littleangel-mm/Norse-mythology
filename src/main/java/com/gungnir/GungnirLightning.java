package com.gungnir;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class GungnirLightning {
	private static final DustParticleOptions RED_LIGHTNING = new DustParticleOptions(new Vector3f(1.0F, 0.02F, 0.0F), 1.4F);
	private static final float LIGHTNING_DAMAGE = 5.0F;
	private static final double STRIKE_HEIGHT = 32.0D;
	private static final int SEGMENTS = 24;
	private static final int PARTICLES_PER_SEGMENT = 9;

	private GungnirLightning() {
	}

	public static void strike(ServerLevel level, LivingEntity target) {
		if (!target.isAlive()) {
			return;
		}

		Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
		Vec3 start = end.add(0.0D, STRIKE_HEIGHT, 0.0D);
		RandomSource random = level.random;
		Vec3 previous = start;

		for (int segment = 1; segment <= SEGMENTS; segment++) {
			double progress = (double) segment / SEGMENTS;
			double offsetScale = segment == SEGMENTS ? 0.0D : 0.55D;
			Vec3 next = new Vec3(
				end.x + (random.nextDouble() - 0.5D) * offsetScale,
				start.y + (end.y - start.y) * progress,
				end.z + (random.nextDouble() - 0.5D) * offsetScale
			);
			spawnSegment(level, previous, next);
			previous = next;
		}

		level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.4F, 1.25F);
		level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.0F, 1.55F);
		target.hurt(target.damageSources().lightningBolt(), LIGHTNING_DAMAGE);
	}

	private static void spawnSegment(ServerLevel level, Vec3 start, Vec3 end) {
		Vec3 delta = end.subtract(start);
		for (int i = 0; i <= PARTICLES_PER_SEGMENT; i++) {
			double progress = (double) i / PARTICLES_PER_SEGMENT;
			Vec3 point = start.add(delta.scale(progress));
			level.sendParticles(RED_LIGHTNING, point.x, point.y, point.z, 2, 0.02D, 0.02D, 0.02D, 0.0D);
		}
	}
}
