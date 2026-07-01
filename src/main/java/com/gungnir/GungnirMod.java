package com.gungnir;

import com.gungnir.effect.BleedingEffect;
import com.gungnir.item.GungnirItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GungnirMod implements ModInitializer {
	public static final String MOD_ID = "gungnir";
	public static final String BLEEDING_TAG = MOD_ID + ".bleeding";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final MobEffect BLEEDING = Registry.register(
		BuiltInRegistries.MOB_EFFECT,
		id("bleeding"),
		new BleedingEffect()
	);

	public static final Item GUNGNIR = Registry.register(
		BuiltInRegistries.ITEM,
		id("gungnir"),
		new GungnirItem(new Item.Properties().durability(250))
	);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> entries.accept(GUNGNIR));
		LOGGER.info("Gungnir is ready.");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
