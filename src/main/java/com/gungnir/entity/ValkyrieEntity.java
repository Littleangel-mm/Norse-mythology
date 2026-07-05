package com.gungnir.entity;

import com.gungnir.GungnirMod;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class ValkyrieEntity extends PathfinderMob {
	private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(ValkyrieEntity.class, EntityDataSerializers.BOOLEAN);
	private static final String CHOSEN_EINHERJAR_TAG = "ChosenEinherjar";
	private static final String DEAD_CHOSEN_HANDLED_TAG = "DeadChosenHandled";
	private static final String BREEDING_COOLDOWN_TAG = "BreedingCooldown";
	private static final String LAST_CHOSEN_X_TAG = "LastChosenX";
	private static final String LAST_CHOSEN_Y_TAG = "LastChosenY";
	private static final String LAST_CHOSEN_Z_TAG = "LastChosenZ";
	private static final String CHILD_OF_VALKYRIE_TAG_PREFIX = GungnirMod.MOD_ID + ".valkyrie_child.";
	private static final double CHOSEN_SCAN_RANGE = 32.0D;
	private static final double SUPPORT_RANGE = 48.0D;
	private static final double MAX_COMBAT_RANGE_SQR = 48.0D * 48.0D;
	private static final int CHOSEN_SCAN_INTERVAL = 100;
	private static final int HEAL_INTERVAL = 100;
	private static final int BREEDING_COOLDOWN = 20 * 60 * 5;
	private static final int REVENGE_EQUIPMENT_SCAN_TICKS = 20 * 12;
	private UUID chosenEinherjarUuid;
	private int chosenScanCooldown;
	private int healCooldown;
	private int breedingCooldown;
	private int deadChosenEquipmentScanTicks;
	private int bondParticleTicks;
	private boolean deadChosenHandled;
	private double lastChosenX;
	private double lastChosenY;
	private double lastChosenZ;

	public ValkyrieEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
		this.setPersistenceRequired();
		this.setCanPickUpLoot(false);
		equipInfiniteBow();
	}

	public static AttributeSupplier.Builder createValkyrieAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0D)
			.add(Attributes.ATTACK_DAMAGE, 2.0D)
			.add(Attributes.MOVEMENT_SPEED, 0.1D)
			.add(Attributes.FLYING_SPEED, 0.1D)
			.add(Attributes.FOLLOW_RANGE, 48.0D)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.05D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new ValkyrieFlightBowGoal(this));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1D, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, this::isHostileTarget));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, 10, true, false, entity -> entity instanceof Villager villager && isOwnChildVillager(villager)));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(FLYING, false);
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
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnData, CompoundTag tag) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
		equipInfiniteBow();
		return data;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) {
			return;
		}

		clearInvalidTarget();
		if (this.getMainHandItem().isEmpty()) {
			equipInfiniteBow();
		}
		if (this.breedingCooldown > 0) {
			this.breedingCooldown--;
		}
		if (--this.healCooldown <= 0) {
			this.healCooldown = HEAL_INTERVAL;
			healBond();
		}
		if (--this.chosenScanCooldown <= 0) {
			this.chosenScanCooldown = CHOSEN_SCAN_INTERVAL;
			chooseStrongEinherjar();
		}
		spawnBondParticles();
		supportChosenEinherjar();
		handleDeadChosenEinherjar();
		tryBreedWithChosenEinherjar();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (this.chosenEinherjarUuid != null) {
			tag.putUUID(CHOSEN_EINHERJAR_TAG, this.chosenEinherjarUuid);
		}
		tag.putBoolean(DEAD_CHOSEN_HANDLED_TAG, this.deadChosenHandled);
		tag.putInt(BREEDING_COOLDOWN_TAG, this.breedingCooldown);
		tag.putDouble(LAST_CHOSEN_X_TAG, this.lastChosenX);
		tag.putDouble(LAST_CHOSEN_Y_TAG, this.lastChosenY);
		tag.putDouble(LAST_CHOSEN_Z_TAG, this.lastChosenZ);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID(CHOSEN_EINHERJAR_TAG)) {
			this.chosenEinherjarUuid = tag.getUUID(CHOSEN_EINHERJAR_TAG);
		}
		this.deadChosenHandled = tag.getBoolean(DEAD_CHOSEN_HANDLED_TAG);
		this.breedingCooldown = tag.getInt(BREEDING_COOLDOWN_TAG);
		this.lastChosenX = tag.getDouble(LAST_CHOSEN_X_TAG);
		this.lastChosenY = tag.getDouble(LAST_CHOSEN_Y_TAG);
		this.lastChosenZ = tag.getDouble(LAST_CHOSEN_Z_TAG);
	}

	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity killedEntity) {
		boolean killed = super.killedEntity(level, killedEntity);
		if (killedEntity instanceof Villager villager && isOwnChildVillager(villager)) {
			convertVillagerToEinherjar(level, killedEntity);
		}
		return killed;
	}

	@Override
	public void die(DamageSource damageSource) {
		releaseChosenEinherjar();
		clearChosenEinherjarTarget();
		super.die(damageSource);
	}

	@Override
	public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	public boolean isFlying() {
		return this.entityData.get(FLYING);
	}

	public void setFlying(boolean flying) {
		this.entityData.set(FLYING, flying);
		this.setNoGravity(flying);
		if (!flying && !this.onGround()) {
			this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, 0.8D, 0.7D));
		}
	}

	public UUID getChosenEinherjarUuid() {
		return this.chosenEinherjarUuid;
	}

	private void equipInfiniteBow() {
		ItemStack bow = new ItemStack(Items.BOW);
		EnchantmentHelper.setEnchantments(Map.of(Enchantments.INFINITY_ARROWS, 1), bow);
		this.setItemSlot(EquipmentSlot.MAINHAND, bow);
		this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
	}

	private boolean isHostileTarget(LivingEntity target) {
		return target instanceof Enemy && target.isAlive() && target != this;
	}

	private boolean isOwnChildVillager(Villager villager) {
		return villager.isAlive() && villager.getTags().contains(childTag());
	}

	private String childTag() {
		return CHILD_OF_VALKYRIE_TAG_PREFIX + this.getUUID();
	}

	private void chooseStrongEinherjar() {
		if (this.chosenEinherjarUuid != null) {
			return;
		}

		List<EinherjarEntity> candidates = this.level().getEntitiesOfClass(
			EinherjarEntity.class,
			this.getBoundingBox().inflate(CHOSEN_SCAN_RANGE),
			einherjar -> einherjar.isAlive() && einherjar.getBondedValkyrieUuid() == null
		);
		if (candidates.isEmpty()) {
			return;
		}

		int bestScore = candidates.stream().mapToInt(this::scoreEinherjar).max().orElse(0);
		List<EinherjarEntity> strongestCandidates = new ArrayList<>();
		for (EinherjarEntity candidate : candidates) {
			if (scoreEinherjar(candidate) == bestScore) {
				strongestCandidates.add(candidate);
			}
		}

		while (!strongestCandidates.isEmpty()) {
			EinherjarEntity chosen = strongestCandidates.remove(this.getRandom().nextInt(strongestCandidates.size()));
			if (bindEinherjar(chosen)) {
				return;
			}
		}
	}

	private void supportChosenEinherjar() {
		EinherjarEntity chosen = getChosenEinherjar();
		if (chosen == null) {
			return;
		}
		rememberChosenPosition(chosen);

		LivingEntity chosenTarget = chosen.getTarget();
		if (isValidCombatTarget(chosenTarget)) {
			this.setTarget(chosenTarget);
			return;
		}

		LivingEntity attacker = chosen.getLastHurtByMob();
		if (isValidCombatTarget(attacker) && this.distanceToSqr(attacker) <= SUPPORT_RANGE * SUPPORT_RANGE) {
			this.setTarget(attacker);
			return;
		}

		followChosenEinherjar(chosen);
	}

	private EinherjarEntity getChosenEinherjar() {
		if (this.chosenEinherjarUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		Entity entity = serverLevel.getEntity(this.chosenEinherjarUuid);
		if (entity instanceof EinherjarEntity einherjar && einherjar.isAlive()) {
			if (!einherjar.isBondedToValkyrie(this.getUUID())) {
				this.chosenEinherjarUuid = null;
				return null;
			}
			return einherjar;
		}
		return null;
	}

	private void clearChosenEinherjarTarget() {
		EinherjarEntity chosen = getChosenEinherjar();
		if (chosen != null) {
			chosen.clearTargetIfTarget(this);
		}
	}

	private boolean bindEinherjar(EinherjarEntity einherjar) {
		if (!einherjar.tryBindValkyrie(this.getUUID())) {
			return false;
		}
		this.chosenEinherjarUuid = einherjar.getUUID();
		this.deadChosenHandled = false;
		this.deadChosenEquipmentScanTicks = 0;
		this.bondParticleTicks = 80;
		rememberChosenPosition(einherjar);
		return true;
	}

	private void releaseChosenEinherjar() {
		EinherjarEntity chosen = getChosenEinherjar();
		if (chosen != null) {
			chosen.releaseBondedValkyrie(this.getUUID());
		}
	}

	private void spawnBondParticles() {
		if (this.bondParticleTicks <= 0) {
			return;
		}
		this.bondParticleTicks--;
		if (this.bondParticleTicks % 8 != 0 || !(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		serverLevel.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.5D, this.getZ(), 2, 0.35D, 0.35D, 0.35D, 0.02D);
		EinherjarEntity chosen = getChosenEinherjar();
		if (chosen != null) {
			serverLevel.sendParticles(ParticleTypes.HEART, chosen.getX(), chosen.getY() + 1.6D, chosen.getZ(), 2, 0.35D, 0.35D, 0.35D, 0.02D);
		}
	}

	private void followChosenEinherjar(EinherjarEntity chosen) {
		if (this.getTarget() != null) {
			return;
		}

		double distance = this.distanceToSqr(chosen);
		if (distance <= 4.0D * 4.0D && Math.abs(this.getY() - chosen.getY()) < 1.25D) {
			this.setFlying(false);
			this.getNavigation().stop();
			return;
		}

		this.setFlying(true);
		this.getNavigation().stop();
		Vec3 desired = chosen.position().add(0.0D, 1.7D, 0.0D);
		Vec3 toDesired = desired.subtract(this.position());
		Vec3 current = this.getDeltaMovement().scale(0.55D);
		Vec3 steering = toDesired.scale(0.08D);
		if (steering.length() > 0.28D) {
			steering = steering.normalize().scale(0.28D);
		}
		this.setDeltaMovement(current.add(steering));
		this.hasImpulse = true;
	}

	private void rememberChosenPosition(EinherjarEntity einherjar) {
		this.lastChosenX = einherjar.getX();
		this.lastChosenY = einherjar.getY();
		this.lastChosenZ = einherjar.getZ();
	}

	private void healBond() {
		if (this.getHealth() < this.getMaxHealth()) {
			this.heal(1.0F);
		}

		EinherjarEntity chosen = getChosenEinherjar();
		if (chosen != null) {
			rememberChosenPosition(chosen);
			if (chosen.getHealth() < chosen.getMaxHealth()) {
				chosen.heal(1.0F);
				this.level().playSound(null, chosen.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.35F, 1.6F);
			}
		}
	}

	private void handleDeadChosenEinherjar() {
		if (this.chosenEinherjarUuid == null || getChosenEinherjar() != null) {
			return;
		}

		if (!this.deadChosenHandled) {
			this.deadChosenHandled = true;
			this.deadChosenEquipmentScanTicks = REVENGE_EQUIPMENT_SCAN_TICKS;
		}

		if (this.deadChosenEquipmentScanTicks <= 0) {
			return;
		}
		this.deadChosenEquipmentScanTicks--;
		inheritEquipmentNearLastChosenPosition();
	}

	private void inheritEquipmentNearLastChosenPosition() {
		Vec3 deathPosition = new Vec3(this.lastChosenX, this.lastChosenY, this.lastChosenZ);
		if (this.distanceToSqr(deathPosition) > 3.0D * 3.0D) {
			this.getNavigation().moveTo(this.lastChosenX, this.lastChosenY, this.lastChosenZ, 1.15D);
		}

		List<ItemEntity> drops = this.level().getEntitiesOfClass(
			ItemEntity.class,
			this.getBoundingBox().inflate(2.5D),
			item -> item.isAlive() && !item.getItem().isEmpty()
		);
		for (ItemEntity drop : drops) {
			ItemStack stack = drop.getItem();
			if (equipInheritedStack(stack)) {
				this.take(drop, 1);
				stack.shrink(1);
				if (stack.isEmpty()) {
					drop.discard();
				}
			}
		}
	}

	private boolean equipInheritedStack(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (isWeapon(stack) && weaponScore(stack) > weaponScore(this.getMainHandItem())) {
			ItemStack inherited = stack.copy();
			inherited.setCount(1);
			this.setItemSlot(EquipmentSlot.MAINHAND, inherited);
			this.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
			return true;
		}
		if (stack.getItem() instanceof ArmorItem armorItem) {
			EquipmentSlot slot = armorItem.getEquipmentSlot();
			if (slot.getType() == EquipmentSlot.Type.ARMOR && armorScore(stack) > armorScore(this.getItemBySlot(slot))) {
				ItemStack inherited = stack.copy();
				inherited.setCount(1);
				this.setItemSlot(slot, inherited);
				this.setDropChance(slot, 1.0F);
				return true;
			}
		}
		return false;
	}

	private void tryBreedWithChosenEinherjar() {
		EinherjarEntity chosen = getChosenEinherjar();
		if (chosen == null || this.breedingCooldown > 0 || this.getTarget() != null || hasRecentCombat()) {
			return;
		}
		if (!chosen.canBreedWithValkyrie(this.getUUID()) || this.distanceToSqr(chosen) > 4.0D * 4.0D) {
			return;
		}

		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Villager child = EntityType.VILLAGER.create(serverLevel);
		if (child == null) {
			return;
		}

		child.moveTo((this.getX() + chosen.getX()) * 0.5D, this.getY(), (this.getZ() + chosen.getZ()) * 0.5D, this.getYRot(), 0.0F);
		child.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(child.blockPosition()), MobSpawnType.BREEDING, null, null);
		child.setAge(-24000);
		child.addTag(childTag());
		serverLevel.addFreshEntity(child);
		this.breedingCooldown = BREEDING_COOLDOWN;
		chosen.markValkyrieBreedingCooldown(BREEDING_COOLDOWN);
		serverLevel.playSound(null, child.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.9F, 1.2F);
	}

	private boolean hasRecentCombat() {
		return this.getLastHurtByMob() != null && this.tickCount - this.getLastHurtByMobTimestamp() <= 200;
	}

	private void clearInvalidTarget() {
		LivingEntity target = this.getTarget();
		if (target != null && !isValidCombatTarget(target)) {
			this.setTarget(null);
			this.getNavigation().stop();
			if (this.isFlying()) {
				this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, 0.6D, 0.5D));
			}
		}
	}

	private boolean isValidCombatTarget(LivingEntity target) {
		return target != null
			&& target.isAlive()
			&& !target.isRemoved()
			&& target != this
			&& this.distanceToSqr(target) <= MAX_COMBAT_RANGE_SQR
			&& !(target instanceof EinherjarEntity)
			&& !(target instanceof ValkyrieEntity);
	}

	private int scoreEinherjar(EinherjarEntity einherjar) {
		int score = (int) einherjar.getHealth() * 10;
		score += weaponScore(einherjar.getMainHandItem());
		for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
			score += armorScore(einherjar.getItemBySlot(slot));
		}
		return score;
	}

	private static int weaponScore(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (stack.is(GungnirMod.GUNGNIR)) {
			return 10000;
		}
		if (stack.getItem() instanceof TridentItem) {
			return 8000 + enchantmentBonus(stack);
		}
		if (stack.getItem() instanceof BowItem) {
			return 6500 + enchantmentBonus(stack);
		}
		if (stack.getItem() instanceof SwordItem) {
			return 5000 + tierBonus(stack) + enchantmentBonus(stack);
		}
		if (stack.getItem() instanceof AxeItem) {
			return 4500 + tierBonus(stack) + enchantmentBonus(stack);
		}
		return 0;
	}

	private static int tierBonus(ItemStack stack) {
		if (stack.getItem() instanceof TieredItem tieredItem) {
			return tieredItem.getTier().getLevel() * 120 + (int) (tieredItem.getTier().getAttackDamageBonus() * 20.0F);
		}
		return 0;
	}

	private static int armorScore(ItemStack stack) {
		if (!(stack.getItem() instanceof ArmorItem armorItem)) {
			return 0;
		}
		return armorItem.getDefense() * 100
			+ (int) (armorItem.getToughness() * 25.0F)
			+ enchantmentBonus(stack);
	}

	private static int enchantmentBonus(ItemStack stack) {
		return EnchantmentHelper.getEnchantments(stack).values().stream().mapToInt(Integer::intValue).sum() * 10;
	}

	private static boolean isWeapon(ItemStack stack) {
		return stack.is(GungnirMod.GUNGNIR)
			|| stack.getItem() instanceof TridentItem
			|| stack.getItem() instanceof BowItem
			|| stack.getItem() instanceof SwordItem
			|| stack.getItem() instanceof AxeItem;
	}

	private void convertVillagerToEinherjar(ServerLevel level, LivingEntity villager) {
		EinherjarEntity einherjar = GungnirMod.EINHERJAR.create(level);
		if (einherjar == null) {
			return;
		}

		einherjar.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), 0.0F);
		einherjar.finalizeSpawn(level, level.getCurrentDifficultyAt(einherjar.blockPosition()), MobSpawnType.CONVERSION, null, null);
		level.addFreshEntity(einherjar);
		level.playSound(null, villager.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 0.75F, 1.25F);
		if (this.chosenEinherjarUuid == null) {
			bindEinherjar(einherjar);
		}
	}

	private static class ValkyrieFlightBowGoal extends Goal {
		private static final double ATTACK_RANGE_SQR = 24.0D * 24.0D;
		private static final double IDEAL_RANGE = 13.0D;
		private static final double MAX_FLIGHT_HEIGHT_ABOVE_TARGET = 16.0D;
		private static final double HOVER_HEIGHT = 8.0D;
		private final ValkyrieEntity valkyrie;
		private int attackTime;
		private int unseenTicks;

		ValkyrieFlightBowGoal(ValkyrieEntity valkyrie) {
			this.valkyrie = valkyrie;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.valkyrie.isValidCombatTarget(this.valkyrie.getTarget())
				&& this.valkyrie.getMainHandItem().getItem() instanceof BowItem;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void start() {
			this.attackTime = 15;
			this.unseenTicks = 0;
			this.valkyrie.getNavigation().stop();
			this.valkyrie.setFlying(true);
		}

		@Override
		public void stop() {
			this.valkyrie.setFlying(false);
			this.valkyrie.getNavigation().stop();
			this.valkyrie.setDeltaMovement(this.valkyrie.getDeltaMovement().multiply(0.45D, 0.45D, 0.45D));
		}

		@Override
		public void tick() {
			LivingEntity target = this.valkyrie.getTarget();
			if (!this.valkyrie.isValidCombatTarget(target)) {
				this.valkyrie.setTarget(null);
				return;
			}

			this.valkyrie.setFlying(true);
			this.valkyrie.getLookControl().setLookAt(target, 30.0F, 30.0F);
			moveToShootingPosition(target);
			boolean canSee = this.valkyrie.hasLineOfSight(target);
			if (canSee) {
				this.unseenTicks = 0;
			} else if (++this.unseenTicks > 60) {
				this.valkyrie.setTarget(null);
				return;
			}

			if (--this.attackTime <= 0 && this.valkyrie.distanceToSqr(target) <= ATTACK_RANGE_SQR && canSee) {
				this.attackTime = 35;
				shoot(target);
			}
		}

		private void moveToShootingPosition(LivingEntity target) {
			Vec3 away = this.valkyrie.position().subtract(target.position());
			if (away.horizontalDistanceSqr() < 0.01D) {
				away = new Vec3(1.0D, 0.0D, 0.0D);
			}

			Vec3 horizontal = new Vec3(away.x, 0.0D, away.z).normalize().scale(IDEAL_RANGE);
			double desiredY = Mth.clamp(
				target.getY() + HOVER_HEIGHT,
				target.getY() + 4.0D,
				target.getY() + MAX_FLIGHT_HEIGHT_ABOVE_TARGET
			);
			Vec3 desired = new Vec3(target.getX() + horizontal.x, desiredY, target.getZ() + horizontal.z);
			Vec3 toDesired = desired.subtract(this.valkyrie.position());
			Vec3 current = this.valkyrie.getDeltaMovement().scale(0.55D);
			Vec3 steering = toDesired.scale(0.08D);
			if (steering.length() > 0.32D) {
				steering = steering.normalize().scale(0.32D);
			}
			this.valkyrie.setDeltaMovement(current.add(steering));
			this.valkyrie.hasImpulse = true;
		}

		private void shoot(LivingEntity target) {
			Level level = this.valkyrie.level();
			if (!(level instanceof ServerLevel)) {
				return;
			}

			Arrow arrow = new Arrow(level, this.valkyrie);
			arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
			double x = target.getX() - this.valkyrie.getX();
			double y = target.getY(0.3333333333333333D) - arrow.getY();
			double z = target.getZ() - this.valkyrie.getZ();
			double horizontal = Math.sqrt(x * x + z * z);
			arrow.shoot(x, y + horizontal * 0.2D, z, 1.6F, 4.0F);
			level.addFreshEntity(arrow);
			level.playSound(null, this.valkyrie.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.25F);
		}
	}
}
