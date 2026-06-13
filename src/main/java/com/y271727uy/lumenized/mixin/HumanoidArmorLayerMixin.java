package com.y271727uy.lumenized.mixin;

import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.client.shader.RenderUtils;
import com.y271727uy.lumenized.client.ResourceUtils;
import com.y271727uy.lumenized.client.LumenizedRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * @author KilaBash
 * @date 2022/05/02
 * @implNote HumanoidArmorLayerMixin, used to inject emissive + bloom armor via custom resource pack.
 */
@Mixin(HumanoidArmorLayer.class)//FIXME
public abstract class HumanoidArmorLayerMixin {

    @Shadow protected abstract ResourceLocation getArmorLocation(ArmorItem armorItem, boolean bl, String string);

    @Inject(method = "renderModel", at = @At(value = "RETURN"))
    private void injectRenderModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ArmorItem armorItem,HumanoidModel humanoidModel, boolean hasFoil, float r, float g, float b,@Nullable String resourceLocation, CallbackInfo ci) {
        ResourceLocation armorResource = this.getArmorLocation(armorItem, hasFoil, resourceLocation);
        ResourceLocation bloomResource = Objects.requireNonNull(ResourceLocation.tryParse(armorResource.getNamespace() + ":" + armorResource.getPath().replace(".png", "_bloom.png")));
        if (ResourceUtils.isResourceExist(bloomResource)) {
            PoseStack finalStack = RenderUtils.copyPoseStack(poseStack);
            PostProcessing.BLOOM_UNITY.postEntity(sourceConsumer -> humanoidModel.renderToBuffer(finalStack, sourceConsumer.getBuffer(LumenizedRenderTypes.emissiveArmor(bloomResource)), 0xF000F0, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F));
        }
    }
}
