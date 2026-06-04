package com.y271727uy.lumenized.client.rendertarget;

import com.y271727uy.lumenized.comp.iris.IrisHandle;
import com.y271727uy.lumenized.core.MixinPluginShared;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

/**
 * only used for "intarget": "lumenized:composite_source",
 */
public class SelectRenderTarget extends RenderTarget {

    public SelectRenderTarget() {
        super(false);
    }

    @Override
    public int getColorTextureId() {
        return MixinPluginShared.IS_IRIS_LOAD ? IrisHandle.INSTANCE.getCompositeId()
                : Minecraft.getInstance().getMainRenderTarget().getColorTextureId();
    }
}
