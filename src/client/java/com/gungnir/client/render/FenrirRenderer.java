package com.gungnir.client.render;

import com.gungnir.entity.FenrirEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class FenrirRenderer extends GeoEntityRenderer<FenrirEntity> {
	public FenrirRenderer(EntityRendererProvider.Context context) {
		super(context, new FenrirGeoModel());
		this.shadowRadius = 2.3F;
		withScale(5.0F);
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}
}
