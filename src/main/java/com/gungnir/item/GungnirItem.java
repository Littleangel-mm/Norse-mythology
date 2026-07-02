package com.gungnir.item;

import com.gungnir.GungnirMod;
import com.gungnir.GungnirTridentState;
import com.gungnir.mixin.ThrownTridentAccessor;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public class GungnirItem extends TridentItem {
	private static final int LOYALTY_LEVEL = 3;
	private static final int THROW_THRESHOLD_TIME = 10;
	private static final float SHOOT_POWER = 2.5F;

	public GungnirItem(Properties properties) {
		super(properties);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
		int usedTicks = this.getUseDuration(stack) - remainingUseTicks;
		if (usedTicks < THROW_THRESHOLD_TIME || !(user instanceof Player player)) {
			return;
		}

		ensureLoyalty(stack);
		if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
			ItemStack thrownStack = stack.copy();
			thrownStack.setCount(1);

			ThrownTrident trident = new ThrownTrident(level, player, thrownStack);
			((ThrownTridentAccessor) trident).gungnir$setTridentItem(thrownStack);
			((GungnirTridentState) trident).gungnir$setGungnirProjectile(true);
			trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SHOOT_POWER, 1.0F);
			trident.pickup = player.getAbilities().instabuild
				? net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY
				: net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED;

			serverLevel.addFreshEntity(trident);
			level.playSound(null, trident, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);

			if (!player.getAbilities().instabuild) {
				stack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(user.getUsedItemHand()));
				stack.shrink(1);
			}
		}

		player.awardStat(Stats.ITEM_USED.get(this));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ensureLoyalty(stack);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public Component getName(ItemStack stack) {
		return super.getName(stack).copy().withStyle(ChatFormatting.GOLD);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.gungnir.gungnir.tooltip.1").withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable("item.gungnir.gungnir.tooltip.2").withStyle(ChatFormatting.DARK_RED));
		tooltip.add(Component.translatable("item.gungnir.gungnir.tooltip.3").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.gungnir.gungnir.tooltip.4").withStyle(ChatFormatting.RED));
		super.appendHoverText(stack, level, tooltip, context);
	}

	private static void ensureLoyalty(ItemStack stack) {
		if (!stack.is(GungnirMod.GUNGNIR)) {
			return;
		}

		Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
		if (enchantments.getOrDefault(Enchantments.LOYALTY, 0) < LOYALTY_LEVEL) {
			enchantments.put(Enchantments.LOYALTY, LOYALTY_LEVEL);
			EnchantmentHelper.setEnchantments(enchantments, stack);
		}
	}
}
