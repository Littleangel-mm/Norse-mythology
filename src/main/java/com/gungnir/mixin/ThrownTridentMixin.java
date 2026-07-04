package com.gungnir.mixin;

import com.gungnir.GungnirMod;
import com.gungnir.GungnirLightning;
import com.gungnir.GungnirTridentState;
import com.gungnir.entity.EinherjarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
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
		if (gungnir$isEinherjarProjectile() && !this.level().isClientSide && (this.inGround || this.dealtDamage || this.tickCount > 100)) {
			this.discard();
			return;
		}

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
			gungnir$markOwnerAttackTarget(livingEntity);
			gungnir$applyGungnirEffects(livingEntity);
			if (gungnir$isEinherjarProjectile()) {
				this.discard();
			} else {
				gungnir$returnToOwnerNow();
			}
		}
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
	private void gungnir$hitEnderImmuneTargets(EntityHitResult hitResult, CallbackInfo info) {
		if (!gungnir$isGungnir() || this.level().isClientSide) {
			return;
		}

		Entity hitEntity = hitResult.getEntity();
		if (hitEntity == this.getOwner()) {
			return;
		}

		if (gungnir$hurtEnderTarget(hitEntity)) {
			info.cancel();
			if (gungnir$isEinherjarProjectile()) {
				this.discard();
			} else {
				gungnir$returnToOwnerNow();
			}
		}
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
	private void gungnir$preventEinherjarTridentFriendlyFire(EntityHitResult hitResult, CallbackInfo info) {
		if (this.getOwner() instanceof EinherjarEntity && hitResult.getEntity() instanceof EinherjarEntity && !this.level().isClientSide) {
			this.discard();
			info.cancel();
		}
	}

	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void gungnir$discardEinherjarProjectileOnHit(EntityHitResult hitResult, CallbackInfo info) {
		if (gungnir$isEinherjarProjectile() && !this.level().isClientSide) {
			this.discard();
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

	private boolean gungnir$hurtEnderTarget(Entity hitEntity) {
		if (hitEntity instanceof EnderDragonPart dragonPart) {
			EnderDragon dragon = dragonPart.parentMob;
			boolean hurt = dragon.hurt(dragonPart, gungnir$divineDamageSource(), 12.0F);
			if (hurt) {
				gungnir$markOwnerAttackTarget(dragon);
				gungnir$applyGungnirEffects(dragon);
			}
			return hurt;
		}

		if (hitEntity instanceof EnderDragon dragon) {
			boolean hurt = dragon.hurt(gungnir$divineDamageSource(), 12.0F);
			if (hurt) {
				gungnir$markOwnerAttackTarget(dragon);
				gungnir$applyGungnirEffects(dragon);
			}
			return hurt;
		}

		if (hitEntity instanceof EnderMan enderMan) {
			boolean hurt = enderMan.hurt(gungnir$divineDamageSource(), 12.0F);
			if (hurt) {
				gungnir$markOwnerAttackTarget(enderMan);
				gungnir$applyGungnirEffects(enderMan);
			}
			return hurt;
		}

		return false;
	}

	private DamageSource gungnir$divineDamageSource() {
		Entity owner = this.getOwner();
		if (owner instanceof Player player) {
			return this.damageSources().playerAttack(player);
		}
		if (owner instanceof LivingEntity livingOwner) {
			return this.damageSources().mobAttack(livingOwner);
		}
		return this.damageSources().magic();
	}

	private void gungnir$applyGungnirEffects(LivingEntity target) {
		target.addTag(GungnirMod.BLEEDING_TAG);
		target.addEffect(new MobEffectInstance(GungnirMod.BLEEDING, BLEEDING_DURATION, 0, false, true, true));
		if (this.level() instanceof ServerLevel serverLevel) {
			GungnirLightning.strike(serverLevel, target);
		}
	}

	private void gungnir$markOwnerAttackTarget(LivingEntity target) {
		Entity owner = this.getOwner();
		if (owner instanceof LivingEntity livingOwner) {
			livingOwner.setLastHurtMob(target);
		}
		if (owner instanceof Player player) {
			player.setLastHurtMob(target);
		}
	}

	private boolean gungnir$isEinherjarProjectile() {
		return this.getOwner() instanceof EinherjarEntity;
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
