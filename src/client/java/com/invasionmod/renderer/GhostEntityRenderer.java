package com.invasionmod.renderer;

import com.invasionmod.entity.GhostEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import static com.invasionmod.InvasionMod.MOD_ID;

public class GhostEntityRenderer extends LivingEntityRenderer<GhostEntity, PlayerEntityModel<GhostEntity>> {

    public GhostEntityRenderer(EntityRendererFactory.Context context, PlayerEntityModel<GhostEntity> entityModel, float f) {
        super(context, entityModel, f);
    }

    @Override
    public Identifier getTexture(GhostEntity entity) {
        return new Identifier(MOD_ID, "textures/entity/ghost.png");
    }

    @Override
    public void render(GhostEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {

        this.setModelPose(livingEntity);
        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);

    }

    public Vec3d getPositionOffset(GhostEntity ghostEntity, float f) {
        return ghostEntity.isInSneakingPose() ? new Vec3d(0.0, -0.125, 0.0) : super.getPositionOffset(ghostEntity, f);
    }

    private void setModelPose(GhostEntity ghostEntity) {
        PlayerEntityModel<GhostEntity> playerEntityModel = this.getModel();

        playerEntityModel.setVisible(!ghostEntity.isInSneakingPose());
        playerEntityModel.hat.visible = false;
        playerEntityModel.jacket.visible = false;
        playerEntityModel.leftPants.visible = false;
        playerEntityModel.rightPants.visible = false;
        playerEntityModel.leftSleeve.visible = false;
        playerEntityModel.rightSleeve.visible = false;
        playerEntityModel.sneaking = ghostEntity.isInSneakingPose();
        BipedEntityModel.ArmPose armPose = getArmPose(ghostEntity, Hand.MAIN_HAND);
        BipedEntityModel.ArmPose armPose2 = getArmPose(ghostEntity, Hand.OFF_HAND);
        if (armPose.isTwoHanded()) {
            armPose2 = ghostEntity.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
        }

        if (ghostEntity.getMainArm() == Arm.RIGHT) {
            playerEntityModel.rightArmPose = armPose;
            playerEntityModel.leftArmPose = armPose2;
        } else {
            playerEntityModel.rightArmPose = armPose2;
            playerEntityModel.leftArmPose = armPose;
        }
    }

    private static BipedEntityModel.ArmPose getArmPose(GhostEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        if (itemStack == null || itemStack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        } else {
            if (player.getActiveHand() == hand && player.getItemUseTimeLeft() > 0) {
                UseAction useAction = itemStack.getUseAction();
                if (useAction == UseAction.BLOCK) {
                    return BipedEntityModel.ArmPose.BLOCK;
                }

                if (useAction == UseAction.BOW) {
                    return BipedEntityModel.ArmPose.BOW_AND_ARROW;
                }

                if (useAction == UseAction.SPEAR) {
                    return BipedEntityModel.ArmPose.THROW_SPEAR;
                }

                if (useAction == UseAction.CROSSBOW && hand == player.getActiveHand()) {
                    return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (useAction == UseAction.SPYGLASS) {
                    return BipedEntityModel.ArmPose.SPYGLASS;
                }

                if (useAction == UseAction.TOOT_HORN) {
                    return BipedEntityModel.ArmPose.TOOT_HORN;
                }

                if (useAction == UseAction.BRUSH) {
                    return BipedEntityModel.ArmPose.BRUSH;
                }
            } else if (!player.handSwinging && itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
                return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
            }

            return BipedEntityModel.ArmPose.ITEM;
        }
    }

    protected void scale(GhostEntity ghostEntity, MatrixStack matrixStack, float f) {
        matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    protected void renderLabelIfPresent(GhostEntity ghostEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
    }

    protected void setupTransforms(GhostEntity ghostEntity, MatrixStack matrixStack, float f, float g, float h) {
        float i = ghostEntity.getLeaningPitch(h);
        float j = ghostEntity.getPitch(h);
        float k;
        float l;
        if (ghostEntity.isFallFlying()) {
            super.setupTransforms(ghostEntity, matrixStack, f, g, h);
            k = (float) ghostEntity.getRoll() + h;
            l = MathHelper.clamp(k * k / 100.0F, 0.0F, 1.0F);
            if (!ghostEntity.isUsingRiptide()) {
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(l * (-90.0F - j)));
            }

            Vec3d vec3d = ghostEntity.getRotationVec(h);
            Vec3d vec3d2 = ghostEntity.lerpVelocity(h);
            double d = vec3d2.horizontalLengthSquared();
            double e = vec3d.horizontalLengthSquared();
            if (d > 0.0 && e > 0.0) {
                double m = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(d * e);
                double n = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) (Math.signum(n) * Math.acos(m))));
            }
        } else if (i > 0.0F) {
            super.setupTransforms(ghostEntity, matrixStack, f, g, h);
            k = ghostEntity.isTouchingWater() ? -90.0F - j : -90.0F;
            l = MathHelper.lerp(i, 0.0F, k);
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(l));
            if (ghostEntity.isInSwimmingPose()) {
                matrixStack.translate(0.0F, -1.0F, 0.3F);
            }
        } else {
            super.setupTransforms(ghostEntity, matrixStack, f, g, h);
        }

    }
}