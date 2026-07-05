package com.gungnir.client.render;

import com.gungnir.entity.ValkyrieEntity;
import java.util.List;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public class ValkyrieElytraModel extends AgeableListModel<ValkyrieEntity> {
	private final ModelPart leftWing;
	private final ModelPart rightWing;

	public ValkyrieElytraModel(ModelPart root) {
		this.leftWing = root.getChild("left_wing");
		this.rightWing = root.getChild("right_wing");
	}

	@Override
	protected Iterable<ModelPart> headParts() {
		return List.of();
	}

	@Override
	protected Iterable<ModelPart> bodyParts() {
		return List.of(this.leftWing, this.rightWing);
	}

	@Override
	public void setupAnim(ValkyrieEntity valkyrie, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		float flap = valkyrie.isFlying() ? Mth.sin(ageInTicks * 0.45F) * 0.08F : Mth.sin(ageInTicks * 0.08F) * 0.02F;
		float xRot = valkyrie.isFlying() ? 0.12F + flap : 0.28F;
		float yRot = valkyrie.isFlying() ? 0.34F : 0.08F;
		float zRot = valkyrie.isFlying() ? -0.42F - flap : -0.26F;

		this.leftWing.x = 5.0F;
		this.leftWing.y = 0.0F;
		this.leftWing.xRot = xRot;
		this.leftWing.yRot = yRot;
		this.leftWing.zRot = zRot;

		this.rightWing.x = -this.leftWing.x;
		this.rightWing.y = this.leftWing.y;
		this.rightWing.xRot = this.leftWing.xRot;
		this.rightWing.yRot = -this.leftWing.yRot;
		this.rightWing.zRot = -this.leftWing.zRot;
	}
}
