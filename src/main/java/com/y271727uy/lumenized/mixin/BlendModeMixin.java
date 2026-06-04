package com.y271727uy.lumenized.mixin;

import com.mojang.blaze3d.shaders.BlendMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author KilaBash
 * @date 2022/7/10
 * @implNote BlendModeMixin
 */
@Mixin(BlendMode.class)
public interface BlendModeMixin {
    @Accessor
    void setLastApplied(BlendMode mode);
    @Accessor
    BlendMode getLastApplied();
}
