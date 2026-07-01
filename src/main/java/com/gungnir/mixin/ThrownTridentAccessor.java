package com.gungnir.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.entity.projectile.ThrownTrident;

@Mixin(ThrownTrident.class)
public interface ThrownTridentAccessor {
	@Accessor("tridentItem")
	ItemStack gungnir$getTridentItem();

	@Accessor("tridentItem")
	void gungnir$setTridentItem(ItemStack stack);
}
