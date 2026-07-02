package com.gungnir.mixin;

import com.gungnir.GungnirMod;
import com.gungnir.GungnirLightning;
import com.gungnir.GungnirTridentState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends AbstractArrow implements GungnirTridentState {
	private static final double HOMING_RANGE = 5.0D;
	private static final int BLEEDING_DURATION = Integer.MAX_VALUE;
	private static final String GUNGNIR_PROJECTILE_TAG = "GungnirProjectile";
	@Unique
	private static final EntityDataAccessor<Boolean> GUNGNIR_PROJECTILE = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BOOLEAN);
	@Unique
	private boolean gungnir$returningToOwner;

	@Shadow
	private ItemStack tridentItem;
	@Shadow
	private boolean dealtDamage;

	protected ThrownTridentMixin(EntityType<? extends AbstractArrow> entityType, net.minecraft.world.level.Level level) {
		super(entityType, level);
	}

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void gungnir$defineSyncedData(CallbackInfo info) {
		this.entityData.define(GUNGNIR_PROJECTILE, false);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void gungnir$homeTowardHostileTarget(CallbackInfo info) {
		if (!gungnir$isGungnir() || this.level().isClientSide || this.inGround) {
			return;
		}

		if (gungnir$returningToOwner) {
			gungnir$returnToOwnerNow();
			return;
		}

		LivingEntity target = gungnir$findTarget();
		if (target == null) {
			return;
		}

		Vec3 toTarget = target.getEyePosition().subtract(this.position());
		if (toTarget.lengthSqr() <= 0.0001D) {
			return;
		}

		double speed = Math.max(this.getDeltaMovement().length(), 0.75D);
		Vec3 current = this.getDeltaMovement().scale(0.2D);
		Vec3 homing = toTarget.normalize().scale(speed).scale(0.8D);
		Vec3 movement = current.add(homing);
		this.setDeltaMovement(movement);
		gungnir$syncRotationToMovement(movement);
		this.hasImpulse = true;
	}

	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void gungnir$startBleeding(EntityHitResult hitResult, CallbackInfo info) {
		if (!gungnir$isGungnir() || this.level().isClientSide) {
			return;
		}

		Entity hitEntity = hitResult.getEntity();
		if (hitEntity instanceof LivingEntity livingEntity && hitEntity != this.getOwner()) {
			livingEntity.addTag(GungnirMod.BLEEDING_TAG);
			livingEntity.addEffect(new MobEffectInstance(GungnirMod.BLEEDING, BLEEDING_DURATION, 0, false, true, true));
			if (this.level() instanceof ServerLevel serverLevel) {
				GungnirLightning.strike(serverLevel, livingEntity);
			}
			gungnir$returnToOwnerNow();
		}
	}

	private void gungnir$returnToOwnerNow() {
		Entity owner = this.getOwner();
		if (owner == null || !owner.isAlive()) {
			return;
		}

		this.setNoPhysics(true);
		this.dealtDamage = true;
		this.inGround = false;
		gungnir$returningToOwner = true;
		Vec3 toOwner = owner.getEyePosition().subtract(this.position());
		if (toOwner.lengthSqr() > 0.0001D) {
			Vec3 movement = toOwner.normalize().scale(0.85D);
			this.setDeltaMovement(movement);
			gungnir$syncRotationToMovement(movement);
			this.hasImpulse = true;
		}
	}

	private void gungnir$syncRotationToMovement(Vec3 movement) {
		if (movement.lengthSqr() <= 0.0001D) {
			return;
		}

		double horizontal = movement.horizontalDistance();
		this.setYRot((float) (Mth.atan2(movement.x, movement.z) * Mth.RAD_TO_DEG));
		this.setXRot((float) (Mth.atan2(movement.y, horizontal) * Mth.RAD_TO_DEG));
	}

	private LivingEntity gungnir$findTarget() {
		Entity owner = this.getOwner();
		AABB searchBox = this.getBoundingBox().inflate(HOMING_RANGE);

		return this.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
			entity.isAlive()
				&& entity != owner
				&& entity instanceof Enemy
		).stream().min((left, right) -> Double.compare(
			left.distanceToSqr(this),
			right.distanceToSqr(this)
		)).orElse(null);
	}

	private boolean gungnir$isGungnir() {
		ItemStack stack = ((ThrownTridentAccessor) this).gungnir$getTridentItem();
		return stack.is(GungnirMod.GUNGNIR) || gungnir$isGungnirProjectile();
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void gungnir$saveProjectileState(CompoundTag tag, CallbackInfo info) {
		tag.putBoolean(GUNGNIR_PROJECTILE_TAG, gungnir$isGungnirProjectile());
		tag.putBoolean("GungnirReturningToOwner", gungnir$returningToOwner);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void gungnir$readProjectileState(CompoundTag tag, CallbackInfo info) {
		gungnir$setGungnirProjectile(tag.getBoolean(GUNGNIR_PROJECTILE_TAG) || gungnir$isGungnir());
		gungnir$returningToOwner = tag.getBoolean("GungnirReturningToOwner");
	}

	@Override
	public boolean gungnir$isGungnirProjectile() {
		return this.entityData.get(GUNGNIR_PROJECTILE);
	}

	@Override
	public void gungnir$setGungnirProjectile(boolean gungnirProjectile) {
		this.entityData.set(GUNGNIR_PROJECTILE, gungnirProjectile);
	}
}
