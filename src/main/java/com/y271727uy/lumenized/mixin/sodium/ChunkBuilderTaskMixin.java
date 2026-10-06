package com.y271727uy.lumenized.mixin.sodium;

import com.y271727uy.lumenized.core.sodium.ChunkBuilderTaskAccess;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkBuilderTask.class)
public abstract class ChunkBuilderTaskMixin implements ChunkBuilderTaskAccess {
    @Shadow
    @Final
    protected RenderSection render;

    @Override
    public RenderSection lumenized$getRenderSection() {
        return this.render;
    }
}
