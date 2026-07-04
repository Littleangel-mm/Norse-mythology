package com.gungnir.client;

import com.gungnir.GungnirMod;
import com.gungnir.client.render.EinherjarRenderer;
import com.gungnir.client.render.FenrirRenderer;
import com.gungnir.client.render.ValkyrieModel;
import com.gungnir.client.render.ValkyrieRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class GungnirClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(GungnirMod.FENRIR, FenrirRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(EinherjarRenderer.LAYER_LOCATION, EinherjarRenderer::createBodyLayer);
		EntityRendererRegistry.register(GungnirMod.EINHERJAR, EinherjarRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(ValkyrieRenderer.LAYER_LOCATION, ValkyrieModel::createBodyLayer);
		EntityRendererRegistry.register(GungnirMod.VALKYRIE, ValkyrieRenderer::new);
	}
}
