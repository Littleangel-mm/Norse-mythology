package com.gungnir.client.render;

import com.gungnir.entity.ValkyrieEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class ValkyrieModel<T extends ValkyrieEntity> extends HumanoidModel<T> {
	private final ModelPart leftWing;
	private final ModelPart rightWing;

	public ValkyrieModel(ModelPart root) {
		super(root);
		this.leftWing = this.body.getChild("left_wing");
		this.rightWing = this.body.getChild("right_wing");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition body = mesh.getRoot().getChild("body");
		PartDefinition leftWing = body.addOrReplaceChild(
			"left_wing",
			CubeListBuilder.create(),
			PartPose.offsetAndRotation(2.0F, -1.0F, 2.8F, 0.16F, -0.35F, 0.12F)
		);
		leftWing.addOrReplaceChild(
			"upper",
			CubeListBuilder.create()
				.texOffs(32, 32)
				.addBox(0.0F, -2.0F, -0.5F, 9.0F, 5.0F, 1.0F, CubeDeformation.NONE),
			PartPose.ZERO
		);
		leftWing.addOrReplaceChild("feather_1", featherBox(3, 11), PartPose.offsetAndRotation(1.0F, 1.5F, 0.0F, 0.0F, 0.0F, -0.24F));
		leftWing.addOrReplaceChild("feather_2", featherBox(3, 14), PartPose.offsetAndRotation(3.5F, 2.0F, 0.0F, 0.0F, 0.0F, -0.12F));
		leftWing.addOrReplaceChild("feather_3", featherBox(3, 17), PartPose.offsetAndRotation(6.0F, 2.5F, 0.0F, 0.0F, 0.0F, 0.0F));
		leftWing.addOrReplaceChild("feather_4", featherBox(3, 15), PartPose.offsetAndRotation(8.5F, 3.0F, 0.0F, 0.0F, 0.0F, 0.13F));
		leftWing.addOrReplaceChild("feather_5", featherBox(2, 12), PartPose.offsetAndRotation(11.0F, 3.5F, 0.0F, 0.0F, 0.0F, 0.25F));

		PartDefinition rightWing = body.addOrReplaceChild(
			"right_wing",
			CubeListBuilder.create(),
			PartPose.offsetAndRotation(-2.0F, -1.0F, 2.8F, 0.16F, 0.35F, -0.12F)
		);
		rightWing.addOrReplaceChild(
			"upper",
			CubeListBuilder.create()
				.texOffs(32, 32)
				.mirror()
				.addBox(-9.0F, -2.0F, -0.5F, 9.0F, 5.0F, 1.0F, CubeDeformation.NONE),
			PartPose.ZERO
		);
		rightWing.addOrReplaceChild("feather_1", mirroredFeatherBox(3, 11), PartPose.offsetAndRotation(-1.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.24F));
		rightWing.addOrReplaceChild("feather_2", mirroredFeatherBox(3, 14), PartPose.offsetAndRotation(-3.5F, 2.0F, 0.0F, 0.0F, 0.0F, 0.12F));
		rightWing.addOrReplaceChild("feather_3", mirroredFeatherBox(3, 17), PartPose.offsetAndRotation(-6.0F, 2.5F, 0.0F, 0.0F, 0.0F, 0.0F));
		rightWing.addOrReplaceChild("feather_4", mirroredFeatherBox(3, 15), PartPose.offsetAndRotation(-8.5F, 3.0F, 0.0F, 0.0F, 0.0F, -0.13F));
		rightWing.addOrReplaceChild("feather_5", mirroredFeatherBox(2, 12), PartPose.offsetAndRotation(-11.0F, 3.5F, 0.0F, 0.0F, 0.0F, -0.25F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static CubeListBuilder featherBox(int width, int height) {
		return CubeListBuilder.create()
			.texOffs(42, 40)
			.addBox(0.0F, 0.0F, -0.45F, width, height, 1.0F, CubeDeformation.NONE);
	}

	private static CubeListBuilder mirroredFeatherBox(int width, int height) {
		return CubeListBuilder.create()
			.texOffs(42, 40)
			.mirror()
			.addBox(-width, 0.0F, -0.45F, width, height, 1.0F, CubeDeformation.NONE);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		float flap = entity.isFlying() ? Mth.sin(ageInTicks * 0.55F) * 0.32F : Mth.sin(ageInTicks * 0.08F) * 0.05F;
		float spread = entity.isFlying() ? 1.05F : 0.48F;
		this.leftWing.yRot = -spread - flap;
		this.rightWing.yRot = spread + flap;
		this.leftWing.xRot = entity.isFlying() ? 0.04F : 0.18F;
		this.rightWing.xRot = entity.isFlying() ? 0.04F : 0.18F;
		this.leftWing.zRot = entity.isFlying() ? 0.28F : 0.1F;
		this.rightWing.zRot = entity.isFlying() ? -0.28F : -0.1F;
	}
}
