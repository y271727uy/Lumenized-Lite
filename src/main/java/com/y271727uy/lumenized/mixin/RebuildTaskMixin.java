package com.y271727uy.lumenized.mixin;

import com.google.common.collect.ImmutableList;
import com.y271727uy.lumenized.client.light.ColorPointLight;
import com.y271727uy.lumenized.client.light.LightManager;
import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.core.IRenderChunk;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author KilaBash
 * @date 2022/05/02
 * @implNote RebuildTaskMixin, used to compile and save light info to the chunk.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public abstract class RebuildTaskMixin {
    @SuppressWarnings("target") @Shadow(aliases = {"this$1", "f_112859_", "field_20839"}) @Final ChunkRenderDispatcher.RenderChunk this$1;
    ImmutableList.Builder<ColorPointLight> lights;

    @Redirect(method = "compile",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 0))
    private BlockState injectCompile(RenderChunkRegion instance, BlockPos pPos) {
        BlockState blockstate = instance.getBlockState(pPos);
        FluidState fluidstate = blockstate.getFluidState();
        if (LightManager.INSTANCE.isBlockHasLight(blockstate.getBlock(), fluidstate)) {
            ColorPointLight light = LightManager.INSTANCE.getBlockStateLight(instance, pPos, blockstate, fluidstate);
            if (light != null) {
                lights.add(light);
            }
        }

        PostProcessing.setupBloom(blockstate, fluidstate);
        return blockstate;
    }

    @Inject(method = "compile", at = @At(value = "HEAD"))
    @SuppressWarnings("rawtypes")
    private void injectCompilePre(float x, float y, float z, ChunkBufferBuilderPack arg, CallbackInfoReturnable cir) {
        lights = ImmutableList.builder();
    }

    @Inject(method = "compile", at = @At(value = "RETURN"))
    @SuppressWarnings("rawtypes")
    private void injectCompilePost(float x, float y, float z, ChunkBufferBuilderPack arg, CallbackInfoReturnable cir) {
        if (this$1 instanceof IRenderChunk) {
            ((IRenderChunk) this$1).setShimmerLights(lights.build());
        }
        PostProcessing.cleanBloom();
    }
}
