package com.gungnir.client.render;

import com.gungnir.GungnirMod;
import com.gungnir.entity.FenrirEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FenrirGeoModel extends GeoModel<FenrirEntity> {
	@Override
	public ResourceLocation getModelResource(FenrirEntity fenrir) {
		return GungnirMod.id("geo/fenrir.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FenrirEntity fenrir) {
		return fenrir.isEnraged()
			? GungnirMod.id("textures/entity/fenrir_enraged.png")
			: GungnirMod.id("textures/entity/fenrir.png");
	}

	@Override
	public ResourceLocation getAnimationResource(FenrirEntity fenrir) {
		return GungnirMod.id("animations/fenrir.animation.json");
	}
}
