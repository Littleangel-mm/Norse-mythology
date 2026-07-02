package com.gungnir;

import com.gungnir.effect.BleedingEffect;
import com.gungnir.entity.EinherjarEntity;
import com.gungnir.entity.FenrirEntity;
import com.gungnir.item.GungnirItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
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

	public static final EntityType<FenrirEntity> FENRIR = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		id("fenrir"),
		FabricEntityTypeBuilder.create(MobCategory.MONSTER, FenrirEntity::new)
			.dimensions(EntityDimensions.scalable(3.0F, 4.25F))
			.trackRangeBlocks(96)
			.trackedUpdateRate(3)
			.build()
	);

	public static final EntityType<EinherjarEntity> EINHERJAR = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		id("einherjar"),
		FabricEntityTypeBuilder.create(MobCategory.CREATURE, EinherjarEntity::new)
			.dimensions(EntityDimensions.scalable(0.6F, 1.95F))
			.trackRangeBlocks(64)
			.trackedUpdateRate(3)
			.build()
	);

	public static final Item GUNGNIR = Registry.register(
		BuiltInRegistries.ITEM,
		id("gungnir"),
		new GungnirItem(new Item.Properties().durability(250))
	);

	public static final Item FENRIR_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		id("fenrir_spawn_egg"),
		new SpawnEggItem(FENRIR, 0x101014, 0xb81420, new Item.Properties())
	);

	public static final Item EINHERJAR_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		id("einherjar_spawn_egg"),
		new SpawnEggItem(EINHERJAR, 0x2f3545, 0xc7a45b, new Item.Properties())
	);

	@Override
	public void onInitialize() {
		FabricDefaultAttributeRegistry.register(FENRIR, FenrirEntity.createFenrirAttributes());
		FabricDefaultAttributeRegistry.register(EINHERJAR, EinherjarEntity.createEinherjarAttributes());
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(GUNGNIR);
			entries.accept(FENRIR_SPAWN_EGG);
			entries.accept(EINHERJAR_SPAWN_EGG);
		});
		LOGGER.info("Gungnir is ready.");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
