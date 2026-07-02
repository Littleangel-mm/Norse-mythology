package com.gungnir.entity;

import com.gungnir.GungnirMod;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class EinherjarEntity extends PathfinderMob {
	private static final int PICKUP_INTERVAL = 20;
	private static final int EAT_INTERVAL = 60;
	private int pickupCooldown;
	private int eatCooldown;

	public EinherjarEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
		this.setCanPickUpLoot(true);
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			this.setDropChance(slot, 1.0F);
		}
	}

	public static AttributeSupplier.Builder createEinherjarAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0D)
			.add(Attributes.ATTACK_DAMAGE, 3.0D)
			.add(Attributes.MOVEMENT_SPEED, 0.28D)
			.add(Attributes.FOLLOW_RANGE, 48.0D)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new EinherjarBowGoal(this, 1.0D, 18.0F));
		this.goalSelector.addGoal(3, new EinherjarMeleeGoal(this, 1.15D, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, entity -> entity instanceof Enemy));
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType spawnType, SpawnGroupData spawnData, CompoundTag tag) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
		if (this.getMainHandItem().isEmpty()) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		}
		return data;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) {
			return;
		}

		if (--this.pickupCooldown <= 0) {
			this.pickupCooldown = PICKUP_INTERVAL;
			pickUpUsefulItems();
		}
		if (--this.eatCooldown <= 0) {
			this.eatCooldown = EAT_INTERVAL;
			eatIfNeeded();
		}
	}

	private void pickUpUsefulItems() {
		List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.6D), item -> item.isAlive() && !item.hasPickUpDelay());
		for (ItemEntity itemEntity : items) {
			ItemStack stack = itemEntity.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			if (tryEquipWeapon(stack) || tryStoreFood(stack)) {
				this.take(itemEntity, stack.getCount());
				itemEntity.discard();
				return;
			}
		}
	}

	private boolean tryEquipWeapon(ItemStack stack) {
		if (weaponScore(stack) <= weaponScore(this.getMainHandItem())) {
			return false;
		}

		ItemStack oldWeapon = this.getMainHandItem();
		if (!oldWeapon.isEmpty()) {
			this.spawnAtLocation(oldWeapon.copy());
		}
		ItemStack newWeapon = stack.copy();
		newWeapon.setCount(1);
		this.setItemSlot(EquipmentSlot.MAINHAND, newWeapon);
		return true;
	}

	private boolean tryStoreFood(ItemStack stack) {
		if (!stack.getItem().isEdible()) {
			return false;
		}

		ItemStack offhand = this.getOffhandItem();
		if (!offhand.isEmpty() && !ItemStack.isSameItemSameTags(offhand, stack)) {
			return false;
		}

		ItemStack stored = stack.copy();
		stored.setCount(Math.min(stack.getCount(), 16));
		if (!offhand.isEmpty()) {
			stored.grow(offhand.getCount());
			stored.setCount(Math.min(stored.getCount(), 16));
		}
		this.setItemSlot(EquipmentSlot.OFFHAND, stored);
		return true;
	}

	private void eatIfNeeded() {
		if (this.getHealth() > this.getMaxHealth() * 0.65F) {
			return;
		}

		ItemStack food = this.getOffhandItem();
		if (food.isEmpty() || !food.getItem().isEdible()) {
			return;
		}

		this.heal(4.0F);
		food.shrink(1);
		this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8F, 1.0F);
		if (food.isEmpty()) {
			this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
	}

	private boolean isUsingBow() {
		return this.getMainHandItem().getItem() instanceof BowItem;
	}

	private static int weaponScore(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (stack.is(GungnirMod.GUNGNIR)) {
			return 100;
		}
		if (stack.getItem() instanceof TridentItem) {
			return 80;
		}
		if (stack.getItem() instanceof BowItem) {
			return 60;
		}
		if (stack.getItem() instanceof SwordItem) {
			return 50;
		}
		if (stack.getItem() instanceof AxeItem) {
			return 45;
		}
		return 0;
	}

	private static class EinherjarMeleeGoal extends MeleeAttackGoal {
		private final EinherjarEntity einherjar;

		EinherjarMeleeGoal(EinherjarEntity einherjar, double speedModifier, boolean followingTargetEvenIfNotSeen) {
			super(einherjar, speedModifier, followingTargetEvenIfNotSeen);
			this.einherjar = einherjar;
		}

		@Override
		public boolean canUse() {
			return !this.einherjar.isUsingBow() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !this.einherjar.isUsingBow() && super.canContinueToUse();
		}
	}

	private static class EinherjarBowGoal extends Goal {
		private final EinherjarEntity einherjar;
		private final double speedModifier;
		private final float attackRadiusSqr;
		private int attackTime;

		EinherjarBowGoal(EinherjarEntity einherjar, double speedModifier, float attackRadius) {
			this.einherjar = einherjar;
			this.speedModifier = speedModifier;
			this.attackRadiusSqr = attackRadius * attackRadius;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.einherjar.isUsingBow() && this.einherjar.getTarget() != null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void start() {
			this.attackTime = 20;
		}

		@Override
		public void tick() {
			LivingEntity target = this.einherjar.getTarget();
			if (target == null) {
				return;
			}

			double distance = this.einherjar.distanceToSqr(target);
			if (distance > this.attackRadiusSqr * 0.75D) {
				this.einherjar.getNavigation().moveTo(target, this.speedModifier);
			} else {
				this.einherjar.getNavigation().stop();
			}

			this.einherjar.getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (--this.attackTime <= 0) {
				this.attackTime = 30;
				shoot(target);
			}
		}

		private void shoot(LivingEntity target) {
			Level level = this.einherjar.level();
			if (!(level instanceof ServerLevel)) {
				return;
			}

			Arrow arrow = new Arrow(level, this.einherjar);
			double x = target.getX() - this.einherjar.getX();
			double y = target.getY(0.3333333333333333D) - arrow.getY();
			double z = target.getZ() - this.einherjar.getZ();
			double horizontal = Math.sqrt(x * x + z * z);
			arrow.shoot(x, y + horizontal * 0.2D, z, 1.6F, 8.0F);
			level.addFreshEntity(arrow);
			level.playSound(null, this.einherjar.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
	}
}
