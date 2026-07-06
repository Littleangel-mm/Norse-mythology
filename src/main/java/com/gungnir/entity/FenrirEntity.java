package com.gungnir.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FenrirEntity extends PathfinderMob implements GeoEntity {
	private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(FenrirEntity.class, EntityDataSerializers.BOOLEAN);
	private static final String ENRAGED_TAG = "Enraged";
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.fenrir.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.fenrir.walk");
	private static final RawAnimation ENRAGED_IDLE = RawAnimation.begin().thenLoop("animation.fenrir.enraged_idle");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.fenrir.run");
	private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.fenrir.bite");
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

	public FenrirEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
		this.xpReward = 120;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createFenrirAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 750.0D)
			.add(Attributes.ATTACK_DAMAGE, 12.0D)
			.add(Attributes.MOVEMENT_SPEED, 0.32D)
			.add(Attributes.FOLLOW_RANGE, 64.0D)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.75D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.75D));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, player -> this.isEnraged()));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::shouldHuntAtNight));
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(ENRAGED, false);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		Entity attacker = source.getEntity();
		if (hurt && attacker instanceof Player player) {
			setEnraged(true);
			setTarget(player);
		}
		return hurt;
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		boolean hurt = super.doHurtTarget(target);
		if (hurt) {
			triggerAnim("attack", "bite");
		}
		return hurt;
	}

	private boolean shouldHuntAtNight(LivingEntity target) {
		return !this.level().isDay()
			&& target instanceof Enemy
			&& target.isAlive()
			&& target != this;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean(ENRAGED_TAG, isEnraged());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setEnraged(tag.getBoolean(ENRAGED_TAG));
	}

	public boolean isEnraged() {
		return this.entityData.get(ENRAGED);
	}

	public void setEnraged(boolean enraged) {
		this.entityData.set(ENRAGED, enraged);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 5, state -> {
			if (state.isMoving()) {
				state.setAnimation(isEnraged() ? RUN : WALK);
			} else {
				state.setAnimation(isEnraged() ? ENRAGED_IDLE : IDLE);
			}
			return PlayState.CONTINUE;
		}));
		controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.CONTINUE)
			.triggerableAnim("bite", BITE));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animationCache;
	}
}
