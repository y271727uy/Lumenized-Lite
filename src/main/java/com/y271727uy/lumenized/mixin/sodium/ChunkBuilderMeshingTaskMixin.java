package com.y271727uy.lumenized.mixin.sodium;

import com.y271727uy.lumenized.core.sodium.ChunkBuilderTaskAccess;
import com.google.common.collect.ImmutableList;
import com.y271727uy.lumenized.client.light.ColorPointLight;
import com.y271727uy.lumenized.client.light.LightManager;
import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.core.IRenderChunk;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author KilaBash
 * @date 2022/05/28
 * @implNote ChunkRenderRebuildTaskMixin
 */
@Mixin(ChunkBuilderMeshingTask.class)

public abstract class ChunkBuilderMeshingTaskMixin {
    @Unique
    ImmutableList.Builder<ColorPointLight> shimmer$lights;

    @Redirect(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;"),
            remap = false)
    private BlockState injectChunkCompileAddLight(LevelSlice slice, int x, int y, int z) {
        // Call through the mapped vanilla view interface.  Calling the
        // Sodium method directly makes javac expose Fabric's intermediary
        // class_1920 return type from LevelSlice's inherited bridge.
        BlockAndTintGetter view = slice;
        var blockState = view.getBlockState(new BlockPos(x, y, z));
        if (!blockState.isAir()) {
            var fluidState = blockState.getFluidState();
            if (LightManager.INSTANCE.isBlockHasLight(blockState.getBlock(), fluidState)) {
                var light = LightManager.INSTANCE.getBlockStateLight(slice, new BlockPos(x, y, z), blockState, fluidState);
                if (light != null) shimmer$lights.add(light);
            }
            PostProcessing.setupBloom(blockState, fluidState);
        }
        return blockState;
    }

    @Inject(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", at = @At("HEAD"), remap = false)
    private void injectChunkCompilePre(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        shimmer$lights = ImmutableList.builder();
    }

    @Inject(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", at = @At("RETURN"), remap = false)
    private void injectChunkCompilePost(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        RenderSection render = ((ChunkBuilderTaskAccess) (Object) this).lumenized$getRenderSection();
        if (render instanceof IRenderChunk shimmerRenderChunk) {
            shimmerRenderChunk.setShimmerLights(shimmer$lights.build());
        }
        shimmer$lights = null;
        PostProcessing.cleanBloom();
    }
}
