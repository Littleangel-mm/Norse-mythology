package com.gungnir.mixin;

import com.gungnir.GungnirMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class DivineVillagerMixin {
	@Unique
	private static final String BUILD_X_TAG = "GungnirDivineBuildX";
	@Unique
	private static final String BUILD_Y_TAG = "GungnirDivineBuildY";
	@Unique
	private static final String BUILD_Z_TAG = "GungnirDivineBuildZ";
	@Unique
	private static final String BUILD_INDEX_TAG = "GungnirDivineBuildIndex";
	@Unique
	private static final String BUILD_COOLDOWN_TAG = "GungnirDivineBuildCooldown";
	@Unique
	private static final int BUILD_INTERVAL = 10;
	@Unique
	private static final int SITE_SCAN_INTERVAL = 80;
	@Unique
	private static final int EXISTING_VILLAGE_SCAN_RADIUS = 48;
	@Unique
	private static final List<BuildStep> VILLAGE_PLAN = createVillagePlan();

	@Unique
	private BlockPos gungnir$buildOrigin;
	@Unique
	private int gungnir$buildIndex;
	@Unique
	private int gungnir$buildCooldown;
	@Unique
	private int gungnir$siteScanCooldown;

	@Inject(method = "tick", at = @At("TAIL"))
	private void gungnir$tickDivineVillageBuilder(CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		if (villager.level().isClientSide || villager.isBaby()) {
			return;
		}

		if (!hasValkyrieChildTag(villager.getTags())) {
			return;
		}

		awakenDivineFate(villager);
		if (!villager.getTags().contains(GungnirMod.DIVINE_BUILDER_TAG) || villager.getTags().contains(GungnirMod.DIVINE_VILLAGE_BUILT_TAG)) {
			return;
		}

		tickConstruction(villager);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void gungnir$saveDivineBuilderData(CompoundTag tag, CallbackInfo ci) {
		if (this.gungnir$buildOrigin != null) {
			tag.putInt(BUILD_X_TAG, this.gungnir$buildOrigin.getX());
			tag.putInt(BUILD_Y_TAG, this.gungnir$buildOrigin.getY());
			tag.putInt(BUILD_Z_TAG, this.gungnir$buildOrigin.getZ());
		}
		tag.putInt(BUILD_INDEX_TAG, this.gungnir$buildIndex);
		tag.putInt(BUILD_COOLDOWN_TAG, this.gungnir$buildCooldown);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void gungnir$readDivineBuilderData(CompoundTag tag, CallbackInfo ci) {
		if (tag.contains(BUILD_X_TAG) && tag.contains(BUILD_Y_TAG) && tag.contains(BUILD_Z_TAG)) {
			this.gungnir$buildOrigin = new BlockPos(tag.getInt(BUILD_X_TAG), tag.getInt(BUILD_Y_TAG), tag.getInt(BUILD_Z_TAG));
		}
		this.gungnir$buildIndex = tag.getInt(BUILD_INDEX_TAG);
		this.gungnir$buildCooldown = tag.getInt(BUILD_COOLDOWN_TAG);
	}

	@Unique
	private static boolean hasValkyrieChildTag(Set<String> tags) {
		for (String tag : tags) {
			if (tag.startsWith(GungnirMod.VALKYRIE_CHILD_TAG_PREFIX)) {
				return true;
			}
		}
		return false;
	}

	@Unique
	private void awakenDivineFate(Villager villager) {
		if (villager.getTags().contains(GungnirMod.VALKYRIE_CHOSEN_CHILD_TAG)
			|| villager.getTags().contains(GungnirMod.DIVINE_VILLAGER_TAG)) {
			return;
		}

		if (villager.getRandom().nextInt(100) == 0) {
			villager.addTag(GungnirMod.VALKYRIE_CHOSEN_CHILD_TAG);
			villager.level().playSound(null, villager.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.NEUTRAL, 0.35F, 1.75F);
			return;
		}

		villager.addTag(GungnirMod.DIVINE_VILLAGER_TAG);
		villager.addTag(GungnirMod.DIVINE_BUILDER_TAG);
		villager.level().playSound(null, villager.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.7F, 1.35F);
	}

	@Unique
	private void tickConstruction(Villager villager) {
		if (!(villager.level() instanceof ServerLevel level)) {
			return;
		}

		if (this.gungnir$buildOrigin == null) {
			if (this.gungnir$siteScanCooldown-- > 0) {
				return;
			}
			this.gungnir$siteScanCooldown = SITE_SCAN_INTERVAL;
			this.gungnir$buildOrigin = findBuildOrigin(villager, level);
			if (this.gungnir$buildOrigin == null) {
				return;
			}
			villager.level().playSound(null, this.gungnir$buildOrigin, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 0.8F, 1.2F);
		}

		if (this.gungnir$buildIndex >= VILLAGE_PLAN.size()) {
			villager.addTag(GungnirMod.DIVINE_VILLAGE_BUILT_TAG);
			villager.level().playSound(null, this.gungnir$buildOrigin, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 0.9F, 1.1F);
			return;
		}

		BuildStep step = VILLAGE_PLAN.get(this.gungnir$buildIndex);
		BlockPos target = this.gungnir$buildOrigin.offset(step.offset());
		if (villager.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) > 5.5D * 5.5D) {
			villager.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.65D);
			return;
		}

		villager.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
		if (this.gungnir$buildCooldown-- > 0) {
			return;
		}

		this.gungnir$buildCooldown = BUILD_INTERVAL;
		if (applyStep(level, target, step)) {
			this.gungnir$buildIndex++;
			level.playSound(null, target, step.clear() ? SoundEvents.GRASS_BREAK : SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.45F, 1.0F);
		} else {
			this.gungnir$buildIndex++;
		}
	}

	@Unique
	private BlockPos findBuildOrigin(Villager villager, ServerLevel level) {
		if (hasNearbyVillageCore(level, villager.blockPosition())) {
			villager.addTag(GungnirMod.DIVINE_VILLAGE_BUILT_TAG);
			return null;
		}

		BlockPos center = villager.blockPosition().below();
		if (isValidBuildSite(level, center)) {
			return center;
		}

		for (int radius = 4; radius <= 12; radius += 4) {
			for (int attempt = 0; attempt < 24; attempt++) {
				int x = villager.getRandom().nextInt(radius * 2 + 1) - radius;
				int z = villager.getRandom().nextInt(radius * 2 + 1) - radius;
				BlockPos candidate = center.offset(x, 0, z);
				if (isValidBuildSite(level, candidate)) {
					return candidate;
				}
			}
		}
		return null;
	}

	@Unique
	private static boolean hasNearbyVillageCore(ServerLevel level, BlockPos center) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int x = -EXISTING_VILLAGE_SCAN_RADIUS; x <= EXISTING_VILLAGE_SCAN_RADIUS; x++) {
			for (int y = -8; y <= 8; y++) {
				for (int z = -EXISTING_VILLAGE_SCAN_RADIUS; z <= EXISTING_VILLAGE_SCAN_RADIUS; z++) {
					cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					if (level.getBlockState(cursor).is(Blocks.LODESTONE)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Unique
	private static boolean isValidBuildSite(ServerLevel level, BlockPos origin) {
		for (int x = -8; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				BlockPos ground = origin.offset(x, 0, z);
				BlockState groundState = level.getBlockState(ground);
				if (!isNaturalGround(groundState)) {
					return false;
				}
				if (!isReplaceable(level.getBlockState(ground.above())) || !isReplaceable(level.getBlockState(ground.above(2)))) {
					return false;
				}
			}
		}
		return true;
	}

	@Unique
	private static boolean isNaturalGround(BlockState state) {
		return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL);
	}

	@Unique
	private static boolean isReplaceable(BlockState state) {
		return state.isAir()
			|| state.is(Blocks.GRASS)
			|| state.is(Blocks.TALL_GRASS)
			|| state.is(Blocks.FERN)
			|| state.is(Blocks.LARGE_FERN)
			|| state.is(Blocks.SNOW);
	}

	@Unique
	private static boolean applyStep(ServerLevel level, BlockPos target, BuildStep step) {
		BlockState current = level.getBlockState(target);
		if (step.clear()) {
			if (current.isAir()) {
				return true;
			}
			level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			return true;
		}
		if (current.is(step.state().getBlock()) && current.equals(step.state())) {
			return true;
		}
		level.setBlock(target, step.state(), Block.UPDATE_ALL);
		return true;
	}

	@Unique
	private static List<BuildStep> createVillagePlan() {
		List<BuildStep> steps = new ArrayList<>();

		steps.add(clear(0, 1, -9));
		steps.add(place(0, 0, -9, Blocks.LODESTONE.defaultBlockState()));

		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = 1; y <= 5; y++) {
					steps.add(clear(x, y, z));
				}
			}
		}

		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				steps.add(place(x, 0, z, Blocks.OAK_PLANKS.defaultBlockState()));
			}
		}

		for (int y = 1; y <= 3; y++) {
			for (int x = -4; x <= 4; x++) {
				for (int z = -4; z <= 4; z++) {
					boolean edge = x == -4 || x == 4 || z == -4 || z == 4;
					boolean doorway = z == -4 && x >= -1 && x <= 1 && y <= 2;
					if (!edge || doorway) {
						continue;
					}
					BlockState wall = (Math.abs(x) == 4 && Math.abs(z) == 4) ? Blocks.OAK_LOG.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
					steps.add(place(x, y, z, wall));
				}
			}
		}

		for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= 5; z++) {
				steps.add(place(x, 4, z, Blocks.DARK_OAK_SLAB.defaultBlockState()));
			}
		}

		steps.add(place(0, 1, 0, Blocks.CRAFTING_TABLE.defaultBlockState()));
		steps.add(place(2, 1, 2, Blocks.CHEST.defaultBlockState()));
		steps.add(place(-2, 1, 2, Blocks.TORCH.defaultBlockState()));
		steps.add(place(2, 1, -2, Blocks.TORCH.defaultBlockState()));

		for (int z = -8; z <= -5; z++) {
			steps.add(place(0, 0, z, Blocks.DIRT_PATH.defaultBlockState()));
			steps.add(clear(0, 1, z));
		}

		steps.add(place(0, 1, -9, Blocks.BELL.defaultBlockState()));
		steps.add(place(-1, 1, -9, Blocks.TORCH.defaultBlockState()));
		steps.add(place(1, 1, -9, Blocks.TORCH.defaultBlockState()));

		for (int x = 6; x <= 10; x++) {
			for (int z = -3; z <= 3; z++) {
				if (x == 8 && z == 0) {
					steps.add(place(x, 0, z, Blocks.WATER.defaultBlockState()));
				} else {
					steps.add(place(x, 0, z, Blocks.FARMLAND.defaultBlockState()));
					steps.add(place(x, 1, z, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE)));
				}
			}
		}

		for (int z = -8; z <= 3; z++) {
			steps.add(place(4, 0, z, Blocks.DIRT_PATH.defaultBlockState()));
		}

		return steps;
	}

	@Unique
	private static BuildStep clear(int x, int y, int z) {
		return new BuildStep(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), true);
	}

	@Unique
	private static BuildStep place(int x, int y, int z, BlockState state) {
		return new BuildStep(new BlockPos(x, y, z), state, false);
	}

	@Unique
	private record BuildStep(BlockPos offset, BlockState state, boolean clear) {
	}
}
