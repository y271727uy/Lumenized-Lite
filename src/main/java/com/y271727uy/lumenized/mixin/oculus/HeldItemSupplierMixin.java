package com.y271727uy.lumenized.mixin.oculus;

import com.y271727uy.lumenized.client.light.LightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.uniforms.IdMapUniforms.HeldItemSupplier", remap = false)
public abstract class HeldItemSupplierMixin {

    @Shadow(remap = false)
    private int lightValue;

    @Shadow(remap = false)
    private Vector3f lightColor;

    @Shadow(remap = false) @Final private InteractionHand hand;

    @Inject(method = "update", at = @At("TAIL"))
    private void injectColoredLight(CallbackInfo ci) {
        if (hand != InteractionHand.MAIN_HAND) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var light = LightManager.INSTANCE.getPlayerHeldItemLight(player);
        if (light != null) {
            lightValue = (int) light.radius;
            lightColor = new Vector3f(light.r, light.g, light.b);
            LightManager.INSTANCE.removePlayerLight(player.getUUID());
        }
    }
}
