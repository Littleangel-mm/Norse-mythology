package com.gungnir.entity;

import com.gungnir.GungnirMod;
import com.gungnir.GungnirLightning;
import com.gungnir.GungnirTridentState;
import com.gungnir.mixin.ThrownTridentAccessor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class EinherjarEntity extends PathfinderMob {
	private static final int PICKUP_INTERVAL = 20;
	private static final int EAT_INTERVAL = 60;
	private static final int MAX_FOOD_STACK = 32;
	private static final int STARTING_FOOD_COUNT = 32;
	private static final int GUNGNIR_BLEEDING_DURATION = Integer.MAX_VALUE;
	private static final String WEAPON_INVENTORY_TAG = "GungnirWeaponInventory";
	private final List<ItemStack> weaponInventory = new ArrayList<>();
	private int pickupCooldown;
	private int eatCooldown;

	public EinherjarEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
		this.setCanPickUpLoot(false);
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.COOKED_BEEF, STARTING_FOOD_COUNT));
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
		this.goalSelector.addGoal(2, new EinherjarThrownWeaponGoal(this, 1.0D, 24.0F));
		this.goalSelector.addGoal(3, new EinherjarBowGoal(this, 1.0D, 18.0F));
		this.goalSelector.addGoal(4, new EinherjarMeleeGoal(this, 1.15D, true));
		this.goalSelector.addGoal(5, new FollowGungnirPlayerGoal(this, 1.0D));
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
		if (this.getOffhandItem().isEmpty()) {
			this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.COOKED_BEEF, STARTING_FOOD_COUNT));
		}
		return data;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide) {
			return;
		}

		selectBestWeapon();
		if (--this.pickupCooldown <= 0) {
			this.pickupCooldown = PICKUP_INTERVAL;
			pickUpUsefulItems();
		}
		if (--this.eatCooldown <= 0) {
			this.eatCooldown = EAT_INTERVAL;
			eatIfNeeded();
		}
		supportNearbyGungnirPlayer();
	}

	private void pickUpUsefulItems() {
		List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.6D), item -> item.isAlive() && !item.hasPickUpDelay());
		for (ItemEntity itemEntity : items) {
			ItemStack stack = itemEntity.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			if (tryEquipWeapon(stack)) {
				consumeItem(itemEntity, 1);
				return;
			}
			if (tryEquipArmor(stack)) {
				consumeItem(itemEntity, 1);
				return;
			}
			int storedFood = tryStoreFood(stack);
			if (storedFood > 0) {
				consumeItem(itemEntity, storedFood);
				return;
			}
		}
	}

	private boolean tryEquipWeapon(ItemStack stack) {
		if (!isWeapon(stack)) {
			return false;
		}

		ItemStack newWeapon = stack.copy();
		newWeapon.setCount(1);
		this.weaponInventory.add(newWeapon);
		selectBestWeapon();
		return true;
	}

	private boolean tryEquipArmor(ItemStack stack) {
		if (!(stack.getItem() instanceof ArmorItem armorItem)) {
			return false;
		}

		EquipmentSlot slot = armorItem.getEquipmentSlot();
		if (slot.getType() != EquipmentSlot.Type.ARMOR) {
			return false;
		}

		if (armorScore(stack) <= armorScore(this.getItemBySlot(slot))) {
			return false;
		}

		ItemStack oldArmor = this.getItemBySlot(slot);
		if (!oldArmor.isEmpty()) {
			this.spawnAtLocation(oldArmor.copy());
		}
		ItemStack newArmor = stack.copy();
		newArmor.setCount(1);
		this.setItemSlot(slot, newArmor);
		return true;
	}

	private int tryStoreFood(ItemStack stack) {
		if (!stack.getItem().isEdible()) {
			return 0;
		}

		ItemStack offhand = this.getOffhandItem();
		if (!offhand.isEmpty() && !ItemStack.isSameItemSameTags(offhand, stack)) {
			return 0;
		}

		int room = offhand.isEmpty() ? MAX_FOOD_STACK : MAX_FOOD_STACK - offhand.getCount();
		if (room <= 0) {
			return 0;
		}

		int storedCount = Math.min(stack.getCount(), room);
		ItemStack stored = stack.copy();
		stored.setCount(storedCount);
		if (!offhand.isEmpty()) {
			stored.grow(offhand.getCount());
		}
		this.setItemSlot(EquipmentSlot.OFFHAND, stored);
		return storedCount;
	}

	private void consumeItem(ItemEntity itemEntity, int count) {
		ItemStack stack = itemEntity.getItem();
		this.take(itemEntity, count);
		stack.shrink(count);
		if (stack.isEmpty()) {
			itemEntity.discard();
		}
	}

	@Override
	public boolean wantsToPickUp(ItemStack stack) {
		return isUsefulPickup(stack);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		ListTag weapons = new ListTag();
		for (ItemStack stack : this.weaponInventory) {
			if (!stack.isEmpty() && isWeapon(stack)) {
				weapons.add(stack.save(new CompoundTag()));
			}
		}
		tag.put(WEAPON_INVENTORY_TAG, weapons);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.weaponInventory.clear();
		ListTag weapons = tag.getList(WEAPON_INVENTORY_TAG, Tag.TAG_COMPOUND);
		for (int i = 0; i < weapons.size(); i++) {
			ItemStack stack = ItemStack.of(weapons.getCompound(i));
			if (!stack.isEmpty() && isWeapon(stack)) {
				stack.setCount(1);
				this.weaponInventory.add(stack);
			}
		}
		selectBestWeapon();
	}

	@Override
	protected boolean canReplaceCurrentItem(ItemStack candidate, ItemStack current) {
		if (isWeapon(candidate) || isWeapon(current)) {
			return weaponScore(candidate) > weaponScore(current);
		}
		if (candidate.getItem() instanceof ArmorItem || current.getItem() instanceof ArmorItem) {
			return armorScore(candidate) > armorScore(current);
		}
		return super.canReplaceCurrentItem(candidate, current);
	}

	@Override
	public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
		boolean hurt = super.doHurtTarget(target);
		if (hurt && target instanceof LivingEntity livingEntity && this.getMainHandItem().is(GungnirMod.GUNGNIR)) {
			livingEntity.addTag(GungnirMod.BLEEDING_TAG);
			livingEntity.addEffect(new MobEffectInstance(GungnirMod.BLEEDING, GUNGNIR_BLEEDING_DURATION, 0, false, true, true));
			if (this.level() instanceof ServerLevel serverLevel) {
				GungnirLightning.strike(serverLevel, livingEntity);
			}
		}
		return hurt;
	}

	@Override
	protected void dropEquipment() {
		preserveMainHandWeaponForDrop();
		super.dropEquipment();
		dropStoredWeapons();
	}

	@Override
	protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
		preserveMainHandWeaponForDrop();
		super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
		dropStoredWeapons();
	}

	private void preserveMainHandWeaponForDrop() {
		ItemStack current = this.getMainHandItem();
		if (!current.isEmpty() && isWeapon(current)) {
			ItemStack preserved = current.copy();
			preserved.setCount(1);
			this.weaponInventory.add(preserved);
			this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
		preserveArmorForDrop();
	}

	private void preserveArmorForDrop() {
		for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
			ItemStack armor = this.getItemBySlot(slot);
			if (!armor.isEmpty()) {
				this.spawnAtLocation(armor.copy());
				this.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}

	private void dropStoredWeapons() {
		for (ItemStack stack : this.weaponInventory) {
			if (!stack.isEmpty()) {
				this.spawnAtLocation(stack.copy());
			}
		}
		this.weaponInventory.clear();
	}

	private void eatIfNeeded() {
		if (this.getHealth() > this.getMaxHealth() * 0.65F) {
			return;
		}

		ItemStack food = this.getOffhandItem();
		if (food.isEmpty() || !food.getItem().isEdible()) {
			return;
		}

		this.heal(6.0F);
		food.shrink(1);
		this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8F, 1.0F);
		if (food.isEmpty()) {
			this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
	}

	private boolean isUsingBow() {
		return this.getMainHandItem().getItem() instanceof BowItem;
	}

	private boolean isUsingThrownWeapon() {
		ItemStack stack = this.getMainHandItem();
		return stack.is(GungnirMod.GUNGNIR) || stack.getItem() instanceof TridentItem;
	}

	private boolean isUsefulPickup(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (isWeapon(stack)) {
			return true;
		}
		if (stack.getItem() instanceof ArmorItem armorItem) {
			EquipmentSlot slot = armorItem.getEquipmentSlot();
			return slot.getType() == EquipmentSlot.Type.ARMOR && armorScore(stack) > armorScore(this.getItemBySlot(slot));
		}
		return stack.getItem().isEdible();
	}

	private void supportNearbyGungnirPlayer() {
		Player leader = findNearbyGungnirPlayer(32.0D);
		if (leader == null) {
			return;
		}

		LivingEntity sharedTarget = findSharedTarget(leader);
		if (sharedTarget != null) {
			this.setTarget(sharedTarget);
		}

		double distance = this.distanceToSqr(leader);
		LivingEntity target = this.getTarget();
		if ((target == null || !target.isAlive() || distance > 14.0D * 14.0D) && distance > 6.0D * 6.0D) {
			this.getNavigation().moveTo(leader, 1.05D);
		}
	}

	private Player findNearbyGungnirPlayer(double range) {
		return this.level().getEntitiesOfClass(
			Player.class,
			this.getBoundingBox().inflate(range),
			player -> player.isAlive() && hasGungnirInHand(player)
		).stream().min((left, right) -> Double.compare(
			left.distanceToSqr(this),
			right.distanceToSqr(this)
		)).orElse(null);
	}

	private LivingEntity findSharedTarget(Player player) {
		LivingEntity attackedByPlayer = player.getLastHurtMob();
		if (isValidSharedTarget(attackedByPlayer)) {
			return attackedByPlayer;
		}
		LivingEntity attackingPlayer = player.getLastHurtByMob();
		if (isValidSharedTarget(attackingPlayer)) {
			return attackingPlayer;
		}
		return null;
	}

	private boolean isValidSharedTarget(LivingEntity target) {
		return target != null
			&& target.isAlive()
			&& target instanceof Enemy
			&& target != this
			&& this.distanceToSqr(target) <= 48.0D * 48.0D;
	}

	private void stashCurrentWeapon() {
		ItemStack current = this.getMainHandItem();
		if (!current.isEmpty() && isWeapon(current)) {
			ItemStack stored = current.copy();
			stored.setCount(1);
			this.weaponInventory.add(stored);
		}
	}

	private void selectBestWeapon() {
		removeInvalidStoredWeapons();

		ItemStack current = this.getMainHandItem();
		int bestScore = weaponScore(current);
		int bestIndex = -1;
		for (int i = 0; i < this.weaponInventory.size(); i++) {
			ItemStack stored = this.weaponInventory.get(i);
			int score = weaponScore(stored);
			if (score > bestScore) {
				bestScore = score;
				bestIndex = i;
			}
		}

		if (bestIndex < 0) {
			return;
		}

		ItemStack bestWeapon = this.weaponInventory.remove(bestIndex);
		if (!current.isEmpty() && isWeapon(current)) {
			ItemStack storedCurrent = current.copy();
			storedCurrent.setCount(1);
			this.weaponInventory.add(storedCurrent);
		}
		this.setItemSlot(EquipmentSlot.MAINHAND, bestWeapon.copy());
		removeInvalidStoredWeapons();
	}

	private void removeInvalidStoredWeapons() {
		Iterator<ItemStack> iterator = this.weaponInventory.iterator();
		while (iterator.hasNext()) {
			ItemStack stack = iterator.next();
			if (stack.isEmpty() || !isWeapon(stack)) {
				iterator.remove();
			} else {
				stack.setCount(1);
			}
		}
	}

	private static boolean isWeapon(ItemStack stack) {
		return stack.is(GungnirMod.GUNGNIR)
			|| stack.getItem() instanceof TridentItem
			|| stack.getItem() instanceof BowItem
			|| stack.getItem() instanceof SwordItem
			|| stack.getItem() instanceof AxeItem;
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

	private static class EinherjarMeleeGoal extends MeleeAttackGoal {
		private final EinherjarEntity einherjar;

		EinherjarMeleeGoal(EinherjarEntity einherjar, double speedModifier, boolean followingTargetEvenIfNotSeen) {
			super(einherjar, speedModifier, followingTargetEvenIfNotSeen);
			this.einherjar = einherjar;
		}

		@Override
		public boolean canUse() {
			return !this.einherjar.isUsingBow() && !this.einherjar.isUsingThrownWeapon() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !this.einherjar.isUsingBow() && !this.einherjar.isUsingThrownWeapon() && super.canContinueToUse();
		}
	}

	private static class EinherjarThrownWeaponGoal extends Goal {
		private static final double MELEE_RANGE_SQR = 9.0D;
		private static final double RETREAT_RANGE_SQR = 25.0D;
		private static final int MELEE_COOLDOWN_TICKS = 20;
		private final EinherjarEntity einherjar;
		private final double speedModifier;
		private final float attackRadiusSqr;
		private int attackTime;
		private int meleeTime;

		EinherjarThrownWeaponGoal(EinherjarEntity einherjar, double speedModifier, float attackRadius) {
			this.einherjar = einherjar;
			this.speedModifier = speedModifier;
			this.attackRadiusSqr = attackRadius * attackRadius;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return this.einherjar.isUsingThrownWeapon() && this.einherjar.getTarget() != null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void start() {
			this.attackTime = 20;
			this.meleeTime = 0;
		}

		@Override
		public void tick() {
			LivingEntity target = this.einherjar.getTarget();
			if (target == null) {
				return;
			}

			double distance = this.einherjar.distanceToSqr(target);
			if (distance <= MELEE_RANGE_SQR) {
				this.einherjar.getNavigation().stop();
				melee(target);
				retreatFrom(target);
			} else if (distance <= RETREAT_RANGE_SQR) {
				retreatFrom(target);
			} else if (distance > this.attackRadiusSqr * 0.8D) {
				this.einherjar.getNavigation().moveTo(target, this.speedModifier);
			} else {
				this.einherjar.getNavigation().stop();
			}

			this.einherjar.getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (distance > MELEE_RANGE_SQR && distance <= this.attackRadiusSqr && this.einherjar.hasLineOfSight(target) && --this.attackTime <= 0) {
				this.attackTime = 45;
				throwWeapon(target);
			}
		}

		private void melee(LivingEntity target) {
			if (this.meleeTime > 0) {
				this.meleeTime--;
				return;
			}
			this.meleeTime = MELEE_COOLDOWN_TICKS;
			this.einherjar.swing(InteractionHand.MAIN_HAND);
			this.einherjar.doHurtTarget(target);
		}

		private void retreatFrom(LivingEntity target) {
			Vec3 away = this.einherjar.position().subtract(target.position());
			if (away.horizontalDistanceSqr() <= 0.0001D) {
				return;
			}
			Vec3 retreat = this.einherjar.position().add(away.normalize().scale(6.0D));
			this.einherjar.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, this.speedModifier * 1.1D);
		}

		private void throwWeapon(LivingEntity target) {
			Level level = this.einherjar.level();
			if (!(level instanceof ServerLevel serverLevel)) {
				return;
			}

			ItemStack weapon = this.einherjar.getMainHandItem();
			ItemStack thrownStack = weapon.copy();
			thrownStack.setCount(1);
			removeLoyalty(thrownStack);
			ThrownTrident trident = new ThrownTrident(level, this.einherjar, thrownStack);
			((ThrownTridentAccessor) trident).gungnir$setTridentItem(thrownStack);
			if (thrownStack.is(GungnirMod.GUNGNIR)) {
				((GungnirTridentState) trident).gungnir$setGungnirProjectile(true);
			}
			trident.pickup = AbstractArrow.Pickup.DISALLOWED;

			double x = target.getX() - this.einherjar.getX();
			double y = target.getY(0.3333333333333333D) - trident.getY();
			double z = target.getZ() - this.einherjar.getZ();
			double horizontal = Math.sqrt(x * x + z * z);
			float inaccuracy = thrownStack.is(GungnirMod.GUNGNIR) ? 1.0F : 6.0F;
			trident.shoot(x, y + horizontal * 0.2D, z, 2.5F, inaccuracy);
			serverLevel.addFreshEntity(trident);
			level.playSound(null, this.einherjar.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);
		}

		private void removeLoyalty(ItemStack stack) {
			Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
			if (enchantments.remove(Enchantments.LOYALTY) != null) {
				EnchantmentHelper.setEnchantments(enchantments, stack);
			}
		}
	}

	private static class FollowGungnirPlayerGoal extends Goal {
		private final EinherjarEntity einherjar;
		private final double speedModifier;
		private Player leader;

		FollowGungnirPlayerGoal(EinherjarEntity einherjar, double speedModifier) {
			this.einherjar = einherjar;
			this.speedModifier = speedModifier;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			this.leader = findLeader();
			return this.leader != null;
		}

		@Override
		public boolean canContinueToUse() {
			return this.leader != null
				&& this.leader.isAlive()
				&& hasGungnirInHand(this.leader)
				&& this.einherjar.distanceToSqr(this.leader) <= 42.0D * 42.0D;
		}

		@Override
		public void stop() {
			this.leader = null;
			this.einherjar.getNavigation().stop();
		}

		@Override
		public void tick() {
			if (this.leader == null) {
				return;
			}

			this.einherjar.getLookControl().setLookAt(this.leader, 20.0F, 20.0F);
			LivingEntity sharedTarget = findSharedTarget(this.leader);
			if (sharedTarget != null) {
				this.einherjar.setTarget(sharedTarget);
			}

			double distance = this.einherjar.distanceToSqr(this.leader);
			if (distance > 7.0D * 7.0D) {
				this.einherjar.getNavigation().moveTo(this.leader, this.speedModifier);
			} else if (distance < 2.5D * 2.5D) {
				Vec3 away = this.einherjar.position().subtract(this.leader.position());
				if (away.horizontalDistanceSqr() > 0.0001D) {
					Vec3 step = this.einherjar.position().add(away.normalize().scale(3.0D));
					this.einherjar.getNavigation().moveTo(step.x, step.y, step.z, this.speedModifier);
				}
			} else if (this.einherjar.getTarget() == null) {
				this.einherjar.getNavigation().stop();
			}
		}

		private Player findLeader() {
			return this.einherjar.level().getEntitiesOfClass(
				Player.class,
				this.einherjar.getBoundingBox().inflate(24.0D),
				player -> player.isAlive() && hasGungnirInHand(player)
			).stream().min((left, right) -> Double.compare(
				left.distanceToSqr(this.einherjar),
				right.distanceToSqr(this.einherjar)
			)).orElse(null);
		}

		private LivingEntity findSharedTarget(Player player) {
			LivingEntity attackedByPlayer = player.getLastHurtMob();
			if (isValidSharedTarget(attackedByPlayer)) {
				return attackedByPlayer;
			}
			LivingEntity attackingPlayer = player.getLastHurtByMob();
			if (isValidSharedTarget(attackingPlayer)) {
				return attackingPlayer;
			}
			return null;
		}

		private boolean isValidSharedTarget(LivingEntity target) {
			return target != null
				&& target.isAlive()
				&& target instanceof Enemy
				&& target != this.einherjar
				&& this.einherjar.distanceToSqr(target) <= 48.0D * 48.0D;
		}

		private static boolean hasGungnirInHand(Player player) {
			return EinherjarEntity.hasGungnirInHand(player);
		}
	}

	private static boolean hasGungnirInHand(Player player) {
		return player.getMainHandItem().is(GungnirMod.GUNGNIR) || player.getOffhandItem().is(GungnirMod.GUNGNIR);
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
