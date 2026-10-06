package com.y271727uy.lumenized.client.postprocessing;

import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * @author KilaBash
 * @date 2022/5/11
 * @implNote PostMultiBufferSource, for proper rendering pipeline
 */
public class PostMultiBufferSource extends MultiBufferSource.BufferSource {
    public final static PostMultiBufferSource BUFFER_SOURCE = new PostMultiBufferSource();

    private static ByteBufferBuilder newBuffer() {
        return new ByteBufferBuilder(256);
    }

    protected PostMultiBufferSource() {
        super(newBuffer(), new Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder>());
    }

    @Override
    public void endBatch() {
        super.endBatch();
    }
}
