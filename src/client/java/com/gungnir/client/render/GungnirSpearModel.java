package com.gungnir.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

public class GungnirSpearModel extends Model {
	private static final String ROOT = "root";
	private final ModelPart root;

	public GungnirSpearModel(ModelPart root) {
		super(RenderType::entityCutoutNoCull);
		this.root = root.getChild(ROOT);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot().addOrReplaceChild(ROOT, CubeListBuilder.create(), PartPose.ZERO);

		root.addOrReplaceChild(
			"shaft",
			CubeListBuilder.create()
				.texOffs(0, 16)
				.addBox(-0.5F, -12.0F, -0.5F, 1.0F, 20.0F, 1.0F),
			PartPose.ZERO
		);
		root.addOrReplaceChild(
			"tip",
			CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(-1.5F, -20.0F, -1.5F, 3.0F, 7.0F, 3.0F),
			PartPose.ZERO
		);
		root.addOrReplaceChild(
			"neck",
			CubeListBuilder.create()
				.texOffs(12, 0)
				.addBox(-1.0F, -14.0F, -1.0F, 2.0F, 2.0F, 2.0F),
			PartPose.ZERO
		);
		root.addOrReplaceChild(
			"tail",
			CubeListBuilder.create()
				.texOffs(12, 8)
				.addBox(-1.0F, 11.0F, -1.0F, 2.0F, 4.0F, 2.0F),
			PartPose.ZERO
		);

		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
		this.root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
	}
}
